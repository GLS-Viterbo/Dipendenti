import { apiFetch } from "./client";

// COMPANIES
export const getActiveCompanies = () => apiFetch("/api/companies?activeOnly=true")
export const addCompany = (newCompany) => apiFetch("/api/companies", {method: 'POST', body:newCompany})
export const updateCompany = (companyId, updatedCompany) => apiFetch("/api/companies/" + companyId, {method: 'PUT', body: updatedCompany})
export const delComapny = (companyId) => apiFetch("/api/companies/" + companyId, {method: 'DELETE'})

// USERS
export const getActiveUsers  = () => apiFetch("/api/users")
export const addUser = (newUser) => apiFetch("/api/users", {method: 'POST', body: newUser})
export const updateUser = (userId, updatedUser) => apiFetch("/api/users/" + userId, {method: 'PUT', body: updatedUser})
export const deleteUser = (userId) => apiFetch("/api/users/" + userId, {method: 'DELETE'})

// ROLES
export const getAllRoles = () => apiFetch("/api/roles")
export const getUserRoles = (userId) => apiFetch("/api/roles/user/" + userId)
export const assignRole = (userId, roleId) => apiFetch("/api/users/" + userId + "/roles/" + roleId, {method: 'POST'})
export const revokeRole = (userId, roleId) => apiFetch("/api/users/" + userId + "/roles/" + roleId, {method: 'DELETE'})