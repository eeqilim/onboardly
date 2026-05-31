import { Alert } from "antd";

export default function StatusAlert({ status, className }) {
  if (!status?.message) return null;

  return (
    <Alert
      className={className}
      message={status.message}
      type={status.type || "info"}
      showIcon
    />
  );
}
