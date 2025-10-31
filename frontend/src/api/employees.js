// Fetch for employees module
import { apiFetch, apiFetchFile, apiMultipart } from './client';

export const getEmployeeCount = () => apiFetch('/api/employees/count');
export const getAllEmployees =  () => apiFetch("/api/employees")
export const getEmployeeById = (id) => apiFetch("/api/employees/" + id)
export const getEmployeesWithoutCard = () => apiFetch("/api/employees/without-card")
export const createEmployee = (employee) =>
  apiFetch("/api/employees", { method: "POST", body: employee });
export const updateEmployee = (employeeId, updatedEmployee) =>
  apiFetch("/api/employees/" + employeeId, { method: "PUT", body: updatedEmployee });
export const deleteEmployee = (employeeId) => apiFetch("/api/employees/" + employeeId, {method: "DELETE"})
export const getEmployeeGroups = (employeeId) => apiFetch("/api/employees/" + employeeId + "/groups")
export const removeMemberFromGroup = (employeeId, groupId) => apiFetch("/api/groups/" + groupId + "/members/" + employeeId, {method: 'DELETE'}) 
export const addMemberToGroup = (employeeId, groupId) => apiFetch("/api/groups/" + groupId + "/members/" + employeeId, {method: 'POST'}) 

// DOCUMENTS
export const getEmployeeDocuments = (employeeId) => apiFetch("/api/documents/employee/" + employeeId)
export const downloadDocument = (documentId) => apiFetchFile("/api/documents/" + documentId + "/download", {}, 'blob')
export const deleteDocument = (documentId) => apiFetch("/api/documents/" + documentId, {method:'DELETE'})
export const addDocument = (formData) => 
    apiMultipart("/api/documents/upload", formData, { method: 'POST' });


// DEADLINES
export const getEmployeeDeadlines = (employeeId) => apiFetch("/api/deadlines/employee/" + employeeId)
export const deleteDeadline = (deadlineId) => apiFetch("/api/deadlines" + deadlineId)
export const addDeadline = (newDeadline) => apiFetch("/api/deadlines", {method: 'POST', body: newDeadline})


// GROUPS
export const getAllGroups = () => apiFetch("/api/groups")
export const createGroup = (newGroup) => apiFetch("/api/groups", {method: 'POST', body: newGroup })
export const addEmployeeToGroup = (employeeId, groupId) => apiFetch("/api/groups/" + groupId + "/members/" + employeeId, {method: 'POST'})