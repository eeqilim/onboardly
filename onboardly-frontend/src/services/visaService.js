import api from "../api/axios";

export function getVisaStatus() {
  return api.get("/employee/visa/me/stem-opt/progress");
}

export function getVisaDocuments() {
  // TODO: Confirm backend returns every uploaded employee visa/work authorization document from this endpoint.
  return api.get("/employee/documents/me");
}

export function uploadVisaDocument(formData, step) {
  return api.post(`/employee/visa/me/stem-opt/upload?step=${step}`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

export function reviewVisaStatus(visaStatusId, status, hrFeedback = "") {
  return api.post(`/employee/visa/hr/${visaStatusId}/review`, { status, hrFeedback });
}

export function getStemOptProgress() {
  return api.get("/employee/visa/me/stem-opt/progress");
}

export function getActiveVisaStatus() {
  return api.get("/employee/visa/me/active");
}

export function getVisaHistory() {
  return api.get("/employee/visa/me/history");
}

export function getAllEmployeeWithActiveVisa() {
  return api.get("/employee/visa/hr/all");
};