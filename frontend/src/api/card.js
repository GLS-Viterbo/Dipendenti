import { apiFetch } from "./client";

// ===================
// CARD API
// ===================

export const createCard = (card) =>
  apiFetch("/api/cards", { method: "POST", body: card });

export const restoreCard = (id) =>
  apiFetch(`/api/cards/restore/${id}`, { method: "POST" });

export const deleteCard = (id) =>
  apiFetch(`/api/cards/${id}`, { method: "DELETE" });

export const getAllCards = () => apiFetch("/api/cards");

export const getDeletedCards = () => apiFetch("/api/cards/deleted");

export const getUnassignedCards = () => apiFetch("/api/cards/unassigned");

export const getCardById = (id) => apiFetch(`/api/cards/${id}`);

export const getNotDeletedCardCount = () => apiFetch("/api/cards/count");

export const getAssignedCardWithDetails = () => apiFetch("/api/cards/detailed")

// ===================
// ASSIGNMENT API
// ===================

export const assignCard = (assignment) =>
  apiFetch("/api/assignments", { method: "POST", body: assignment });

export const revokeAssignment = (id) =>
  apiFetch(`/api/assignments/${id}`, { method: "DELETE" });

export const getAssignedCardCount = () => apiFetch("/api/assignments/count");

export const getEmployeeAssignments = (employeeId) =>
  apiFetch(`/api/assignments/employee/${employeeId}`);

export const getCardHistory = (cardId) =>
  apiFetch(`/api/assignments/card/${cardId}/history`);

export const getAssignedEmployee = (cardId) =>
  apiFetch(`/api/assignments/card/${cardId}/employee`);

export const getCardAssignment = (cardId) =>
  apiFetch(`/api/assignments/card/${cardId}`);

// ===================
// ACCESS LOGS API
// ===================

export const readCard = (cardUid) =>
  apiFetch("/api/access-logs/read", { method: "POST", body: { cardUid } });

export const getLogsInRange = ({ start, end, date } = {}) => {
  const params = new URLSearchParams();
  if (start) params.append("start", start);
  if (end) params.append("end", end);
  if (date) params.append("date", date);
  return apiFetch(`/api/access-logs?${params.toString()}`);
};

export const getLogsByEmployee = (employeeId, start, end) => {
  const params = new URLSearchParams();
  if (start) params.append("start", start);
  if (end) params.append("end", end);
  return apiFetch(`/api/access-logs/employee/${employeeId}?${params.toString()}`);
};

export const getWorkStatus = (id) => apiFetch(`/api/access-logs/status/${id}`);

export const getEmployeeStatus = (employeeId) =>
  apiFetch(`/api/access-logs/status/${employeeId}`);

export const updateLog = (id, accessLog) =>
  apiFetch(`/api/access-logs/${id}`, { method: "PUT", body: accessLog });

export const deleteLog = (id) =>
  apiFetch(`/api/access-logs/${id}`, { method: "DELETE" });

export const getAllAnomalies = (startDate, endDate) => {
  const params = new URLSearchParams();
  params.append("startDate", startDate);
  params.append("endDate", endDate);
  return apiFetch(`/api/access-logs/anomalies?${params.toString()}`);
};

export const getActiveCount = () => apiFetch("/api/access-logs/count");