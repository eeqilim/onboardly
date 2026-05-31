import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, Empty, List, Space, Tag, Upload } from "antd";
import {
  DownloadOutlined,
  EyeOutlined,
  FileTextOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import {
  downloadDocument,
  getDocumentPreviewUrl,
  getMyOnboardingDocuments,
  getOnboardingTemplates,
  getTemplatePreviewUrl,
  uploadOnboardingDocument,
} from "../../services/onboardingService";
import { getMyOnboardingApplication } from "../../services/employeeService";
import { useAuth } from "../../auth/AuthContext";
import { DocumentPreviewModal, PageHeader } from "../../components/common";
import "./Onboarding.css";

function getSourceDocumentId(document) {
  return document.sourceDocumentId || document.personalDocumentId || null;
}

function isDocumentUploaded(document) {
  return Boolean(
    document.uploaded ||
    getSourceDocumentId(document) ||
    document.uploadedFileUrl ||
    document.s3Url,
  );
}

function extractPreviewUrl(response) {
  const data = response?.data;
  if (typeof data === "string") return data;
  return (
    data?.url ||
    data?.previewUrl ||
    data?.s3Url ||
    data?.downloadUrl ||
    data?.data ||
    ""
  );
}

function triggerBlobDownload(blob, fileName = "document") {
  const blobUrl = URL.createObjectURL(blob);
  const link = window.document.createElement("a");
  link.href = blobUrl;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(blobUrl);
}

// Backend wraps every payload in DataResponse { message, data }. Peel that.
function unwrapDataResponse(response) {
  const payload = response?.data;
  if (Array.isArray(payload)) return payload;
  if (payload && Array.isArray(payload.data)) return payload.data;
  return [];
}

function mergeTemplatesWithUploads(templates, uploadedDocs) {
  const uploadsByType = new Map();
  for (const uploaded of uploadedDocs) {
    if (!uploaded?.type) continue;
    const existing = uploadsByType.get(uploaded.type);
    // If multiple uploads exist for the same type, prefer the one that
    // actually carries a sourceDocumentId so preview/download will work.
    if (
      !existing ||
      (!existing.sourceDocumentId && uploaded.sourceDocumentId)
    ) {
      uploadsByType.set(uploaded.type, uploaded);
    }
  }

  return templates.map((template) => {
    const uploaded = uploadsByType.get(template.type) || null;
    return {
      documentId: template.type,
      type: template.type,
      name: template.title,
      description: template.description,
      required: template.isRequired === 1,
      templateUrl: template.path,
      sourceDocumentId: uploaded?.sourceDocumentId || null,
      digitalDocumentId: uploaded?.id || null,
    };
  });
}

// Set true to bypass the backend and drive the page off `mockDocuments` for UI work.
// templateUrls below are stale (real flow fetches presigned S3 URLs via getTemplatePreviewUrl)
const USE_MOCK_DATA = false;

const mockDocuments = [
  {
    documentId: "W4",
    type: "W4",
    name: "W-4 Form",
    required: true,
    templateUrl: "/templates/onboarding/fw4.pdf",
    sourceDocumentId: null,
  },
  {
    documentId: "I9",
    type: "I9",
    name: "I-9 Form",
    required: true,
    templateUrl: "/templates/onboarding/i9.pdf",
    sourceDocumentId: null,
  },
  {
    documentId: "COMPANY_POLICY",
    type: "COMPANY_POLICY",
    name: "Company Policy Agreement",
    required: true,
    templateUrl: "/templates/onboarding/company-policy.pdf",
    sourceDocumentId: null,
  },
  {
    documentId: "DIRECT_DEPOSIT",
    type: "DIRECT_DEPOSIT",
    name: "Direct Deposit Form",
    required: false,
    templateUrl: "/templates/onboarding/direct-deposit.pdf",
    sourceDocumentId: null,
  },
];

export default function OnboardingDocuments() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [documents, setDocuments] = useState([]);
  const [selectedFiles, setSelectedFiles] = useState({});
  const [preview, setPreview] = useState({
    open: false,
    url: null,
    name: null,
  });

  const [status, setStatus] = useState({ message: "", type: "info" });
  const [loading, setLoading] = useState(true);
  const [uploadingDocumentId, setUploadingDocumentId] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  // applicationWorkflowId can come from login (stored on user) or be
  // re-fetched here as a fallback if the user reloaded mid-onboarding.
  const [workflowId, setWorkflowId] = useState(
    user?.applicationWorkflowId ?? null,
  );

  useEffect(() => {
    let active = true;

    async function loadDocuments() {
      setLoading(true);
      setStatus({ message: "", type: "info" });

      if (USE_MOCK_DATA) {
        if (!active) return;
        setDocuments(mockDocuments);
        setLoading(false);
        return;
      }

      try {
        const [templatesResponse, uploadedResponse, applicationResponse] =
          await Promise.all([
            getOnboardingTemplates(),
            // No uploaded docs yet (e.g. fresh application) is expected —
            // swallow and merge with an empty list so the checklist still renders.
            getMyOnboardingDocuments().catch(() => ({ data: { data: [] } })),
            // Refresh applicationWorkflowId in case it wasn't captured at login.
            workflowId == null
              ? getMyOnboardingApplication().catch(() => null)
              : Promise.resolve(null),
          ]);
        if (!active) return;

        const templates = unwrapDataResponse(templatesResponse);
        const uploadedDocs = unwrapDataResponse(uploadedResponse);
        setDocuments(mergeTemplatesWithUploads(templates, uploadedDocs));

        const fetchedWorkflowId =
          applicationResponse?.data?.applicationWorkflowId ?? null;
        if (fetchedWorkflowId != null && workflowId == null) {
          setWorkflowId(fetchedWorkflowId);
        }
      } catch {
        if (active) {
          setStatus({
            message: "Failed to load onboarding documents.",
            type: "error",
          });
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    loadDocuments();

    return () => {
      active = false;
    };
  }, [reloadKey]);

  function handleFileChange(documentId, file) {
    setSelectedFiles((current) => ({
      ...current,
      [documentId]: file,
    }));
  }

  async function uploadDocument(document) {
    const documentId = document.documentId || document.id;
    const file = selectedFiles[documentId];

    if (!file) {
      setStatus({
        message: "Please select a file before uploading.",
        type: "warning",
      });
      return;
    }

    if (!document.type) {
      setStatus({
        message: "Document is missing a type — cannot upload.",
        type: "error",
      });
      return;
    }

    if (workflowId == null) {
      setStatus({
        message:
          "Could not resolve onboarding application — please reload the page.",
        type: "error",
      });
      return;
    }

    if (USE_MOCK_DATA) {
      setDocuments((current) =>
        current.map((item) =>
          (item.documentId || item.id) === documentId
            ? {
                ...item,
                uploaded: true,
                uploadedFileName: file.name,
                uploadedFileUrl: "",
              }
            : item,
        ),
      );

      setSelectedFiles((current) => ({
        ...current,
        [documentId]: null,
      }));

      setStatus({ message: "Document uploaded.", type: "success" });
      return;
    }

    try {
      setUploadingDocumentId(documentId);
      setStatus({ message: "", type: "info" });

      await uploadOnboardingDocument(file, {
        applicationId: workflowId,
        applicationType: "ONBOARDING",
        title: document.name || document.type,
        documentType: document.type,
      });

      setStatus({ message: "Document uploaded.", type: "success" });
      setReloadKey((current) => current + 1);
    } catch {
      setStatus({ message: "Failed to upload document.", type: "error" });
    } finally {
      setUploadingDocumentId(null);
    }
  }

  async function previewUploadedDocument(document) {
    const sourceDocumentId = getSourceDocumentId(document);

    if (!sourceDocumentId) {
      setStatus({
        message: "No uploaded copy available to preview.",
        type: "warning",
      });
      return;
    }

    try {
      const response = await getDocumentPreviewUrl(sourceDocumentId);
      const url = extractPreviewUrl(response);

      if (!url) {
        setStatus({ message: "No preview URL available.", type: "error" });
        return;
      }

      setPreview({ open: true, url, name: document.name });
    } catch {
      setStatus({
        message: "Failed to load uploaded document preview.",
        type: "error",
      });
    }
  }

  async function downloadUploadedDocument(document) {
    const sourceDocumentId = getSourceDocumentId(document);

    if (!sourceDocumentId) {
      setStatus({
        message: "No uploaded copy available to download.",
        type: "warning",
      });
      return;
    }

    try {
      const response = await downloadDocument(sourceDocumentId);
      triggerBlobDownload(response.data, document.name || "document");
    } catch {
      setStatus({
        message: "Failed to download uploaded document.",
        type: "error",
      });
    }
  }

  // template.path is an S3 key — must be exchanged for a presigned URL
  // before it can be rendered or downloaded.
  async function resolveTemplateUrl(document) {
    const key = document.templateUrl || document.url;
    if (!key) return null;
    const response = await getTemplatePreviewUrl(key);
    return extractPreviewUrl(response) || null;
  }

  async function previewTemplate(document) {
    if (!(document.templateUrl || document.url)) {
      setStatus({ message: "No template available.", type: "warning" });
      return;
    }
    try {
      const url = await resolveTemplateUrl(document);
      if (!url) {
        setStatus({ message: "No preview URL available.", type: "error" });
        return;
      }
      setPreview({ open: true, url, name: document.name });
    } catch {
      setStatus({
        message: "Failed to load template preview.",
        type: "error",
      });
    }
  }

  async function downloadTemplate(document) {
    if (!(document.templateUrl || document.url)) {
      setStatus({ message: "No template available.", type: "warning" });
      return;
    }
    try {
      const url = await resolveTemplateUrl(document);
      if (!url) {
        setStatus({ message: "No download URL available.", type: "error" });
        return;
      }
      const fileResponse = await fetch(url);
      const blob = await fileResponse.blob();
      triggerBlobDownload(blob, document.name || "template");
    } catch {
      setStatus({
        message: "Failed to download template.",
        type: "error",
      });
    }
  }

  function validateRequiredDocuments() {
    const missingRequiredDocuments = documents.filter(
      (document) => Boolean(document.required) && !isDocumentUploaded(document),
    );

    if (missingRequiredDocuments.length > 0) {
      return `Please upload all required documents: ${missingRequiredDocuments
        .map((document) => document.name)
        .join(", ")}`;
    }

    return "";
  }

  function submitApplication() {
    const error = validateRequiredDocuments();

    if (error) {
      setStatus({ message: error, type: "warning" });
      return;
    }

    // The actual onboarding submit (POST /employee/onboarding/submit with
    // the form payload) already ran on the prior Onboarding form page and
    // set status to PENDING. There's no separate "finalize docs" backend
    // endpoint, so this button just gates on required uploads client-side
    // and navigates to the waiting-review screen.
    navigate("/employee/waiting-review");
  }

  if (loading) {
    return <div className="onboarding-page">Loading...</div>;
  }

  return (
    <div className="onboarding-page">
      <PageHeader title="Onboarding Documents" status={status} />

      <Card
        className="onboarding-card"
        title={
          <span className="onboarding-card-title">
            <FileTextOutlined />
            <span>Required Documents</span>
          </span>
        }
      >
        {documents.length === 0 ? (
          <Empty description="No onboarding documents found" />
        ) : (
          <List
            className="onboarding-document-list"
            dataSource={documents}
            renderItem={(document) => {
              const documentId = document.documentId || document.id;
              const templateUrl = document.templateUrl || document.url;
              const uploaded = isDocumentUploaded(document);

              return (
                <List.Item key={documentId}>
                  <Card className="onboarding-document-card" size="small">
                    <div className="onboarding-document-row">
                      <div className="onboarding-document-main">
                        <FileTextOutlined />
                        <Button
                          type="link"
                          className="onboarding-document-link"
                          onClick={() => previewTemplate(document)}
                          disabled={!templateUrl}
                        >
                          {document.name || "Document"}
                        </Button>
                        {document.required && <Tag color="red">Required</Tag>}
                        {uploaded && <Tag color="green">Uploaded</Tag>}
                      </div>

                      <Space wrap>
                        <Button
                          icon={<EyeOutlined />}
                          onClick={() => previewTemplate(document)}
                          disabled={!templateUrl}
                        >
                          Preview Template
                        </Button>

                        {templateUrl && (
                          <Button
                            icon={<DownloadOutlined />}
                            onClick={() => downloadTemplate(document)}
                          >
                            Download Template
                          </Button>
                        )}

                        <Upload
                          accept=".pdf,.jpg,.jpeg,.png"
                          beforeUpload={() => false}
                          maxCount={1}
                          onChange={(info) =>
                            handleFileChange(
                              documentId,
                              info.fileList[0]?.originFileObj ||
                                info.fileList[0] ||
                                null,
                            )
                          }
                        >
                          <Button icon={<UploadOutlined />}>Select File</Button>
                        </Upload>

                        <Button
                          type="primary"
                          icon={<UploadOutlined />}
                          onClick={() => uploadDocument(document)}
                          loading={uploadingDocumentId === documentId}
                        >
                          Upload
                        </Button>

                        {uploaded && (
                          <>
                            <Button
                              icon={<EyeOutlined />}
                              onClick={() => previewUploadedDocument(document)}
                            >
                              View Uploaded
                            </Button>
                            <Button
                              icon={<DownloadOutlined />}
                              onClick={() => downloadUploadedDocument(document)}
                            >
                              Download Uploaded
                            </Button>
                          </>
                        )}
                      </Space>
                    </div>
                  </Card>
                </List.Item>
              );
            }}
          />
        )}
      </Card>

      <div className="onboarding-actions">
        <Button type="primary" onClick={submitApplication}>
          Submit
        </Button>
      </div>

      <DocumentPreviewModal
        open={preview.open}
        title={preview.name || "Document Preview"}
        url={preview.url}
        onClose={() =>
          setPreview({
            open: false,
            url: null,
            name: null,
          })
        }
      />
    </div>
  );
}
