import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Alert,
  Avatar,
  Button,
  Card,
  Input,
  Select,
  Typography,
  Upload,
} from "antd";
import {
  ContactsOutlined,
  HomeOutlined,
  IdcardOutlined,
  PhoneOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  UploadOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { useAuth } from "../../auth/AuthContext";
import {
  getMyOnboardingApplication,
  submitEmployeeOnboarding,
  uploadEmployeeDocument,
} from "../../services/employeeService";
import "./Onboarding.css";

const { Title } = Typography;

const USE_MOCK_DATA = false;
const BYPASS_UPLOAD_FAILURES_FOR_LOCAL_TESTING = true;

const STATES = [
  "AL",
  "AK",
  "AZ",
  "AR",
  "CA",
  "CO",
  "CT",
  "DE",
  "FL",
  "GA",
  "HI",
  "ID",
  "IL",
  "IN",
  "IA",
  "KS",
  "KY",
  "LA",
  "ME",
  "MD",
  "MA",
  "MI",
  "MN",
  "MS",
  "MO",
  "MT",
  "NE",
  "NV",
  "NH",
  "NJ",
  "NM",
  "NY",
  "NC",
  "ND",
  "OH",
  "OK",
  "OR",
  "PA",
  "RI",
  "SC",
  "SD",
  "TN",
  "TX",
  "UT",
  "VT",
  "VA",
  "WA",
  "WV",
  "WI",
  "WY",
];

const initialForm = {
  firstName: "",
  middleName: "",
  lastName: "",
  preferredName: "",
  email: "",
  ssn: "",
  dateOfBirth: "",
  gender: "",
  cellPhone: "",
  workPhone: "",

  address: {
    line1: "",
    line2: "",
    city: "",
    state: "",
    zip: "",
  },

  isCitizenOrResident: "",
  citizenOrGreenCard: "",
  workAuthorization: "",
  otherWorkAuthorization: "",
  workAuthStartDate: "",
  workAuthEndDate: "",

  hasDriverLicense: "",
  driverLicenseNumber: "",
  driverLicenseExpirationDate: "",

  reference: {
    firstName: "",
    middleName: "",
    lastName: "",
    phone: "",
    email: "",
    address: "",
    relationship: "",
  },

  emergencyContacts: [
    {
      firstName: "",
      middleName: "",
      lastName: "",
      phone: "",
      email: "",
      relationship: "",
    },
  ],
};

function TextInput({
  label,
  name,
  value,
  onChange,
  type = "text",
  required = false,
  disabled = false,
  error = false,
}) {
  return (
    <label
      className={`onboarding-input-group${
        error ? " onboarding-input-group-error" : ""
      }`}
    >
      <span>
        {label}
        {required ? " *" : ""}
      </span>
      <Input
        name={name}
        type={type}
        value={value ?? ""}
        onChange={onChange}
        disabled={disabled}
        status={error ? "error" : undefined}
      />
    </label>
  );
}

function SelectInput({
  label,
  name,
  value,
  onChange,
  options,
  required = false,
  error = false,
}) {
  return (
    <label
      className={`onboarding-input-group${
        error ? " onboarding-input-group-error" : ""
      }`}
    >
      <span>
        {label}
        {required ? " *" : ""}
      </span>

      <Select
        value={value || undefined}
        placeholder="Select"
        onChange={(selectedValue) =>
          onChange({ target: { name, value: selectedValue } })
        }
        status={error ? "error" : undefined}
        options={options.map((option) => ({
          value: option.value || option,
          label: option.label || option,
        }))}
      />
    </label>
  );
}

function FileInput({
  label,
  onChange,
  accept = ".pdf,.jpg,.jpeg,.png",
  error = false,
}) {
  return (
    <label
      className={`onboarding-input-group${
        error ? " onboarding-input-group-error" : ""
      }`}
    >
      <span>{label}</span>
      <Upload
        accept={accept}
        beforeUpload={() => false}
        maxCount={1}
        onChange={(info) =>
          onChange(info.fileList[0]?.originFileObj || info.fileList[0] || null)
        }
      >
        <Button danger={error} icon={<UploadOutlined />}>
          Select File
        </Button>
      </Upload>
    </label>
  );
}

function CardTitle({ icon, children }) {
  return (
    <span className="onboarding-card-title">
      {icon}
      <span>{children}</span>
    </span>
  );
}

// Strip non-digits and keep a leading +. The backend's @Pattern for phone
// numbers is `\+?\d{10,15}`. Returns the formatted value when valid, or the
// raw trimmed input otherwise so the frontend validator can flag it inline.
function normalisePhone(raw) {
  if (!raw) return null;
  const trimmed = String(raw).trim();
  if (!trimmed) return null;
  const plus = trimmed.startsWith("+") ? "+" : "";
  const digits = trimmed.replace(/\D/g, "");
  if (digits.length >= 10 && digits.length <= 15) return plus + digits;
  return trimmed;
}

function isValidPhone(raw) {
  if (!raw) return true; // empty is fine — required-ness is checked separately
  const digits = String(raw).replace(/\D/g, "");
  return digits.length >= 10 && digits.length <= 15;
}

// Backend pattern: `\d{3}-\d{2}-\d{4}`. Auto-dashes 9 plain digits; passes
// through correctly-formatted input; otherwise returns the raw value so the
// frontend validator can show a clear message.
function normaliseSsn(raw) {
  if (!raw) return null;
  const trimmed = String(raw).trim();
  if (!trimmed) return null;
  if (/^\d{3}-\d{2}-\d{4}$/.test(trimmed)) return trimmed;
  const digits = trimmed.replace(/\D/g, "");
  if (digits.length === 9) {
    return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`;
  }
  return trimmed;
}

function isValidSsn(raw) {
  if (!raw) return true;
  return /^\d{3}-\d{2}-\d{4}$/.test(normaliseSsn(raw) || "");
}

function emptyToNull(value) {
  if (value === undefined || value === null) return null;
  const trimmed = String(value).trim();
  return trimmed ? trimmed : null;
}

function deriveCitizenshipStatus(form) {
  if (form.isCitizenOrResident === "YES") {
    return form.citizenOrGreenCard === "GREEN_CARD" ? "GREEN_CARD" : "CITIZEN";
  }
  return "NON_RESIDENT";
}

// Frontend exposes "F1 (CPT/OPT)" as a single option but the backend splits
// it into F1_CPT, F1_OPT, and F1_OPT_STEM. We send F1_OPT_STEM so the visa
// page renders the full STEM OPT step workflow; HR clarifies sub-type per user.
function mapVisaType(workAuthorization) {
  switch (workAuthorization) {
    case "H1B":
    case "L2":
    case "H4":
    case "OTHER":
      return { visaType: workAuthorization, visaTypeOther: null };
    case "F1_CPT_OPT":
      return { visaType: "F1_OPT_STEM", visaTypeOther: null };
    default:
      return { visaType: "OTHER", visaTypeOther: workAuthorization || null };
  }
}

function buildContact(source, type) {
  return {
    type,
    firstName: source.firstName?.trim() || "",
    lastName: source.lastName?.trim() || "",
    middleName: emptyToNull(source.middleName),
    cellPhone: normalisePhone(source.phone),
    alternatePhone: null,
    email: emptyToNull(source.email),
    relationship: emptyToNull(source.relationship),
    address: emptyToNull(source.address),
  };
}

// The backend's referenceContact field is optional, but if present its
// firstName/lastName are @NotBlank. Send null when the user didn't fill it in.
function buildReferenceContactIfComplete(source) {
  const firstName = source.firstName?.trim();
  const lastName = source.lastName?.trim();
  if (!firstName || !lastName) return null;
  return buildContact(source, "REFERENCE");
}

function buildOnboardingPayload(form) {
  const citizenshipStatus = deriveCitizenshipStatus(form);
  const isNonResident = citizenshipStatus === "NON_RESIDENT";
  const hasDriverLicense = form.hasDriverLicense === "YES";

  const visaSource = isNonResident ? mapVisaType(form.workAuthorization) : null;

  return {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    middleName: emptyToNull(form.middleName),
    preferredName: emptyToNull(form.preferredName),
    email: form.email.trim(),
    cellPhone: normalisePhone(form.cellPhone),
    alternatePhone: normalisePhone(form.workPhone),
    gender: emptyToNull(form.gender),
    ssn: normaliseSsn(form.ssn),
    dateOfBirth: emptyToNull(form.dateOfBirth),
    citizenshipStatus,
    driverLicense: hasDriverLicense
      ? emptyToNull(form.driverLicenseNumber)
      : null,
    driverLicenseExpiration: hasDriverLicense
      ? emptyToNull(form.driverLicenseExpirationDate)
      : null,
    address: {
      type: "PRIMARY",
      addressLine1: form.address.line1.trim(),
      addressLine2: emptyToNull(form.address.line2),
      city: form.address.city.trim(),
      state: form.address.state,
      zipCode: form.address.zip.trim(),
    },
    referenceContact: buildReferenceContactIfComplete(form.reference),
    emergencyContacts: form.emergencyContacts.map((c) =>
      buildContact(c, "EMERGENCY"),
    ),
    visaInfo: isNonResident
      ? {
          visaType: visaSource.visaType,
          visaTypeOther: visaSource.visaTypeOther,
          startDate: emptyToNull(form.workAuthStartDate),
          endDate: emptyToNull(form.workAuthEndDate),
        }
      : null,
  };
}

function mergeNestedValue(current, path, value) {
  const [root, field] = path.split(".");

  if (!field) {
    return {
      ...current,
      [root]: value,
    };
  }

  return {
    ...current,
    [root]: {
      ...current[root],
      [field]: value,
    },
  };
}

export default function Onboarding() {
  const { user, isRejected } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState(initialForm);

  const displayEmail = user?.email || form.email;

  const [avatarFile, setAvatarFile] = useState(null);
  const [avatarPreview, setAvatarPreview] = useState("");
  const [workAuthFile, setWorkAuthFile] = useState(null);
  const [driverLicenseFile, setDriverLicenseFile] = useState(null);
  const [status, setStatus] = useState({ message: "", type: "info" });
  const [invalidFields, setInvalidFields] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  // Carried on user from /onboarding/start; refetch as fallback so reloaded
  // sessions can still attach uploads to the right application.
  const [workflowId, setWorkflowId] = useState(
    user?.applicationWorkflowId ?? null,
  );

  useEffect(() => {
    if (workflowId != null) return;
    let active = true;
    getMyOnboardingApplication()
      .then((response) => {
        if (!active) return;
        const fetched = response?.data?.applicationWorkflowId ?? null;
        if (fetched != null) setWorkflowId(fetched);
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [workflowId]);

  function hasFieldError(fieldName) {
    return invalidFields.includes(fieldName);
  }

  function clearFieldError(fieldName) {
    setInvalidFields((current) =>
      current.filter((field) => field !== fieldName),
    );
  }

  function handleChange(event) {
    const { name, value } = event.target;

    clearFieldError(name);
    setForm((current) => mergeNestedValue(current, name, value));
  }

  function handleAvatarChange(file) {
    setAvatarFile(file);

    if (!file) {
      setAvatarPreview("");
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => setAvatarPreview(event.target.result);
    reader.readAsDataURL(file);
  }

  function handleEmergencyContactChange(index, event) {
    const { name, value } = event.target;
    clearFieldError(`emergencyContacts.${index}.${name}`);

    setForm((current) => ({
      ...current,
      emergencyContacts: current.emergencyContacts.map(
        (contact, contactIndex) =>
          contactIndex === index
            ? {
                ...contact,
                [name]: value,
              }
            : contact,
      ),
    }));
  }

  function addEmergencyContact() {
    setForm((current) => ({
      ...current,
      emergencyContacts: [
        ...current.emergencyContacts,
        {
          firstName: "",
          middleName: "",
          lastName: "",
          phone: "",
          email: "",
          relationship: "",
        },
      ],
    }));
  }

  function removeEmergencyContact(index) {
    setForm((current) => ({
      ...current,
      emergencyContacts: current.emergencyContacts.filter(
        (_, itemIndex) => itemIndex !== index,
      ),
    }));
  }

  function validateForm() {
    const errors = [];
    const addError = (field, message) => errors.push({ field, message });

    if (!form.firstName.trim())
      addError("firstName", "First name is required.");
    if (!form.lastName.trim()) addError("lastName", "Last name is required.");
    if (!form.ssn.trim()) {
      addError("ssn", "SSN is required.");
    } else if (!isValidSsn(form.ssn)) {
      addError("ssn", "SSN must be 9 digits (e.g. 123-45-6789).");
    }
    if (!form.dateOfBirth)
      addError("dateOfBirth", "Date of birth is required.");
    if (!form.gender) addError("gender", "Gender is required.");
    if (!form.cellPhone.trim()) {
      addError("cellPhone", "Cell phone is required.");
    } else if (!isValidPhone(form.cellPhone)) {
      addError("cellPhone", "Cell phone must be 10–15 digits.");
    }
    if (form.workPhone.trim() && !isValidPhone(form.workPhone)) {
      addError("workPhone", "Work phone must be 10–15 digits.");
    }

    if (!form.address.line1.trim())
      addError("address.line1", "Address line 1 is required.");
    if (!form.address.city.trim())
      addError("address.city", "City is required.");
    if (!form.address.state) addError("address.state", "State is required.");
    if (!form.address.zip.trim()) addError("address.zip", "ZIP is required.");

    if (!form.isCitizenOrResident) {
      addError(
        "isCitizenOrResident",
        "Please answer whether you are a citizen or permanent resident.",
      );
    }

    if (form.isCitizenOrResident === "YES" && !form.citizenOrGreenCard) {
      addError("citizenOrGreenCard", "Please select Green Card or Citizen.");
    }

    if (form.isCitizenOrResident === "NO") {
      if (!form.workAuthorization) {
        addError("workAuthorization", "Work authorization is required.");
      }
      if (
        form.workAuthorization === "OTHER" &&
        !form.otherWorkAuthorization.trim()
      ) {
        addError(
          "otherWorkAuthorization",
          "Please specify your work authorization.",
        );
      }
      if (!form.workAuthStartDate) {
        addError(
          "workAuthStartDate",
          "Work authorization start date is required.",
        );
      }
      if (!form.workAuthEndDate) {
        addError(
          "workAuthEndDate",
          "Work authorization expiration date is required.",
        );
      }
      if (!workAuthFile) {
        addError("workAuthFile", "Work authorization document is required.");
      }
    }

    if (!form.hasDriverLicense) {
      addError(
        "hasDriverLicense",
        "Please answer whether you have a driver's license.",
      );
    }

    if (form.hasDriverLicense === "YES") {
      if (!form.driverLicenseNumber.trim()) {
        addError("driverLicenseNumber", "Driver license number is required.");
      }
      if (!form.driverLicenseExpirationDate) {
        addError(
          "driverLicenseExpirationDate",
          "Driver license expiration date is required.",
        );
      }
      if (!driverLicenseFile) {
        addError("driverLicenseFile", "Driver license document is required.");
      }
    }

    const completeEmergencyContactIndex = form.emergencyContacts.findIndex(
      (contact) =>
        contact.firstName.trim() &&
        contact.lastName.trim() &&
        contact.phone.trim() &&
        contact.relationship.trim(),
    );

    if (completeEmergencyContactIndex === -1) {
      const contactIndex = 0;
      const firstContact = form.emergencyContacts[contactIndex];

      if (!firstContact.firstName.trim()) {
        addError(
          `emergencyContacts.${contactIndex}.firstName`,
          "Emergency contact first name is required.",
        );
      }
      if (!firstContact.lastName.trim()) {
        addError(
          `emergencyContacts.${contactIndex}.lastName`,
          "Emergency contact last name is required.",
        );
      }
      if (!firstContact.phone.trim()) {
        addError(
          `emergencyContacts.${contactIndex}.phone`,
          "Emergency contact phone is required.",
        );
      }
      if (!firstContact.relationship.trim()) {
        addError(
          `emergencyContacts.${contactIndex}.relationship`,
          "Emergency contact relationship is required.",
        );
      }
    }

    // Any emergency contact whose phone is provided but malformed.
    form.emergencyContacts.forEach((contact, index) => {
      if (contact.phone.trim() && !isValidPhone(contact.phone)) {
        addError(
          `emergencyContacts.${index}.phone`,
          `Emergency contact ${index + 1} phone must be 10–15 digits.`,
        );
      }
    });

    // Reference contact: it's optional, but if the user started filling it,
    // require first + last name and a valid phone (if a phone was entered).
    const refFilled = Object.values(form.reference).some((v) =>
      String(v || "").trim(),
    );
    if (refFilled) {
      if (!form.reference.firstName.trim()) {
        addError("reference.firstName", "Reference first name is required.");
      }
      if (!form.reference.lastName.trim()) {
        addError("reference.lastName", "Reference last name is required.");
      }
      if (form.reference.phone.trim() && !isValidPhone(form.reference.phone)) {
        addError("reference.phone", "Reference phone must be 10–15 digits.");
      }
    }

    return errors;
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const errors = validateForm();

    if (errors.length > 0) {
      setInvalidFields(errors.map((error) => error.field));
      setStatus({
        message: "Please complete the highlighted required fields.",
        description: errors.map((error) => error.message),
        type: "warning",
      });
      return;
    }

    setInvalidFields([]);

    if (USE_MOCK_DATA) {
      setStatus({ message: "Mock onboarding submitted.", type: "success" });
      navigate("/employee/onboarding/documents");
      return;
    }

    // Files go through /employee/documents/upload (multipart). The submit
    // endpoint itself expects JSON and validates that required documents are
    // already uploaded, so files must be sent first.
    if (workflowId == null) {
      setStatus({
        message:
          "Could not resolve onboarding application — please reload and try again.",
        type: "error",
      });
      return;
    }

    const uploadMetaBase = {
      applicationId: workflowId,
      applicationType: "ONBOARDING",
    };

    const uploads = [];
    if (avatarFile) {
      uploads.push(
        uploadEmployeeDocument(avatarFile, {
          ...uploadMetaBase,
          title: "Avatar",
          documentType: "AVATAR",
        }),
      );
    }
    if (driverLicenseFile && form.hasDriverLicense === "YES") {
      uploads.push(
        uploadEmployeeDocument(driverLicenseFile, {
          ...uploadMetaBase,
          title: "Driver License",
          documentType: "DRIVER_LICENSE",
        }),
      );
    }
    if (workAuthFile && form.isCitizenOrResident === "NO") {
      uploads.push(
        uploadEmployeeDocument(workAuthFile, {
          ...uploadMetaBase,
          title: "Work Authorization",
          documentType: "WORK_AUTH",
        }),
      );
    }

    try {
      setSubmitting(true);
      setStatus({ message: "", type: "info" });
      let uploadBypassed = false;

      if (uploads.length > 0) {
        try {
          await Promise.all(uploads);
        } catch (uploadError) {
          if (!BYPASS_UPLOAD_FAILURES_FOR_LOCAL_TESTING) {
            throw uploadError;
          }

          uploadBypassed = true;
          console.warn(
            "Bypassing document upload failure for local testing.",
            uploadError,
          );
        }
      }

      try {
        await submitEmployeeOnboarding(
          buildOnboardingPayload({
            ...form,
            email: user?.email || form.email,
          }),
        );
      } catch (submitError) {
        if (!uploadBypassed) {
          throw submitError;
        }
      }

      navigate("/employee/onboarding/documents");
    } catch (err) {
      const serverMessage =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        "Submission failed. Please check your inputs.";
      setStatus({ message: serverMessage, type: "error" });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="onboarding-page">
      <div className="onboarding-title-row">
        <Title level={2}>Onboarding Application</Title>
        {status.message && (
          <Alert
            message={status.message}
            description={
              Array.isArray(status.description) ? (
                <ul className="onboarding-alert-list">
                  {status.description.map((message) => (
                    <li key={message}>{message}</li>
                  ))}
                </ul>
              ) : (
                status.description
              )
            }
            type={status.type}
            showIcon
          />
        )}
      </div>

      {isRejected && (
        <Alert
          className="onboarding-status-alert"
          type="warning"
          showIcon
          message="Your onboarding application needs updates."
          description={
            user?.onboardingComment ||
            user?.rejectionComment ||
            "Please review your form and documents before submitting again."
          }
        />
      )}

      <form className="onboarding-form" onSubmit={handleSubmit}>
        <div className="onboarding-column">
          <Card
            className="onboarding-card onboarding-card-profile"
            title={
              <CardTitle icon={<UserOutlined />}>
                Personal Information
              </CardTitle>
            }
          >
            <div className="onboarding-profile-layout">
              <Avatar
                size={72}
                src={avatarPreview || undefined}
                icon={!avatarPreview && <UserOutlined />}
              />

              <div className="onboarding-grid">
                <TextInput
                  label="First Name"
                  name="firstName"
                  value={form.firstName}
                  onChange={handleChange}
                  required
                  error={hasFieldError("firstName")}
                />
                <TextInput
                  label="Last Name"
                  name="lastName"
                  value={form.lastName}
                  onChange={handleChange}
                  required
                  error={hasFieldError("lastName")}
                />
                <TextInput
                  label="Middle Name"
                  name="middleName"
                  value={form.middleName}
                  onChange={handleChange}
                />
                <TextInput
                  label="Preferred Name"
                  name="preferredName"
                  value={form.preferredName}
                  onChange={handleChange}
                />
                <FileInput
                  label="Avatar"
                  accept="image/*"
                  onChange={handleAvatarChange}
                />
              </div>
            </div>
          </Card>

          <Card
            className="onboarding-card"
            title={
              <CardTitle icon={<IdcardOutlined />}>
                Identity Information
              </CardTitle>
            }
          >
            <div className="onboarding-grid">
              <TextInput
                label="SSN"
                name="ssn"
                value={form.ssn}
                onChange={handleChange}
                required
                error={hasFieldError("ssn")}
              />
              <TextInput
                label="Date of Birth"
                name="dateOfBirth"
                value={form.dateOfBirth}
                onChange={handleChange}
                type="date"
                required
                error={hasFieldError("dateOfBirth")}
              />

              <SelectInput
                label="Gender"
                name="gender"
                value={form.gender}
                onChange={handleChange}
                required
                error={hasFieldError("gender")}
                options={[
                  { value: "MALE", label: "Male" },
                  { value: "FEMALE", label: "Female" },
                  { value: "OTHER", label: "Other" },
                  { value: "PREFER_NOT_TO_SAY", label: "I Prefer Not to Say" },
                ]}
              />
            </div>
          </Card>

          <Card
            className="onboarding-card"
            title={
              <CardTitle icon={<PhoneOutlined />}>
                Contact Information
              </CardTitle>
            }
          >
            <div className="onboarding-grid">
              <TextInput
                label="Cell Phone"
                name="cellPhone"
                value={form.cellPhone}
                onChange={handleChange}
                required
                error={hasFieldError("cellPhone")}
              />
              <TextInput
                label="Work Phone"
                name="workPhone"
                value={form.workPhone}
                onChange={handleChange}
                error={hasFieldError("workPhone")}
              />
              <TextInput
                label="Email"
                name="email"
                value={displayEmail}
                onChange={handleChange}
                type="email"
                disabled
              />
            </div>
          </Card>

          <Card
            className="onboarding-card onboarding-card-wide"
            title={
              <CardTitle icon={<HomeOutlined />}>Current Address</CardTitle>
            }
          >
            <div className="onboarding-grid onboarding-address-grid">
              <TextInput
                label="Address Line 1"
                name="address.line1"
                value={form.address.line1}
                onChange={handleChange}
                required
                error={hasFieldError("address.line1")}
              />
              <TextInput
                label="Address Line 2"
                name="address.line2"
                value={form.address.line2}
                onChange={handleChange}
              />
              <TextInput
                label="City"
                name="address.city"
                value={form.address.city}
                onChange={handleChange}
                required
                error={hasFieldError("address.city")}
              />
              <SelectInput
                label="State"
                name="address.state"
                value={form.address.state}
                onChange={handleChange}
                options={STATES}
                required
                error={hasFieldError("address.state")}
              />
              <TextInput
                label="ZIP"
                name="address.zip"
                value={form.address.zip}
                onChange={handleChange}
                required
                error={hasFieldError("address.zip")}
              />
            </div>
          </Card>
        </div>

        <div className="onboarding-column">
          <Card
            className="onboarding-card onboarding-card-wide"
            title={
              <CardTitle icon={<SafetyCertificateOutlined />}>
                Work Authorization
              </CardTitle>
            }
          >
            <div className="onboarding-grid">
              <SelectInput
                label="Are you a citizen or permanent resident of the U.S.?"
                name="isCitizenOrResident"
                value={form.isCitizenOrResident}
                onChange={handleChange}
                required
                error={hasFieldError("isCitizenOrResident")}
                options={[
                  { value: "YES", label: "Yes" },
                  { value: "NO", label: "No" },
                ]}
              />

              {form.isCitizenOrResident === "YES" && (
                <SelectInput
                  label="Select Type"
                  name="citizenOrGreenCard"
                  value={form.citizenOrGreenCard}
                  onChange={handleChange}
                  required
                  error={hasFieldError("citizenOrGreenCard")}
                  options={[
                    { value: "GREEN_CARD", label: "Green Card" },
                    { value: "CITIZEN", label: "Citizen" },
                  ]}
                />
              )}

              {form.isCitizenOrResident === "NO" && (
                <>
                  <SelectInput
                    label="What is your work authorization?"
                    name="workAuthorization"
                    value={form.workAuthorization}
                    onChange={handleChange}
                    required
                    error={hasFieldError("workAuthorization")}
                    options={[
                      { value: "H1B", label: "H1-B" },
                      { value: "L2", label: "L2" },
                      { value: "F1_CPT_OPT", label: "F1 (CPT/OPT)" },
                      { value: "H4", label: "H4" },
                      { value: "OTHER", label: "Other" },
                    ]}
                  />

                  {form.workAuthorization === "OTHER" && (
                    <TextInput
                      label="Specify Work Authorization"
                      name="otherWorkAuthorization"
                      value={form.otherWorkAuthorization}
                      onChange={handleChange}
                      required
                      error={hasFieldError("otherWorkAuthorization")}
                    />
                  )}

                  <TextInput
                    label="Start Date"
                    name="workAuthStartDate"
                    value={form.workAuthStartDate}
                    onChange={handleChange}
                    type="date"
                    required
                    error={hasFieldError("workAuthStartDate")}
                  />
                  <TextInput
                    label="Expiration Date"
                    name="workAuthEndDate"
                    value={form.workAuthEndDate}
                    onChange={handleChange}
                    type="date"
                    required
                    error={hasFieldError("workAuthEndDate")}
                  />
                  <FileInput
                    label="Work Authorization *"
                    onChange={(file) => {
                      setWorkAuthFile(file);
                      clearFieldError("workAuthFile");
                    }}
                    error={hasFieldError("workAuthFile")}
                  />
                </>
              )}

              <SelectInput
                label="Do you have a driver's license?"
                name="hasDriverLicense"
                value={form.hasDriverLicense}
                onChange={handleChange}
                required
                error={hasFieldError("hasDriverLicense")}
                options={[
                  { value: "YES", label: "Yes" },
                  { value: "NO", label: "No" },
                ]}
              />

              {form.hasDriverLicense === "YES" && (
                <>
                  <TextInput
                    label="Driver License Number"
                    name="driverLicenseNumber"
                    value={form.driverLicenseNumber}
                    onChange={handleChange}
                    required
                    error={hasFieldError("driverLicenseNumber")}
                  />
                  <TextInput
                    label="Expiration Date"
                    name="driverLicenseExpirationDate"
                    value={form.driverLicenseExpirationDate}
                    onChange={handleChange}
                    type="date"
                    required
                    error={hasFieldError("driverLicenseExpirationDate")}
                  />
                  <FileInput
                    label="Driver's License *"
                    onChange={(file) => {
                      setDriverLicenseFile(file);
                      clearFieldError("driverLicenseFile");
                    }}
                    error={hasFieldError("driverLicenseFile")}
                  />
                </>
              )}
            </div>
          </Card>
        </div>

        <div className="onboarding-column">
          <Card
            className="onboarding-card"
            title={<CardTitle icon={<ContactsOutlined />}>Reference</CardTitle>}
          >
            <div className="onboarding-grid">
              <TextInput
                label="First Name"
                name="reference.firstName"
                value={form.reference.firstName}
                onChange={handleChange}
                error={hasFieldError("reference.firstName")}
              />
              <TextInput
                label="Last Name"
                name="reference.lastName"
                value={form.reference.lastName}
                onChange={handleChange}
                error={hasFieldError("reference.lastName")}
              />
              <TextInput
                label="Middle Name"
                name="reference.middleName"
                value={form.reference.middleName}
                onChange={handleChange}
              />
              <TextInput
                label="Phone"
                name="reference.phone"
                value={form.reference.phone}
                onChange={handleChange}
                error={hasFieldError("reference.phone")}
              />
              <TextInput
                label="Email"
                name="reference.email"
                value={form.reference.email}
                onChange={handleChange}
                type="email"
              />
              <TextInput
                label="Relationship"
                name="reference.relationship"
                value={form.reference.relationship}
                onChange={handleChange}
              />
            </div>
          </Card>

          <Card
            className="onboarding-card onboarding-card-wide"
            title={
              <CardTitle icon={<TeamOutlined />}>Emergency Contact</CardTitle>
            }
          >
            <div className="onboarding-list">
              {form.emergencyContacts.map((contact, index) => (
                <Card
                  className="onboarding-nested-card"
                  key={index}
                  size="small"
                  title={`Contact ${index + 1}`}
                >
                  <div className="onboarding-grid">
                    <TextInput
                      label="First Name"
                      name="firstName"
                      value={contact.firstName}
                      onChange={(event) =>
                        handleEmergencyContactChange(index, event)
                      }
                      required
                      error={hasFieldError(
                        `emergencyContacts.${index}.firstName`,
                      )}
                    />
                    <TextInput
                      label="Last Name"
                      name="lastName"
                      value={contact.lastName}
                      onChange={(event) =>
                        handleEmergencyContactChange(index, event)
                      }
                      required
                      error={hasFieldError(
                        `emergencyContacts.${index}.lastName`,
                      )}
                    />
                    <TextInput
                      label="Middle Name"
                      name="middleName"
                      value={contact.middleName}
                      onChange={(event) =>
                        handleEmergencyContactChange(index, event)
                      }
                    />
                    <TextInput
                      label="Phone"
                      name="phone"
                      value={contact.phone}
                      onChange={(event) =>
                        handleEmergencyContactChange(index, event)
                      }
                      required
                      error={hasFieldError(`emergencyContacts.${index}.phone`)}
                    />
                    <TextInput
                      label="Email"
                      name="email"
                      value={contact.email}
                      onChange={(event) =>
                        handleEmergencyContactChange(index, event)
                      }
                      type="email"
                    />
                    <TextInput
                      label="Relationship"
                      name="relationship"
                      value={contact.relationship}
                      onChange={(event) =>
                        handleEmergencyContactChange(index, event)
                      }
                      required
                      error={hasFieldError(
                        `emergencyContacts.${index}.relationship`,
                      )}
                    />
                  </div>

                  {form.emergencyContacts.length > 1 && (
                    <Button
                      danger
                      htmlType="button"
                      onClick={() => removeEmergencyContact(index)}
                    >
                      Remove Contact
                    </Button>
                  )}
                </Card>
              ))}
            </div>

            <Button
              type="primary"
              icon={<PlusOutlined />}
              htmlType="button"
              onClick={addEmergencyContact}
            >
              Add Emergency Contact
            </Button>
          </Card>

          <div className="onboarding-actions">
            <Button type="primary" htmlType="submit" loading={submitting}>
              {submitting ? "Submitting..." : "Submit"}
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
}
