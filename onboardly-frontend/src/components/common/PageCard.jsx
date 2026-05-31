import { Card } from "antd";
import "./common.css";

export default function PageCard({ className = "", children, ...props }) {
  return (
    <Card className={`app-page-card ${className}`.trim()} {...props}>
      {children}
    </Card>
  );
}
