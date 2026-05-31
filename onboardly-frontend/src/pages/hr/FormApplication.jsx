import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import {
  Avatar,
  Button,
  Card,
  Descriptions,
  Divider,
  Input,
  List,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from "antd";
import {
  ContactsOutlined,
  FileTextOutlined,
  HomeOutlined,
  IdcardOutlined,
  MessageOutlined,
  PhoneOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { DocumentPreviewModal, PageHeader } from "../../components/common";
//add by weihao
import { getEmployeeOnboardingForm, addApplicationComment, reviewApplication } from "../../services/onboardingService";

// import { getEmployeeOnboardingForm, addApplicationComment } from "../../services/onboardingService";
import { useAuth } from "../../auth/AuthContext"

const { Title, Text } = Typography;
const { TextArea } = Input;

// Mock API & Data
const USE_MOCK_DATA = false;

const MOCK_APPLICATION_DATA = {
  onboardingApplication: {
    id: "EMP-10293",
    status: "Pending Review",
    submittedDate: "2026-05-22",
  },
  firstName: "Jane",
  middleName: "Maria",
  lastName: "Doe",
  preferredName: "Jane",
  email: "jane.doe@example.com",
  ssn: "***-**-1234",
  dateOfBirth: "1992-08-14",
  gender: "FEMALE",
  cellPhone: "(555) 123-4567",
  workPhone: "",
  address: {
    type: "PRIMARY",
    addressLine1: "123 Tech Lane",
    addressLine2: "Apt 4B",
    city: "San Francisco",
    state: "CA",
    zipCode: "94105",
  },
  // isCitizenOrResident: "NO",
  // citizenOrGreenCard: "",
  citizenshipStatus: "NON_RESIDENT",
  workAuthorization: "H1B",
  otherWorkAuthorization: "",
  workAuthStartDate: "2024-01-01",
  workAuthEndDate: "2027-01-01",
  workAuthFileUrl: "/company-policy.pdf",
  // hasDriverLicense: "YES",
  driverLicense: "D12345678",
  driverLicenseExpirationDate: "2028-08-14",
  driverLicenseFileUrl: "/company-policy.pdf",
  avatarUrl: "https://api.dicebear.com/7.x/avataaars/svg?seed=Jane",
  reference: {
    firstName: "Alan",
    middleName: "",
    lastName: "Turing",
    phone: "(555) 987-6543",
    email: "alan@example.com",
    relationship: "Former Manager",
  },
  emergencyContacts: [
    {
      firstName: "John",
      middleName: "",
      lastName: "Doe",
      phone: "(555) 111-2222",
      email: "john.doe@example.com",
      relationship: "Spouse",
    },
  ],
};

const MOCK_EXISTING_COMMENTS = [
  {
    id: 1,
    author: "Alice HR",
    date: "2026-05-23 09:15 AM",
    content: "Application received. Everything looks in order, just need to verify the H1B document validity.",
  },
];

// Mock fetch function simulating network delay
const fetchApplicationById = (id) => {

  if (USE_MOCK_DATA) {return new Promise((resolve) => {
    setTimeout(() => {
      resolve(MOCK_APPLICATION_DATA);
    }, 1000);
  });}
};

const mapBackendToFrontendState = (backendDto) => {
  
  const primaryAddress = backendDto.addresses.find(
    (addr) => addr.type === 'PRIMARY'
  ) || {};

  const secondaryAddress = backendDto.addresses.find(
    (addr) => addr.type === 'SECONDARY'
  ) || null;

  return {
    ...backendDto,               // Keep all other fields (firstName, lastName, etc.)
    address: primaryAddress,     // The newly mapped primary address object
    secondaryAddress: secondaryAddress, // The newly mapped secondary address object
  };
};

// Main Component
export default function FormApplication() {
  const { id } = useParams(); // Retrieves employee ID from URL route, e.g., /hr/review/:id
  const { user } = useAuth(); // Get current hr info from auth context 

  const [loading, setLoading] = useState(true);
  const [application, setApplication] = useState(null);
  const [comments, setComments] = useState([]);
  //changed by weihao
  const [preview, setPreview] = useState({ open: false, url: null, name: null });
  const [newComment, setNewComment] = useState("");
  // ==================

  useEffect(() => {
    //test
    console.log('id:', id);
    setLoading(true);
    
    async function loadApplication(id) {
      if (USE_MOCK_DATA) {
        setApplication(MOCK_APPLICATION_DATA);
        setComments(MOCK_EXISTING_COMMENTS);
        setLoading(false);
        return;
      }

      try {
        const response = await getEmployeeOnboardingForm(id);
        const applicationData = mapBackendToFrontendState(response.data);
        //test
        // console.log('applicationData:', applicationData);

        setApplication(applicationData);
        setComments(applicationData.onboardingApplication.comments);
      } catch (error) {
        console.error("Error fetching application data:", error);
      } finally {
        setLoading(false);
      }
    }

    loadApplication(id);
    // Simulate fetching data on mount
    // fetchApplicationById(id).then((data) => {
    //   setApplication(data);
    //   setComments(MOCK_EXISTING_COMMENTS);
    //   setLoading(false);
    // });

  }, [id]);

  const handleAddComment = async() => {
    if (!newComment.trim()) return;

    if (USE_MOCK_DATA){
      const newCommentObj = {
        id: Date.now(),
        author: user?.username || "Current HR User", // Would come from your auth context
        date: new Date().toLocaleString(),
        content: newComment,
      };

      setComments([...comments, newCommentObj]);
      setNewComment("");
      message.success("Comment added to application.");
      return;
    }

    const commentToAdd = { content: newComment };

    try {
      const savedComment = await addApplicationComment(application.onboardingApplication.id, commentToAdd);
      const newCommentObj = {
        id: savedComment.data.id,
        authorId: savedComment.data.authorId,
        date: savedComment.data.createdAt,
        content: savedComment.data.content,
      };
      setComments([...comments, newCommentObj]);
      setNewComment("");
      message.success("Comment added to application.");
    } catch (error) {
      console.error("Error adding comment:", error);
      message.error("Failed to add comment.");
    }
  };

  // add by weihao
  const handleReject = async () => {
    //test
    console.log('reject clicked');
    const hrFeedback = window.prompt("Please provide a reason for rejection:");
    //test
    console.log('hrFeedback:', hrFeedback);
    if (!hrFeedback) return;

    try {
      await reviewApplication(application.onboardingApplication.id, {
        status: "REJECTED",
        hrFeedback,
      });
      message.success("Application rejected.");
    } catch (error) {
      console.error("Error rejecting application:", error);
      message.error("Failed to reject application.");
    }
  };

  const handleApprove = async () => {
    try {
      await reviewApplication(application.onboardingApplication.id, {
        status: "APPROVED",
        hrFeedback: "",
      });
      message.success(`Application for ${application.firstName} approved.`);
    } catch (error) {
      console.error("Error approving application:", error);
      message.error("Failed to approve application.");
    }
  };
//===========================
  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "100px" }}>
        <Spin size="large" tip="Loading Application Details..." />
      </div>
    );
  }

  if (!application) {
    return <div>Application not found.</div>;
  }

  return (
    <div style={{ maxWidth: 1000, margin: "0 auto", padding: "24px" }}>
      {/* Header Section */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: 24 }}>
        <div>
          <Title level={2} style={{ margin: 0 }}>Onboarding Review</Title>
          <Text type="secondary">
            Application ID: {application.onboardingApplication.id} | Submitted: {application.onboardingApplication.submittedAt}
          </Text>
        </div>
        <Space>
          <Tag color={application.onboardingApplication.status === "Pending Review" ? "gold" : "green"}>
            {application.onboardingApplication.status.toUpperCase()}
          </Tag>
          <Button type="primary" onClick={handleApprove}>Approve Application</Button>
          {/* add by weihao */}
          <Button danger onClick={handleReject}>Reject Application</Button>
        </Space>
      </div>

      <Space direction="vertical" size="large" style={{ width: "100%" }}>
        
        {/* Personal Info Card */}
        <Card title={<><UserOutlined /> Personal Information</>}>
          <div style={{ display: "flex", gap: "32px", alignItems: "flex-start" }}>
            <Avatar size={100} src={application.avatarUrl} icon={<UserOutlined />} />
            <Descriptions column={2} style={{ flex: 1 }}>
              <Descriptions.Item label="First Name">{application.firstName}</Descriptions.Item>
              <Descriptions.Item label="Last Name">{application.lastName}</Descriptions.Item>
              <Descriptions.Item label="Middle Name">{application.middleName || "N/A"}</Descriptions.Item>
              <Descriptions.Item label="Preferred Name">{application.preferredName || "N/A"}</Descriptions.Item>
            </Descriptions>
          </div>
        </Card>

        {/* Identity & Contact Card */}
        <Card title={<><IdcardOutlined /> Identity & Contact</>}>
          <Descriptions column={{ xxl: 3, xl: 3, lg: 2, md: 1, sm: 1, xs: 1 }} bordered size="small">
            <Descriptions.Item label="SSN">{application.ssn}</Descriptions.Item>
            <Descriptions.Item label="Date of Birth">{application.dateOfBirth}</Descriptions.Item>
            <Descriptions.Item label="Gender">{application.gender}</Descriptions.Item>
            <Descriptions.Item label="Cell Phone">{application.cellPhone}</Descriptions.Item>
            <Descriptions.Item label="Work Phone">{application.alternatePhone || "N/A"}</Descriptions.Item>
            <Descriptions.Item label="Email">{application.email}</Descriptions.Item>
          </Descriptions>
        </Card>

        {/* Address Card */}
        <Card title={<><HomeOutlined /> Current Address</>}>
          <Descriptions column={2}>
            <Descriptions.Item label="Address Line 1">{application.address.addressLine1}</Descriptions.Item>
            <Descriptions.Item label="Address Line 2">{application.address.addressLine2 || ""}</Descriptions.Item>
            <Descriptions.Item label="City">{application.address.city}</Descriptions.Item>
            <Descriptions.Item label="State/ZIP">
              {application.address.state} {application.address.zipCode}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* Work Authorization & Drivers License */}
        <Card title={<><SafetyCertificateOutlined /> Compliance & Documents</>}>
          <Descriptions column={2} layout="vertical" bordered size="small">
            <Descriptions.Item label="US Citizen/Resident?">
              <Tag color={application.citizenshipStatus === "CITIZEN" || application.citizenshipStatus === "GREEN_CARD" ? "green" : "blue"}>
                {application.citizenshipStatus}
              </Tag>
            </Descriptions.Item>

            {application.citizenshipStatus !== "NON_RESIDENT" ? (
              <Descriptions.Item label="Type">{application.citizenshipStatus}</Descriptions.Item>
            ) : (
              <>
                <Descriptions.Item label="Work Authorization Type">
                  {application.visaStatuses === "OTHER" ? application.otherWorkAuthorization : application.workAuthorization}
                </Descriptions.Item>
                <Descriptions.Item label="Valid Dates">
                  {application.workAuthStartDate} to {application.workAuthEndDate}
                </Descriptions.Item>
                <Descriptions.Item label="Authorization Document">
                  <Button type="link" icon={<FileTextOutlined />} size="small">View Document</Button>
                </Descriptions.Item>
              </>
            )}
          </Descriptions>

          <Divider />

          <Descriptions column={2} layout="vertical" bordered size="small">
            <Descriptions.Item label="Has Driver's License?">
              {/* changed ny weihao */}
              {application.driverLicense?.trim().length > 0 ? "YES" : "NO"}
            </Descriptions.Item>
            {/* changed by weihao */}
            {application.driverLicense?.trim().length > 0 && (
              <>
                <Descriptions.Item label="License Number">{application.driverLicense}</Descriptions.Item>
                <Descriptions.Item label="Expiration Date">{application.driverLicenseExpirationDate}</Descriptions.Item>
                <Descriptions.Item label="License Document">
                  <Button type="link" icon={<FileTextOutlined />} size="small">View Document</Button>
                </Descriptions.Item>
              </>
            )}
          </Descriptions>
        </Card>

        {/* Reference */}
        <Card title={<><ContactsOutlined /> Reference</>}>
          <Descriptions column={2}>
            <Descriptions.Item label="Name">
              {application.reference.firstName} {application.reference.middleName} {application.reference.lastName}
            </Descriptions.Item>
            <Descriptions.Item label="Relationship">{application.reference.relationship}</Descriptions.Item>
            <Descriptions.Item label="Phone">{application.reference.phone}</Descriptions.Item>
            <Descriptions.Item label="Email">{application.reference.email}</Descriptions.Item>
          </Descriptions>
        </Card>

        {/* Emergency Contacts */}
        <Card title={<><TeamOutlined /> Emergency Contacts</>}>
          {application.emergencyContacts.map((contact, index) => (
            <Card type="inner" title={`Contact ${index + 1}: ${contact.relationship}`} key={index} style={{ marginBottom: index !== application.emergencyContacts.length -1 ? 16 : 0 }}>
              <Descriptions column={2}>
                <Descriptions.Item label="Name">{contact.firstName} {contact.middleName} {contact.lastName}</Descriptions.Item>
                <Descriptions.Item label="Phone">{contact.phone}</Descriptions.Item>
                <Descriptions.Item label="Email">{contact.email || "N/A"}</Descriptions.Item>
              </Descriptions>
            </Card>
          ))}
        </Card>

        {/* HR Review & Comments Section */}
        <Card 
          title={<><MessageOutlined /> HR Review & Comments</>} 
          style={{ border: "1px solid #1677ff", background: "#f0f5ff" }}
        >
          <List
            className="comment-list"
            itemLayout="horizontal"
            dataSource={comments}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                // changed by weihao
                  avatar={<Avatar style={{ backgroundColor: '#1677ff' }}>{item.author?.charAt(0) || item.authorId || 'H'}</Avatar>}
                  title={<Text strong>{item.authorId} <Text type="secondary" style={{fontSize: 12, marginLeft: 8}}>{item.createdAt}</Text></Text>}
                  description={<Text style={{ color: '#000' }}>{item.content}</Text>}
                />
              </List.Item>
            )}
          />
          <div style={{ marginTop: 24 }}>
            <TextArea 
              rows={4} 
              value={newComment} 
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Add an internal note or review comment regarding this application..." 
            />
            <Button 
              type="primary" 
              onClick={handleAddComment} 
              style={{ marginTop: 16, float: "right" }}
            >
              Add Comment
            </Button>
            <div style={{ clear: 'both' }}></div>
          </div>
        </Card>

      </Space>
      
      <DocumentPreviewModal
        open={preview.open}
        title={preview.name || "Document Preview"}
        url={preview.url}
        onClose={() =>
          setPreview({
            open: false,
            url: null,
            name: null,
          })
        }
      />
    </div>
  );
}