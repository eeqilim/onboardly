import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Alert, Button, Card, Empty, List, Space, Typography } from "antd";
import {
  HomeOutlined,
  PhoneOutlined,
  TeamOutlined,
  ToolOutlined,
} from "@ant-design/icons";
import { getMyHousing } from "../../services/housingService";
import "./Housing.css";

const { Text, Title } = Typography;

const USE_MOCK_DATA = false;
const mockHouse = {
  address: "123 St, Los Angeles, CA 90001",
  residents: [
    { id: 1, firstName: "", preferredName: "John", phone: "123-456-7890" },
    {
      id: 2,
      firstName: "Alice",
      preferredName: "",
      phone: "222-333-4444",
      address: "123 St, Los Angeles, CA 90001",
    },
  ],
};

function Field({ label, value }) {
  return (
    <div className="housing-field">
      <Text type="secondary">{label}</Text>
      <Text strong>{value || "-"}</Text>
    </div>
  );
}

export default function Housing() {
  const [house, setHouse] = useState(null);
  const [residents, setResidents] = useState([]);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadHousing() {
      setLoading(true);

      if (USE_MOCK_DATA) {
        setHouse(mockHouse);
        setLoading(false);
        return;
      }

      try {
        const { data } = await getMyHousing();

        if (!data || !data.houseId) {
          setStatus("No housing assigned.");
          setHouse(null);
          return;
        }

        setHouse(data);
        setResidents(data.residents || []);
      } catch {
        setStatus("Failed to load housing information.");
      } finally {
        setLoading(false);
      }
    }

    loadHousing();
  }, []);

  if (loading) return <div className="housing-page">Loading...</div>;

  if (!house) {
    return (
      <div className="housing-page">{status || "No housing assigned."}</div>
    );
  }

  return (
    <div className="housing-page">
      <div className="housing-title-row">
        <Title level={2}>Housing</Title>
        {status && <Alert message={status} type="info" showIcon />}
      </div>

      <div className="housing-layout">
        <Card
          className="housing-card housing-card-house"
          title={
            <span className="housing-card-title">
              <HomeOutlined />
              <span>House Detail</span>
            </span>
          }
        >
          <Field label="Address" value={house.address} />
        </Card>

        <Card
          className="housing-card housing-card-residents"
          title={
            <span className="housing-card-title">
              <TeamOutlined />
              <span>Employees</span>
            </span>
          }
        >
          {residents.length === 0 ? (
            <Empty description="No residents found" />
          ) : (
            <List
              itemLayout="horizontal"
              dataSource={residents}
              renderItem={(employee) => (
                <List.Item key={employee.employeeId}>
                  <List.Item.Meta
                    title={
                      employee.preferredName || employee.firstName || "Unnamed"
                    }
                    description={
                      <Space size="small">
                        <PhoneOutlined />
                        <Text type="secondary">
                          {employee.cellPhone || "-"}
                        </Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          )}
        </Card>

        <div className="housing-actions">
          <Link to="/employee/facility-reports">
            <Button type="primary" icon={<ToolOutlined />}>
              Report Facility Issue
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
