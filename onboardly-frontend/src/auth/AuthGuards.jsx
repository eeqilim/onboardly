import { Navigate } from "react-router-dom";
import { Layout } from "antd";
import Navbar from "../components/Navbar";
import { useAuth } from "./AuthContext";

const { Content } = Layout;

function getEmployeeDefaultPath({
  isApproved,
  isRejected,
  isPending,
  isNotStarted,
}) {
  if (isApproved) return "/employee/home";
  if (isRejected || isNotStarted) return "/employee/onboarding";
  if (isPending) return "/employee/waiting-review";

  return "/employee/onboarding";
}

function ProtectedLayout({ children }) {
  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Navbar />
      <Content style={{ padding: 24, background: "#f0f2f5" }}>
        {children}
      </Content>
    </Layout>
  );
}

export function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();

  if (loading) return <p>Loading...</p>;
  if (!isAuthenticated) return <Navigate to="/login" replace />;

  return <ProtectedLayout>{children}</ProtectedLayout>;
}

export function EmployeeRoute({ children }) {
  const { isAuthenticated, isEmployee, isHR, loading } = useAuth();

  if (loading) return <p>Loading...</p>;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (isHR) return <Navigate to="/hr/home" replace />;
  if (!isEmployee) return <Navigate to="/login" replace />;

  return <ProtectedLayout>{children}</ProtectedLayout>;
}

export function ApprovedEmployeeRoute({ children }) {
  const {
    isAuthenticated,
    isEmployee,
    isHR,
    isApproved,
    isRejected,
    isPending,
    isNotStarted,
    loading,
  } = useAuth();

  if (loading) return <p>Loading...</p>;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (isHR) return <Navigate to="/hr/home" replace />;
  if (!isEmployee) return <Navigate to="/login" replace />;
  if (!isApproved) {
    return (
      <Navigate
        to={getEmployeeDefaultPath({
          isApproved,
          isRejected,
          isPending,
          isNotStarted,
        })}
        replace
      />
    );
  }

  return <ProtectedLayout>{children}</ProtectedLayout>;
}

export function HrRoute({ children }) {
  const {
    isAuthenticated,
    isEmployee,
    isHR,
    isApproved,
    isRejected,
    isPending,
    isNotStarted,
    loading,
  } = useAuth();

  if (loading) return <p>Loading...</p>;
  if (!isAuthenticated) return <Navigate to="/login" replace />;

  if (isEmployee) {
    return (
      <Navigate
        to={getEmployeeDefaultPath({
          isApproved,
          isRejected,
          isPending,
          isNotStarted,
        })}
        replace
      />
    );
  }

  if (!isHR) return <Navigate to="/login" replace />;

  return <ProtectedLayout>{children}</ProtectedLayout>;
}
