import { useNavigate } from "react-router-dom";
import { Button, Result, Typography } from "antd";
import { useAuth } from "../../auth/AuthContext";
import { PageCard } from "../../components/common";
import "./WaitingReview.css";

const { Paragraph } = Typography;

export default function WaitingReview() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <div className="waiting-review-page">
      <PageCard className="waiting-review-card">
        <Result
          icon={<span className="waiting-review-icon">⏳</span>}
          title="Please wait for HR to review your application"
          subTitle={
            <Paragraph className="waiting-review-message">
              Your onboarding application has been submitted successfully. You
              will receive an email once HR makes a decision.
            </Paragraph>
          }
          extra={<Button onClick={handleLogout}>Logout</Button>}
        />
      </PageCard>
    </div>
  );
}
