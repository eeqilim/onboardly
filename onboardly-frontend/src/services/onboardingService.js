import api from "../api/axios";

export function getOnboardingTemplates() {
  return api.get("/application/documents/templates", {
    params: { applicationType: "ONBOARDING" },
  });
}

export function getMyOnboardingDocuments() {
  return api.get("/application/documents/me/application-type/ONBOARDING");
}

export function uploadOnboardingDocument(file, meta) {
  const data = new FormData();
  data.append("file", file);
  data.append(
    "meta",
    new Blob([JSON.stringify(meta)], { type: "application/json" }),
  );
  return api.post("/employee/documents/upload", data, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

export function submitOnboardingApplication() {
  return api.post("/employee/onboarding/submit");
}

export function getDocumentPreviewUrl(documentId) {
  return api.get(`/employee/documents/${documentId}/preview-url`);
}

export function getTemplatePreviewUrl(key) {
  return api.get("/employee/documents/template-preview-url", {
    params: { key },
  });
}

export function downloadDocument(documentId) {
  return api.get(`/employee/documents/${documentId}/download`, {
    responseType: "blob",
  });
}

export function deleteDocument(documentId) {
  return api.delete(`/employee/documents/${documentId}`);
}

export function getEmployeeOnboardingForm(employeeId) {
  return api.get(`/employee/onboarding/hr/${employeeId}`);
}

export function addApplicationComment(applicationId, comment) {
  return api.post(`/employee/onboarding/${applicationId}/comment`, comment);
}

export function getPendingOnboardingApplications() {
  return api.get("/employee/onboarding/pending");
}
// add by weihao
export function reviewApplication(applicationId, payload) {
  return api.post(`/employee/onboarding/${applicationId}/review`, payload);
}
