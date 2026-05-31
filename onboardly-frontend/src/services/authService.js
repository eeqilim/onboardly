import api from "../api/axios";

export function loginUser(credentials) {
  return api.post("/auth/login", credentials);
}

export function generateRegistrationToken(payload) {
  return api.post("/auth/token/generate", payload);
}

export function registerEmployeeAccount(payload) {
  return api.post("/auth/register", payload);
}
