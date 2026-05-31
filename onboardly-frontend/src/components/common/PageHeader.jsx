import { Alert, Typography } from "antd";
import "./common.css";

const { Title } = Typography;

export default function PageHeader({ title, status, children }) {
  return (
    <div className="app-page-header">
      <div className="app-page-header-main">
        <Title level={2}>{title}</Title>
        {children}
      </div>

      {status?.message && (
        <Alert message={status.message} type={status.type || "info"} showIcon />
      )}
    </div>
  );
}
