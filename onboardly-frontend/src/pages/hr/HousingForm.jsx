import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, InputNumber, Button, Card, Typography, Row, Col, message } from 'antd';
import { HomeOutlined, UserOutlined } from '@ant-design/icons';
import { createLandlord, createHousing } from '../../services/housingService';

const { Title } = Typography;

// --- Mock API Calls ---
const USE_MOCK_API = false;

const mockCreateLandlord = async (landlordData) => {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ id: 'LL-9981', ...landlordData }), 800);
  });
};

const mockCreateHouse = async (houseData, landlordId) => {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ id: 'HS-4452', landlordId, ...houseData }), 800);
  });
};

export default function HousingForm() {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);

  const onFinish = async (values) => {
    setSubmitting(true);

    if (USE_MOCK_API) {
      const newLandlord = await mockCreateLandlord(landlordPayload);
      const newHouse = await mockCreateHouse(housePayload, newLandlord.id);
      message.success('Housing and Landlord created successfully!');
      navigate(`/hr/housing/facility-form/${newHouse.id}`);
      return;
    }

    try {
      // 1. Create Landlord
      const landlordPayload = {
        firstName: values.landlordFirstName,
        lastName: values.landlordLastName,
        email: values.landlordEmail,
        cellPhone: values.landlordPhone,
      };        
      const newLandlord = await createLandlord(landlordPayload);

      console.log("newLandlord response:", newLandlord);

      const landlordId = newLandlord?.id || newLandlord?.data?.id;

      if (!landlordId) {
        message.error("Failed to get landlord ID after creating landlord");
        return;
      }

      // 2. Create House using the new Landlord's ID
      const housePayload = {
        address: values.address,
        maxOccupant: values.maxOccupants,
        landlordId: landlordId,
      };
      const newHouse = await createHousing(housePayload); 

      console.log("newHouse response:", newHouse);

      const houseId = newHouse?.id || newHouse?.data?.id;

      if (!houseId) {
        message.error("Failed to get house ID after creating house");
        return;
      }

      message.success('Housing and Landlord created successfully!');
      
      // 3. Redirect to the Facility Form passing the new house ID in the URL
      navigate(`/hr/housing/facility-form/${houseId}`);
    } catch (error) {
      message.error('Failed to create housing. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '24px' }}>
      <Title level={2}>Add New Housing</Title>
      
      <Form 
        form={form} 
        layout="vertical" 
        onFinish={onFinish}
        initialValues={{ maxOccupants: 1 }}
      >
        {/* House Section */}
        <Card 
          title={<><HomeOutlined /> House Information</>} 
          style={{ marginBottom: 24 }}
        >
          <Form.Item
            name="address"
            label="Property Address"
            rules={[{ required: true, message: 'Please enter the property address' }]}
          >
            <Input placeholder="123 Main St, City, State, ZIP" />
          </Form.Item>
          
          <Form.Item
            name="maxOccupants"
            label="Maximum Occupants"
            rules={[{ required: true, message: 'Please specify max occupants' }]}
          >
            <InputNumber min={1} max={50} style={{ width: '100%' }} />
          </Form.Item>
        </Card>

        {/* Landlord Section */}
        <Card 
          title={<><UserOutlined /> Landlord Information</>} 
          style={{ marginBottom: 24 }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="landlordFirstName"
                label="First Name"
                rules={[{ required: true, message: 'First name is required' }]}
              >
                <Input placeholder="John" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="landlordLastName"
                label="Last Name"
                rules={[{ required: true, message: 'Last name is required' }]}
              >
                <Input placeholder="Doe" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="landlordEmail"
                label="Email Address"
                rules={[
                  { required: true, message: 'Email is required' },
                  { type: 'email', message: 'Enter a valid email' }
                ]}
              >
                <Input placeholder="john.doe@example.com" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="landlordPhone"
                label="Cell Phone"
                rules={[{ required: true, message: 'Phone number is required' }]}
              >
                <Input placeholder="(555) 123-4567" />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        <Form.Item style={{ textAlign: 'right' }}>
          <Button onClick={() => navigate('/hr/housing')} style={{ marginRight: 16 }}>
            Cancel
          </Button>
          <Button type="primary" htmlType="submit" loading={submitting}>
            Next: Add Facilities
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
}