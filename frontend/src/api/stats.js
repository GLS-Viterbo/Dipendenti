import { getCurrentYearMonth } from "../utils/utils";
import { apiFetch } from "./client";

export const getEmployeeStatsForYearMonth = (employeeId, yearMonth = getCurrentYearMonth()) => {
  return apiFetch(`/api/stats/employee/${employeeId}/monthly?yearMonth=${yearMonth}`);
};