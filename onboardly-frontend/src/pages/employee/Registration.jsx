import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button, Form, Input, Typography } from "antd";
import { AuthCard, AuthPage } from "../../components/auth";
import { StatusAlert } from "../../components/common";
import { useAuth } from "../../auth/AuthContext";
import { registerEmployeeAccount } from "../../services/authService";

const { Text } = Typography;

const USE_MOCK_DATA = false;

const initialForm = {
  token: "",
  invitedEmail: "",
  email: "",
  username: "",
  password: "",
  confirmPassword: "",
};

export default function Registration() {
  const [registrationForm] = Form.useForm();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();

  const [form, setForm] = useState(initialForm);
  const [status, setStatus] = useState({ message: "", type: "info" });
  const [loadingToken, setLoadingToken] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadRegistrationLink() {
      const tokenFromUrl = searchParams.get("token") || "";
      const emailFromUrl = searchParams.get("email") || "";

      if (!tokenFromUrl) {
        setStatus({
          message: "Invalid registration link. Please contact HR.",
          type: "error",
        });
        setLoadingToken(false);
        return;
      }

      if (USE_MOCK_DATA) {
        if (!active) return;

        setForm((prev) => ({
          ...prev,
          token: tokenFromUrl,
          invitedEmail: emailFromUrl,
          email: emailFromUrl,
        }));
        registrationForm.setFieldsValue({
          email: emailFromUrl,
        });
        setStatus({
          message: emailFromUrl
            ? `Invitation verified for ${emailFromUrl}.`
            : "Invitation verified. Enter the email address used for your invitation.",
          type: emailFromUrl ? "success" : "info",
        });

        setLoadingToken(false);
        return;
      }

      if (!active) return;

      setForm((prev) => ({
        ...prev,
        token: tokenFromUrl,
        invitedEmail: emailFromUrl,
        email: emailFromUrl,
      }));

      registrationForm.setFieldsValue({ email: emailFromUrl });
      setStatus({
        message: emailFromUrl
          ? `Invitation loaded for ${emailFromUrl}.`
          : "Invitation loaded. Enter the email address used for your invitation.",
        type: emailFromUrl ? "success" : "info",
      });
      setLoadingToken(false);
    }

    loadRegistrationLink();

    return () => {
      active = false;
    };
  }, [registrationForm, searchParams]);

  function validateForm(formValues = form) {
    if (!formValues.token) return "Registration token is missing.";

    if (!formValues.email.trim()) return "Email is required.";

    if (!formValues.username.trim()) return "Username is required.";

    if (formValues.username.trim().length < 4) {
      return "Username must be at least 4 characters.";
    }

    if (!formValues.password) return "Password is required.";

    if (formValues.password.length < 8) {
      return "Password must be at least 8 characters.";
    }

    if (formValues.password !== formValues.confirmPassword) {
      return "Passwords do not match.";
    }

    return "";
  }

  async function handleSubmit(values) {
    const nextForm = {
      ...form,
      ...values,
    };

    setForm(nextForm);

    const error = validateForm(nextForm);

    if (error) {
      setStatus({ message: error, type: "warning" });
      return;
    }

    if (USE_MOCK_DATA) {
      setStatus({ message: "Mock registration successful.", type: "success" });
      login("mock-jwt-token", {
        userId: 1,
        username: nextForm.username,
        email: nextForm.email,
        role: "EMPLOYEE",
        onboardingStatus: "NOT_STARTED",
      });
      navigate("/employee/onboarding");
      return;
    }

    try {
      setSubmitting(true);
      setStatus({ message: "", type: "info" });

      // TODO: Confirm backend validates that submitted email matches the registration token email.
      const response = await registerEmployeeAccount({
        token: nextForm.token,
        email: nextForm.email,
        username: nextForm.username,
        password: nextForm.password,
      });

      const token = response.data?.token;
      // TODO: Confirm backend returns onboardingStatus after registration; default to NOT_STARTED until then.
      const user = response.data?.user || {
        username: response.data?.username || nextForm.username,
        email: nextForm.email,
        role: response.data?.role || "EMPLOYEE",
        onboardingStatus: response.data?.onboardingStatus || "NOT_STARTED",
      };

      if (token && user) {
        login(token, user);
      }

      navigate("/login");
    } catch (error) {
      setStatus({
        message:
          error.response?.data?.message ||
          "Registration failed. Username or email may already be used.",
        type: "error",
      });
    } finally {
      setSubmitting(false);
    }
  }

  if (loadingToken) {
    return (
      <AuthPage>
        <AuthCard title="Create Your Account" width={480}>
          Validating registration token...
        </AuthCard>
      </AuthPage>
    );
  }

  return (
    <AuthPage>
      <AuthCard title="Create Your Account" width={480}>
        <StatusAlert className="auth-alert" status={status} />

        <Form
          className="auth-form"
          form={registrationForm}
          layout="vertical"
          initialValues={form}
          onFinish={handleSubmit}
          onValuesChange={(_, values) =>
            setForm((prev) => ({
              ...prev,
              ...values,
            }))
          }
        >
          <Form.Item
            label="Email"
            name="email"
            rules={[{ required: true, message: "Email is required." }]}
          >
            <Input
              type="email"
              autoComplete="email"
              disabled={Boolean(form.invitedEmail)}
            />
          </Form.Item>

          <Form.Item
            label="Username"
            name="username"
            rules={[
              { required: true, message: "Username is required." },
              { min: 4, message: "Username must be at least 4 characters." },
            ]}
          >
            <Input autoComplete="username" />
          </Form.Item>

          <Form.Item
            label="Password"
            name="password"
            rules={[
              { required: true, message: "Password is required." },
              { min: 8, message: "Password must be at least 8 characters." },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>

          <Form.Item
            label="Confirm Password"
            name="confirmPassword"
            dependencies={["password"]}
            rules={[
              { required: true, message: "Please confirm your password." },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue("password") === value) {
                    return Promise.resolve();
                  }

                  return Promise.reject(new Error("Passwords do not match."));
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>

          <Button
            type="primary"
            block
            htmlType="submit"
            loading={submitting}
            disabled={!form.token}
          >
            Signup
          </Button>

          <div className="auth-form-link">
            <Text type="secondary">Already have an account? </Text>
            <Link to="/login">Sign in</Link>
          </div>
        </Form>
      </AuthCard>
    </AuthPage>
  );
}
