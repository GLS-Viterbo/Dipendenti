import { apiFetch } from "./client";

export const setContract = (contractData) => apiFetch("/api/contracts", { method: "POST", body: contractData })
export const getEmployeeContract = (employeeId) => apiFetch("/api/contracts/employee/" + employeeId)
export const getEmployeeContracts = (employeeId) => apiFetch("/api/contracts/employee/" + employeeId + "/all")
export const modifyContract = (contractId, contractData) => apiFetch("/api/contracts/"+ contractId, {method:'PUT', body: contractData})
export const invalidateContract = (contractId) => apiFetch("/api/contracts/" + contractId, {method: 'DELETE'})
export const addContract = (contractData) => apiFetch("/api/contracts", {method: 'POST', body: contractData})