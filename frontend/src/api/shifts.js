import { apiFetch } from './client';

export const getAllShifts = () => apiFetch("/api/shifts")
export const createShift = (newShift) => apiFetch("/api/shifts", {method: 'POST', body: newShift})
export const getEmployeeShiftAssociations = (employeeId) => apiFetch("/api/shifts/associations/employee/" + employeeId)
export const getEmployeeShiftAssignments = (employeeId, startDate, endDate) => apiFetch("/api/shifts/assignments/employee/" + employeeId + "?startDate="+ startDate + "&endDate=" + endDate);  
export const createShiftAssociation  = (newAssociation) => apiFetch("/api/shifts/associations", {method: 'POST', body: newAssociation}) 
export const assignShiftManually = (newAssignment) => apiFetch("/api/shifts/assignments/manual", {method:'POST', body:newAssignment}) 
export const deleteShiftAssociation = (assId) => apiFetch("/api/shifts/associations/" + assId, {method:'DELETE'})
export const deleteShiftAssignment = (assId) => apiFetch("/api/shifts/assignments/" + assId, {method:'DELETE'})
export const modifyAssignment = (newAssignment) => apiFetch("/api/shifts/assignments/" + newAssignment.id, {method: 'PUT', body: newAssignment})

export const generateAssignments = (startDate, endDate) => apiFetch("/api/shifts/assignments/generate?startDate=" + startDate + "&endDate=" + endDate, {method: 'POST'})

