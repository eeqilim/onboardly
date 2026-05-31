import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
// changed by weihao
import { Table, Button, Form, Input, Card, Space, Typography, Tag, message } from 'antd';
import { SendOutlined, FormOutlined, FileOutlined } from '@ant-design/icons';
import { generateRegistrationToken } from '../../services/authService';
import { getPendingOnboardingApplications } from '../../services/onboardingService';
import { getEmployeeProfiles } from '../../services/employeeService';
import { getApplicationsByType } from '../../services/applicationService';
// add by weihao
import { reviewApplication } from '../../services/onboardingService';
//======================

const { Title } = Typography;

const USE_MOCK_DATA = false;

const employeeForms = [
  { employeeId: 1, applicantName: 'John Doe', applicationForm: 'Link to form', supportingDocumentation: 'Link to documents', applicationStatus: 'Open' },
  { employeeId: 2, applicantName: 'Jane Smith', applicationForm: 'Link to form', supportingDocumentation: 'Link to documents', applicationStatus: 'Completed' },
  { employeeId: 3, applicantName: 'Michael Johnson', applicationForm: 'Link to form', supportingDocumentation: 'Link to documents', applicationStatus: 'Rejected' },
];

const getStatusColor = (status) => {
  switch (status) {
    case 'Completed':
      return 'green';
    case 'Rejected':
      return 'red';
    case 'Open':
      return 'orange';
    default:
      return 'blue';
  }
};

const HiringManagement = () => {
  const { user } = useAuth();
  const [form] = Form.useForm();
  // const [employees, setEmployees] = useState([]);
  // const [applications, setApplications] = useState([]);
  const [tableData, setTableData] = useState([]);
  const [loading, setLoading] = useState(false);
  
  const [reviewForm] = Form.useForm();
  const [isReviewModalVisible, setIsReviewModalVisible] = useState(false);

  // Tracks the current action: { employeeId: string, action: 'APPROVED' | 'REJECTED' }
  const [reviewContext, setReviewContext] = useState(null); 
  const [submitting, setSubmitting] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    const fetchApplications = async () => {
      setLoading(true);
      if (USE_MOCK_DATA) {
        setTableData(employeeForms);
        setLoading(false);
        return;
      }

      try {
        const [applicationResponse, employeeResponse] = await Promise.all([
          getPendingOnboardingApplications(),
        // const applicationResponse = await getApplicationsByType('ONBOARDING');
          getEmployeeProfiles()
        ]);

        const applications = applicationResponse.data; 
        const employees = employeeResponse.data.content;
        
        //test
        console.log('employeeResponse.data:', employeeResponse.data);

        const mergedData = applications.map((app) => {
          // Find the employee whose ID matches the application's employeeId
          const matchingEmployee = employees.find((emp) => emp.id === app.employeeId);

          return {
            applicationId: app.id,
            employeeId: app.employeeId,
            // Fallback to empty object if employee is missing to prevent crashes
            userId: matchingEmployee?.userId, 
            
            // Format the applicant name
            applicantName: matchingEmployee 
              ? `${matchingEmployee.firstName} ${matchingEmployee.lastName}`
              : 'Unknown Applicant',
              
            // Transform 'PENDING' to 'Open', otherwise keep the original status
            applicationStatus: app.status === 'PENDING' ? 'Open' : app.status,
          };
        });

        // Set the finalized data directly into the table state
        setTableData(mergedData);

      }
      catch (error) {
        console.error('Error fetching onboarding applications:', error);
      }
      finally {
        setLoading(false);
      }
    }

    fetchApplications();
  }, []);
  //changed by weihao
  const handleApprove = async (record) => {
    try {
      await reviewApplication(record.applicationId, {
        status: "APPROVED",
        hrFeedback: "",
      });
      message.success("Application approved.");
      setTableData((prev) => prev.filter((item) => item.applicationId !== record.applicationId));

    } catch (error) {
      console.error("Error approving application:", error);
      message.error("Failed to approve application.");
    }
  };
// changed by weihao
  const handleReject = async (record) => {
    const hrFeedback = window.prompt("Please provide a reason for rejection:");
    if (!hrFeedback) return;

    try {
      await reviewApplication(record.applicationId, {
        status: "REJECTED",
        hrFeedback,
      });
      message.success("Application rejected.");
      setTableData((prev) => prev.filter((item) => item.applicationId !== record.applicationId));

    } catch (error) {
      console.error("Error rejecting application:", error);
      message.error("Failed to reject application.");
    }
  };

  const handleSubmit = async (values) => {
    try {
      console.log('Generate token and send email to:', values.employeeEmail);
      await generateRegistrationToken({ email: values.employeeEmail, createdById: user.userId });
      form.resetFields();
    } catch (error) {
      console.error('Error generating registration token:', error);
    }
  };

  const handleViewApplication = (record) => {
    navigate(`/hr/form-application/${record.employeeId}`);
  };

  const handleViewDocuments = (record) => {  
    navigate(`/hr/received-documents/${record.employeeId}`, { state: { source: 'hiring'}});
  };

  const showReviewModal = (employeeId, action) => {
    setReviewContext({ employeeId, action });
    setIsReviewModalVisible(true);
  };

  const handleModalSubmit = async () => {
    try {
      // Validates the form and grabs the text area input
      const values = await form.validateFields();
      
      setSubmitting(true);

      // This object maps to ReviewApplicationRequest DTO
      const payload = {
        status: reviewContext.action,
        hrFeedback: values.hrFeedback || "", // Fallback to empty string if no comment
      };

      await reviewOnboardingApplication(reviewContext.employeeId, payload);
      console.log(`Sending to API for Employee ${reviewContext.employeeId}:`, payload);

      message.success(`Application successfully ${reviewContext.action.toLowerCase()}.`);
      
      // Cleanup and close
      setIsReviewModalVisible(false);
      form.resetFields();
      setReviewContext(null);
      
      // Update the local table data to reflect the change immediately
      setTableData((currentData) =>
        currentData.map((item) =>
          item.employeeId === reviewContext.employeeId
            ? { ...item, applicationStatus: reviewContext.action === 'APPROVED' ? 'Completed' : 'Rejected' }
            : item
        )
      );      
      
    } catch (error) {
      // Triggers if form validation fails (e.g., they didn't provide required feedback)
      // or if the API call throws an error.
      console.error("Submission failed:", error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleModalCancel = () => {
    setIsReviewModalVisible(false);
    form.resetFields();
    setReviewContext(null);
  };

  const columns = [
    {
      title: 'Applicant Name',
      dataIndex: 'applicantName',
      key: 'applicantName',
      sorter: (a, b) => a.applicantName.localeCompare(b.applicantName),
    },
    {
      title: 'Application Form',
      dataIndex: 'applicationForm',
      key: 'applicationForm',
      render: (_, record) => (
        <Button
          type="link"
          icon={<FormOutlined />}
          onClick={() => handleViewApplication(record)}
        >
          View Form
        </Button>
      ),
    },
    {
      title: 'Supporting Documentation',
      dataIndex: 'supportingDocumentation',
      key: 'supportingDocumentation',
      render: (_, record) => (
        <Button
          type="link"
          icon={<FileOutlined />}
          onClick={() => handleViewDocuments(record)}
        >
          View Documents
        </Button>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'applicationStatus',
      key: 'applicationStatus',
      render: (status) => <Tag color={getStatusColor(status)}>{status}</Tag>,
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) =>
        record.applicationStatus === 'Open' ? (
          <Space size="small">
            {/* changed by weihao */}
            <Button type="primary" size="small" onClick={() => handleApprove(record)}>
              Approve
            </Button>
            {/* changed by weihao */}
            <Button danger size="small" onClick={() => handleReject(record)}>
              Reject
            </Button>
          </Space>
        ) : null,
    },
  ];

  return (
    <div>
      <Card style={{ marginBottom: 24 }}>
        <Title level={2}>New Employee Onboarding</Title>
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            label="New Employee Email"
            name="employeeEmail"
            rules={[{ required: true, message: 'Please enter the new employee\'s email', type: 'email' }]}
          >
            <Input placeholder="Enter the new employee's email" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<SendOutlined />}>
              Generate Token and Send Email
            </Button>
          </Form.Item>
        </Form>
      </Card>
      <Card>
        <Title level={2}>Application Review</Title>
        <Table 
          columns={columns} 
          dataSource={tableData} 
          loading={loading} 
          rowKey="applicationId" 
          bordered 
        />
      </Card>
      {/* <Modal
        title={reviewContext?.action === 'APPROVED' ? "Approve Application" : "Reject Application"}
        open={isReviewModalVisible}
        onOk={handleModalSubmit}
        onCancel={handleModalCancel}
        confirmLoading={submitting}
        okText="Submit Decision"
        // Make the submit button red if they are rejecting
        okButtonProps={{ danger: reviewContext?.action === 'REJECTED' }} 
        destroyOnClose={true}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="hrFeedback"
            label="HR Comments / Feedback"
            // Make feedback mandatory if rejecting, but optional if approving
            rules={[
              { 
                required: reviewContext?.action === 'REJECTED', 
                message: 'Please provide a reason for the rejection.' 
              }
            ]}
          >
            <Input.TextArea 
              rows={4} 
              placeholder={
                reviewContext?.action === 'REJECTED' 
                  ? "Explain why this application is being rejected..." 
                  : "Add optional notes about this approval..."
              } 
            />
          </Form.Item>
        </Form>
      </Modal> */}
    </div>
  );
};

export default HiringManagement;