import { Space, Typography } from "antd";
import { useAuth } from "../../auth/AuthContext";
import { PageCard } from "../../components/common";
import "./EmployeeHome.css";

const { Text, Title } = Typography;

export default function EmployeeHome() {
  const { user } = useAuth();
  const displayName = user?.preferredName || user?.username || "Employee";

  return (
    <div className="employee-home-page">
      <PageCard className="employee-home-welcome-card">
        <Space direction="vertical" size={4}>
          <Text type="secondary">Hello {displayName},</Text>
          <Title level={2}>Welcome to Onboardly!</Title>
        </Space>
      </PageCard>
    </div>
  );
}
