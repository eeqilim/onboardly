import api from "../api/axios";

export function getEmployeeProfile(employeeId) {
  return api.get(
    employeeId ? `/employee/profile/${employeeId}` : "/employee/profile/me",
  );
}

export function getEmployeeDocuments(employeeId) {
  return api.get(
    employeeId
      ? `/employee/documents/employee/${employeeId}`
      : "/employee/documents/me",
  );
}

export function updateEmployeeProfileSection(employeeId, section, values) {
  return api.put(`/employee/profile/${employeeId}/${section}`, values);
}

export function addEmployeeContact(employeeId, values) {
  return api.post(`/employee/profile/${employeeId}/contact`, values);
}

export function updateEmployeeContact(employeeId, contactId, values) {
  return api.put(
    `/employee/profile/${employeeId}/contact/${contactId}`,
    values,
  );
}

export function deleteEmployeeContact(employeeId, contactId) {
  return api.delete(`/employee/profile/${employeeId}/contact/${contactId}`);
}

export function uploadEmployeeAvatar(employeeId, file) {
  const data = new FormData();
  data.append("file", file);
  return api.post(`/employee/profile/${employeeId}/avatar`, data, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

export function submitEmployeeOnboarding(payload) {
  return api.post("/employee/onboarding/submit", payload);
}

// Backend's @RequestPart needs the metadata as a JSON-typed part, not a plain
// form field — wrap it in a Blob so the Content-Type of that part is
// application/json instead of text/plain.
export function uploadEmployeeDocument(file, meta) {
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

export function startEmployeeOnboarding() {
  return api.post("/employee/onboarding/start");
}

export function getMyOnboardingApplication() {
  return api.get("/employee/onboarding/me");
}

// weihao changed
export function getEmployeeProfiles(page = 0) {
  return api.get(`/employee/hr/employees?page=${page}`);
}
// =====================
export function searchEmployeeProfiles(query) {
  return api.get(`/employee/hr/employees/search?q=${query}`);
}

export function addDocumentComment(documentId, comment) {
  return api.post(`/employee/documents/${documentId}/hr-comment`, {
    comment: comment,
  });
}

export function getDocumentPreviewUrl(documentId) {
  return api.get(`/employee/documents/${documentId}/preview-url`);
}

export function getApplicationTracking() {
  return api.get("/employee/hr/tracking");
}

export function getHouseSummaries() {
  return api.get("/employee/hr/housing/summary");
}

export function getAvailableHousings() {
  return api.get("/employee/hr/housing/available");
}

export function assignEmployeeToHouse(employeeId, houseId) {
  return api.put(`/employee/hr/employees/${employeeId}/house/${houseId}`);
}

export function removeEmployeeFromHouse(employeeId) {
  return api.delete(`/employee/hr/employees/${employeeId}/house`);
}

export function getHouseResidents(houseId) {
  return api.get(`/employee/hr/house/${houseId}/residents`);
}
