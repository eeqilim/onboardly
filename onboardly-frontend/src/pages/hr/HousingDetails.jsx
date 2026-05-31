import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { 
  Typography, 
  Descriptions, 
  Table, 
  Tag, 
  Button, 
  Modal, 
  List, 
  Input, 
  Space, 
  Card, 
  Divider,
  Spin,
  Tooltip,
  message 
} from 'antd';
import { 
    HomeOutlined,
    TeamOutlined,
    UserOutlined,
    PhoneOutlined,
    MailOutlined,
    BankOutlined,
    ToolOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import {
  getHouseDetails,
  getFacilities,
  getHouseReports,
  getFacilityReportDetails,
  addFacilityReportComment,
  updateFacilityReportComment,
} from "../../services/housingService";
import { useAuth } from '../../auth/AuthContext';
import { getEmployeeProfile } from '../../services/employeeService';

const { Title, Text } = Typography;
const { TextArea } = Input;

const USE_MOCK_DATA = false;

// Static Mock Data
const houseData = {
    id: 1,
    address: '123 Main St',
    landlordName: 'John Doe', 
    landlordEmail: 'john.doe@example.com',
    landlordPhone: '555-123-4567',
    maxOccupant: 3,
};

const facilityData = [
  {
    id: 1,
    type: 'Beds',   
    quantity: 3,
    description: 'Queen size beds'
  },
  {
    id: 2,
    type: 'Mattresses',
    quantity: 3,
  },
  {
    id: 3,
    type: 'Tables',
    quantity: 2,
  },
  {
    id: 4,
    type: 'Chairs',
    quantity: 3,
  }
];

const mockReports = [
  {
    id: 1,
    facilityId: 1,
    employeeId: 1,
    title: "Mock Report",
    description: "Mock Report Description",
    createdByName: "John",
    createDate: "2026-05-01",
    status: "OPEN",
    comments: [
      {
        id: 1,
        facilityReportId: 1,
        employeeId: 1,
        comment: "Mock Report Comment",
        commenterName: "John",
        createDate: "2026-05-02",
        lastModificationDate: null,
      },
    ],
  },
];
// [
//     {
//         id: 1,
//         facilityId: 1,
//         employeeId: 1,
//         title: "Mock Report",
//         description: "Mock Report Description",
//         createdBy: "John",
//         createDate: "2026-05-01",
//         status: "OPEN",
//         comments: [
//         {
//             commentId: 1,
//             employeeId: 1,
//             comment: "Mock Report Comment",
//             createdByName: "John",
//             createdByCurrentUser: true,
//             createdDate: "2026-05-02",
//             lastModifiedDate: null,
//         },
//         ],
//     },
//     {
//         id: 2,
//         facilityId: 2,
//         employeeId: 1,
//         title: "Mock Report 2",
//         description: "Mock Report Description",
//         createdBy: "John",
//         createDate: "2026-05-01",
//         status: "OPEN",
//         comments: [
//         {
//             id: 1,
//             employeeId: 1,
//             comment: "Mock Report Comment",
//             createdByName: "You",
//             createdByCurrentUser: true,
//             createdDate: "2026-05-02",
//             lastModifiedDate: null,
//         },
//         ],
//     },
//     {
//         id: 3,
//         facilityId: 3,
//         employeeId: 1,
//         title: "Mock Report 3",
//         description: "Mock Report Description",
//         createdBy: "John",
//         createDate: "2026-05-01",
//         status: "OPEN",
//         comments: [
//         {
//             id: 1,
//             employeeId: 1,
//             comment: "Mock Report Comment",
//             createdByName: "You",
//             createdByCurrentUser: true,
//             createdDate: "2026-05-02",
//             lastModifiedDate: null,
//         },
//         ],
//     },
    
// ];

const HousingDetails = () => {
    // State to hold and mutate house, facilities and reports data
    const [houseDetails, setHouseDetails] = useState(null);
    const [facilities, setFacilities] = useState([]);
    const [reports, setReports] = useState([]);  
    const [employee, setEmployee] = useState(null);

    const { houseId } = useParams();  
    const [loading, setLoading] = useState(true);
    const [reportsLoading, setReportsLoading] = useState(false);

    const [tableParams, setTableParams] = useState({
        pagination: {
            current: 1, 
            pageSize: 5,
            total: 0,   
        },
    });

    // Modal State
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [selectedReport, setSelectedReport] = useState(null);
    const [isLoadingDetails, setIsLoadingDetails] = useState(false);
    
    // Comment Input State
    const [commentText, setCommentText] = useState('');
    const [editingCommentId, setEditingCommentId] = useState(null);

    const { user } = useAuth();
    const CURRENT_USER = user.username;
    
    // Fetch house details and reports on component mount
    useEffect(() => {
        async function loadHousing() {
            setLoading(true);

            if (USE_MOCK_DATA) {
                setHouseDetails(houseData);
                setFacilities(facilityData);
                // setEmployee({id: "1"});
                setLoading(false);
                return;
            }

            try {
                const houseResponse = await getHouseDetails(houseId);
                setHouseDetails(houseResponse.data);

                const facilitiesResponse = await getFacilities(houseId);
                setFacilities(facilitiesResponse.data);

                
                // const employeeResponse = await getEmployeeProfile();
                // setEmployee(employeeResponse.data);
                // console.log("Employee Profile:", employeeResponse.data);
            } catch (error) {
                console.error('Failed to load housing details or facilities:', error);
            }
            finally {
                setLoading(false);
            }
        }

        loadHousing();
    }, []);

    const fetchReports = async () => {
        setReportsLoading(true);

        if (USE_MOCK_DATA) {
            setReports(mockReports);

            setTableParams((prev) => ({
                ...prev,
                pagination: {
                    ...prev.pagination,
                    total: mockReports.length,
                },
            }));
            setReportsLoading(false);
            return;
        }

        try {
            const springPage = tableParams.pagination.current - 1;
            const size = tableParams.pagination.pageSize;
            const response = await getHouseReports(houseId, springPage, size);

            const { content, totalElements } = response.data;

            setReports(content);

            setTableParams((prev) => ({
                ...prev,
                pagination: {
                    ...prev.pagination,
                    total: totalElements,
                },
            }));
        } catch (error) {
            console.error("Failed to fetch reports:",error
            );
        } finally {
            setReportsLoading(false);
        }
    };

    useEffect(() => {
        if (USE_MOCK_DATA || houseId) {
            fetchReports();
        }
    }, [tableParams.pagination.current, tableParams.pagination.pageSize, houseId]);

    const handleTableChange = (pagination) => {
        setTableParams({
            pagination,
        });
    };

    // Table Columns Definition
    const columns = [
        {
            title: 'Title',
            dataIndex: 'title',
            key: 'title',
            render: (text) => <b>{text}</b>,
        },
        {
            title: 'Date Reported',
            dataIndex: 'createDate',
            key: 'createDate',
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            render: (status) => {
                let color = status === 'CLOSED' ? 'green' : status === 'IN_PROGRESS' ? 'blue' : 'red';
                return <Tag color={color}>{status}</Tag>;
            },
        },
        {
            title: 'Action',
            key: 'action',
            render: (_, record) => (
                <Button type="primary" onClick={() => handleOpenModal(record)}>
                    View Details
                </Button>
            ),
        },
    ];

    // Handlers
    const handleOpenModal = async (report) => {
        setIsLoadingDetails(true);
        setIsModalVisible(true);
        setCommentText('');
        setEditingCommentId(null);

        try {

            if (USE_MOCK_DATA) {
                setSelectedReport(report);
                return;
            }

            const reportDetails =
                await getFacilityReportDetails(report.id);

            setSelectedReport(reportDetails.data);

        } catch (error) {
            console.error('Failed to load report details:', error);

            message.error(
                'Failed to load report details. Please try again later.'
            );

            setIsModalVisible(false);

        } finally {
            setIsLoadingDetails(false);
        }
    };

    const handleCloseModal = () => {
        setIsModalVisible(false);
        setSelectedReport(null);
    };

    const handleSaveComment = async () => {
        if (!commentText.trim() || !selectedReport) {
            return;
        }

        try {
            const payload = {
                employeeId: user.userId,
                comment: commentText,
            };

            // UPDATE COMMENT
            if (editingCommentId) {

                if (!USE_MOCK_DATA) {
                    await updateFacilityReportComment(
                        selectedReport.id,
                        editingCommentId,
                        payload
                    );
                }

                const updatedComments = (selectedReport.comments || []).map((comment) =>
                    comment.id === editingCommentId
                        ? {
                            ...comment,
                            comment: commentText,
                            lastModificationDate: new Date().toISOString(),
                        }
                        : comment
                );

                const updatedReport = {
                    ...selectedReport,
                    comments: updatedComments,
                };

                setSelectedReport(updatedReport);

                setReports((prev) =>
                    prev.map((report) =>
                        report.id === selectedReport.id
                            ? updatedReport
                            : report
                    )
                );

                message.success("Comment updated successfully.");

            } else {

                // ADD COMMENT

                let newComment = {
                    id: Date.now(),
                    facilityReportId: selectedReport.id,
                    employeeId: payload.employeeId,
                    comment: commentText,
                    commenterName: CURRENT_USER,
                    createDate: new Date().toISOString(),
                    lastModificationDate: null,
                };

                if (!USE_MOCK_DATA) {
                    const response = await addFacilityReportComment(
                        selectedReport.id,
                        payload
                    );

                    newComment = response.data;
                }

                const updatedReport = {
                    ...selectedReport,
                    comments: [
                        ...(selectedReport.comments || []),
                        newComment,
                    ],
                };

                setSelectedReport(updatedReport);

                setReports((prev) =>
                    prev.map((report) =>
                        report.id === selectedReport.id
                            ? updatedReport
                            : report
                    )
                );

                message.success("Comment added successfully.");
            }

            setCommentText('');
            setEditingCommentId(null);

        } catch (error) {
            console.error("Failed to save comment:", error);
            message.error("Failed to save comment.");
        }
    };

    const handleEditClick = (comment) => {
        setEditingCommentId(comment.id);
        setCommentText(comment.comment);
    };

    if (loading || !houseDetails) {
        return <Spin fullscreen />;
    }

    return (
        
        <div style={{ padding: '24px' }}>
            <Title level={2}>Housing Management</Title>

            <Space direction="vertical" size="large" style={{ display: 'flex' }}>
                {/* House Information */}
                <Card bordered={false} style={{ boxShadow: '0 1px 2px -2px rgba(0, 0, 0, 0.16)' }}>
                    <Descriptions title={ <Title level={3}><HomeOutlined /> House Information</Title>} bordered column={{ xxl: 3, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }}>
                        <Descriptions.Item label={<><HomeOutlined /> Address</>}>{houseDetails.address}</Descriptions.Item>
                        <Descriptions.Item label={<><TeamOutlined /> Max Occupants</>}>{houseDetails.maxOccupant}</Descriptions.Item>
                        <Descriptions.Item label={<><UserOutlined /> Landlord Name</>}>{houseDetails.landlordName}</Descriptions.Item>
                        <Descriptions.Item label={<><MailOutlined /> Landlord Email</>}>{houseDetails.landlordEmail}</Descriptions.Item>
                        <Descriptions.Item label={<><PhoneOutlined /> Landlord Phone</>}>{houseDetails.landlordPhone}</Descriptions.Item>
                    </Descriptions>
                </Card>

                {/* Facility Information */}
                <Card bordered={false} style={{ boxShadow: '0 1px 2px -2px rgba(0, 0, 0, 0.16)' }}>
                    <Descriptions title={ <Title level={3}><BankOutlined /> Facility Information</Title>} bordered column={{ xxl: 4, xl: 3, lg: 3, md: 2, sm: 1, xs: 1 }}>
                        {facilities && facilities.length > 0 ? (
                            facilities.map((facility) => (
                                <Descriptions.Item key={facility.id} label= {facility.type}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <span>{facility.quantity}</span>
                                        
                                        {facility.description && (
                                            <Tooltip title={facility.description ? facility.description : 'No additional details'}>
                                            <InfoCircleOutlined style={{ color: '#8c8c8c', marginLeft: 8 }} />
                                            </Tooltip>
                                        )}
                                    </div>
                                </Descriptions.Item>
                            ))
                        ) : (
                            <Descriptions.Item label="Facilities">No facilities available</Descriptions.Item>
                        )}
                        
                    </Descriptions>
                </Card>

                {/* Facility Reports Table */}
                <Card 
                    title={<Title level={3} style={{ marginTop: 20, marginBottom: 20 }}><ToolOutlined /> Facility Reports</Title>} 
                    bordered={false} 
                    style={{ boxShadow: '0 1px 2px -2px rgba(0, 0, 0, 0.16)' }}
                >
                    <Table
                        columns={columns}
                        dataSource={reports}
                        rowKey="id"
                        loading={reportsLoading}
                        pagination={tableParams.pagination}
                        onChange={handleTableChange}
                    />
                </Card>
            </Space>

            {/* Report Details Pop-up (Modal) */}
            <Modal
                title={ selectedReport ? `Report Details: ${selectedReport.title}` : "Report Details" }
                open={isModalVisible}
                onCancel={handleCloseModal}
                footer={[
                    <Button key="close" onClick={handleCloseModal}>
                        Close
                    </Button>
                ]}
                width={700}
            >
                <Spin spinning = {isLoadingDetails} tip = "Loading report details...">
                    {!isLoadingDetails && selectedReport && (
                        <Space direction="vertical" style={{ width: '100%' }} size="middle">
                            <Descriptions column={2} size="small" bordered>
                                <Descriptions.Item label="Status">
                                    <Tag color={selectedReport.status === 'CLOSED' ? 'green' : selectedReport.status === 'IN_PROGRESS' ? 'blue' : 'red'}>
                                        {selectedReport.status}
                                    </Tag>
                                </Descriptions.Item>
                                <Descriptions.Item label="Reported By">{selectedReport.createdByName}</Descriptions.Item>
                                <Descriptions.Item label="Date">{new Date(selectedReport.createDate).toLocaleString()}</Descriptions.Item>
                                <Descriptions.Item label="Description" span={2}>{selectedReport.description}</Descriptions.Item>
                            </Descriptions>

                            <Divider orientation="left">Comments</Divider>

                            <List
                                itemLayout="horizontal"
                                dataSource={selectedReport.comments || []}
                                renderItem={item => (
                                    <List.Item
                                        actions={
                                            item.createdByName === user.username
                                                ? [
                                                    <Button
                                                        type="link"
                                                        onClick={() => handleEditClick(item)}
                                                    >
                                                        Edit
                                                    </Button>
                                                ]
                                                : []
                                        }
                                    >
                                        <List.Item.Meta
                                            title={
                                                <Text strong>
                                                    {item.commenterName}
                                                    <Text
                                                        type="secondary"
                                                        style={{ fontSize: '12px', marginLeft: 8 }}
                                                    >
                                                        {new Date(item.createDate).toLocaleString()}
                                                    </Text>
                                                </Text>
                                            }
                                            description={item.comment}
                                        />
                                    </List.Item>
                                )}
                            />

                            <div style={{ marginTop: '16px' }}>
                                <TextArea 
                                    rows={3} 
                                    value={commentText} 
                                    onChange={(e) => setCommentText(e.target.value)} 
                                    placeholder="Write a comment..." 
                                />
                                <Button 
                                    type="primary" 
                                    style={{ marginTop: '12px', float: 'right' }} 
                                    onClick={handleSaveComment}
                                >
                                    {editingCommentId ? 'Update Comment' : 'Add Comment'}
                                </Button>
                                {/* Clear float */}
                                <div style={{ clear: 'both' }}></div>
                            </div>
                        </Space>
                    )}
                </Spin>
            </Modal>
        </div>
    );
};

export default HousingDetails;