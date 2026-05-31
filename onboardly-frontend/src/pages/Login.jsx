import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { loginUser } from "../services/authService";
import { startEmployeeOnboarding } from "../services/employeeService";
import { Alert, Button, Form, Input, Typography } from "antd";
import { AuthCard, AuthPage } from "../components/auth";

const { Text } = Typography;
const USE_MOCK_DATA = false;

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();

  // const [formValues, setFormValues] = useState({
  //   username: "",
  //   password: "",
  // });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function redirectUser(user) {
    if (user.role === "HR") {
      navigate("/hr/home");
      return;
    }

    if (user.role === "EMPLOYEE") {
      switch (user.onboardingStatus) {
        case "APPROVED":
          navigate("/employee/home");
          break;
        case "PENDING":
          navigate("/employee/waiting-review");
          break;
        case "REJECTED":
        case "NOT_STARTED":
        default:
          navigate("/employee/onboarding");
          break;
      }
    }
  }

  // Backend returns role as a comma-joined authority list like "ROLE_EMPLOYEE".
  // Strip the prefix and prefer HR if multiple roles are present.
  function normaliseRole(raw) {
    if (!raw) return raw;
    const roles = String(raw)
      .split(",")
      .map((role) => role.trim())
      .map((role) => (role.startsWith("ROLE_") ? role.slice(5) : role));

    return roles.includes("HR") ? "HR" : roles[0];
  }

  // function handleChange(event) {
  //   const { name, value } = event.target;

  //   setFormValues((current) => ({
  //     ...current,
  //     [name]: value,
  //   }));
  // }

  // async function handleSubmit(event) {
  //   event.preventDefault();
  async function handleFinish(values) {
    setError("");

    try {
      setSubmitting(true);

      if (USE_MOCK_DATA) {
        const mockToken = "mock-jwt-token";

        const mockUser =
          values.username === "hr"
            ? {
                userId: 1,
                username: "hr",
                role: "HR",
                firstName: "HR",
              }
            : {
                userId: 2,
                username: values.username,
                role: "EMPLOYEE",
                onboardingStatus: "APPROVED",
                firstName: "Demo",
                // isCitizenOrResident: "YES",
                // citizenOrGreenCard: "CITIZEN",
                // isCitizenOrResident: "YES",
                // citizenOrGreenCard: "GREEN_CARD",
                isCitizenOrResident: "NO",
                workAuthorization: "F1_CPT_OPT",
              };

        login(mockToken, mockUser);

        redirectUser(mockUser);

        return;
      }

      const response = await loginUser({
        usernameOrEmail: values.username,
        password: values.password,
      });
      //test
      console.log("response.data:", response.data);

      const token = response.data.token;
      const role = normaliseRole(response.data.role);
      let onboardingStatus = response.data.onboardingStatus;
      let applicationWorkflowId = null;

      localStorage.setItem("jwt", token);

      if (role === "EMPLOYEE") {
        try {
          const startResp = await startEmployeeOnboarding();
          onboardingStatus = startResp.data?.status ?? onboardingStatus;
          applicationWorkflowId = startResp.data?.applicationWorkflowId ?? null;
        } catch (e) {
          console.error("Failed to initialise onboarding application", e);
        }
      }

      const user = response.data.user || {
        username: response.data.username,
        // add by weihao
        userId: response.data.userId,
        email: response.data.email,
        // ==============================
        role,
        onboardingStatus,
      };
      user.role = role;
      user.onboardingStatus = onboardingStatus;
      if (applicationWorkflowId != null) {
        user.applicationWorkflowId = applicationWorkflowId;
      }

      //test
      console.log("user:", user);

      login(token, user);
      redirectUser(user);
    } catch {
      setError("Invalid username or password.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthPage>
      <AuthCard title="Onboardly Portal">
        <Form
          className="auth-form"
          name="login"
          layout="vertical"
          onFinish={handleFinish}
        >
          <Form.Item
            label="Username or Email"
            name="username"
            rules={[
              { required: true, message: "Enter your username or email" },
            ]}
          >
            <Input placeholder="Enter username or email" />
          </Form.Item>

          <Form.Item
            label="Password"
            name="password"
            rules={[{ required: true, message: "Enter your password" }]}
          >
            <Input.Password placeholder="Enter password" />
          </Form.Item>

          {error && (
            <Alert
              className="auth-alert"
              type="error"
              message={error}
              showIcon
            />
          )}

          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={submitting}>
              {submitting ? "Logging in..." : "Login"}
            </Button>
          </Form.Item>

          <div className="auth-form-link">
            <Text type="secondary">Have an invitation? </Text>
            <Link to="/register">Create your account</Link>
          </div>
        </Form>
      </AuthCard>
    </AuthPage>
  );
}
