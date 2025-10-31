import React, { useState, useEffect } from 'react';
import { Users, Search, UserPlus, CheckCircle, XCircle, Eye, Mail, Phone } from 'lucide-react';
import { getAllEmployees, createEmployee } from '../api/employees';
import { getWorkStatus } from '../api/card';
import AddEmployeeModal from '../components/AddEmployeeModal';

const EmployeeListPage = ({ onNavigateToDetails }) => {
    const [employees, setEmployees] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState('all');
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);


    useEffect(() => {
        fetchData()
    }, []);

    async function fetchData() {
        try {
            const employees = await getAllEmployees();
            const employeesWithStatus = await Promise.all(
                employees.map(async (employee) => {
                    const workStatus = await getWorkStatus(employee.id);
                    return {
                        ...employee,
                        isWorking: workStatus.isWorking,
                    };
                })
            );
            setEmployees(employeesWithStatus);
            setLoading(false);
        } catch (error) {
            console.error('Errore nel recupero dei dati dei dipendenti:', error.message);
        }
    }


    const filteredEmployees = employees.filter(emp => {
        const matchesSearch =
            emp.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
            emp.surname.toLowerCase().includes(searchTerm.toLowerCase()) ||
            emp.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
            emp.taxCode.toLowerCase().includes(searchTerm.toLowerCase());

        const matchesStatus =
            filterStatus === 'all' ||
            (filterStatus === 'working' && emp.isWorking) ||
            (filterStatus === 'not-working' && !emp.isWorking);

        return matchesSearch && matchesStatus && !emp.deleted;
    });

    const workingCount = employees.filter(e => e.isWorking && !e.deleted).length;
    const notWorkingCount = employees.filter(e => !e.isWorking && !e.deleted).length;

    return (
        <div>
            {/* Stats Cards */}
            <div className="row g-3 mb-4">
                <div className="col-md-4">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="text-muted mb-1 small">Totale Dipendenti</p>
                                    <h3 className="fw-bold mb-0">{employees.filter(e => !e.deleted).length}</h3>
                                </div>
                                <div className="bg-primary bg-opacity-10 rounded p-2">
                                    <Users size={24} className="text-primary" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-md-4">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="text-muted mb-1 small">Al Lavoro</p>
                                    <h3 className="fw-bold mb-0 text-success">{workingCount}</h3>
                                </div>
                                <div className="bg-success bg-opacity-10 rounded p-2">
                                    <CheckCircle size={24} className="text-success" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-md-4">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="text-muted mb-1 small">Non al Lavoro</p>
                                    <h3 className="fw-bold mb-0 text-secondary">{notWorkingCount}</h3>
                                </div>
                                <div className="bg-secondary bg-opacity-10 rounded p-2">
                                    <XCircle size={24} className="text-secondary" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Search and Filter Bar */}
            <div className="card border-0 shadow-sm mb-4">
                <div className="card-body">
                    <div className="row g-3 align-items-center">
                        <div className="col-md-6">
                            <div className="input-group">
                                <span className="input-group-text bg-white border-end-0">
                                    <Search size={18} className="text-muted" />
                                </span>
                                <input
                                    type="text"
                                    className="form-control border-start-0"
                                    placeholder="Cerca per nome, cognome, email o codice fiscale..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>
                        </div>

                        <div className="col-md-4">
                            <select
                                className="form-select"
                                value={filterStatus}
                                onChange={(e) => setFilterStatus(e.target.value)}
                            >
                                <option value="all">Tutti i dipendenti</option>
                                <option value="working">Solo al lavoro</option>
                                <option value="not-working">Non al lavoro</option>
                            </select>
                        </div>

                        <div className="col-md-2">
                            <button
                                className="btn btn-primary w-100 d-flex align-items-center justify-content-center gap-2"
                                onClick={() => setIsModalOpen(true)}
                            >
                                <UserPlus size={18} />
                                <span>Aggiungi</span>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Employee List - resto del codice uguale */}
            <div className="card border-0 shadow-sm">
                <div className="card-header bg-white border-bottom">
                    <div className="d-flex justify-content-between align-items-center">
                        <h5 className="mb-0 fw-semibold">Elenco Dipendenti</h5>
                        <span className="badge bg-primary rounded-pill">{filteredEmployees.length} risultati</span>
                    </div>
                </div>
                <div className="card-body p-0">
                    {loading ? (
                        <div className="text-center py-5">
                            <div className="spinner-border text-primary" role="status">
                                <span className="visually-hidden">Caricamento...</span>
                            </div>
                        </div>
                    ) : filteredEmployees.length === 0 ? (
                        <div className="text-center py-5">
                            <Users size={48} className="text-muted mb-3" />
                            <p className="text-muted">Nessun dipendente trovato</p>
                        </div>
                    ) : (
                        <div className="table-responsive">
                            <table className="table table-hover align-middle mb-0">
                                <thead className="bg-light">
                                    <tr>
                                        <th className="border-0 px-4 py-3">
                                            <small className="text-muted fw-semibold">STATO</small>
                                        </th>
                                        <th className="border-0 px-4 py-3">
                                            <small className="text-muted fw-semibold">DIPENDENTE</small>
                                        </th>
                                        <th className="border-0 px-4 py-3">
                                            <small className="text-muted fw-semibold">CODICE FISCALE</small>
                                        </th>
                                        <th className="border-0 px-4 py-3">
                                            <small className="text-muted fw-semibold">CONTATTI</small>
                                        </th>
                                        <th className="border-0 px-4 py-3">
                                            <small className="text-muted fw-semibold">CITTÀ</small>
                                        </th>
                                        <th className="border-0 px-4 py-3 text-end">
                                            <small className="text-muted fw-semibold">AZIONI</small>
                                        </th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredEmployees.map((employee) => (
                                        <tr key={employee.id}>
                                            <td className="px-4 py-3">
                                                {employee.isWorking ? (
                                                    <span className="badge bg-success bg-opacity-10 text-success border border-success d-inline-flex align-items-center gap-1 px-2 py-1">
                                                        <CheckCircle size={14} />
                                                        <small>Al lavoro</small>
                                                    </span>
                                                ) : (
                                                    <span className="badge bg-secondary bg-opacity-10 text-secondary border border-secondary d-inline-flex align-items-center gap-1 px-2 py-1">
                                                        <XCircle size={14} />
                                                        <small>Assente</small>
                                                    </span>
                                                )}
                                            </td>
                                            <td className="px-4 py-3">
                                                <div className="d-flex align-items-center gap-3">
                                                    <div
                                                        className="bg-primary bg-opacity-10 rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
                                                        style={{ width: '40px', height: '40px' }}
                                                    >
                                                        <span className="text-primary fw-bold small">
                                                            {employee.name.charAt(0)}{employee.surname.charAt(0)}
                                                        </span>
                                                    </div>
                                                    <div>
                                                        <div className="fw-semibold">{employee.name} {employee.surname}</div>
                                                    </div>
                                                </div>
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className="font-monospace small text-muted">{employee.taxCode.toUpperCase()}</span>
                                            </td>
                                            <td className="px-4 py-3">
                                                <div className="d-flex flex-column gap-1">
                                                    <div className="d-flex align-items-center gap-2 small">
                                                        <Mail size={14} className="text-muted" />
                                                        <span className="text-muted">{employee.email}</span>
                                                    </div>
                                                    <div className="d-flex align-items-center gap-2 small">
                                                        <Phone size={14} className="text-muted" />
                                                        <span className="text-muted">{employee.phone}</span>
                                                    </div>
                                                </div>
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className="text-muted">{employee.city}</span>
                                            </td>
                                            <td className="px-4 py-3 text-end">
                                                <button
                                                    className="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-2"
                                                    onClick={() => onNavigateToDetails(employee.id)}
                                                >
                                                    <Eye size={16} />
                                                    <span>Dettagli</span>
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>

            {/* Modal */}
            <AddEmployeeModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onRefresh={fetchData}
            />
        </div>
    );
};

export default EmployeeListPage;