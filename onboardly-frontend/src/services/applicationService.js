import api from "../api/axios";

export function getApplicationWorkflow(applicationId) {
  return api.get(`/application/${applicationId}`);
}

export function getEmployeeApplications(employeeId) {
  return api.get(`/application/employee/${employeeId}`);
}

export function advanceApplicationWorkflow(applicationId, payload) {
  return api.patch(`/application/${applicationId}/advance`, payload);
}

export function reviewApplicationWorkflow(applicationId, payload) {
  return api.put(`/application/${applicationId}/review`, payload);
}

export function getOptStemApplications() {
  return api.get('/application/type/OPT_STEM');
}

export function getApplicationsByType(applicationType) {
  return api.get(`/application/type/${applicationType}`);
}