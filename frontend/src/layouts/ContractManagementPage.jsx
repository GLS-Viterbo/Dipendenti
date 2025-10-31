import React, { useState, useMemo, useEffect } from 'react';
import { Search, Filter, Edit, X, Briefcase, Calendar, Clock, Users, CheckCircle, XCircle, AlertCircle, Save, Plane, Coffee, Plus, Trash } from 'lucide-react';
import { getAllEmployees } from '../api/employees';
import { addContract, getEmployeeContracts, invalidateContract, modifyContract } from '../api/contract';
import { getEmployeeAccrual, updateEmployeeAccrual } from '../api/absence';
import { toast } from "react-toastify";

const ContractManagementPage = () => {
    const [employees, setEmployees] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState('all');
    const [showEditModal, setShowEditModal] = useState(false);
    const [showAccrualModal, setShowAccrualModal] = useState(false);
    const [showNewContractModal, setShowNewContractModal] = useState(false);
    const [selectedContract, setSelectedContract] = useState(null);
    const [selectedAccrual, setSelectedAccrual] = useState(null);
    const [selectedEmployeeForNew, setSelectedEmployeeForNew] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchEmployeesData();
    }, []);

    async function fetchEmployeesData() {
        setLoading(true);

        try {
            const allEmployees = await getAllEmployees();

            const employeesWithDetails = await Promise.all(
                allEmployees.map(async (employee) => {
                    let contracts = null;
                    let accrual = null;

                    try {
                        contracts = await getEmployeeContracts(employee.id);
                    } catch (err) {
                        console.warn(`Errore nel fetch dei contratti per employee ${employee.id}:`, err);
                        contracts = [];
                    }

                    try {
                        accrual = await getEmployeeAccrual(employee.id);
                    } catch (err) {
                        console.warn(`Errore nel fetch dell'accrual per employee ${employee.id}:`, err);
                        accrual = null;
                    }

                    return {
                        ...employee,
                        contracts: contracts || [],
                        accrual
                    };
                })
            );

            setEmployees(employeesWithDetails);

        } catch (err) {
            console.error('Errore nel fetch della lista employee:', err);
        } finally {
            setLoading(false);
        }
    }

    const [contractForm, setContractForm] = useState({
        startDate: '',
        endDate: '',
        monthlyWorkingHours: '',
        valid: true
    });

    const [accrualForm, setAccrualForm] = useState({
        vacationHoursPerMonth: '',
        rolHoursPerMonth: ''
    });

    const filteredEmployees = useMemo(() => {
        return employees.filter(emp => {
            const matchesSearch =
                emp.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                emp.surname.toLowerCase().includes(searchTerm.toLowerCase());

            const hasActiveContract = emp.contracts && emp.contracts.length > 0 && emp.contracts.some(c => c.valid);
            const matchesStatus =
                filterStatus === 'all' ||
                (filterStatus === 'active' && hasActiveContract) ||
                (filterStatus === 'inactive' && !hasActiveContract);

            return matchesSearch && matchesStatus;
        });
    }, [employees, searchTerm, filterStatus]);

    const stats = useMemo(() => {
        const total = employees.length;
        const activeContracts = employees.filter(e => e.contracts && e.contracts.length > 0 && e.contracts.some(c => c.valid)).length;
        const inactiveContracts = total - activeContracts;
        const totalContracts = employees.reduce((sum, e) => sum + (e.contracts ? e.contracts.length : 0), 0);

        return { total, activeContracts, inactiveContracts, totalContracts };
    }, [employees]);

    const formatDate = (dateString) => {
        if (!dateString) return 'Indeterminato';
        return new Date(dateString).toLocaleDateString('it-IT');
    };

    const handleEditContract = (employee, contract) => {
        setSelectedContract({ ...contract, employeeId: employee.id, employeeName: `${employee.name} ${employee.surname}` });
        setContractForm({
            startDate: contract.startDate,
            endDate: contract.endDate || '',
            monthlyWorkingHours: contract.monthlyWorkingHours,
            valid: true
        });
        setShowEditModal(true);
    };

    const handleNewContract = (employee) => {
        setSelectedEmployeeForNew({ id: employee.id, name: `${employee.name} ${employee.surname}` });
        setContractForm({
            startDate: '',
            endDate: '',
            monthlyWorkingHours: '',
            valid: true
        });
        setShowNewContractModal(true);
    };

    const handleEditAccrual = (employee) => {
        setSelectedAccrual({
            employeeId: employee.id,
            employeeName: `${employee.name} ${employee.surname}`,
            ...employee.accrual
        });
        setAccrualForm({
            vacationHoursPerMonth: employee.accrual?.vacationHoursPerMonth || '',
            rolHoursPerMonth: employee.accrual?.rolHoursPerMonth || ''
        });
        setShowAccrualModal(true);
    };

    const handleSaveContract = async (e) => {
        e.preventDefault();
        try {
            await modifyContract(selectedContract.id, { ...selectedContract, ...contractForm })
            toast.success("Contratto modificato con successo");
            await fetchEmployeesData()
            setShowEditModal(false);
            setSelectedContract(null);
        } catch (error) {
            toast.error("Errore nel salvataggio del contratto");
            console.error(error)
        }
    };

    const handleSaveNewContract = async (e) => {
        e.preventDefault();
        try {
            await addContract({
                employeeId: selectedEmployeeForNew.id,
                ...contractForm,
            })
            toast.success("Contratto creato con successo!");
            setShowNewContractModal(false);
            setSelectedEmployeeForNew(null);
            await fetchEmployeesData()
        } catch (error) {
            toast.error("Errore nella creazione del contratto");
            console.error(error)
        }
    };

    const handleSaveAccrual = async (e) => {
        e.preventDefault();
        try {
            await updateEmployeeAccrual({ ...selectedAccrual, ...accrualForm })
            toast.success("Maturazione aggiornata con successo!");
            await fetchEmployeesData()
            setShowAccrualModal(false);
            setSelectedAccrual(null);
        } catch (error) {
            toast.error("Errore nel salvataggio della maturazione");
            console.error(error)
        }
    };

    const handleInvalidateContract = async (contractId) => {
        if (confirm("Sei sicuro di voler invalidare questo contratto?")) {
            try {
                await invalidateContract(contractId);
                toast.success("Contratto invalidato con successo!");
                fetchEmployeesData();
            } catch (error) {
                toast.error("Errore nell'invalidazione del contratto");
                console.error(error)
            }
        }
    }

    if (loading) {
        return (
            <div className="text-center py-5">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Caricamento...</span>
                </div>
            </div>
        );
    }

    return (
        <div>
            {/* Stats Cards */}
            <div className="row g-3 mb-4">
                <div className="col-md-3">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="text-muted mb-1 small">Totale Dipendenti</p>
                                    <h4 className="fw-bold mb-0">{stats.total}</h4>
                                </div>
                                <div className="bg-primary bg-opacity-10 rounded p-2">
                                    <Users size={20} className="text-primary" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-md-3">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="text-muted mb-1 small">Contratti Attivi</p>
                                    <h4 className="fw-bold mb-0 text-success">{stats.activeContracts}</h4>
                                </div>
                                <div className="bg-success bg-opacity-10 rounded p-2">
                                    <CheckCircle size={20} className="text-success" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-md-3">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="text-muted mb-1 small">Contratti Scaduti</p>
                                    <h4 className="fw-bold mb-0 text-secondary">{stats.inactiveContracts}</h4>
                                </div>
                                <div className="bg-secondary bg-opacity-10 rounded p-2">
                                    <XCircle size={20} className="text-secondary" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-md-3">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-start">
                                <div>
                                    <p className="text-muted mb-1 small">Totale Contratti</p>
                                    <h4 className="fw-bold mb-0 text-info">{stats.totalContracts}</h4>
                                </div>
                                <div className="bg-info bg-opacity-10 rounded p-2">
                                    <Briefcase size={20} className="text-info" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Search and Filter */}
            <div className="card border-0 shadow-sm mb-4">
                <div className="card-body">
                    <div className="row g-3 align-items-center">
                        <div className="col-md-8">
                            <div className="input-group">
                                <span className="input-group-text bg-white">
                                    <Search size={18} />
                                </span>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="Cerca dipendente..."
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
                                <option value="all">Tutti i contratti</option>
                                <option value="active">Solo attivi</option>
                                <option value="inactive">Solo scaduti</option>
                            </select>
                        </div>
                    </div>
                </div>
            </div>

            {/* Employee List */}
            <div className="card border-0 shadow-sm">
                <div className="card-header bg-white border-bottom">
                    <h5 className="mb-0 fw-semibold">Elenco Contratti</h5>
                </div>
                <div className="card-body p-0">
                    {filteredEmployees.length === 0 ? (
                        <div className="text-center py-5">
                            <Briefcase size={48} className="text-muted mb-3" />
                            <p className="text-muted">Nessun contratto trovato</p>
                        </div>
                    ) : (
                        <div className="accordion" id="contractsAccordion">
                            {filteredEmployees.map((employee) => {
                                const hasActiveContract = employee.contracts && employee.contracts.length > 0 && employee.contracts.some(c => c.valid);
                                const hasContracts = employee.contracts && employee.contracts.length > 0;
                                const hasAccrual = employee.accrual !== null && employee.accrual !== undefined;

                                return (
                                    <div key={employee.id} className="accordion-item border-0 border-bottom">
                                        <h2 className="accordion-header">
                                            <button
                                                className="accordion-button collapsed"
                                                type="button"
                                                data-bs-toggle="collapse"
                                                data-bs-target={`#collapse${employee.id}`}
                                            >
                                                <div className="d-flex align-items-center gap-3 w-100">
                                                    <div
                                                        className="bg-primary bg-opacity-10 rounded-circle d-flex align-items-center justify-content-center flex-shrink-0"
                                                        style={{ width: '40px', height: '40px' }}
                                                    >
                                                        <span className="text-primary fw-bold small">
                                                            {employee.name.charAt(0)}{employee.surname.charAt(0)}
                                                        </span>
                                                    </div>
                                                    <div className="flex-grow-1">
                                                        <div className="fw-semibold">{employee.name} {employee.surname}</div>
                                                        <small className="text-muted">
                                                            {hasContracts
                                                                ? `${employee.contracts.length} contratt${employee.contracts.length !== 1 ? 'i' : 'o'}`
                                                                : 'Nessun contratto'}
                                                        </small>
                                                    </div>
                                                    {!hasContracts && (
                                                        <span className="badge bg-warning text-dark">
                                                            <AlertCircle size={14} className="me-1" />
                                                            Nessun Contratto
                                                        </span>
                                                    )}
                                                    {hasContracts && hasActiveContract && (
                                                        <span className="badge bg-success">Contratto Attivo</span>
                                                    )}
                                                    {hasContracts && !hasActiveContract && (
                                                        <span className="badge bg-secondary">Nessun Contratto Attivo</span>
                                                    )}
                                                </div>
                                            </button>
                                        </h2>
                                        <div
                                            id={`collapse${employee.id}`}
                                            className="accordion-collapse collapse"
                                            data-bs-parent="#contractsAccordion"
                                        >
                                            <div className="accordion-body bg-light">
                                                {/* Accrual Card */}
                                                <div className="card border-0 shadow-sm mb-3">
                                                    <div className="card-header bg-white d-flex justify-content-between align-items-center">
                                                        <h6 className="mb-0 fw-semibold d-flex align-items-center gap-2">
                                                            <Clock size={18} className="text-primary" />
                                                            Maturazione Ferie e Permessi
                                                        </h6>
                                                        <button
                                                            className="btn btn-sm btn-outline-primary"
                                                            onClick={() => handleEditAccrual(employee)}
                                                        >
                                                            {hasAccrual ? (
                                                                <>
                                                                    <Edit size={14} className="me-1" />
                                                                    Modifica
                                                                </>
                                                            ) : (
                                                                <>
                                                                    <Plus size={14} className="me-1" />
                                                                    Configura
                                                                </>
                                                            )}
                                                        </button>
                                                    </div>
                                                    <div className="card-body">
                                                        {hasAccrual ? (
                                                            <div className="row g-3">
                                                                <div className="col-md-6">
                                                                    <div className="d-flex align-items-center gap-3">
                                                                        <div className="bg-primary bg-opacity-10 rounded p-2">
                                                                            <Plane size={20} className="text-primary" />
                                                                        </div>
                                                                        <div>
                                                                            <small className="text-muted d-block">Ferie Mensili</small>
                                                                            <span className="fw-bold">{employee.accrual.vacationHoursPerMonth} ore</span>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                                <div className="col-md-6">
                                                                    <div className="d-flex align-items-center gap-3">
                                                                        <div className="bg-success bg-opacity-10 rounded p-2">
                                                                            <Coffee size={20} className="text-success" />
                                                                        </div>
                                                                        <div>
                                                                            <small className="text-muted d-block">ROL Mensili</small>
                                                                            <span className="fw-bold">{employee.accrual.rolHoursPerMonth} ore</span>
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        ) : (
                                                            <div className="alert alert-warning mb-0 d-flex align-items-center gap-2">
                                                                <AlertCircle size={20} />
                                                                <div>
                                                                    <strong>Maturazione non configurata</strong>
                                                                    <p className="mb-0 small">Clicca su "Configura" per impostare le ore di maturazione mensili</p>
                                                                </div>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>

                                                {/* Contracts */}
                                                <div className="card border-0 shadow-sm">
                                                    <div className="card-header bg-white d-flex justify-content-between align-items-center">
                                                        <h6 className="mb-0 fw-semibold d-flex align-items-center gap-2">
                                                            <Briefcase size={18} className="text-primary" />
                                                            Storico Contratti
                                                        </h6>
                                                        <button
                                                            className="btn btn-sm btn-primary"
                                                            onClick={() => handleNewContract(employee)}
                                                        >
                                                            <Plus size={14} className="me-1" />
                                                            Nuovo Contratto
                                                        </button>
                                                    </div>
                                                    <div className="card-body p-0">
                                                        {!hasContracts ? (
                                                            <div className="p-4 text-center">
                                                                <div className="bg-warning bg-opacity-10 rounded-circle d-inline-flex p-3 mb-3">
                                                                    <AlertCircle size={32} className="text-warning" />
                                                                </div>
                                                                <h6 className="fw-semibold mb-2">Nessun contratto presente</h6>
                                                                <p className="text-muted small mb-3">
                                                                    Questo dipendente non ha ancora un contratto registrato nel sistema.
                                                                    Clicca su "Nuovo Contratto" per crearne uno.
                                                                </p>
                                                            </div>
                                                        ) : (
                                                            <div className="table-responsive">
                                                                <table className="table table-hover mb-0">
                                                                    <thead className="bg-light">
                                                                        <tr>
                                                                            <th className="py-3">Data Inizio</th>
                                                                            <th className="py-3">Data Fine</th>
                                                                            <th className="py-3">Ore Mensili</th>
                                                                            <th className="py-3 text-center">Stato</th>
                                                                            <th className="py-3 text-end">Azioni</th>
                                                                        </tr>
                                                                    </thead>
                                                                    <tbody>
                                                                        {employee.contracts
                                                                            .sort((a, b) => new Date(b.startDate) - new Date(a.startDate))
                                                                            .map((contract) => (
                                                                                <tr key={contract.id}>
                                                                                    <td className="py-3">
                                                                                        <div className="d-flex align-items-center gap-2">
                                                                                            <Calendar size={16} className="text-muted" />
                                                                                            <span>{formatDate(contract.startDate)}</span>
                                                                                        </div>
                                                                                    </td>
                                                                                    <td className="py-3">{formatDate(contract.endDate)}</td>
                                                                                    <td className="py-3">
                                                                                        <span className="badge bg-primary bg-opacity-10 text-primary px-3 py-2">
                                                                                            {contract.monthlyWorkingHours} ore
                                                                                        </span>
                                                                                    </td>
                                                                                    <td className="py-3 text-center">
                                                                                        {contract.valid ? (
                                                                                            <span className="badge bg-success">
                                                                                                <CheckCircle size={14} className="me-1" />
                                                                                                Attivo
                                                                                            </span>
                                                                                        ) : (
                                                                                            <span className="badge bg-secondary">
                                                                                                <XCircle size={14} className="me-1" />
                                                                                                Scaduto
                                                                                            </span>
                                                                                        )}
                                                                                    </td>
                                                                                    <td className="py-3 text-end">
                                                                                        <button
                                                                                            className="btn btn-sm btn-outline-primary mr-2"
                                                                                            onClick={() => handleEditContract(employee, contract)}
                                                                                        >
                                                                                            <Edit size={14} />
                                                                                        </button>
                                                                                        {contract.valid && (
                                                                                            <button
                                                                                                className="btn btn-sm btn-outline-danger"
                                                                                                onClick={() => handleInvalidateContract(contract.id)}
                                                                                            >
                                                                                                <Trash size={14} />
                                                                                            </button>
                                                                                        )}
                                                                                    </td>
                                                                                </tr>
                                                                            ))}
                                                                    </tbody>
                                                                </table>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            </div>

            {/* Edit Contract Modal */}
            {showEditModal && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">
                                    <Edit size={20} className="me-2" />
                                    Modifica Contratto
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowEditModal(false)}
                                />
                            </div>
                            <form onSubmit={handleSaveContract}>
                                <div className="modal-body">
                                    <div className="alert alert-info d-flex align-items-center gap-2 mb-4">
                                        <AlertCircle size={18} />
                                        <small>
                                            Stai modificando il contratto di <strong>{selectedContract?.employeeName}</strong>
                                        </small>
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label fw-semibold">
                                            Data Inizio <span className="text-danger">*</span>
                                        </label>
                                        <input
                                            type="date"
                                            className="form-control"
                                            value={contractForm.startDate}
                                            onChange={(e) => setContractForm({ ...contractForm, startDate: e.target.value })}
                                            required
                                        />
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label fw-semibold">Data Fine</label>
                                        <input
                                            type="date"
                                            className="form-control"
                                            value={contractForm.endDate}
                                            onChange={(e) => setContractForm({ ...contractForm, endDate: e.target.value })}
                                        />
                                        <small className="text-muted">Lascia vuoto per contratto a tempo indeterminato</small>
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label fw-semibold">
                                            Ore Lavorative Mensili <span className="text-danger">*</span>
                                        </label>
                                        <div className="input-group">
                                            <input
                                                type="number"
                                                className="form-control"
                                                value={contractForm.monthlyWorkingHours}
                                                onChange={(e) => setContractForm({ ...contractForm, monthlyWorkingHours: e.target.value })}
                                                min="0"
                                                step="0.5"
                                                required
                                            />
                                            <span className="input-group-text">ore</span>
                                        </div>
                                    </div>
                                </div>
                                <div className="modal-footer">
                                    <button
                                        type="button"
                                        className="btn btn-secondary"
                                        onClick={() => setShowEditModal(false)}
                                    >
                                        Annulla
                                    </button>
                                    <button type="submit" className="btn btn-primary">
                                        <Save size={16} className="me-1" />
                                        Salva Modifiche
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}

            {/* New Contract Modal */}
            {showNewContractModal && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">
                                    <Plus size={20} className="me-2" />
                                    Nuovo Contratto
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowNewContractModal(false)}
                                />
                            </div>
                            <form onSubmit={handleSaveNewContract}>
                                <div className="modal-body">
                                    <div className="alert alert-info d-flex align-items-center gap-2 mb-4">
                                        <AlertCircle size={18} />
                                        <small>
                                            Stai creando un nuovo contratto per <strong>{selectedEmployeeForNew?.name}</strong>
                                        </small>
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label fw-semibold">
                                            Data Inizio <span className="text-danger">*</span>
                                        </label>
                                        <input
                                            type="date"
                                            className="form-control"
                                            value={contractForm.startDate}
                                            onChange={(e) => setContractForm({ ...contractForm, startDate: e.target.value })}
                                            required
                                        />
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label fw-semibold">Data Fine</label>
                                        <input
                                            type="date"
                                            className="form-control"
                                            value={contractForm.endDate}
                                            onChange={(e) => setContractForm({ ...contractForm, endDate: e.target.value })}
                                        />
                                        <small className="text-muted">Lascia vuoto per contratto a tempo indeterminato</small>
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label fw-semibold">
                                            Ore Lavorative Mensili <span className="text-danger">*</span>
                                        </label>
                                        <div className="input-group">
                                            <input
                                                type="number"
                                                className="form-control"
                                                value={contractForm.monthlyWorkingHours}
                                                onChange={(e) => setContractForm({ ...contractForm, monthlyWorkingHours: e.target.value })}
                                                min="0"
                                                step="0.5"
                                                placeholder="Es. 160"
                                                required
                                            />
                                            <span className="input-group-text">ore</span>
                                        </div>
                                    </div>
                                </div>
                                <div className="modal-footer">
                                    <button
                                        type="button"
                                        className="btn btn-secondary"
                                        onClick={() => setShowNewContractModal(false)}
                                    >
                                        Annulla
                                    </button>
                                    <button type="submit" className="btn btn-primary">
                                        <Save size={16} className="me-1" />
                                        Crea Contratto
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}

            {/* Edit Accrual Modal */}
            {showAccrualModal && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">
                                    <Clock size={20} className="me-2" />
                                    {selectedAccrual && !selectedAccrual.vacationHoursPerMonth ? 'Configura Maturazione' : 'Modifica Maturazione'}
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowAccrualModal(false)}
                                />
                            </div>
                            <form onSubmit={handleSaveAccrual}>
                                <div className="modal-body">
                                    <div className="alert alert-info d-flex align-items-center gap-2 mb-4">
                                        <AlertCircle size={18} />
                                        <small>
                                            Stai {selectedAccrual && !selectedAccrual.vacationHoursPerMonth ? 'configurando' : 'modificando'} la maturazione per <strong>{selectedAccrual?.employeeName}</strong>
                                        </small>
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label fw-semibold">
                                            Ore Ferie Mensili <span className="text-danger">*</span>
                                        </label>
                                        <div className="input-group">
                                            <span className="input-group-text bg-light">
                                                <Plane size={16} />
                                            </span>
                                            <input
                                                type="number"
                                                className="form-control"
                                                value={accrualForm.vacationHoursPerMonth}
                                                onChange={(e) => setAccrualForm({ ...accrualForm, vacationHoursPerMonth: e.target.value })}
                                                min="0"
                                                step="0.01"
                                                placeholder="Es. 13.33"
                                                required
                                            />
                                            <span className="input-group-text">ore</span>
                                        </div>
                                        <small className="text-muted">Es. 13.33 ore (160 ore annuali / 12 mesi)</small>
                                    </div>

                                    <div className="mb-3">
                                        <label className="form-label fw-semibold">
                                            Ore ROL Mensili <span className="text-danger">*</span>
                                        </label>
                                        <div className="input-group">
                                            <span className="input-group-text bg-light">
                                                <Coffee size={16} />
                                            </span>
                                            <input
                                                type="number"
                                                className="form-control"
                                                value={accrualForm.rolHoursPerMonth}
                                                onChange={(e) => setAccrualForm({ ...accrualForm, rolHoursPerMonth: e.target.value })}
                                                min="0"
                                                step="0.01"
                                                placeholder="Es. 5.83"
                                                required
                                            />
                                            <span className="input-group-text">ore</span>
                                        </div>
                                        <small className="text-muted">Es. 5.83 ore (70 ore annuali / 12 mesi)</small>
                                    </div>

                                    <div className="alert alert-light border">
                                        <strong className="d-block mb-2">Calcolo automatico:</strong>
                                        <div className="row g-2 small">
                                            <div className="col-6">
                                                <div className="text-muted">Ferie annuali:</div>
                                                <div className="fw-semibold">
                                                    {accrualForm.vacationHoursPerMonth ? (parseFloat(accrualForm.vacationHoursPerMonth) * 12).toFixed(2) : '0'} ore
                                                </div>
                                            </div>
                                            <div className="col-6">
                                                <div className="text-muted">ROL annuali:</div>
                                                <div className="fw-semibold">
                                                    {accrualForm.rolHoursPerMonth ? (parseFloat(accrualForm.rolHoursPerMonth) * 12).toFixed(2) : '0'} ore
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div className="modal-footer">
                                    <button
                                        type="button"
                                        className="btn btn-secondary"
                                        onClick={() => setShowAccrualModal(false)}
                                    >
                                        Annulla
                                    </button>
                                    <button type="submit" className="btn btn-primary">
                                        <Save size={16} className="me-1" />
                                        {selectedAccrual && !selectedAccrual.vacationHoursPerMonth ? 'Salva Configurazione' : 'Salva Modifiche'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ContractManagementPage;