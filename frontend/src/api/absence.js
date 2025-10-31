import { get3MonthsFromNow, getTodayDate } from "../utils/utils";
import { apiFetch, apiMultipart } from "./client";

export const getTodayApprovedCount = () => apiFetch("/api/absences/today")
export const createApprovedAbsence = (absenceData) => apiFetch("/api/absences", {method: 'POST', body: absenceData}) 
export const deleteAbsence = (absenceId) => apiFetch("/api/absences/" + absenceId, {method: 'DELETE'})
export const getFutureToApproveCount = () => apiFetch("/api/absences/future")
export const getDetailedEmployeeBalance = (employeeId) => apiFetch("/api/absences/" + employeeId + "/detailed-balance")
export const getAbsencesInRange = (startDate, endDate) => apiFetch("/api/absences/detailed?startDate=" + startDate + "&endDate=" + endDate)
export const getEmployeeAbsencesInRange = (startDate, endDate, employeeId) => apiFetch("/api/absences/detailed?startDate=" + startDate + "&endDate=" + endDate + "&employeeId=" + employeeId)
export const initializeBalance = (balanceData) => apiFetch("/api/absences/init", { method: "POST", body: balanceData })
export const getEmployeeAccrual = (employeeId) => apiFetch("/api/absences/" + employeeId + "/accrual")
export const updateEmployeeAccrual = (accrualData) => apiFetch("/api/absences/" + accrualData.id + "/accrual", {method: 'PUT', body: accrualData})
export const updateEmployeeBalance = (balanceId, balanceData) => apiFetch("/api/absences/" + balanceId + "/balance", {method: 'PUT', body: balanceData})
export const getNext3MonthEmployeeAbsences = (employeeId) => apiFetch("/api/absences/detailed?startDate=" + getTodayDate() + "&endDate=" + get3MonthsFromNow() + "&employeeId=" + employeeId)
export const getAbsenceNeeded = (employeeId, startDate, endDate) => apiFetch("/api/absences/needed?startDate=" + startDate + "&endDate=" + endDate + "&employeeId=" + employeeId)

// HOLIDAYS

export const addHoliday = (newHoliday) => apiFetch("/api/holidays", {method: 'POST', body: newHoliday}) 
export const getHolidaysInRange = (startDate, endDate) => apiFetch("/api/holidays?startDate=" + startDate + "&endDate=" + endDate)
export const deleteHoliday = (holidayId) => apiFetch("/api/holidays/" + holidayId, {method: 'DELETE'}) 