import { useState, useEffect } from "react";
import { useLocation, useParams } from "react-router-dom";
import {
  Button,
  Card,
  Divider,
  Input,
  List,
  Space,
  Tag,
  Typography,
  message,
} from "antd";
import {
  EyeOutlined,
  FilePdfOutlined,
  FileImageOutlined,
  FileOutlined,
  MessageOutlined,
} from "@ant-design/icons";
import { DocumentPreviewModal } from "../../components/common";
import { getEmployeeDocuments, addDocumentComment } from "../../services/employeeService";
import { getDocumentPreviewUrl } from "../../services/onboardingService";

const { Title, Text } = Typography;
const { TextArea } = Input;

export default function ReceivedDocuments() {
  const { id } = useParams();
  const location = useLocation();

  const allowComments = location.state?.source === "hiring";

  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);

  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [previewTitle, setPreviewTitle] = useState("");

  const [commentInputs, setCommentInputs] = useState({});

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getEmployeeDocuments(id)
      .then((res) => setDocuments(res.data || []))
      .catch(() => message.error("Could not load documents."))
      .finally(() => setLoading(false));
  }, [id]);

  const handleOpenPreview = async (doc) => {
    try {
      const res = await getDocumentPreviewUrl(doc.id);
      setPreviewUrl(res.data?.url || null);
      setPreviewTitle(doc.title || "Document Preview");
      setPreviewOpen(true);
    } catch {
      message.error("Could not load document preview.");
    }
  };

  const handleClosePreview = () => {
    setPreviewOpen(false);
    setPreviewUrl(null);
    setPreviewTitle("");
  };

  const handleCommentChange = (docId, value) => {
    setCommentInputs((prev) => ({ ...prev, [docId]: value }));
  };

  const handleAddComment = async (docId) => {
    const text = commentInputs[docId];
    if (!text || !text.trim()) return;

    try {
      await addDocumentComment(docId, text.trim());
      setDocuments((prev) =>
        prev.map((doc) =>
          doc.id === docId ? { ...doc, comment: text.trim() } : doc
        )
      );
      setCommentInputs((prev) => ({ ...prev, [docId]: "" }));
      message.success("Comment saved.");
    } catch {
      message.error("Failed to save comment.");
    }
  };

  const getFileIcon = (s3Key) => {
    if (!s3Key) return <FileOutlined style={{ fontSize: "32px", color: "#8c8c8c" }} />;
    const ext = s3Key.split(".").pop()?.toLowerCase();
    if (ext === "pdf") return <FilePdfOutlined style={{ fontSize: "32px", color: "#cf1322" }} />;
    if (["jpg", "jpeg", "png", "gif", "webp"].includes(ext))
      return <FileImageOutlined style={{ fontSize: "32px", color: "#1677ff" }} />;
    return <FileOutlined style={{ fontSize: "32px", color: "#8c8c8c" }} />;
  };

  return (
    <div style={{ maxWidth: 900, margin: "0 auto", padding: "24px" }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>Received Documents</Title>
        <Text type="secondary">
          Employee ID: {id} | View mode:{" "}
          {allowComments ? "Hiring Management (Review & Comment)" : "Visa Status (View Only)"}
        </Text>
      </div>

      <List
        loading={loading}
        itemLayout="vertical"
        dataSource={documents}
        locale={{ emptyText: "No documents found." }}
        renderItem={(doc) => (
          <Card style={{ marginBottom: 24, boxShadow: "0 1px 2px rgba(0,0,0,0.05)" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <Space size="large">
                {getFileIcon(doc.s3Key)}
                <div>
                  <Text strong style={{ fontSize: "16px" }}>{doc.title || "Document"}</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: "12px" }}>
                    Type: {doc.documentType}
                  </Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: "12px" }}>
                    Uploaded:{" "}
                    {doc.createdAt ? new Date(doc.createdAt).toLocaleDateString() : "N/A"}
                  </Text>
                </div>
                <Tag color="blue">{doc.applicationType}</Tag>
              </Space>

              <Button
                type="primary"
                icon={<EyeOutlined />}
                onClick={() => handleOpenPreview(doc)}
              >
                Preview
              </Button>
            </div>

            {allowComments && (
              <>
                <Divider style={{ margin: "16px 0" }} />
                <div>
                  <Space style={{ marginBottom: 12 }}>
                    <MessageOutlined />
                    <Text strong>HR Comment</Text>
                  </Space>

                  {doc.comment && (
                    <Text
                      style={{
                        display: "block",
                        marginBottom: 12,
                        padding: "8px",
                        background: "#fafafa",
                        borderRadius: 4,
                      }}
                    >
                      {doc.comment}
                    </Text>
                  )}

                  <div style={{ display: "flex", gap: "8px", alignItems: "flex-start" }}>
                    <TextArea
                      rows={2}
                      placeholder="Add or update HR comment on this document..."
                      value={commentInputs[doc.id] || ""}
                      onChange={(e) => handleCommentChange(doc.id, e.target.value)}
                      style={{ flex: 1 }}
                    />
                    <Button
                      onClick={() => handleAddComment(doc.id)}
                      disabled={!commentInputs[doc.id]?.trim()}
                    >
                      Save Note
                    </Button>
                  </div>
                </div>
              </>
            )}
          </Card>
        )}
      />

      <DocumentPreviewModal
        open={previewOpen}
        title={previewTitle}
        url={previewUrl}
        onClose={handleClosePreview}
      />
    </div>
  );
}
