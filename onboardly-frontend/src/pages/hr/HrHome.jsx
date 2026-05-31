import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Table, Tag, Typography, message } from 'antd';
import { getApplicationTracking } from '../../services/employeeService';


const { Title } = Typography;

// ==========================================
//  Mock Data
// ==========================================
const USE_MOCK_DATA = false;

const MOCK_APPLICATION_DATA = [
  { id: 1, employeeName: 'John Doe', applicationType: 'ONBOARDING', status: 'PENDING', lastModified: '2025-12-31' },
  { id: 2, employeeName: 'Jane Smith', applicationType: 'ONBOARDING', status: 'PENDING', lastModified: '2025-06-30' },
  { id: 3, employeeName: 'Michael Johnson', applicationType: 'STEM-OPT', status: 'AWAITING_REVIEW', lastModified: '2025-09-15' },
  { id: 4, employeeName: 'Jane Smith', applicationType: 'STEM-OPT', status: 'AWAITING_REVIEW', lastModified: '2025-07-01' },
];

// ==========================================
//  UI Helper Functions
// ==========================================
const getStatusColor = (status) => {
  switch (status) {
    case 'AWAITING_REVIEW':
      return 'green';
    case 'PENDING':
      return 'orange';
    default:
      return 'blue';
  }
};

const formatStatusText = (status) => {
  switch (status) {
    case 'AWAITING_REVIEW':
      return 'Awaiting Review';
    case 'PENDING':
      return 'Pending';
    default:
      return status;
  }
};

// ==========================================
//  Main Component
// ==========================================
const HrHome = () => {
  const navigate = useNavigate();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);

  // Fetch the entire dataset ONE time when the component mounts
  useEffect(() => {
    const loadData = async () => {
      setLoading(true);

      if (USE_MOCK_DATA) {
        setData(MOCK_APPLICATION_DATA);
        setLoading(false);
        return;
      }

      try {
        //changed by weihao
        const response = await getApplicationTracking();
        const responseList = response.data;

        // const responseList = await fetchApplicationsApi();
        setData(responseList);
      } catch (error) {
        console.error("Failed to fetch applications:", error);
        message.error("Could not load application data.");
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []); // Empty dependency array ensures this only runs once

  // Column Definitions
  const columns = [
    {
      title: 'Employee Name',
      dataIndex: 'employeeName',
      key: 'employeeName',
      sorter: (a, b) => a.employeeName.localeCompare(b.employeeName),
    },
    {
      title: 'Application Type',
      dataIndex: 'applicationType',
      key: 'applicationType',
      sorter: (a, b) => a.applicationType.localeCompare(b.applicationType),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      sorter: (a, b) => a.status.localeCompare(b.status),
      render: (status) => (
        <Tag color={getStatusColor(status)}>
          {formatStatusText(status)}
        </Tag>
      ),
    },
    {
      title: 'Last Modified',
      dataIndex: 'lastModified',
      key: 'lastModified',
      sorter: (a, b) => new Date(a.lastModified) - new Date(b.lastModified),
    },
  ];

  return (
    <Card bordered={false} style={{ boxShadow: '0 1px 2px -2px rgba(0, 0, 0, 0.16)' }}>
      <Title level={2}>Application Tracking</Title>
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        bordered
        onRow={(record) => ({
          onClick: () => {
            if (record.applicationType === 'ONBOARDING') {
              navigate(`/hr/form-application/${record.employeeId}`);
            } else if (record.applicationType === 'STEM_OPT_REVIEW') {
              navigate(`/hr/received-documents/${record.employeeId}`);
            }
          },
          style: { cursor: 'pointer' },
        })}
        pagination={{
          pageSize: 5,
          showSizeChanger: true,
          pageSizeOptions: ['5', '10', '20'],
        }}
      />
    </Card>
  );
};

export default HrHome;