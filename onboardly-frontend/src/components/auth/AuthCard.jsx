import { Card } from "antd";

export default function AuthCard({ title, width = 420, children }) {
  return (
    <Card
      className="auth-card"
      style={{ "--auth-card-width": `${width}px` }}
      title={<span className="auth-card-title">{title}</span>}
    >
      {children}
    </Card>
  );
}
