import React, { useState } from 'react';
import { X, User, Mail, Phone, MapPin, Calendar, FileText, Hash, Briefcase, Clock, Plane, Coffee } from 'lucide-react';
import { setContract } from '../api/contract';
import { initializeBalance } from '../api/absence';
import { createEmployee, deleteEmployee } from '../api/employees';
import { toast } from "react-toastify";
import { useAuth } from '../utils/AuthContext';

const AddEmployeeModal = ({ isOpen, onClose, onRefresh }) => {
    const { user } = useAuth();
    const [activeTab, setActiveTab] = useState('employee');
    const [employeeId, setEmployeeId] = useState(null);
    const [isEmployeeSaved, setIsEmployeeSaved] = useState(false);

    const [formData, setFormData] = useState({
        name: '',
        surname: '',
        taxCode: '',
        birthday: '',
        address: '',
        city: '',
        email: '',
        phone: '',
        note: '',
        companyId: user.companyId
    });

    const [contractData, setContractData] = useState({
        startDate: '',
        endDate: '',
        monthlyWorkingHours: '',
        vacationHoursPerMonth: '',
        rolHoursPerMonth: ''
    });

    const [errors, setErrors] = useState({});
    const [contractErrors, setContractErrors] = useState({});

    const handleChange = (e) => {
        const { name, value } = e.target;

        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const handleContractChange = (e) => {
        const { name, value } = e.target;
        setContractData(prev => ({
            ...prev,
            [name]: value
        }));
        if (contractErrors[name]) {
            setContractErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const validateForm = () => {
        const newErrors = {};

        if (!formData.name.trim()) newErrors.name = 'Il nome è obbligatorio';
        if (!formData.surname.trim()) newErrors.surname = 'Il cognome è obbligatorio';
        if (!formData.taxCode.trim()) newErrors.taxCode = 'Il codice fiscale è obbligatorio';
        if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
            newErrors.email = 'Email non valida';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const validateContractForm = () => {
        const newErrors = {};

        if (!contractData.startDate) newErrors.startDate = 'La data di inizio è obbligatoria';
        if (!contractData.monthlyWorkingHours) newErrors.monthlyWorkingHours = 'Le ore mensili sono obbligatorie';
        if (contractData.monthlyWorkingHours && isNaN(contractData.monthlyWorkingHours)) {
            newErrors.monthlyWorkingHours = 'Inserire un numero valido';
        }

        setContractErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmitEmployee = async (e) => {
        e.preventDefault();
        formData.taxCode = formData.taxCode.toUpperCase();

        if (validateForm()) {
            try {
                const savedEmployee = await createEmployee(formData);
                if (savedEmployee && savedEmployee.id) {
                    setEmployeeId(savedEmployee.id);
                    setIsEmployeeSaved(true);
                    setActiveTab('contract');
                }
                if (onRefresh) {
                    await onRefresh();
                }
            } catch (error) {
                toast.error('Errore nel salvataggio del dipendente');
                console.error(error)
            }
        }
    };

    const handleSubmitContract = async (e) => {
        e.preventDefault();

        if (validateContractForm()) {
            try {
                await setContract({
                    employeeId: employeeId,
                    startDate: contractData.startDate,
                    endDate: contractData.endDate,
                    monthlyWorkingHours: contractData.monthlyWorkingHours,
                    valid: true
                });
                await initializeBalance({
                    employeeId: employeeId,
                    vacationHoursPerMonth: contractData.vacationHoursPerMonth,
                    rolHoursPerMonth: contractData.rolHoursPerMonth
                })
                handleClose();
                if (onRefresh) {
                    await onRefresh();
                }
            } catch (error) {
                toast.error('Errore nel salvataggio del contratto');
                console.error(error)
            }
        }
    };

    const deleteCreatedEmployee = async () => {
        // If modal is canceld after employee is created deleting employee
        if (isEmployeeSaved) {
            const confirmDelete = confirm(
                "Annullando l'inserimento del contratto il dipendente verrà eliminato. Vuoi procedere?"
            );
            if (!confirmDelete) { return; }
            try {
                await deleteEmployee(employeeId)
                toast.done("Dipendente eliminato con successo")
                if (onRefresh) {
                    await onRefresh();
                }
            } catch (error) {
                toast.error("Errore nell'eliminazione del dipendente");
                console.error(error)
            }
        }
    }

    const handleClose = () => {

        setFormData({
            name: '',
            surname: '',
            taxCode: '',
            birthday: '',
            address: '',
            city: '',
            email: '',
            phone: '',
            note: '',
            companyId: user.companyId
        });
        setContractData({
            startDate: '',
            endDate: '',
            monthlyWorkingHours: '',
            vacationHoursPerMonth: '',
            rolHoursPerMonth: ''
        });
        setErrors({});
        setContractErrors({});
        setActiveTab('employee');
        setEmployeeId(null);
        setIsEmployeeSaved(false);
        onClose();
    };

    if (!isOpen) return null;

    return (
        <>
            {/* Backdrop */}
            <div
                className="modal-backdrop fade show"
                style={{ zIndex: 1040 }}
                onClick={handleClose}
            />

            {/* Modal */}
            <div
                className="modal fade show d-block"
                tabIndex="-1"
                style={{ zIndex: 1050 }}
            >
                <div className="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-lg">
                    <div className="modal-content border-0 shadow-lg">
                        {/* Header */}
                        <div className="modal-header bg-primary text-white border-0">
                            <div className="d-flex align-items-center gap-2">
                                <div className="bg-white bg-opacity-25 rounded p-2">
                                    <User size={24} />
                                </div>
                                <div>
                                    <h5 className="modal-title mb-0 fw-bold">
                                        {activeTab === 'employee' ? 'Aggiungi Nuovo Dipendente' : 'Aggiungi Contratto'}
                                    </h5>
                                    <small className="opacity-75">
                                        {activeTab === 'employee'
                                            ? 'Compila i campi per registrare un nuovo dipendente'
                                            : 'Inserisci i dettagli del contratto di lavoro'}
                                    </small>
                                </div>
                            </div>
                            <button
                                type="button"
                                className="btn-close btn-close-white"
                                onClick={handleClose}
                            />
                        </div>

                        {/* Tabs */}
                        <ul className="nav nav-tabs px-3 pt-3 border-0 bg-light">
                            <li className="nav-item">
                                <button
                                    className={`nav-link ${activeTab === 'employee' ? 'active' : ''} d-flex align-items-center gap-2`}
                                    onClick={() => setActiveTab('employee')}
                                    disabled={activeTab === 'contract'}
                                    style={{
                                        cursor: activeTab === 'contract' ? 'not-allowed' : 'pointer',
                                        opacity: activeTab === 'contract' ? 0.6 : 1
                                    }}
                                >
                                    <User size={16} />
                                    Dati Dipendente
                                    {isEmployeeSaved && (
                                        <span className="badge bg-success rounded-pill">✓</span>
                                    )}
                                </button>
                            </li>
                            <li className="nav-item">
                                <button
                                    className={`nav-link ${activeTab === 'contract' ? 'active' : ''} d-flex align-items-center gap-2`}
                                    onClick={() => setActiveTab('contract')}
                                    disabled={!isEmployeeSaved}
                                    style={{
                                        cursor: !isEmployeeSaved ? 'not-allowed' : 'pointer',
                                        opacity: !isEmployeeSaved ? 0.6 : 1
                                    }}
                                >
                                    <Briefcase size={16} />
                                    Contratto
                                    {!isEmployeeSaved && (
                                        <span className="badge bg-secondary rounded-pill" style={{ fontSize: '0.65rem' }}>
                                            Bloccato
                                        </span>
                                    )}
                                </button>
                            </li>
                        </ul>

                        {/* Body */}
                        <div className="modal-body p-4">
                            {/* Tab Dipendente */}
                            {activeTab === 'employee' && (
                                <form onSubmit={handleSubmitEmployee} id="addEmployeeForm">
                                    {/* Dati Anagrafici */}
                                    <div className="mb-4">
                                        <h6 className="fw-semibold text-primary mb-3 d-flex align-items-center gap-2">
                                            <User size={18} />
                                            Dati Anagrafici
                                        </h6>

                                        <div className="row g-3">
                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Nome <span className="text-danger">*</span>
                                                </label>
                                                <input
                                                    type="text"
                                                    className={`form-control ${errors.name ? 'is-invalid' : ''}`}
                                                    name="name"
                                                    value={formData.name}
                                                    onChange={handleChange}
                                                    placeholder="Es. Mario"
                                                    disabled={isEmployeeSaved}
                                                />
                                                {errors.name && (
                                                    <div className="invalid-feedback">{errors.name}</div>
                                                )}
                                            </div>

                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Cognome <span className="text-danger">*</span>
                                                </label>
                                                <input
                                                    type="text"
                                                    className={`form-control ${errors.surname ? 'is-invalid' : ''}`}
                                                    name="surname"
                                                    value={formData.surname}
                                                    onChange={handleChange}
                                                    placeholder="Es. Rossi"
                                                    disabled={isEmployeeSaved}
                                                />
                                                {errors.surname && (
                                                    <div className="invalid-feedback">{errors.surname}</div>
                                                )}
                                            </div>

                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Codice Fiscale <span className="text-danger">*</span>
                                                </label>
                                                <div className="input-group">
                                                    <span className="input-group-text bg-light border-end-0">
                                                        <Hash size={16} className="text-muted" />
                                                    </span>
                                                    <input
                                                        type="text"
                                                        className={`form-control border-start-0 ${errors.taxCode ? 'is-invalid' : ''}`}
                                                        name="taxCode"
                                                        value={formData.taxCode}
                                                        onChange={handleChange}
                                                        placeholder="Es. RSSMRA80A01H501Z"
                                                        maxLength="16"
                                                        style={{ textTransform: 'uppercase' }}
                                                        disabled={isEmployeeSaved}
                                                    />
                                                    {errors.taxCode && (
                                                        <div className="invalid-feedback">{errors.taxCode}</div>
                                                    )}
                                                </div>
                                            </div>

                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Data di Nascita
                                                </label>
                                                <div className="input-group">
                                                    <span className="input-group-text bg-light border-end-0">
                                                        <Calendar size={16} className="text-muted" />
                                                    </span>
                                                    <input
                                                        type="date"
                                                        className="form-control border-start-0"
                                                        name="birthday"
                                                        value={formData.birthday}
                                                        onChange={handleChange}
                                                        disabled={isEmployeeSaved}
                                                    />
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Contatti */}
                                    <div className="mb-4">
                                        <h6 className="fw-semibold text-primary mb-3 d-flex align-items-center gap-2">
                                            <Mail size={18} />
                                            Contatti
                                        </h6>

                                        <div className="row g-3">
                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Email
                                                </label>
                                                <div className="input-group">
                                                    <span className="input-group-text bg-light border-end-0">
                                                        <Mail size={16} className="text-muted" />
                                                    </span>
                                                    <input
                                                        type="email"
                                                        className={`form-control border-start-0 ${errors.email ? 'is-invalid' : ''}`}
                                                        name="email"
                                                        value={formData.email}
                                                        onChange={handleChange}
                                                        placeholder="nome.cognome@azienda.it"
                                                        disabled={isEmployeeSaved}
                                                    />
                                                    {errors.email && (
                                                        <div className="invalid-feedback">{errors.email}</div>
                                                    )}
                                                </div>
                                            </div>

                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Telefono
                                                </label>
                                                <div className="input-group">
                                                    <span className="input-group-text bg-light border-end-0">
                                                        <Phone size={16} className="text-muted" />
                                                    </span>
                                                    <input
                                                        type="tel"
                                                        className="form-control border-start-0"
                                                        name="phone"
                                                        value={formData.phone}
                                                        onChange={handleChange}
                                                        placeholder="+39 333 1234567"
                                                        disabled={isEmployeeSaved}
                                                    />
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Residenza */}
                                    <div className="mb-4">
                                        <h6 className="fw-semibold text-primary mb-3 d-flex align-items-center gap-2">
                                            <MapPin size={18} />
                                            Residenza
                                        </h6>

                                        <div className="row g-3">
                                            <div className="col-md-8">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Indirizzo
                                                </label>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    name="address"
                                                    value={formData.address}
                                                    onChange={handleChange}
                                                    placeholder="Via/Piazza e numero civico"
                                                    disabled={isEmployeeSaved}
                                                />
                                            </div>

                                            <div className="col-md-4">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Città
                                                </label>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    name="city"
                                                    value={formData.city}
                                                    onChange={handleChange}
                                                    placeholder="Es. Roma"
                                                    disabled={isEmployeeSaved}
                                                />
                                            </div>
                                        </div>
                                    </div>

                                    {/* Note */}
                                    <div className="mb-3">
                                        <h6 className="fw-semibold text-primary mb-3 d-flex align-items-center gap-2">
                                            <FileText size={18} />
                                            Note Aggiuntive
                                        </h6>

                                        <textarea
                                            className="form-control"
                                            name="note"
                                            value={formData.note}
                                            onChange={handleChange}
                                            rows="3"
                                            placeholder="Eventuali note o informazioni aggiuntive..."
                                            maxLength="200"
                                            disabled={isEmployeeSaved}
                                        />
                                        <div className="form-text text-end">
                                            {formData.note.length}/200 caratteri
                                        </div>
                                    </div>

                                    {isEmployeeSaved && (
                                        <div className="alert alert-success d-flex align-items-center gap-2" role="alert">
                                            <User size={18} />
                                            <div>
                                                <strong>Dipendente salvato con successo!</strong>
                                                <br />
                                                <small>Passa alla tab "Contratto" per completare l'inserimento.</small>
                                            </div>
                                        </div>
                                    )}
                                </form>
                            )}

                            {/* Tab Contratto */}
                            {activeTab === 'contract' && (
                                <form onSubmit={handleSubmitContract} id="addContractForm">
                                    <div className="alert alert-info d-flex align-items-center gap-2 mb-4" role="alert">
                                        <Briefcase size={18} />
                                        <small>
                                            Stai inserendo il contratto per <strong>{formData.name} {formData.surname}</strong>
                                        </small>
                                    </div>

                                    {/* Date Contratto */}
                                    <div className="mb-4">
                                        <h6 className="fw-semibold text-primary mb-3 d-flex align-items-center gap-2">
                                            <Calendar size={18} />
                                            Periodo Contrattuale
                                        </h6>

                                        <div className="row g-3">
                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Data Inizio <span className="text-danger">*</span>
                                                </label>
                                                <div className="input-group">
                                                    <span className="input-group-text bg-light border-end-0">
                                                        <Calendar size={16} className="text-muted" />
                                                    </span>
                                                    <input
                                                        type="date"
                                                        className={`form-control border-start-0 ${contractErrors.startDate ? 'is-invalid' : ''}`}
                                                        name="startDate"
                                                        value={contractData.startDate}
                                                        onChange={handleContractChange}
                                                    />
                                                    {contractErrors.startDate && (
                                                        <div className="invalid-feedback">{contractErrors.startDate}</div>
                                                    )}
                                                </div>
                                            </div>

                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Data Fine
                                                </label>
                                                <div className="input-group">
                                                    <span className="input-group-text bg-light border-end-0">
                                                        <Calendar size={16} className="text-muted" />
                                                    </span>
                                                    <input
                                                        type="date"
                                                        className="form-control border-start-0"
                                                        name="endDate"
                                                        value={contractData.endDate}
                                                        onChange={handleContractChange}
                                                    />
                                                </div>
                                                <div className="form-text">
                                                    Lascia vuoto per contratto a tempo indeterminato
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Orari e Permessi */}
                                    <div className="mb-4">
                                        <h6 className="fw-semibold text-primary mb-3 d-flex align-items-center gap-2">
                                            <Clock size={18} />
                                            Orari e Permessi
                                        </h6>

                                        <div className="row g-3">
                                            <div className="col-md-12">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Ore Lavorative Mensili <span className="text-danger">*</span>
                                                </label>
                                                <div className="input-group">
                                                    <span className="input-group-text bg-light border-end-0">
                                                        <Clock size={16} className="text-muted" />
                                                    </span>
                                                    <input
                                                        type="number"
                                                        className={`form-control border-start-0 ${contractErrors.monthlyWorkingHours ? 'is-invalid' : ''}`}
                                                        name="monthlyWorkingHours"
                                                        value={contractData.monthlyWorkingHours}
                                                        onChange={handleContractChange}
                                                        placeholder="Es. 160"
                                                        min="0"
                                                        step="0.5"
                                                    />
                                                    <span className="input-group-text bg-light border-start-0">ore</span>
                                                    {contractErrors.monthlyWorkingHours && (
                                                        <div className="invalid-feedback">{contractErrors.monthlyWorkingHours}</div>
                                                    )}
                                                </div>
                                            </div>

                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Ore Ferie Mensili
                                                </label>
                                                <div className="input-group">
                                                    <span className="input-group-text bg-light border-end-0">
                                                        <Plane size={16} className="text-muted" />
                                                    </span>
                                                    <input
                                                        type="number"
                                                        className="form-control border-start-0"
                                                        name="vacationHoursPerMonth"
                                                        value={contractData.vacationHoursPerMonth}
                                                        onChange={handleContractChange}
                                                        placeholder="Es. 13.33"
                                                        min="0"
                                                        step="0.01"
                                                    />
                                                    <span className="input-group-text bg-light border-start-0">ore</span>
                                                </div>
                                            </div>

                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-muted">
                                                    Ore ROL Mensili
                                                </label>
                                                <div className="input-group">
                                                    <span className="input-group-text bg-light border-end-0">
                                                        <Coffee size={16} className="text-muted" />
                                                    </span>
                                                    <input
                                                        type="number"
                                                        className="form-control border-start-0"
                                                        name="rolHoursPerMonth"
                                                        value={contractData.rolHoursPerMonth}
                                                        onChange={handleContractChange}
                                                        placeholder="Es. 5.83"
                                                        min="0"
                                                        step="0.01"
                                                    />
                                                    <span className="input-group-text bg-light border-start-0">ore</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </form>
                            )}
                        </div>

                        {/* Footer */}
                        <div className="modal-footer bg-light border-0">
                            <button
                                type="button"
                                className="btn btn-outline-secondary d-flex align-items-center gap-2"
                                onClick={() => (deleteCreatedEmployee(), handleClose())}
                            >
                                <X size={18} />
                                {isEmployeeSaved && activeTab === 'contract' ? 'Annulla Inserimento' : 'Annulla'}
                            </button>
                            {activeTab === 'employee' && !isEmployeeSaved && (
                                <button
                                    type="submit"
                                    form="addEmployeeForm"
                                    className="btn btn-primary d-flex align-items-center gap-2"
                                >
                                    <User size={18} />
                                    Salva Dipendente
                                </button>
                            )}
                            {activeTab === 'contract' && (
                                <button
                                    type="submit"
                                    form="addContractForm"
                                    className="btn btn-success d-flex align-items-center gap-2"
                                >
                                    <Briefcase size={18} />
                                    Salva Contratto
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
};

export default AddEmployeeModal;