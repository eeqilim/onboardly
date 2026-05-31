import api from "../api/axios";

export function getMyHousing() {
  return api.get("/employee/housing/me");
}

export function getEmployeeHousing(houseId) {
  return api.get(houseId ? `/housing/houses/${houseId}` : "/housing/houses");
}

export function getHouses() {
  return api.get("/housing/houses");
}

export function getHouse(houseId) {
  return api.get(`/housing/houses/${houseId}`);
}

export function createHousing(house) {
  return api.post("/housing/houses", house);
}

export function deleteHousing(houseId) {
  return api.delete(`/housing/houses/${houseId}`);
}

export function getHouseDetails(houseId) {
  return api.get(`/housing/houses/${houseId}/details`);
}

export function getHouseReports(houseId, page = 0, size = 5) {
  return api.get(`/housing/houses/${houseId}/reports`, {
    params: { page, size },
  });
}

export function getLandlords() {
  return api.get("/housing/landlords");
}

export function createLandlord(landlord) {
  return api.post("/housing/landlords", landlord);
}

export function getFacilities(houseId) {
  return api.get(
    houseId ? `/housing/facilities/house/${houseId}` : "/housing/facilities",
  );
}

export function createFacility(facility) {
  return api.post("/housing/facilities", facility);
}

export function getFacilityReports(filters = {}) {
  if (filters.employeeId) {
    return api.get(`/housing/reports/employee/${filters.employeeId}`);
  }

  if (filters.facilityId) {
    return api.get(`/housing/reports/facility/${filters.facilityId}`);
  }

  if (filters.status) {
    return api.get(`/housing/reports/status/${filters.status}`);
  }

  return api.get("/housing/reports");
}

export function getFacilityReport(reportId) {
  return api.get(`/housing/reports/${reportId}`);
}

export function getFacilityReportDetails(reportId) {
  return api.get(`/housing/reports/${reportId}/details`);
}

export function createFacilityReport(report) {
  return api.post("/housing/reports", report);
}

export function updateFacilityReport(reportId, report) {
  return api.put(`/housing/reports/${reportId}`, report);
}

export function updateFacilityReportStatus(reportId, status) {
  return api.put(`/housing/reports/${reportId}/status`, { status });
}

export function deleteFacilityReport(reportId) {
  return api.delete(`/housing/reports/${reportId}`);
}

export function getFacilityReportComments(reportId) {
  return api.get(`/housing/report-details/report/${reportId}`);
}

export function addFacilityReportComment(reportId, payload) {
  return api.post("/housing/report-details", {
    facilityReportId: reportId,
    employeeId: payload.employeeId,
    comment: payload.comment || payload.description,
  });
}

export function updateFacilityReportComment(reportId, commentId, payload) {
  return api.put(`/housing/report-details/${commentId}`, {
    facilityReportId: reportId,
    employeeId: payload.employeeId,
    comment: payload.comment || payload.description,
  });
}

export function deleteFacilityReportComment(commentId) {
  return api.delete(`/housing/report-details/${commentId}`);
}
