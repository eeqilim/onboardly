import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Modal, Input, Space, Card, Typography, Tag, message } from 'antd';
import { FileOutlined } from '@ant-design/icons';
import { getAllEmployeeWithActiveVisa, reviewVisaStatus } from '../../services/visaService';

const { Title } = Typography;
const { TextArea } = Input;

const getStatusColor = (status) => {
  switch (status) {
    case 'APPROVED': return 'green';
    case 'REJECTED': return 'red';
    case 'PENDING': return 'orange';
    default: return 'blue';
  }
};

const getDaysLeftTag = (daysLeft) => {
  if (daysLeft == null) { return <Tag>Unknown</Tag>; }
  if (daysLeft <= 30) { return <Tag color="red">Urgent ({daysLeft} days)</Tag>; }
  if (daysLeft <= 90) { return <Tag color="orange">Soon ({daysLeft} days)</Tag>; }
  return <Tag color="green">{daysLeft} days left</Tag>;
};

const VisaStatusManagement = () => {
  const navigate = useNavigate();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectFeedback, setRejectFeedback] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchVisaData = async () => {
    try {
      setLoading(true);

      const res = await getAllEmployeeWithActiveVisa();

      setData(res.data || []);
    } catch {
      message.error('Could not load visa data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVisaData();
  }, []);

  const handleApprove = async (record) => {
    try {
      await reviewVisaStatus(record.visaStatusId, 'APPROVED', '');
      message.success(`Approved STEM OPT for ${record.firstName} ${record.lastName}.`);
      await fetchVisaData();
    } catch {
      message.error('Failed to approve.');
    }
  };

  const handleRejectConfirm = async () => {
    if (!rejectTarget) return;
    try {
      setSubmitting(true);
      await reviewVisaStatus(rejectTarget.visaStatusId, 'REJECTED', rejectFeedback);
      message.success(`Rejected STEM OPT for ${rejectTarget.firstName} ${rejectTarget.lastName}.`);
      setRejectModalOpen(false);
      setRejectFeedback('');
      setRejectTarget(null);
      await fetchVisaData();
    } catch {
      message.error('Failed to reject.');
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    {
      title: 'Employee Name',
      key: 'employeeName',
      render: (_, record) => `${record.firstName} ${record.lastName}`,
      sorter: (a, b) => `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`),
    },
    {
      title: 'Work Authorization',
      key: 'workAuth',
      render: (_, record) => {
        if (!record.workAuth) {
          return 'Unknown';
        }

        return record.workAuth === 'OTHER'
          ? record.visaTypeOther
          : record.workAuth;
        }
    },
    {
      title: 'Expiration Date',
      dataIndex: 'expirationDate',
      key: 'expirationDate',
      sorter: (a, b) => new Date(a.expirationDate) - new Date(b.expirationDate),
    },
    {
      title: 'Days Left',
      dataIndex: 'daysLeft',
      key: 'daysLeft',
      render: (daysLeft) => getDaysLeftTag(daysLeft),
      sorter: (a, b) => a.daysLeft - b.daysLeft,
    },
    {
      title: 'STEM OPT Actions',
      key: 'actions',
      render: (_, record) =>
        record.hasActiveStemOptApp ? (
          <Space size="small">
            <Button type="primary" size="small" loading={submitting} onClick={() => handleApprove(record)}>
              Approve
            </Button>
            <Button
              danger
              size="small"
              onClick={() => { setRejectTarget(record); setRejectModalOpen(true); }}
            >
              Reject
            </Button>
          </Space>
        ) : (
          <Tag color="default">N/A</Tag>
        ),
    },
    {
      title: 'Documents',
      key: 'documents',
      render: (_, record) => (
        <Button
          type="link"
          icon={<FileOutlined />}
          onClick={() => navigate(`/hr/received-documents/${record.employeeId}`, { state: { source: 'visa' } })}
        >
          View Documents
        </Button>
      ),
    },
  ];

  return (
    <Card>
      <Title level={2}>Visa Status Management</Title>
      <Table
        columns={columns}
        dataSource={data}
        rowKey="visaStatusId"
        loading={loading}
        bordered
      />

      <Modal
        title="Reject STEM OPT"
        open={rejectModalOpen}
        onOk={handleRejectConfirm}
        onCancel={() => { setRejectModalOpen(false); setRejectFeedback(''); setRejectTarget(null); }}
        okText="Confirm Reject"
        okButtonProps={{ danger: true, loading: submitting }}
      >
        <TextArea
          rows={4}
          value={rejectFeedback}
          onChange={(e) => setRejectFeedback(e.target.value)}
          placeholder="Provide feedback for the employee (optional)..."
        />
      </Modal>
    </Card>
  );
};

export default VisaStatusManagement;
