import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Card,
  List,
  Space,
  Steps,
  Tag,
  Typography,
  Upload,
} from "antd";
import {
  DownloadOutlined,
  FileTextOutlined,
  InboxOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import {
  getActiveVisaStatus,
  getStemOptProgress,
  getVisaDocuments,
  uploadVisaDocument,
} from "../../services/visaService";
import { uploadEmployeeDocument } from "../../services/employeeService";
import {
  downloadDocument,
  getDocumentPreviewUrl,
  getTemplatePreviewUrl,
} from "../../services/onboardingService";
import { DocumentPreviewModal } from "../../components/common";
import "./VisaStatus.css";

const { Text, Title } = Typography;

const USE_MOCK_DATA = false;
const OPT_STEPS = [
  {
    key: "I983",
    title: "I-983",
    description:
      "Download the I-983, complete it with your DSO, and submit it to your school.",
  },
  {
    key: "I20",
    backendStep: "I_20",
    title: "I-20",
    description:
      "Upload the new I-20 you receive from your school after submitting the I-983.",
  },
  {
    key: "STEM_OPT_RECEIPT",
    backendStep: "OPT_RECEIPT",
    title: "OPT STEM Receipt",
    description: "Upload your OPT STEM receipt after applying.",
  },
  {
    key: "STEM_OPT_EAD",
    backendStep: "OPT_EAD",
    title: "OPT STEM EAD",
    description: "Upload your OPT STEM EAD and wait for HR approval.",
  },
];

const FRONTEND_TO_BACKEND_STEP = Object.fromEntries(
  OPT_STEPS.filter((step) => step.backendStep).map((step) => [
    step.key,
    step.backendStep,
  ]),
);

const mockVisaInfo = {
  applicationId: 1,
  currentStep: "ONBOARDING_SUBMITTED",
  nextStep: "DOWNLOAD_I983",
  status: "Open",
  visaType: "F1_OPT_STEM",
  // visaType: "F1_OPT",
  // visaType: "H1B",
  // visaType: "L2",
  // visaType: "H4",
  // visaType: "OTHER",
  // visaTypeOther: "TN Visa",
  activeFlag: true,
  startDate: "2025-01-01",
  endDate: "2026-01-01",
};

const mockDocuments = [];

function normalizeVisaInfo(data = {}) {
  if (data.status === "Completed") {
    return { ...data, nextStep: "COMPLETED" };
  }

  // Backend StemOptProgressResponse uses enum values I_983 | I_20 | OPT_RECEIPT | OPT_EAD,
  // with nextStep === null once OPT_EAD has been uploaded.
  switch (data.nextStep) {
    case "I_983":
    case null:
    case undefined:
      // Fall through to currentStep below to disambiguate "not started" vs "all done".
      break;
    case "I_20":
      return { ...data, nextStep: "UPLOAD_I20" };
    case "OPT_RECEIPT":
      return { ...data, nextStep: "UPLOAD_STEM_OPT_RECEIPT" };
    case "OPT_EAD":
      return { ...data, nextStep: "UPLOAD_STEM_OPT_EAD" };
    default:
      return { ...data, nextStep: data.nextStep || "DOWNLOAD_I983" };
  }

  if (data.currentStep === "OPT_EAD") {
    return { ...data, nextStep: "WAITING_HR_APPROVAL" };
  }
  return { ...data, nextStep: "DOWNLOAD_I983" };
}

function normalizeWorkAuthorization(value = "") {
  return String(value)
    .trim()
    .toUpperCase()
    .replace(/[\s-/]+/g, "_");
}

function getWorkAuthorizationType(visaInfo) {
  return visaInfo?.visaType || "";
}

function getWorkAuthorizationOtherType(visaInfo) {
  return visaInfo?.visaTypeOther || "";
}

function getVisaStatusLabel(visaInfo) {
  if (typeof visaInfo?.activeFlag === "boolean") {
    return visaInfo.activeFlag ? "Active" : "Inactive";
  }
  return "";
}

function getWorkAuthStartDate(visaInfo) {
  return visaInfo?.startDate;
}

function getWorkAuthEndDate(visaInfo) {
  return visaInfo?.endDate;
}

function isStemOptWorkAuthorization(visaInfo) {
  const workAuthorization = normalizeWorkAuthorization(
    getWorkAuthorizationType(visaInfo),
  );
  return workAuthorization === "F1_OPT_STEM";
}

function getCurrentStep(nextStep) {
  switch (nextStep) {
    case "DOWNLOAD_I983":
      return 0;
    case "UPLOAD_I20":
      return 1;
    case "UPLOAD_STEM_OPT_RECEIPT":
      return 2;
    case "UPLOAD_STEM_OPT_EAD":
    case "WAITING_HR_APPROVAL":
      return 3;
    case "COMPLETED":
      return 4;
    default:
      return 0;
  }
}

function getDocumentId(document) {
  return document.id || document.documentId;
}

function getDocumentUrl(document) {
  return (
    document.url ||
    document.s3Url ||
    document.previewUrl ||
    document.downloadUrl
  );
}

function getPreviewUrl(response) {
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

function downloadBlob(blob, fileName = "document") {
  const blobUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = blobUrl;
  link.download = fileName;
  link.click();

  URL.revokeObjectURL(blobUrl);
}

function getNextStepMessage(nextStep) {
  switch (nextStep) {
    case "DOWNLOAD_I983":
      return "Please download and complete the I-983, then upload your new I-20.";
    case "UPLOAD_I20":
      return "Please upload your new I-20.";
    case "UPLOAD_STEM_OPT_RECEIPT":
      return "Please upload your OPT STEM Receipt.";
    case "UPLOAD_STEM_OPT_EAD":
      return "Please upload your OPT STEM EAD.";
    case "WAITING_HR_APPROVAL":
      return "OPT STEM EAD uploaded. Please wait for HR's response.";
    case "COMPLETED":
      return "Your OPT STEM application is complete.";
    default:
      return "No next step available.";
  }
}

function WorkAuthorizationField({ label, value }) {
  return (
    <div className="visa-field">
      <Text type="secondary">{label}</Text>
      <Text strong>{value || "-"}</Text>
    </div>
  );
}

function UploadButton({ label, documentType, uploading, onUpload }) {
  return (
    <Upload
      accept=".pdf,.jpg,.jpeg,.png"
      beforeUpload={(file) => {
        onUpload(documentType, file);
        return false;
      }}
      maxCount={1}
      showUploadList={false}
    >
      <Button
        type="primary"
        icon={<UploadOutlined />}
        loading={uploading}
        disabled={uploading}
      >
        {label}
      </Button>
    </Upload>
  );
}

export default function VisaStatus() {
  const [visaInfo, setVisaInfo] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [preview, setPreview] = useState({
    open: false,
    url: null,
    name: null,
  });
  const [uploading, setUploading] = useState(false);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadVisaData() {
      setLoading(true);
      setStatus("");

      if (USE_MOCK_DATA) {
        setVisaInfo(normalizeVisaInfo(mockVisaInfo));
        setDocuments(mockDocuments);
        setLoading(false);
        return;
      }

      try {
        const [activeResponse, progressResponse, documentResponse] =
          await Promise.all([
            getActiveVisaStatus().catch(() => ({ data: {} })),
            getStemOptProgress(),
            getVisaDocuments(),
          ]);

        if (!active) return;

        setVisaInfo(
          normalizeVisaInfo({
            ...(activeResponse.data || {}),
            ...(progressResponse.data || {}),
          }),
        );
        setDocuments(documentResponse.data || []);
      } catch {
        if (active) setStatus("Failed to load visa status.");
      } finally {
        if (active) setLoading(false);
      }
    }

    loadVisaData();

    return () => {
      active = false;
    };
  }, []);

  async function refreshVisaData() {
    if (USE_MOCK_DATA) return;

    const [activeResponse, progressResponse, documentResponse] =
      await Promise.all([
        getActiveVisaStatus().catch(() => ({ data: {} })),
        getStemOptProgress(),
        getVisaDocuments(),
      ]);

    setVisaInfo(
      normalizeVisaInfo({
        ...(activeResponse.data || {}),
        ...(progressResponse.data || {}),
      }),
    );
    setDocuments(documentResponse.data || []);
  }

  async function handleUpload(documentType, file) {
    if (!file) return;

    if (USE_MOCK_DATA) {
      const fileUrl = URL.createObjectURL(file);
      const uploadedDocument = {
        id: `${documentType}-${Date.now()}`,
        name: file.name,
        type: documentType,
        createdDate: new Date().toISOString(),
        url: fileUrl,
        mimeType: file.type || "application/octet-stream",
      };

      setDocuments((current) => [uploadedDocument, ...current]);

      const stepIndex = OPT_STEPS.findIndex((s) => s.key === documentType);
      if (stepIndex === -1) {
        setStatus("Unknown step type.");
        return;
      }
      const uploaded = OPT_STEPS[stepIndex];
      const next = OPT_STEPS[stepIndex + 1];

      setVisaInfo((current) =>
        normalizeVisaInfo({
          ...current,
          currentStep: uploaded.backendStep,
          nextStep: next ? next.backendStep : null,
        }),
      );

      setStatus(
        next
          ? `${uploaded.title} uploaded. Next: ${next.title}.`
          : `${uploaded.title} uploaded. Awaiting HR approval.`,
      );

      return;
    }

    try {
      setUploading(true);
      setStatus("");

      const backendStep = FRONTEND_TO_BACKEND_STEP[documentType];
      if (backendStep) {
        if (documentType === "I20" && visaInfo?.nextStep === "DOWNLOAD_I983") {
          const presignResponse = await getTemplatePreviewUrl(
            "templates/opt-stem/i983.pdf",
          );
          const presignedUrl =
            presignResponse?.data?.url || presignResponse?.data;
          const templateResponse = await fetch(presignedUrl);
          const templateBlob = await templateResponse.blob();
          const templateFile = new File([templateBlob], "i983.pdf", {
            type: "application/pdf",
          });
          const i983FormData = new FormData();
          i983FormData.append("file", templateFile);
          await uploadVisaDocument(i983FormData, "I_983");
        }

        const formData = new FormData();
        formData.append("file", file);
        await uploadVisaDocument(formData, backendStep);
      } else {
        await uploadEmployeeDocument(file, {
          title: "Work Authorization",
          documentType: "WORK_AUTH",
        });
      }

      setStatus("Document uploaded successfully.");
      await refreshVisaData();
    } catch {
      setStatus("Upload failed.");
    } finally {
      setUploading(false);
    }
  }

  async function downloadI983() {
    try {
      const presignResponse = await getTemplatePreviewUrl(
        "templates/opt-stem/i983.pdf",
      );
      const presignedUrl = presignResponse?.data?.url || presignResponse?.data;
      const fileResponse = await fetch(presignedUrl);
      const blob = await fileResponse.blob();
      const blobUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = blobUrl;
      link.download = "i983.pdf";
      link.click();
      URL.revokeObjectURL(blobUrl);
      setStatus(
        "I-983 template download started. Fill it out, then upload the signed copy.",
      );
    } catch {
      setStatus("Failed to download I-983 template.");
    }
  }

  async function handlePreviewDocument(document) {
    const directUrl = getDocumentUrl(document);

    if (directUrl) {
      setPreview({
        open: true,
        url: directUrl,
        name: document.title,
        mimeType: document.mimeType,
      });
      return;
    }

    const documentId = getDocumentId(document);

    if (!documentId) {
      setStatus("No preview URL available.");
      return;
    }

    try {
      const response = await getDocumentPreviewUrl(documentId);
      const previewUrl = getPreviewUrl(response);

      if (!previewUrl) {
        setStatus("No preview URL available.");
        return;
      }

      setPreview({
        open: true,
        url: previewUrl,
        name: document.title,
        mimeType: document.mimeType,
      });
    } catch {
      setStatus("Failed to load document preview.");
    }
  }

  async function handleDownloadDocument(document) {
    const directUrl = getDocumentUrl(document);

    if (directUrl) {
      return;
    }

    const documentId = getDocumentId(document);

    if (!documentId) {
      setStatus("No document download available.");
      return;
    }

    try {
      const response = await downloadDocument(documentId);
      downloadBlob(response.data, document.title || "document");
    } catch {
      setStatus("Failed to download document.");
    }
  }

  if (loading) return <div className="visa-page">Loading...</div>;

  if (!visaInfo) {
    return <div className="visa-page">{status || "No visa status found."}</div>;
  }

  const currentStep = getCurrentStep(visaInfo.nextStep);
  const workAuthorizationType = getWorkAuthorizationType(visaInfo);
  const workAuthorizationOtherType = getWorkAuthorizationOtherType(visaInfo);
  const showStemOptWorkflow = isStemOptWorkAuthorization(visaInfo);

  const sortedDocuments = [...documents].sort(
    (a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0),
  );

  return (
    <div className="visa-page">
      <div className="visa-title-row">
        <Title level={2}>Visa Status Management</Title>
        {status && <Alert message={status} type="info" showIcon />}
      </div>

      <div className="visa-layout">
        {/* Top-left: current status summary */}
        <Card
          className="visa-card visa-status-card"
          title="Current Application Status"
        >
          {showStemOptWorkflow ? (
            <div className="visa-field-grid">
              <WorkAuthorizationField
                label="Work Authorization"
                value={workAuthorizationType}
              />
              <WorkAuthorizationField
                label="Status"
                value={getVisaStatusLabel(visaInfo)}
              />
              <WorkAuthorizationField
                label="Start Date"
                value={getWorkAuthStartDate(visaInfo)}
              />
              <WorkAuthorizationField
                label="End Date"
                value={getWorkAuthEndDate(visaInfo)}
              />
              <WorkAuthorizationField
                label="Next Step"
                value={getNextStepMessage(visaInfo.nextStep)}
              />
            </div>
          ) : (
            <div className="visa-field-grid">
              <WorkAuthorizationField
                label="Work Authorization"
                value={workAuthorizationType}
              />
              {normalizeWorkAuthorization(workAuthorizationType) ===
                "OTHER" && (
                <WorkAuthorizationField
                  label="Specified Type"
                  value={workAuthorizationOtherType}
                />
              )}
              <WorkAuthorizationField
                label="Status"
                value={getVisaStatusLabel(visaInfo)}
              />
              <WorkAuthorizationField
                label="Start Date"
                value={getWorkAuthStartDate(visaInfo)}
              />
              <WorkAuthorizationField
                label="End Date"
                value={getWorkAuthEndDate(visaInfo)}
              />
            </div>
          )}
        </Card>

        {showStemOptWorkflow ? (
          <Card
            className="visa-card visa-steps-card"
            title="STEM OPT Application Steps"
          >
            <Steps
              direction="vertical"
              current={currentStep}
              items={OPT_STEPS.map((step) => ({
                title: step.title,
                description: step.description,
              }))}
            />

            <div className="visa-actions-panel">
              <Space direction="vertical" size="middle">
                <Space className="visa-primary-actions" wrap size={[12, 12]}>
                  {(visaInfo.nextStep === "DOWNLOAD_I983" ||
                    visaInfo.nextStep === "UPLOAD_I20") && (
                    <>
                      <Button
                        icon={<DownloadOutlined />}
                        onClick={downloadI983}
                      >
                        Download I-983 Template
                      </Button>
                      <UploadButton
                        label="Upload New I-20"
                        documentType="I20"
                        uploading={uploading}
                        onUpload={handleUpload}
                      />
                    </>
                  )}
                  {visaInfo.nextStep === "UPLOAD_STEM_OPT_RECEIPT" && (
                    <UploadButton
                      label="Upload OPT STEM Receipt"
                      documentType="STEM_OPT_RECEIPT"
                      uploading={uploading}
                      onUpload={handleUpload}
                    />
                  )}
                  {visaInfo.nextStep === "UPLOAD_STEM_OPT_EAD" && (
                    <UploadButton
                      label="Upload OPT STEM EAD"
                      documentType="STEM_OPT_EAD"
                      uploading={uploading}
                      onUpload={handleUpload}
                    />
                  )}
                </Space>

                {visaInfo.nextStep === "WAITING_HR_APPROVAL" && (
                  <Alert
                    message="EAD uploaded. Awaiting HR approval."
                    type="success"
                    showIcon
                  />
                )}
                {visaInfo.nextStep === "COMPLETED" && (
                  <Alert
                    message="Your STEM OPT application is complete."
                    type="success"
                    showIcon
                  />
                )}
              </Space>
            </div>
          </Card>
        ) : (
          <Card
            className="visa-card visa-steps-card"
            title="Work Authorization Documents"
          >
            <Space direction="vertical" size="middle">
              <Text>
                Upload the latest document for your current work authorization.
              </Text>
              <UploadButton
                label=" Upload"
                documentType="WORK_AUTHORIZATION"
                uploading={uploading}
                onUpload={handleUpload}
              />
            </Space>
          </Card>
        )}

        {/* Bottom-left: uploaded documents, sits directly under status card */}
        <Card
          className="visa-card visa-documents-card"
          title="Uploaded Documents"
        >
          {sortedDocuments.length === 0 ? (
            <div className="visa-empty">
              <InboxOutlined />
              <Text type="secondary">No documents uploaded</Text>
            </div>
          ) : (
            <List
              itemLayout="horizontal"
              dataSource={sortedDocuments}
              renderItem={(document) => {
                const url = getDocumentUrl(document);
                const documentId = getDocumentId(document);

                return (
                  <List.Item
                    key={
                      documentId || `${document.title}-${document.createdAt}`
                    }
                    actions={[
                      url ? (
                        <a
                          key="download"
                          href={url}
                          download={document.title || true}
                        >
                          Download
                        </a>
                      ) : (
                        <Button
                          key="download"
                          type="link"
                          disabled={!documentId}
                          onClick={() => handleDownloadDocument(document)}
                        >
                          Download
                        </Button>
                      ),
                    ]}
                  >
                    <List.Item.Meta
                      avatar={
                        <FileTextOutlined className="visa-document-icon" />
                      }
                      title={
                        <Button
                          type="link"
                          className="visa-document-button"
                          onClick={() => handlePreviewDocument(document)}
                        >
                          {document.title || "Document"}
                        </Button>
                      }
                      description={
                        <Space size="small" wrap>
                          <Tag>{document.documentType || "-"}</Tag>
                          <Text type="secondary">
                            {document.createdAt
                              ? new Date(
                                  document.createdAt,
                                ).toLocaleDateString()
                              : "-"}
                          </Text>
                        </Space>
                      }
                    />
                  </List.Item>
                );
              }}
            />
          )}
        </Card>
      </div>

      <DocumentPreviewModal
        open={preview.open}
        title={preview.name || "Document"}
        url={preview.url}
        onClose={() => setPreview({ open: false, url: null, name: null })}
      />
    </div>
  );
}
