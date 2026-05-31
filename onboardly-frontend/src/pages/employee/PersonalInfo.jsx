import { useEffect, useRef, useState } from "react";
import {
  Alert,
  Avatar,
  Button,
  Card,
  Input,
  Select,
  Space,
  Typography,
  Upload,
} from "antd";
import {
  EditOutlined,
  FileTextOutlined,
  HomeOutlined,
  IdcardOutlined,
  PhoneOutlined,
  UploadOutlined,
  UserOutlined,
} from "@ant-design/icons";
import {
  addEmployeeContact,
  deleteEmployeeContact,
  getEmployeeDocuments,
  getEmployeeProfile,
  updateEmployeeContact,
  updateEmployeeProfileSection,
  uploadEmployeeAvatar,
} from "../../services/employeeService";
import {
  downloadDocument,
  getDocumentPreviewUrl,
} from "../../services/onboardingService";
import { DocumentPreviewModal } from "../../components/common";
import defaultAvatar from "../../assets/default-avatar.png";
import "./PersonalInfo.css";
import { DatePicker } from "antd";
import dayjs from "dayjs";

const { Text, Title } = Typography;

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

// mockDocuments urls below are stale (real docs are fetched via presigned S3 URLs)
const USE_MOCK_DATA = false;

const mockProfile = {
  firstName: "John",
  middleName: "",
  lastName: "Doe",
  preferredName: "John",
  avatarUrl: "",
  dateOfBirth: "1998-01-01",
  age: 28,
  gender: "MALE",
  ssn: "123456789",
  personalEmail: "john@example.com",
  workEmail: "john.doe@onboardly.com",
  cellPhone: "123-456-7890",
  workPhone: "555-111-2222",
  workAuthorization: "OPT",
  workAuthStartDate: "2025-01-01",
  workAuthEndDate: "2026-01-01",
  employmentStartDate: "2025-02-01",
  employmentEndDate: "",
  primaryAddress: {
    line1: "123 Main St",
    line2: "Apt 4",
    city: "Los Angeles",
    state: "CA",
    zip: "90001",
  },
  secondaryAddress: null,
  emergencyContacts: [
    {
      firstName: "Jane",
      middleName: "",
      lastName: "Doe",
      relationship: "Sister",
      phone: "123-123-1234",
      email: "jane@example.com",
      address: "456 Oak Ave, Los Angeles, CA 90002",
    },
  ],
};

const mockDocuments = [
  {
    id: 1,
    name: "W-4 Form",
    url: "/templates/onboarding/fw4.pdf",
    createdDate: "2026-05-20",
  },
  {
    id: 2,
    name: "Company Policy Agreement",
    url: "/templates/onboarding/company-policy.pdf",
    createdDate: "2026-05-18",
  },
  {
    id: 3,
    name: "Direct Deposit Form",
    url: "/templates/onboarding/direct-deposit.pdf",
    createdDate: "2026-05-10",
  },
];

function maskSSN(ssn) {
  if (!ssn) return "N/A";
  return `***-**-${ssn.slice(-4)}`;
}

function Field({ label, value }) {
  const displayValue =
    value === null || value === undefined || value === "" ? "-" : value;

  return (
    <div className="pi-field">
      <Text type="secondary">{label}</Text>
      <Text strong>{displayValue}</Text>
    </div>
  );
}

function TextInput({
  label,
  name,
  value,
  onChange,
  type = "text",
  disabled = false,
}) {
  return (
    <label className="pi-input-group">
      <span>{label}</span>
      <Input
        name={name}
        type={type}
        value={value ?? ""}
        onChange={onChange}
        disabled={disabled}
      />
    </label>
  );
}

function AddressInputs({ prefix, address = {}, onChange }) {
  function handleChange(event) {
    const { name, value } = event.target;
    onChange(prefix, name, value);
  }

  return (
    <div className="pi-address-grid">
      <TextInput
        label="Address Line 1"
        name="line1"
        value={address?.line1}
        onChange={handleChange}
      />
      <TextInput
        label="Address Line 2"
        name="line2"
        value={address?.line2}
        onChange={handleChange}
      />
      <TextInput
        label="City"
        name="city"
        value={address?.city}
        onChange={handleChange}
      />

      <label className="pi-input-group">
        <span>State</span>
        <Select
          name="state"
          value={address?.state ?? ""}
          onChange={(value) =>
            handleChange({ target: { name: "state", value } })
          }
          options={[
            { value: "", label: "Select" },
            ...STATES.map((state) => ({ value: state, label: state })),
          ]}
        />
      </label>

      <TextInput
        label="ZIP"
        name="zip"
        value={address?.zip}
        onChange={handleChange}
      />
    </div>
  );
}

function AddressDisplay({ title, address }) {
  return (
    <div className="pi-address-display">
      <Title level={5}>{title}</Title>
      <div className="pi-grid">
        <Field label="Address Line 1" value={address?.line1} />
        <Field label="Address Line 2" value={address?.line2} />
        <Field label="City" value={address?.city} />
        <Field label="State" value={address?.state} />
        <Field label="ZIP" value={address?.zip} />
      </div>
    </div>
  );
}

function toUiAddress(address) {
  if (!address) return null;

  return {
    line1: address.addressLine1 || address.line1 || "",
    line2: address.addressLine2 || address.line2 || "",
    city: address.city || "",
    state: address.state || "",
    zip: address.zipCode || address.zip || "",
  };
}

function toAddressRequest(type, address) {
  return {
    type,
    addressLine1: address?.line1 || "",
    addressLine2: address?.line2 || "",
    city: address?.city || "",
    state: address?.state || "",
    zipCode: address?.zip || "",
  };
}

function toUiContact(contact) {
  return {
    ...contact,
    phone: contact.cellPhone || contact.phone || "",
  };
}

function toContactRequest(contact) {
  return {
    type: "EMERGENCY",
    firstName: contact?.firstName || "",
    lastName: contact?.lastName || "",
    middleName: contact?.middleName || null,
    cellPhone: contact?.phone || contact?.cellPhone || null,
    alternatePhone: contact?.alternatePhone || null,
    email: contact?.email || null,
    relationship: contact?.relationship || null,
    address: contact?.address || null,
  };
}

function validateEmergencyContacts(contacts = []) {
  const requiredFields = [
    ["firstName", "first name"],
    ["lastName", "last name"],
    ["phone", "phone"],
  ];

  for (const [index, contact] of contacts.entries()) {
    for (const [field, label] of requiredFields) {
      if (!String(contact?.[field] || "").trim()) {
        return `Emergency contact ${index + 1} is missing ${label}.`;
      }
    }

    const digits = String(contact.phone || "").replace(/\D/g, "");

    if (digits.length < 10 || digits.length > 15) {
      return `Emergency contact ${index + 1} phone must be 10 to 15 digits.`;
    }
  }

  return "";
}

function getActiveVisaStatus(profile) {
  const visaStatuses = profile?.visaStatuses || [];
  return (
    visaStatuses.find((visa) => visa.activeFlag) ||
    visaStatuses.find((visa) => visa.activeFlag !== false) ||
    null
  );
}

function getWorkAuthorizationLabel(visaStatus) {
  if (!visaStatus) return "";
  return visaStatus.visaType === "OTHER"
    ? visaStatus.visaTypeOther || visaStatus.visaType
    : visaStatus.visaType;
}

function calculateAge(dateOfBirth) {
  console.log("calculateAge input:", dateOfBirth, typeof dateOfBirth);
  if (!dateOfBirth) return "";

  const [year, month, day] = String(dateOfBirth)
    .split("T")[0]
    .split("-")
    .map(Number);
  console.log("parsed:", { year, month, day });

  const birthDate = new Date(year, month - 1, day);
  const today = new Date();
  console.log("birthDate:", birthDate.toString(), "today:", today.toString());

  if (isNaN(birthDate.getTime()) || birthDate > today) return "";

  let age = today.getFullYear() - birthDate.getFullYear();
  const birthdayPassed =
    today.getMonth() > birthDate.getMonth() ||
    (today.getMonth() === birthDate.getMonth() &&
      today.getDate() >= birthDate.getDate());

  const result = birthdayPassed ? age : age - 1;
  console.log("age result:", result);
  return result;
}

function toPersonalRequest(draft, fallback = {}) {
  return {
    email:
      draft.workEmail ||
      draft.email ||
      fallback.workEmail ||
      fallback.email ||
      "",
    personalEmail: draft.personalEmail || null,
    cellPhone: draft.cellPhone || null,
    alternatePhone: draft.alternatePhone || null,
    workPhone: draft.workPhone || null,
    gender: draft.gender || null,
    dateOfBirth: draft.dateOfBirth || null,
  };
}

function normalizeProfile(profile) {
  const addresses = profile?.addresses || [];
  const contacts = profile?.contacts || [];
  const activeVisaStatus = getActiveVisaStatus(profile);

  const primaryAddress = addresses.find(
    (address) => address.type === "PRIMARY",
  );
  const secondaryAddress = addresses.find(
    (address) => address.type === "SECONDARY",
  );

  const dateOfBirth =
    profile?.dateOfBirth || profile?.dob || profile?.birthDate || "";

  return {
    ...profile,
    dateOfBirth,
    workEmail: profile?.email || profile?.workEmail || "",
    personalEmail: profile?.personalEmail || "",
    workPhone: profile?.workPhone || "",
    workAuthorization:
      profile?.workAuthorization || getWorkAuthorizationLabel(activeVisaStatus),
    workAuthStartDate:
      profile?.workAuthStartDate || activeVisaStatus?.startDate || "",
    workAuthEndDate:
      profile?.workAuthEndDate || activeVisaStatus?.endDate || "",
    primaryAddress: toUiAddress(primaryAddress || profile?.primaryAddress),
    secondaryAddress: toUiAddress(
      secondaryAddress || profile?.secondaryAddress,
    ),
    emergencyContacts: contacts.length
      ? contacts
          .filter((contact) => contact.type === "EMERGENCY")
          .map(toUiContact)
      : (profile?.emergencyContacts || []).map(toUiContact),
  };
}

function getFullName(person) {
  return (
    [person.firstName, person.middleName, person.lastName]
      .filter(Boolean)
      .join(" ") || "Unnamed contact"
  );
}

function CardTitle({ icon, text }) {
  return (
    <span className="pi-card-title">
      {icon}
      <span>{text}</span>
    </span>
  );
}

function getDocumentId(document) {
  return document.id || document.documentId;
}

function getDocumentUrl(document) {
  return (
    document.url ||
    document.s3Url ||
    document.previewUrl ||
    document.downloadUrl
  );
}

function getPreviewUrl(response) {
  const data = response?.data;

  if (typeof data === "string") return data;

  return (
    data?.url ||
    data?.previewUrl ||
    data?.s3Url ||
    data?.downloadUrl ||
    data?.data ||
    ""
  );
}

function downloadBlob(blob, fileName = "document") {
  const blobUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = blobUrl;
  link.download = fileName;
  link.click();

  URL.revokeObjectURL(blobUrl);
}

function Section({
  title,
  sectionKey,
  editing,
  onEdit,
  onSave,
  onCancel,
  children,
}) {
  const sectionClass = sectionKey || title.toLowerCase().replace(/\s+/g, "-");

  return (
    <Card
      className={`pi-section pi-section-${sectionClass}`}
      title={title}
      extra={
        editing ? (
          <Space>
            <Button type="primary" onClick={onSave}>
              Save
            </Button>
            <Button onClick={onCancel}>Cancel</Button>
          </Space>
        ) : (
          <Button icon={<EditOutlined />} size="small" onClick={onEdit}>
            Edit
          </Button>
        )
      }
    >
      {children}
    </Card>
  );
}

export default function PersonalInfo() {
  const [profile, setProfile] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [editingSection, setEditingSection] = useState(null);
  const [draft, setDraft] = useState({});
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [preview, setPreview] = useState({
    open: false,
    url: null,
    name: null,
  });

  // Staged avatar (File + its blob: preview URL) lives outside React state so
  // the previous blob URL can be revoked synchronously when replaced or cleared.
  const stagedAvatarRef = useRef(null);

  function clearStagedAvatar() {
    if (stagedAvatarRef.current?.blobUrl) {
      URL.revokeObjectURL(stagedAvatarRef.current.blobUrl);
    }
    stagedAvatarRef.current = null;
  }

  useEffect(() => () => clearStagedAvatar(), []);

  useEffect(() => {
    let active = true;

    async function loadPersonalInfo() {
      setLoading(true);
      setStatus("");

      if (USE_MOCK_DATA) {
        setProfile(mockProfile);
        setDocuments(mockDocuments);
        setLoading(false);
        return;
      }

      try {
        const [profileResponse, documentResponse] = await Promise.all([
          getEmployeeProfile(),
          getEmployeeDocuments(),
        ]);

        if (!active) return;

        setProfile(normalizeProfile(profileResponse.data));
        setDocuments(documentResponse.data || []);
      } catch {
        if (active) setStatus("Failed to load personal information.");
      } finally {
        if (active) setLoading(false);
      }
    }

    loadPersonalInfo();

    return () => {
      active = false;
    };
  }, []);

  function startEdit(section) {
    setEditingSection(section);
    setDraft(profile);
    setStatus("");
  }

  function cancelEdit() {
    if (!window.confirm("Are you sure you want to discard all your changes?")) {
      return;
    }

    clearStagedAvatar();
    setDraft({});
    setEditingSection(null);
  }

  function handleDraftChange(event) {
    const { name, value } = event.target;
    setDraft((current) => ({ ...current, [name]: value }));
  }

  function handleAvatarUpload(file) {
    if (stagedAvatarRef.current?.blobUrl) {
      URL.revokeObjectURL(stagedAvatarRef.current.blobUrl);
    }
    const blobUrl = URL.createObjectURL(file);
    stagedAvatarRef.current = { file, blobUrl };

    setDraft((current) => ({
      ...current,
      avatarUrl: blobUrl,
    }));

    return false;
  }

  function handleAddressChange(addressKey, field, value) {
    setDraft((current) => ({
      ...current,
      [addressKey]: {
        ...(current[addressKey] || {}),
        [field]: value,
      },
    }));
  }

  function handleEmergencyContactChange(index, event) {
    const { name, value } = event.target;

    setDraft((current) => ({
      ...current,
      emergencyContacts: (current.emergencyContacts || []).map(
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
    setDraft((current) => ({
      ...current,
      emergencyContacts: [
        ...(current.emergencyContacts || []),
        {
          firstName: "",
          middleName: "",
          lastName: "",
          phone: "",
          email: "",
          relationship: "",
          address: "",
        },
      ],
    }));
  }

  function removeEmergencyContact(index) {
    setDraft((current) => ({
      ...current,
      emergencyContacts: (current.emergencyContacts || []).filter(
        (_, contactIndex) => contactIndex !== index,
      ),
    }));
  }

  async function saveSection(section, fields) {
    let values = fields.reduce((payload, field) => {
      payload[field] = draft[field];
      return payload;
    }, {});

    if (USE_MOCK_DATA) {
      setProfile((current) => ({
        ...current,
        ...values,
      }));
      setEditingSection(null);
      setDraft({});
      setStatus("Saved successfully.");
      return;
    }

    try {
      const employeeId = profile.id || profile.employeeId;

      if (!employeeId) {
        setStatus("Cannot save changes because employee ID is missing.");
        return;
      }

      let response;

      if (section === "name") {
        values = {
          firstName: draft.firstName,
          lastName: draft.lastName,
          middleName: draft.middleName || null,
          preferredName: draft.preferredName || null,
        };

        response = await updateEmployeeProfileSection(
          employeeId,
          section,
          values,
        );
        response = await updateEmployeeProfileSection(
          employeeId,
          "personal",
          toPersonalRequest(draft, profile),
        );

        if (stagedAvatarRef.current?.file) {
          const avatarResponse = await uploadEmployeeAvatar(
            employeeId,
            stagedAvatarRef.current.file,
          );
          const newAvatarUrl =
            avatarResponse.data?.url || avatarResponse.data || null;
          if (newAvatarUrl) {
            response = {
              ...response,
              data: { ...(response?.data ?? {}), avatarUrl: newAvatarUrl },
            };
          }
          clearStagedAvatar();
        }
      } else if (section === "address") {
        response = await updateEmployeeProfileSection(
          employeeId,
          section,
          toAddressRequest("PRIMARY", draft.primaryAddress),
        );

        if (
          draft.secondaryAddress?.line1 ||
          draft.secondaryAddress?.city ||
          draft.secondaryAddress?.state ||
          draft.secondaryAddress?.zip
        ) {
          response = await updateEmployeeProfileSection(
            employeeId,
            section,
            toAddressRequest("SECONDARY", draft.secondaryAddress),
          );
        }
      } else if (section === "contact") {
        response = await updateEmployeeProfileSection(
          employeeId,
          "personal",
          toPersonalRequest(draft, profile),
        );
      } else if (section === "employment") {
        const contactError = validateEmergencyContacts(
          draft.emergencyContacts || [],
        );

        if (contactError) {
          setStatus(contactError);
          return;
        }

        values = {
          employmentStartDate: draft.employmentStartDate || null,
          employmentEndDate: draft.employmentEndDate || null,
          driverLicense: draft.driverLicense || null,
          driverLicenseExpiration: draft.driverLicenseExpiration || null,
          houseId: draft.houseId || null,
        };

        response = await updateEmployeeProfileSection(
          employeeId,
          section,
          values,
        );

        const originalContacts = profile.emergencyContacts || [];
        const nextContacts = draft.emergencyContacts || [];
        const nextIds = new Set(
          nextContacts.map((contact) => contact.id).filter(Boolean),
        );

        for (const contact of nextContacts) {
          const payload = toContactRequest(contact);

          if (contact.id) {
            response = await updateEmployeeContact(
              employeeId,
              contact.id,
              payload,
            );
          } else {
            response = await addEmployeeContact(employeeId, payload);
          }
        }

        for (const contact of originalContacts) {
          if (contact.id && !nextIds.has(contact.id)) {
            response = await deleteEmployeeContact(employeeId, contact.id);
          }
        }
      } else {
        response = await updateEmployeeProfileSection(
          employeeId,
          section,
          values,
        );
      }

      setProfile((current) =>
        normalizeProfile({
          ...current,
          ...response.data,
          dateOfBirth:
            draft.dateOfBirth ||
            response.data?.dateOfBirth ||
            current.dateOfBirth,
          gender: draft.gender || response.data?.gender || current.gender,
          age: calculateAge(
            draft.dateOfBirth ||
              response.data?.dateOfBirth ||
              current.dateOfBirth,
          ),
        }),
      );

      setEditingSection(null);
      setDraft({});
      setStatus("Saved successfully.");
    } catch {
      setStatus("Failed to save changes.");
    }
  }

  async function handlePreviewDocument(document) {
    const directUrl = getDocumentUrl(document);

    if (directUrl) {
      setPreview({
        open: true,
        url: directUrl,
        name: document.title,
      });
      return;
    }

    const documentId = getDocumentId(document);

    if (!documentId) {
      setStatus("No document preview available.");
      return;
    }

    try {
      const response = await getDocumentPreviewUrl(documentId);
      const previewUrl = getPreviewUrl(response);

      if (!previewUrl) {
        setStatus("No document preview available.");
        return;
      }

      setPreview({
        open: true,
        url: previewUrl,
        name: document.title,
      });
    } catch {
      setStatus("Failed to load document preview.");
    }
  }

  async function handleDownloadDocument(document) {
    const directUrl = getDocumentUrl(document);

    if (directUrl) {
      return;
    }

    const documentId = getDocumentId(document);

    if (!documentId) {
      setStatus("No document download available.");
      return;
    }

    try {
      const response = await downloadDocument(documentId);
      downloadBlob(response.data, document.title || "document");
    } catch {
      setStatus("Failed to download document.");
    }
  }

  if (loading) {
    return <div className="pi-page">Loading...</div>;
  }

  if (!profile) {
    return <div className="pi-page">{status || "No profile found."}</div>;
  }

  const fullName = [profile.firstName, profile.middleName, profile.lastName]
    .filter(Boolean)
    .join(" ");

  const sortedDocuments = [...documents].sort(
    (a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0),
  );

  return (
    <div className="pi-page">
      {/* Title row is outside the column container so it always stays at the top */}
      <div className="pi-title-row">
        <div className="pi-profile-summary">
          <Avatar
            size={72}
            src={profile.avatarUrl || defaultAvatar}
            icon={<UserOutlined />}
          />
          <div className="pi-profile-summary-main">
            <Title level={2}>{fullName || "Personal Information"}</Title>
            <Text type="secondary">
              {profile.email || profile.personalEmail || "No email on file"}
            </Text>
          </div>
          <div className="pi-profile-summary-meta">
            <Text type="secondary">Employment Start</Text>
            <Text strong>{profile.employmentStartDate || "-"}</Text>
          </div>
        </div>
        {status && <Alert message={status} type="info" showIcon />}
      </div>

      <div className="pi-columns">
        {/* Left column: Name, Contact Info */}
        <div className="pi-col">
          <Section
            sectionKey="name"
            title={<CardTitle icon={<UserOutlined />} text="Name" />}
            editing={editingSection === "name"}
            onEdit={() => startEdit("name")}
            onCancel={cancelEdit}
            onSave={() =>
              saveSection("name", [
                "firstName",
                "lastName",
                "middleName",
                "preferredName",
                "dateOfBirth",
                "gender",
              ])
            }
          >
            {editingSection === "name" ? (
              <>
                <div className="pi-avatar-edit">
                  <Avatar
                    size={72}
                    src={draft.avatarUrl || defaultAvatar}
                    icon={<UserOutlined />}
                  />
                  <Upload
                    accept="image/*"
                    beforeUpload={handleAvatarUpload}
                    maxCount={1}
                    showUploadList={false}
                  >
                    <Button icon={<UploadOutlined />}>Upload Avatar</Button>
                  </Upload>
                </div>

                <div className="pi-grid">
                  <TextInput
                    label="First Name"
                    name="firstName"
                    value={draft.firstName}
                    onChange={handleDraftChange}
                  />
                  <TextInput
                    label="Last Name"
                    name="lastName"
                    value={draft.lastName}
                    onChange={handleDraftChange}
                  />
                  <TextInput
                    label="Middle Name"
                    name="middleName"
                    value={draft.middleName}
                    onChange={handleDraftChange}
                  />
                  <TextInput
                    label="Preferred Name"
                    name="preferredName"
                    value={draft.preferredName}
                    onChange={handleDraftChange}
                  />
                  <label className="pi-input-group">
                    <span>Date of Birth</span>
                    <DatePicker
                      value={
                        draft.dateOfBirth ? dayjs(draft.dateOfBirth) : null
                      }
                      onChange={(date, dateString) =>
                        handleDraftChange({
                          target: { name: "dateOfBirth", value: dateString },
                        })
                      }
                      format="YYYY-MM-DD"
                    />
                  </label>

                  <label className="pi-input-group">
                    <span>Gender</span>
                    <Select
                      value={draft.gender ?? ""}
                      onChange={(value) =>
                        handleDraftChange({
                          target: { name: "gender", value },
                        })
                      }
                      options={[
                        { value: "", label: "Select" },
                        { value: "MALE", label: "Male" },
                        { value: "FEMALE", label: "Female" },
                        { value: "OTHER", label: "Other" },
                        {
                          value: "PREFER_NOT_TO_SAY",
                          label: "Prefer not to say",
                        },
                      ]}
                    />
                  </label>
                </div>
              </>
            ) : (
              <div className="pi-grid">
                <Field label="Legal Name" value={fullName} />
                <Field label="Preferred Name" value={profile.preferredName} />
                <Field label="Date of Birth" value={profile.dateOfBirth} />
                <Field
                  label="Age"
                  value={
                    profile.dateOfBirth
                      ? calculateAge(profile.dateOfBirth)
                      : "-"
                  }
                />
                <Field label="Gender" value={profile.gender} />
                <Field label="SSN" value={maskSSN(profile.ssn)} />
              </div>
            )}
          </Section>

          <Section
            sectionKey="contact-info"
            title={<CardTitle icon={<PhoneOutlined />} text="Contact Info" />}
            editing={editingSection === "contact"}
            onEdit={() => startEdit("contact")}
            onCancel={cancelEdit}
            onSave={() =>
              saveSection("contact", [
                "personalEmail",
                "workEmail",
                "cellPhone",
                "workPhone",
              ])
            }
          >
            {editingSection === "contact" ? (
              <div className="pi-grid">
                <TextInput
                  label="Personal Email"
                  name="personalEmail"
                  type="email"
                  value={draft.personalEmail}
                  onChange={handleDraftChange}
                />
                <TextInput
                  label="Work Email"
                  name="workEmail"
                  type="email"
                  value={draft.workEmail}
                  onChange={handleDraftChange}
                />
                <TextInput
                  label="Cell Phone"
                  name="cellPhone"
                  value={draft.cellPhone}
                  onChange={handleDraftChange}
                />
                <TextInput
                  label="Work Phone"
                  name="workPhone"
                  value={draft.workPhone}
                  onChange={handleDraftChange}
                />
              </div>
            ) : (
              <div className="pi-grid">
                <Field label="Personal Email" value={profile.personalEmail} />
                <Field label="Work Email" value={profile.workEmail} />
                <Field label="Cell Phone" value={profile.cellPhone} />
                <Field label="Work Phone" value={profile.workPhone} />
              </div>
            )}
          </Section>
        </div>

        {/* Center column: Address */}
        <div className="pi-col">
          <Section
            sectionKey="address"
            title={<CardTitle icon={<HomeOutlined />} text="Address" />}
            editing={editingSection === "address"}
            onEdit={() => startEdit("address")}
            onCancel={cancelEdit}
            onSave={() =>
              saveSection("address", ["primaryAddress", "secondaryAddress"])
            }
          >
            {editingSection === "address" ? (
              <>
                <Title level={5}>Primary Address</Title>
                <AddressInputs
                  prefix="primaryAddress"
                  address={draft.primaryAddress}
                  onChange={handleAddressChange}
                />

                <Title level={5}>Secondary Address</Title>
                <AddressInputs
                  prefix="secondaryAddress"
                  address={draft.secondaryAddress}
                  onChange={handleAddressChange}
                />
              </>
            ) : (
              <>
                <AddressDisplay
                  title="Primary Address"
                  address={profile.primaryAddress}
                />
                <AddressDisplay
                  title="Secondary Address"
                  address={profile.secondaryAddress}
                />
              </>
            )}
          </Section>
        </div>

        {/* Right column: Employment, Documents */}
        <div className="pi-col">
          <Section
            sectionKey="employment"
            title={<CardTitle icon={<IdcardOutlined />} text="Employment" />}
            editing={editingSection === "employment"}
            onEdit={() => startEdit("employment")}
            onCancel={cancelEdit}
            onSave={() =>
              saveSection("employment", [
                "workAuthorization",
                "workAuthStartDate",
                "workAuthEndDate",
                "employmentStartDate",
                "employmentEndDate",
                "emergencyContacts",
              ])
            }
          >
            {editingSection === "employment" ? (
              <>
                <div className="pi-grid">
                  <TextInput
                    label="Work Authorization"
                    name="workAuthorization"
                    value={draft.workAuthorization}
                    disabled
                    onChange={handleDraftChange}
                  />
                  <TextInput
                    label="Work Authorization Start Date"
                    name="workAuthStartDate"
                    type="date"
                    value={draft.workAuthStartDate}
                    disabled
                    onChange={handleDraftChange}
                  />
                  <TextInput
                    label="Work Authorization End Date"
                    name="workAuthEndDate"
                    type="date"
                    value={draft.workAuthEndDate}
                    disabled
                    onChange={handleDraftChange}
                  />
                  <TextInput
                    label="Employment Start Date"
                    name="employmentStartDate"
                    type="date"
                    value={draft.employmentStartDate}
                    onChange={handleDraftChange}
                  />
                  <TextInput
                    label="Employment End Date"
                    name="employmentEndDate"
                    type="date"
                    value={draft.employmentEndDate}
                    onChange={handleDraftChange}
                  />
                </div>

                <Title level={5}>Emergency Contacts</Title>

                <div className="pi-list">
                  {(draft.emergencyContacts || []).map((contact, index) => (
                    <div className="pi-list-item" key={index}>
                      <div className="pi-grid">
                        <TextInput
                          label="First Name"
                          name="firstName"
                          value={contact.firstName}
                          onChange={(event) =>
                            handleEmergencyContactChange(index, event)
                          }
                        />
                        <TextInput
                          label="Last Name"
                          name="lastName"
                          value={contact.lastName}
                          onChange={(event) =>
                            handleEmergencyContactChange(index, event)
                          }
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
                        />
                        <TextInput
                          label="Email"
                          name="email"
                          type="email"
                          value={contact.email}
                          onChange={(event) =>
                            handleEmergencyContactChange(index, event)
                          }
                        />
                        <TextInput
                          label="Relationship"
                          name="relationship"
                          value={contact.relationship}
                          onChange={(event) =>
                            handleEmergencyContactChange(index, event)
                          }
                        />
                        <TextInput
                          label="Address"
                          name="address"
                          value={contact.address}
                          onChange={(event) =>
                            handleEmergencyContactChange(index, event)
                          }
                        />
                      </div>

                      {(draft.emergencyContacts || []).length > 1 && (
                        <Button
                          type="button"
                          onClick={() => removeEmergencyContact(index)}
                        >
                          Remove Contact
                        </Button>
                      )}
                    </div>
                  ))}
                </div>

                <Button type="button" onClick={addEmergencyContact}>
                  Add Emergency Contact
                </Button>
              </>
            ) : (
              <div className="pi-grid">
                <Field
                  label="Work Authorization"
                  value={profile.workAuthorization}
                />
                <Field
                  label="Work Authorization Start Date"
                  value={profile.workAuthStartDate}
                />
                <Field
                  label="Work Authorization End Date"
                  value={profile.workAuthEndDate}
                />
                <Field
                  label="Employment Start Date"
                  value={profile.employmentStartDate}
                />
                <Field
                  label="Employment End Date"
                  value={profile.employmentEndDate}
                />
              </div>
            )}

            {editingSection !== "employment" && (
              <>
                <Title level={5}>Emergency Contacts</Title>

                {(profile.emergencyContacts || []).length === 0 ? (
                  <p className="pi-muted">No emergency contacts</p>
                ) : (
                  <div className="pi-list">
                    {profile.emergencyContacts.map((contact, index) => (
                      <div
                        className="pi-list-item"
                        key={`${contact.email || contact.phone || index}`}
                      >
                        <div className="pi-grid">
                          <Field
                            label="Full Name"
                            value={getFullName(contact)}
                          />
                          <Field label="Phone" value={contact.phone} />
                          <Field label="Email" value={contact.email} />
                          <Field
                            label="Relationship"
                            value={contact.relationship}
                          />
                          <Field label="Address" value={contact.address} />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </Section>

          <Card
            className="pi-section pi-section-documents"
            title={<CardTitle icon={<FileTextOutlined />} text="Documents" />}
          >
            {sortedDocuments.length === 0 ? (
              <p className="pi-muted">No documents uploaded</p>
            ) : (
              <div className="pi-list">
                {sortedDocuments.map((document) => {
                  const url = getDocumentUrl(document);
                  const documentId = getDocumentId(document);

                  return (
                    <div
                      className="pi-document"
                      key={documentId || url || document.title}
                    >
                      <button
                        type="button"
                        className="pi-document-main"
                        onClick={() => handlePreviewDocument(document)}
                      >
                        <span>
                          <FileTextOutlined /> {document.title || "Document"}
                        </span>
                        <span>
                          {document.createdAt
                            ? new Date(document.createdAt).toLocaleDateString()
                            : "-"}
                        </span>
                      </button>

                      {url ? (
                        <a className="pi-download" href={url} download>
                          Download
                        </a>
                      ) : (
                        <Button
                          type="link"
                          className="pi-download"
                          disabled={!documentId}
                          onClick={() => handleDownloadDocument(document)}
                        >
                          Download
                        </Button>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </Card>
        </div>
      </div>

      <DocumentPreviewModal
        open={preview.open}
        title={preview.name || "Document"}
        url={preview.url}
        onClose={() => setPreview({ open: false, url: null, name: null })}
      />
    </div>
  );
}
