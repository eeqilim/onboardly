import { useState, useEffect, useCallback } from 'react';
import { Table, Button, Card, Space, Typography, Popconfirm, Modal, message } from 'antd';
import { PhoneOutlined, MailOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { getHouses, deleteHousing } from '../../services/housingService';
import { getEmployeeProfiles, getAvailableHousings, getHouseSummaries, assignEmployeeToHouse, removeEmployeeFromHouse } from '../../services/employeeService';

const { Title } = Typography;

const USE_MOCK_DATA = false;

const housingData = [
  {
    houseId: 1,
    address: '123 Main St',
    city: 'Anytown',
    state: 'CA',
    zipCode: '12345',
    bedrooms: 3,
    bathrooms: 2,
    currentOccupants: 2,
    maxOccupant: 4,
    landlordName: 'John Landlord',
    landlordPhone: '555-123-4567',
    landlordEmail: 'john.landlord@example.com',
    
  },
  {
    houseId: 2,
    address: '456 Oak Ave',
    city: 'Anytown',
    state: 'CA',
    zipCode: '12345',
    bedrooms: 2,
    bathrooms: 1,
    currentOccupants: 1,
    maxOccupant: 3,
    landlordName: 'Jane Landlord',
    landlordPhone: '555-987-6543',
    landlordEmail: 'jane.landlord@example.com',
  },
];

const employeeInformation = [
  { id: 1, firstName: 'John', lastName: 'Doe', preferredName: 'Johnny', phone: '555-111-2222', email: 'john.doe@example.com' },
  { id: 2, firstName: 'Jane', lastName: 'Smith', preferredName: 'J.S.', phone: '555-222-3333', email: 'jane.smith@example.com' },
  { id: 3, firstName: 'Michael', lastName: 'Johnson', preferredName: 'MJ', phone: '555-333-4444', email: 'michael.johnson@example.com' },
];

const HousingManagement = () => {
  const [housingList, setHousingList] = useState([]);
  const [employeeList, setEmployeeList] = useState([]);
  const [loading, setLoading] = useState(false);

  const [isModalVisible, setIsModalVisible] = useState(false);
  const [availableHousings, setAvailableHousings] = useState([]);
  const [modalLoading, setModalLoading] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState(null);

  const navigate = useNavigate();

  const fetchHousingData = useCallback(async() => {
    setLoading(true);
    if (USE_MOCK_DATA) {
      setHousingList(housingData);
      setEmployeeList(employeeInformation);
      setLoading(false);
      return;
    }

    try {
      const [houseResponse, houseSummaryResponse, employeeResponse] = await Promise.all([
        getHouses(),
        getHouseSummaries(),
        getEmployeeProfiles()
      ]);

      const houses = houseResponse.data.content ?? houseResponse.data;

      const houseMap = new Map(
        houses.map((house) => [house.id, house])
      );

      const houseSummaries = houseSummaryResponse.data.map((summary) => {
        const houseDetails = houseMap.get(summary.houseId);

        return {
          ...summary,
          landlordEmail: houseDetails?.landlordEmail,
          landlordPhone: houseDetails?.landlordPhone,
          createDate: houseDetails?.createDate,
          lastModificationDate: houseDetails?.lastModificationDate,
        };
      });

      setHousingList(houseSummaries);
      setEmployeeList(employeeResponse.data.content ?? []);
    } catch (error) {
      console.error('Error fetching housing/employee data:', error);
      message.error("Failed to load dashboard data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() =>{
    fetchHousingData();
  }, [fetchHousingData]);

  const handleAddHousing = () => {
    navigate('/hr/housing-form');
  };

  const handleViewDetails = (record) => {
    navigate(`/hr/housing-details/${record.houseId}`);
  };

  // Delete Handler: Executes the API call, then filters the local state
  const handleDeleteHousing = async (record) => {
    try {
      if (!USE_MOCK_DATA) {
        await deleteHousing(record.houseId);
      }
      
      // Update the local state to remove the deleted house
      setHousingList((currentList) => currentList.filter(house => house.houseId !== record.houseId));
      message.success('Housing deleted successfully');
    } catch (error) {
      console.error('Delete failed:', error);
      message.error('Failed to delete housing. It may still be in use.');
    }
  };

  const viewEmployeeProfile = (employee) => {
    navigate(`/hr/employee-profile/${employee.id}`);
  }

  const handleAssignmentClick = async (employee) => {
    setSelectedEmployee(employee);
    setIsModalVisible(true);
    setModalLoading(true);

    if (USE_MOCK_DATA) {
      setAvailableHousings(housingData.filter(h => h.currentOccupants < h.maxOccupant));
      setModalLoading(false);
      return;
    }

    try {
      const response = await getAvailableHousings();
      setAvailableHousings(response.data);
    } catch (error) {
      message.error("Failed to fetch available housing.");
    } finally {
      setModalLoading(false);
    }
  };
  
  const confirmAssignment = async (houseId) => {
    try {
      if (!USE_MOCK_DATA) {
        // employeeId is a string, houseId is a Long based on your setup
        await assignEmployeeToHouse(String(selectedEmployee.id), houseId);
      }
      message.success("Employee successfully assigned to housing!");
      setIsModalVisible(false);
      setSelectedEmployee(null);
      fetchHousingData(); 
    } catch (error) {
      message.error("Failed to assign housing.");
    }
  };

  const handleRemoval = async (employee) => {
    try {
      if (!USE_MOCK_DATA) {
        await removeEmployeeFromHouse(String(employee.id));
      }
      message.success("Employee removed from housing.");
      fetchHousingData(); // Refresh counts
    } catch (error) {
      message.error("Failed to remove employee from housing.");
    }
  };

  const handleModalClose = () => {
    setIsModalVisible(false);
    setSelectedEmployee(null);
  };

  const housingColumns = [
    {
      title: 'Address',
      dataIndex: 'address',
      key: 'address',
      sorter: (a, b) => a.address.localeCompare(b.address),
    },
    {
      title: 'Number of Current Occupants',
      dataIndex: 'currentOccupants',
      key: 'currentOccupants',
    },
    {
      title: 'Max Occupant',
      dataIndex: 'maxOccupant',
      key: 'maxOccupant',
    },
    {
      title: 'Landlord',
      dataIndex: 'landlordName',
      key: 'landlordName',
    },
    {
      title: 'Phone',
      dataIndex: 'landlordPhone',
      key: 'landlordPhone',
      render: (phone) => (
        <a href={`tel:${phone}`}>
          <PhoneOutlined /> {phone}
        </a>
      ),
    },
    {
      title: 'Email',
      dataIndex: 'landlordEmail',
      key: 'landlordEmail',
      render: (email) => (
        <a href={`mailto:${email}`}>
          <MailOutlined /> {email}
        </a>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space size="small">
          <Button type="primary" size="small" onClick={() => handleViewDetails(record)}>
            View
          </Button>
          
          {/* <Button type="default" size="small" onClick={() => handleEditHousing(record)}>
            Edit
          </Button> */}
          
          {/* Wrap the delete button in a Popconfirm to prevent accidental data loss */}
          <Popconfirm
            title="Delete this housing?"
            description={`Are you sure you want to delete ${record.address}?`}
            onConfirm={() => handleDeleteHousing(record)}
            okText="Yes, Delete"
            okType="danger"
            cancelText="Cancel"
          >
            <Button danger size="small">
              Delete
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const employeeColumns = [
    {
      title: 'Name', key: 'name',
      render: (_, record) => {
        const fullName = `${record.firstName || ''} ${record.lastName || ''}`.trim();

        return (
          <span>
            {record.preferredName || fullName || 'Unnamed'}
          </span>
        );
      },
    },
    {
      title: 'Phone', dataIndex: 'phone', key: 'phone',
      render: (phone) => <a href={`tel:${phone}`}><PhoneOutlined /> {phone}</a>
    },
    {
      title: 'Email', dataIndex: 'email', key: 'email',
      render: (email) => <a href={`mailto:${email}`}><MailOutlined /> {email}</a>
    },
    {
      title: 'Actions', key: 'actions',
      render: (_, record) => (
        <Space size="small">
          <Button type="primary" size="small" onClick={(e) => {
            e.stopPropagation();
            handleAssignmentClick(record);
          }}>
            Assign Housing
          </Button>
          <Popconfirm
            title="Remove from current housing?"
            description={`You will have to reassign this employee if you remove them from their current housing.`}
            onConfirm={(e) => {
              e.stopPropagation();
              handleRemoval(record);
            }}
            onCancel={(e) => e.stopPropagation()}
            okText="Remove"
            okType="danger"
            cancelText="Cancel"
          >
            <Button danger size="small" onClick={(e) => e.stopPropagation()}>
              Remove Housing
            </Button>
          </Popconfirm>
        </Space>
      ),
    }
  ];

  const modalColumns = [
    { title: 'Address', dataIndex: 'address', key: 'address' },
    {
      title: 'Residents', key: 'residents',
      render: (_, record) => `${record.currentOccupants} / ${record.maxOccupant}`
    },
    {
      title: 'Action', key: 'action',
      render: (_, record) => (
        <Button type="primary" onClick={() => confirmAssignment(record.houseId)}>
          Assign
        </Button>
      )
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <Card 
        title={<Title level={2} style={{ marginTop: 16 }}>Housing Management</Title>} 
        extra={
          <Button type="primary" onClick={() => handleAddHousing()}>
            Add New Housing
          </Button>
        }
      >
        <Table 
          columns={housingColumns} 
          dataSource={housingList} 
          rowKey="houseId" 
          loading={loading}
          bordered 
        />
      </Card>
      
      <Card>
        <Title level={2}>Employee Information</Title>
        <Table
          columns={employeeColumns}
          dataSource={employeeList}
          rowKey="id"
          loading={loading}
          bordered
          onRow={(record) => {
            return {
              onClick: () => viewEmployeeProfile(record),
              style: { cursor: 'pointer' }
            };
          }}
        />
      </Card>
      <Modal
        title={`Assign Housing for ${selectedEmployee?.preferredName || selectedEmployee?.firstName || 'Employee'}`}
        open={isModalVisible}
        onCancel={handleModalClose}
        footer={null}
        width={700}
      >
        <Table
          columns={modalColumns}
          dataSource={availableHousings}
          rowKey="houseId"
          loading={modalLoading}
          pagination={{ pageSize: 5 }}
          bordered
        />
      </Modal>
    </div>
  );
};

export default HousingManagement;