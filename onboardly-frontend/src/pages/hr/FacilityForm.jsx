import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Form, Input, InputNumber, Button, Card, Typography, Row, Col, Space, Divider, message } from 'antd';
import { PlusOutlined, MinusCircleOutlined, AppstoreAddOutlined } from '@ant-design/icons';
import { createFacility } from '../../services/housingService';

const { Title, Text } = Typography;

// --- Mock API Call ---
const USE_MOCK_API = false;

const mockCreateFacilityFeature = async (houseId, featureData) => {
  return new Promise((resolve) => {
    // Simulating a network request for a single facility feature
    setTimeout(() => resolve({ houseId, ...featureData }), 500);
  });
};

export default function FacilityForm() {
  const [form] = Form.useForm();
  const navigate = useNavigate();

  const params = useParams();// Retrieves the newly created house ID from the URL
  const houseId = params.houseId || params.id;
  console.log("FacilityForm params:", params);
  console.log("FacilityForm houseId:", houseId);

  const [submitting, setSubmitting] = useState(false);

  // The 4 mandatory categories
  const mandatoryFeatures = ['Beds', 'Mattresses', 'Tables', 'Chairs'];

  const onFinish = async (values) => {
    setSubmitting(true);
    if (!houseId || houseId === "undefined" || Number.isNaN(Number(houseId))) {
      message.error("Missing house ID. Please go back and create the house again.");
      setSubmitting(false);
      return;
    }

    if (USE_MOCK_API) {
      const featuresToSubmit = mandatoryFeatures.map(type => ({
        type,
        quantity: values[type].quantity,
        description: values[type].description || '',
      }));

      if (values.additionalFeatures && values.additionalFeatures.length > 0) {
        values.additionalFeatures.forEach(feature => {
          if (feature) {
            featuresToSubmit.push({
              type: feature.type,
              quantity: feature.quantity,
              description: feature.description || '',
            });
          }
        });
      }

      await Promise.all(
        featuresToSubmit.map(feature => mockCreateFacilityFeature(houseId, feature))
      );

      message.success('Facility features configured successfully!');
      setSubmitting(false);
      navigate('/hr/housing');
      return;
    }

    try {
      // 1. Gather mandatory features into a normalized array
      const featuresToSubmit = mandatoryFeatures.map((type) => ({
        houseId: Number(houseId),
        type,
        description: values[type]?.description || "",
        quantity: values[type]?.quantity || 1,
      }));

      // 2. Append any dynamically added additional features
      if (values.additionalFeatures && values.additionalFeatures.length > 0) {
        values.additionalFeatures.forEach((feature) => {
          if (feature?.type) {
            featuresToSubmit.push({
              houseId: Number(houseId),
              type: feature.type,
              description: feature.description || "",
              quantity: feature.quantity || 1,
            });
          }
        });
      }

      // 3. Execute all endpoint calls concurrently. Also converts houseId to number just before sending
      await Promise.all(
        featuresToSubmit.map((feature) => createFacility(feature))
      );

      message.success('Facility features configured successfully!');
      
      // 4. Return to the main Housing Management table
      navigate('/hr/housing-management');
    } catch (error) {
      message.error('Failed to save some facility features. Please check logs.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: '24px' }}>
      <Title level={2}>Configure House Facilities</Title>
      <Text type="secondary" style={{ display: 'block', marginBottom: 24 }}>
        Assigning inventory and facility features to House ID: {houseId}
      </Text>
      
      <Form 
        form={form} 
        layout="vertical" 
        onFinish={onFinish}
        initialValues={{
          Beds: { quantity: 1 },
          Mattresses: { quantity: 1 },
          Tables: { quantity: 1 },
          Chairs: { quantity: 1 },
        }}
      >
        <Card title="Mandatory Features" style={{ marginBottom: 24 }}>
          {mandatoryFeatures.map((featureType) => (
            <Row gutter={16} key={featureType} style={{ marginBottom: 16 }}>
              <Col span={6}>
                <Form.Item label="Type" style={{ marginBottom: 0 }}>
                  <Input value={featureType} disabled style={{ color: '#000', backgroundColor: '#f5f5f5' }} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item 
                  name={[featureType, 'description']} 
                  label="Description (Optional)" 
                  style={{ marginBottom: 0 }}
                >
                  <Input placeholder={`e.g. Queen size, Wooden...`} />
                </Form.Item>
              </Col>
              <Col span={6}>
                <Form.Item 
                  name={[featureType, 'quantity']} 
                  label="Quantity" 
                  rules={[{ required: true, message: 'Required' }]}
                  style={{ marginBottom: 0 }}
                >
                  <InputNumber min={0} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
            </Row>
          ))}
        </Card>

        <Card title={<><AppstoreAddOutlined /> Additional Features</>} style={{ marginBottom: 24 }}>
          <Form.List name="additionalFeatures">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...restField }) => (
                  <Row gutter={16} key={key} style={{ marginBottom: 8, alignItems: 'flex-start' }}>
                    <Col span={6}>
                      <Form.Item
                        {...restField}
                        name={[name, 'type']}
                        rules={[{ required: true, message: 'Type required' }]}
                      >
                        <Input placeholder="e.g. Kitchen, TV" />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        {...restField}
                        name={[name, 'description']}
                      >
                        <Input placeholder="Description (Optional)" />
                      </Form.Item>
                    </Col>
                    <Col span={4}>
                      <Form.Item
                        {...restField}
                        name={[name, 'quantity']}
                        rules={[{ required: true, message: 'Required' }]}
                      >
                        <InputNumber min={1} style={{ width: '100%' }} placeholder="Qty" />
                      </Form.Item>
                    </Col>
                    <Col span={2} style={{ display: 'flex', alignItems: 'center', height: '32px' }}>
                      <MinusCircleOutlined 
                        onClick={() => remove(name)} 
                        style={{ color: '#ff4d4f', fontSize: '18px', cursor: 'pointer' }} 
                      />
                    </Col>
                  </Row>
                ))}
                
                <Form.Item>
                  <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                    Add Custom Feature
                  </Button>
                </Form.Item>
              </>
            )}
          </Form.List>
        </Card>

        <Form.Item style={{ textAlign: 'right' }}>
          <Button type="primary" htmlType="submit" loading={submitting}>
            Complete Housing Setup
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
}