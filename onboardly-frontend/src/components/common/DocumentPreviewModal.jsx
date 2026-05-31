import { Button, Modal, Typography } from "antd";
import "./common.css";

const { Text } = Typography;

export default function DocumentPreviewModal({
  open,
  title = "Document Preview",
  url,
  onClose,
}) {
  return (
    <Modal
      open={open}
      title={title}
      onCancel={onClose}
      footer={[
        url ? (
          <Button key="download" href={url} download>
            Download
          </Button>
        ) : null,
        <Button key="close" onClick={onClose}>
          Close
        </Button>,
      ]}
      width={900}
    >
      {url ? (
        <object
          key={url}
          className="app-document-preview-object"
          data={url}
          type="application/pdf"
        >
          <div className="app-document-preview-fallback">
            <Text>Preview unavailable.</Text>
          </div>
        </object>
      ) : (
        <Text type="secondary">No preview URL available.</Text>
      )}
    </Modal>
  );
}
