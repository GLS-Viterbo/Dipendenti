// Fetch for access module
import { apiFetch } from './client';
import { getStartOfMonth, getEndOfMonth } from "../utils/utils"

export const getAtWrokCount = () => apiFetch('/api/access-logs/count');
export const getDetailedLogs = (date, page, size, search, type, modified, deleted ) => apiFetch(`/api/access-logs/detailed?date=${date}&page=${page}&size=${size}&search=${search}&type=${type}&modified=${modified}&deleted=${deleted}`);
export const getAllDetailedLogs = (date) => apiFetch(`/api/access-logs/detailed/all?date=${date}`);
export const updateAccessLog = (id, log) => apiFetch("/api/access-logs/"+id, { method: "PUT", body: log })
export const deleteLog = (id) => apiFetch("/api/access-logs/"+id, { method: "DELETE"})
export const addManualLog = (log) => apiFetch("/api/access-logs", {method: "POST", body: log})
export const isAtWork = (employeeId) => apiFetch("/api/access-logs/status/" + employeeId)
export const getTodaysEmployeeLogs = (employeeId) => 
  apiFetch(`/api/access-logs/employee/${employeeId}?start=${new Date(new Date().setHours(0,0,0,0)).toISOString()}&end=${new Date(new Date().setHours(23,59,59,999)).toISOString()}`);
export const getAllAnomalies = () => apiFetch(`/api/access-logs/anomalies?startDate=${getStartOfMonth()}&endDate=${getEndOfMonth()}`);
export const hasAnomalies = (employeeId) => apiFetch("/api/access-logs/employee/" + employeeId + "/has-anomalies?startDate=" + getStartOfMonth() + "&endDate=" + getEndOfMonth())


