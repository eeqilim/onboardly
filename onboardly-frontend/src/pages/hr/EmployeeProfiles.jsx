import { useState, useEffect } from 'react';
import { Table, Input, Space, Card, Typography, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { getEmployeeProfiles, searchEmployeeProfiles } from '../../services/employeeService';

const { Title } = Typography;

const employeeProfiles = [
  { user_id: 1, id: 1, firstName: 'John', lastName: 'Doe', preferredName: 'John', ssn: '123-45-6789', startingDate: '2023-01-15', visaStatus: 'H-1B' },
  { user_id: 2, id: 2, firstName: 'Jane', lastName: 'Smith', preferredName: 'Jane', ssn: '987-65-4321', startingDate: '2023-03-20', visaStatus: 'L-1' },
  { user_id: 3, id: 3, firstName: 'Michael', lastName: 'Johnson', preferredName: 'Mike', ssn: '555-55-5555', startingDate: '2023-05-10', visaStatus: 'O-1' },
  { user_id: 4, id: 4, firstName: 'Sarah', lastName: 'Williams', preferredName: 'Sarah', ssn: '111-22-3333', startingDate: '2023-02-01', visaStatus: 'H-1B' },
  { user_id: 5, id: 5, firstName: 'David', lastName: 'Brown', preferredName: 'Dave', ssn: '444-55-6666', startingDate: '2023-04-10', visaStatus: 'L-1' },
];

const EmployeeProfiles = () => {
  const [searchText, setSearchText] = useState('');
  const [employeeData, setEmployeeData] = useState([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  // const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });
  
  // Table Pagination State (Ant Design is 1-indexed)
  const [tableParams, setTableParams] = useState({
    pagination: {
      current: 1,
      pageSize: 10, 
      total: 0,
      simple: true,
    },
  });

  // useEffect(() => {
  //   async function fetchEmployeeProfiles() {
  //     try {
  //       const response = await getEmployeeProfiles(pagination.current);
  //       setEmployeeData(response.data.content);
  //     } catch (error) {
  //       console.error('Failed to fetch employee profiles:', error);
  //     }
  //   }

  //   fetchEmployeeProfiles();
  // }, [pagination.current]);
  
  // const viewEmployeeProfile = (employee) => {
  //   navigate(`/hr/employee-profile/${employee.id}`);
  // };

  // const filteredData = useMemo(() => {
  //   return employeeProfiles.filter((employee) => {
  //     const searchLower = searchText.toLowerCase();
  //     return (
  //       employee.firstName.toLowerCase().includes(searchLower) ||
  //       employee.lastName.toLowerCase().includes(searchLower) ||
  //       employee.preferredName.toLowerCase().includes(searchLower)
  //     );
  //   });
  // }, [searchText]);

  // const columns = [
  //   {
  //     title: 'First Name',
  //     dataIndex: 'firstName',
  //     key: 'firstName',
  //     sorter: (a, b) => a.firstName.localeCompare(b.firstName),
  //   },
  //   {
  //     title: 'Last Name',
  //     dataIndex: 'lastName',
  //     key: 'lastName',
  //     sorter: (a, b) => a.lastName.localeCompare(b.lastName),
  //   },
  //   {
  //     title: 'Preferred Name',
  //     dataIndex: 'preferredName',
  //     key: 'preferredName',
  //   },
  //   {
  //     title: 'SSN',
  //     dataIndex: 'ssn',
  //     key: 'ssn',
  //   },
  //   {
  //     title: 'Starting Date',
  //     dataIndex: 'startingDate',
  //     key: 'startingDate',
  //     sorter: (a, b) => new Date(a.startingDate) - new Date(b.startingDate),
  //   },
  //   {
  //     title: 'Visa Status',
  //     dataIndex: 'visaStatus',
  //     key: 'visaStatus',
  //   },
  // ];
useEffect(() => {
    async function fetchData() {
      setLoading(true);
      try {
        if (searchText.trim() === '') {
          // --- SCENARIO A: NO SEARCH TEXT (Use Paginated Endpoint) ---
          
          // Convert AntD's 1-indexed page to Spring Boot's 0-indexed page
          const springPage = tableParams.pagination.current - 1;
          const response = await getEmployeeProfiles(springPage);
          
          // Extract data from your custom PageResponse DTO
          const { content, totalElements } = response.data;
          
          setEmployeeData(content);
          setTableParams((prev) => ({
            ...prev,
            pagination: { ...prev.pagination, total: totalElements },
          }));
        } else {
          // --- SCENARIO B: ACTIVE SEARCH (Use Search Endpoint) ---
          
          const response = await searchEmployeeProfiles(searchText);
          const searchResults = response.data; // Expected to be List<EmployeeSummaryResponse>
          
          setEmployeeData(searchResults);
          
          // Since the search endpoint returns an unpaginated List, 
          // we tell Ant Design the total is just the length of this returned list.
          setTableParams((prev) => ({
            ...prev,
            pagination: { 
              ...prev.pagination, 
              current: 1, // Reset to page 1 for search results
              total: searchResults.length 
            },
          }));
        }
      } catch (error) {
        console.error('Failed to fetch employee profiles:', error);
        message.error('Failed to load employee data.');
      } finally {
        setLoading(false);
      }
    }

    // A small debounce/timeout here is highly recommended in production 
    // so you don't hit the API on every single keystroke.
    const delayDebounceFn = setTimeout(() => {
      fetchData();
    }, 400);

    return () => clearTimeout(delayDebounceFn);

  }, [tableParams.pagination.current, tableParams.pagination.pageSize, searchText]); 
  // Re-fetch when page changes OR search text changes

  const handleTableChange = (pagination) => {
    setTableParams({ pagination });
  };

  const viewEmployeeProfile = (employee) => {
    navigate(`/hr/employee-profile/${employee.id}`);
  };

  const columns = [
    { title: 'First Name', dataIndex: 'firstName', key: 'firstName' },
    { title: 'Last Name', dataIndex: 'lastName', key: 'lastName' },
    { title: 'Preferred Name', dataIndex: 'preferredName', key: 'preferredName' },
    { title: 'SSN', dataIndex: 'ssn', key: 'ssn' },
    { title: 'Starting Date', dataIndex: 'employmentStartDate', key: 'employmentStartDate' },
    {
      title: 'Visa Status',
      key: 'visaStatus',
      render: (_, record) => {
        const visa = record.activeVisa;

        if (!visa || !visa.activeFlag || new Date(visa.endDate) <= new Date()) {
          return 'No Active Visa';
        }

        const isActive =
          visa.activeFlag &&
          new Date(visa.endDate) > new Date();

        return isActive
          ? `${visa.visaType}: Active`
          : `${visa.visaType}: Expired`;
      }
    },
  ];

  return (
    <Card>
      <Title level={2}>Employee Profiles</Title>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Input
          placeholder="Search by name"
          prefix={<SearchOutlined />}
          value={searchText}
          onChange={(e) => {
            setSearchText(e.target.value);

            setTableParams((prev) => ({
              ...prev,
              pagination: {
                ...prev.pagination,
                current: 1,
              },
            }));
          }}
          size="large"
          style={{ maxWidth: 400 }}
        />
        <Table
          columns={columns}
          dataSource={employeeData}
          rowKey="id"
          loading={loading}
          pagination={tableParams.pagination}
          onChange={handleTableChange}
          bordered
          onRow={(record) => ({
            onClick: () => viewEmployeeProfile(record),
            style: { cursor: 'pointer' },
          })}
        />
      </Space>
    </Card>
  );

  // return (
  //   <Card>
  //     <Title level={2}>Employee Profiles</Title>
  //     <Space direction="vertical" size="large" style={{ width: '100%' }}>
  //       <Input
  //         placeholder="Search by first name, last name, or preferred name"
  //         prefix={<SearchOutlined />}
  //         value={searchText}
  //         onChange={(e) => {
  //           setSearchText(e.target.value);
  //           setPagination({ ...pagination, current: 1 });
  //         }}
  //         size="large"
  //         style={{ maxWidth: 400 }}
  //       />
  //       <Table
  //         columns={columns}
  //         dataSource={filteredData}
  //         rowKey="user_id"
  //         pagination={{
  //           simple: true,
  //           current: pagination.current,
  //           pageSize: pagination.pageSize,
  //           total: filteredData.length,
  //           onChange: (page, pageSize) => setPagination({ current: page, pageSize }),
  //         }}
  //         bordered
  //         onRow = {(record, rowIndex) => {
  //           return {
  //             onClick: () => viewEmployeeProfile(record),
  //             style: { cursor: 'pointer' },
  //           };
  //         }}
  //       />
  //     </Space>
  //   </Card>
  // );
};

export default EmployeeProfiles;