import { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  List,
  Select,
  Space,
  Tag,
  Typography,
} from "antd";
import { CommentOutlined, EditOutlined, ToolOutlined } from "@ant-design/icons";
import { getEmployeeProfile } from "../../services/employeeService";
import {
  addFacilityReportComment,
  createFacilityReport,
  getFacilityReportDetails,
  getFacilityReports,
  getHouseDetails,
  getHouseReports,
  updateFacilityReportComment,
} from "../../services/housingService";
import "./FacilityReport.css";

const { Text, Title } = Typography;
const { TextArea } = Input;

const USE_MOCK_DATA = false;

const mockReports = [
  {
    reportId: 1,
    facilityId: 1,
    employeeId: 1,
    title: "Mock Report",
    description: "Mock Report Description",
    createdBy: "John",
    reportDate: "2026-05-01",
    status: "OPEN",
    comments: [
      {
        commentId: 1,
        employeeId: 1,
        description: "Mock Report Comment",
        createdBy: "You",
        createdByCurrentUser: true,
        createdDate: "2026-05-02",
        lastModifiedDate: null,
      },
    ],
  },
];

const mockFacilities = [
  {
    id: 1,
    type: "Facility 1",
    description: "Facility 1 description",
    quantity: 1,
  },
  {
    id: 2,
    type: "Facility 2",
    description: "Facility 2 description",
    quantity: 1,
  },
];

function getReportId(report) {
  return report.reportId || report.id;
}

function getCommentId(comment) {
  return comment.commentId || comment.id;
}

function getResponseData(response) {
  return response.data?.data || response.data;
}

function getReportList(response) {
  const data = getResponseData(response);
  return data?.content || data || [];
}

function getFacilityLabel(facility) {
  if (!facility) return "-";

  return (
    [facility.type, facility.description].filter(Boolean).join(" - ") ||
    `Facility ${facility.id}`
  );
}

function Field({ label, value, className = "" }) {
  return (
    <div className={`facility-field ${className}`.trim()}>
      <Text type="secondary">{label}</Text>
      <Text strong>{value || "-"}</Text>
    </div>
  );
}

function getCommentDate(comment) {
  return (
    comment.lastModificationDate ||
    comment.lastModifiedDate ||
    comment.createDate ||
    comment.createdDate ||
    "-"
  );
}

function getCommentEmployeeId(comment) {
  return (
    comment.employeeId || comment.createdById || comment.createdByEmployeeId
  );
}

function canEditComment(comment, employeeId) {
  if (comment.createdByCurrentUser) return true;

  const commentEmployeeId = getCommentEmployeeId(comment);

  return (
    commentEmployeeId != null &&
    employeeId != null &&
    String(commentEmployeeId) === String(employeeId)
  );
}

function getReportDate(report) {
  return report.reportDate || report.createDate || report.createdDate || "-";
}

export default function FacilityReports() {
  const [form] = Form.useForm();
  const [reports, setReports] = useState([]);
  const [newReport, setNewReport] = useState({ title: "", description: "" });
  const [employeeId, setEmployeeId] = useState(null);
  const [facilities, setFacilities] = useState([]);
  const [commentText, setCommentText] = useState({});
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingCommentText, setEditingCommentText] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadReports();
  }, []);

  async function loadReports() {
    setLoading(true);
    setStatus("");

    if (USE_MOCK_DATA) {
      setReports(mockReports);
      setFacilities(mockFacilities);
      setEmployeeId(1);
      setLoading(false);
      return;
    }

    try {
      const profileResponse = await getEmployeeProfile();
      const profile = profileResponse.data || {};
      const nextEmployeeId = profile.userId || profile.employeeId;
      const houseId = profile.houseId || profile.house?.id;

      setEmployeeId(nextEmployeeId);

      if (houseId) {
        // TODO: Confirm whether facilities should be loaded from house details or /facilities/house/{houseId}.
        const houseResponse = await getHouseDetails(houseId);
        const houseDetails = getResponseData(houseResponse);
        setFacilities(houseDetails?.facilities || []);
      } else {
        setStatus("No housing assigned.");
      }

      // TODO: Confirm house report endpoint is the intended source for employee-visible reports.
      const reportResponse = houseId
        ? await getHouseReports(houseId)
        : await getFacilityReports({
            employeeId: nextEmployeeId,
          });
      const reportList = getReportList(reportResponse);
      const reportsWithDetails = await Promise.all(
        reportList.map(async (report) => {
          const reportId = getReportId(report);

          if (!reportId) return report;

          try {
            const detailResponse = await getFacilityReportDetails(reportId);
            const detail = getResponseData(detailResponse);
            return { ...report, ...detail };
          } catch {
            return report;
          }
        }),
      );

      setReports(reportsWithDetails);
    } catch {
      setStatus("Failed to load facility reports.");
    } finally {
      setLoading(false);
    }
  }

  function handleReportChange(e) {
    const { name, value } = e.target;
    setNewReport((prev) => ({ ...prev, [name]: value }));
  }

  async function submitReport(values) {
    const reportValues = {
      facilityId: Number(values.facilityId || newReport.facilityId),
      employeeId,
      title: values.title || newReport.title,
      description: values.description || newReport.description,
    };

    if (
      !reportValues.facilityId ||
      !reportValues.employeeId ||
      !reportValues.title.trim() ||
      !reportValues.description.trim()
    ) {
      setStatus("Facility, employee, title, and description are required.");
      return;
    }

    if (USE_MOCK_DATA) {
      const report = {
        reportId: Date.now(),
        facilityId: reportValues.facilityId,
        employeeId: reportValues.employeeId,
        title: reportValues.title,
        description: reportValues.description,
        createdBy: "You",
        reportDate: new Date().toISOString(),
        status: "OPEN",
        comments: [],
      };

      setReports((prev) => [report, ...prev]);
      setNewReport({ title: "", description: "" });
      form.resetFields();
      setStatus("Facility report created.");
      return;
    }

    try {
      await createFacilityReport(reportValues);
      setNewReport({ title: "", description: "" });
      form.resetFields();
      setStatus("Facility report submitted.");
      await loadReports();
    } catch {
      setStatus("Failed to submit facility report.");
    }
  }

  async function addComment(reportId) {
    const description = commentText[reportId];

    if (!description?.trim()) {
      setStatus("Comment cannot be empty.");
      return;
    }

    if (USE_MOCK_DATA) {
      setReports((prev) =>
        prev.map((report) =>
          report.reportId === reportId
            ? {
                ...report,
                comments: [
                  ...(report.comments || []),
                  {
                    commentId: Date.now(),
                    employeeId,
                    description,
                    createdBy: "You",
                    createdByCurrentUser: true,
                    createdDate: new Date().toISOString(),
                    lastModifiedDate: null,
                  },
                ],
              }
            : report,
        ),
      );

      setCommentText((prev) => ({ ...prev, [reportId]: "" }));
      setStatus("Comment added.");
      return;
    }

    try {
      if (!employeeId) {
        setStatus("Cannot add comment because employee ID is missing.");
        return;
      }

      await addFacilityReportComment(reportId, { employeeId, description });

      setCommentText((prev) => ({ ...prev, [reportId]: "" }));
      setStatus("Comment added.");
      await loadReports();
    } catch {
      setStatus("Failed to add comment.");
    }
  }

  function startEditComment(comment) {
    setEditingCommentId(getCommentId(comment));
    setEditingCommentText(comment.comment || comment.description || "");
  }

  function cancelEditComment() {
    setEditingCommentId(null);
    setEditingCommentText("");
  }

  async function updateComment(reportId, commentId) {
    if (!editingCommentText.trim()) {
      setStatus("Comment cannot be empty.");
      return;
    }

    if (USE_MOCK_DATA) {
      setReports((prev) =>
        prev.map((report) =>
          report.reportId === reportId
            ? {
                ...report,
                comments: report.comments.map((comment) =>
                  comment.commentId === commentId
                    ? {
                        ...comment,
                        description: editingCommentText,
                        lastModifiedDate: new Date().toISOString(),
                      }
                    : comment,
                ),
              }
            : report,
        ),
      );

      setEditingCommentId(null);
      setEditingCommentText("");
      setStatus("Comment updated.");
      return;
    }

    try {
      if (!employeeId) {
        setStatus("Cannot update comment because employee ID is missing.");
        return;
      }

      // TODO: Confirm backend enforces that employees can only update their own comments.
      await updateFacilityReportComment(reportId, commentId, {
        employeeId,
        description: editingCommentText,
      });

      setEditingCommentId(null);
      setEditingCommentText("");
      setStatus("Comment updated.");
      await loadReports();
    } catch {
      setStatus("Failed to update comment.");
    }
  }

  if (loading) return <div className="facility-page">Loading...</div>;

  const sortedReports = [...reports].sort(
    (a, b) => new Date(getReportDate(b)) - new Date(getReportDate(a)),
  );

  function getReportFacilityLabel(report) {
    const facility = facilities.find(
      (item) => item.id === (report.facilityId || report.facility?.id),
    );

    return (
      report.facilityName || report.facilityType || getFacilityLabel(facility)
    );
  }

  return (
    <div className="facility-page">
      <div className="facility-title-row">
        <Title level={2}>Facility Reports</Title>
        {status && <Alert message={status} type="info" showIcon />}
      </div>

      <div className="facility-layout">
        <Card
          className="facility-card facility-form-card"
          title={
            <span className="facility-card-title">
              <ToolOutlined />
              <span>Report Facility Issue</span>
            </span>
          }
        >
          <Form form={form} layout="vertical" onFinish={submitReport}>
            <Form.Item
              label="Facility"
              name="facilityId"
              rules={[{ required: true, message: "Please select a facility." }]}
            >
              <Select
                placeholder="Select a facility"
                options={facilities.map((facility) => ({
                  value: facility.id,
                  label: getFacilityLabel(facility),
                }))}
              />
            </Form.Item>

            <Form.Item
              label="Title"
              name="title"
              rules={[{ required: true, message: "Please enter a title." }]}
            >
              <Input
                name="title"
                value={newReport.title}
                onChange={handleReportChange}
                placeholder="Brief title of the issue"
              />
            </Form.Item>

            <Form.Item
              label="Description"
              name="description"
              rules={[
                { required: true, message: "Please enter a description." },
              ]}
            >
              <TextArea
                name="description"
                value={newReport.description}
                onChange={handleReportChange}
                placeholder="Describe the facility issue"
                rows={4}
              />
            </Form.Item>

            <Button type="primary" htmlType="submit">
              Submit Report
            </Button>
          </Form>
        </Card>

        <Card
          className="facility-card facility-reports-card"
          title={
            <span className="facility-card-title">
              <CommentOutlined />
              <span>Existing Reports</span>
            </span>
          }
        >
          {sortedReports.length === 0 ? (
            <Empty description="No facility reports" />
          ) : (
            <List
              className="facility-report-list"
              dataSource={sortedReports}
              renderItem={(report) => (
                <List.Item key={getReportId(report)}>
                  <Card
                    className="facility-report-item"
                    title={report.title}
                    extra={<Tag color="blue">{report.status || "-"}</Tag>}
                  >
                    <div className="facility-grid">
                      <Field label="Description" value={report.description} />
                      <Field
                        label="Created By"
                        value={report.createdByName || report.createdBy}
                      />
                      <Field
                        label="Report Date"
                        value={
                          getReportDate(report) !== "-"
                            ? new Date(
                                getReportDate(report),
                              ).toLocaleDateString()
                            : "-"
                        }
                      />
                      <Field
                        className="facility-field-full"
                        label="Facility"
                        value={getReportFacilityLabel(report)}
                      />
                    </div>

                    <Title level={5}>Comments</Title>

                    {(report.comments || []).length === 0 ? (
                      <Text type="secondary">No comments yet.</Text>
                    ) : (
                      <List
                        className="facility-comment-list"
                        dataSource={report.comments}
                        renderItem={(comment) => (
                          <List.Item key={getCommentId(comment)}>
                            {editingCommentId === getCommentId(comment) ? (
                              <Space
                                direction="vertical"
                                className="facility-comment-edit"
                              >
                                <TextArea
                                  rows={3}
                                  value={editingCommentText}
                                  onChange={(e) =>
                                    setEditingCommentText(e.target.value)
                                  }
                                />

                                <Space>
                                  <Button
                                    type="primary"
                                    onClick={() =>
                                      updateComment(
                                        getReportId(report),
                                        getCommentId(comment),
                                      )
                                    }
                                  >
                                    Save Comment
                                  </Button>

                                  <Button onClick={cancelEditComment}>
                                    Cancel
                                  </Button>
                                </Space>
                              </Space>
                            ) : (
                              <div className="facility-comment">
                                <div className="facility-comment-inline">
                                  <Text>
                                    {comment.comment || comment.description}
                                  </Text>
                                  <Text type="secondary">
                                    {comment.commenterName ||
                                      comment.createdBy ||
                                      "-"}{" "}
                                    ·{" "}
                                    {getCommentDate(comment) !== "-"
                                      ? new Date(
                                          getCommentDate(comment),
                                        ).toLocaleDateString()
                                      : "-"}
                                  </Text>

                                  {canEditComment(comment, employeeId) && (
                                    <Button
                                      icon={<EditOutlined />}
                                      size="small"
                                      type="primary"
                                      onClick={() => startEditComment(comment)}
                                    >
                                      Edit
                                    </Button>
                                  )}
                                </div>
                              </div>
                            )}
                          </List.Item>
                        )}
                      />
                    )}

                    <div className="facility-comment-box">
                      <TextArea
                        placeholder="Add a comment"
                        rows={3}
                        value={commentText[getReportId(report)] || ""}
                        onChange={(e) =>
                          setCommentText((prev) => ({
                            ...prev,
                            [getReportId(report)]: e.target.value,
                          }))
                        }
                      />

                      <Button
                        type="primary"
                        onClick={() => addComment(getReportId(report))}
                      >
                        Add Comment
                      </Button>
                    </div>
                  </Card>
                </List.Item>
              )}
            />
          )}
        </Card>
      </div>
    </div>
  );
}
