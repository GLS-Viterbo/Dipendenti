import React, { useState, useEffect } from 'react';
import { formatDate, formatTime, getDayName } from '../utils/utils';
import { getEmployeeById, deleteEmployee, getEmployeeGroups, getAllGroups, getEmployeeDocuments, deleteDocument, downloadDocument, getEmployeeDeadlines, updateEmployee, removeMemberFromGroup } from '../api/employees';
import { toast } from "react-toastify";
import {
    ArrowLeft, User, Mail, Phone, MapPin, Calendar, FileText,
    CreditCard, Clock, Briefcase, Users, CheckCircle, XCircle,
    Edit, Trash2, Save, X, Repeat, CalendarDays, CalendarCheck,
    Upload, Download, AlertCircle, Bell
} from 'lucide-react';
import { getCardById, getEmployeeAssignments, getWorkStatus } from '../api/card';
import { getDetailedEmployeeBalance, getNext3MonthEmployeeAbsences, updateEmployeeBalance } from '../api/absence';
import { getTodaysEmployeeLogs } from '../api/access';
import { getEmployeeContracts } from '../api/contract';
import EmployeeInfoSection from '../components/EmployeeInfoSection';
import UploadDocumentModal from '../components/UploadDocumentModal';
import AddDeadlineModal from '../components/AddDeadlineModal';
import EditEmployeeModal from '../components/EditEmployeeModal';
import EditBalanceModal from '../components/EditBalanceModal';
import ManageEmployeeGroupsModal from '../components/ManageEmployeeGroupsModal';

const EmployeeDetailPage = ({ employeeId, onNavigateBack }) => {
    const [employee, setEmployee] = useState(null);
    const [activeTab, setActiveTab] = useState('info');
    const [loading, setLoading] = useState(true);
    const [documents, setDocuments] = useState([]);
    const [deadlines, setDeadlines] = useState([]);
    const [showDocumentModal, setShowDocumentModal] = useState(false);
    const [showDeadlineModal, setShowDeadlineModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [showBalanceModal, setShowBalanceModal] = useState(false);
    const [showGroupModal, setShowGroupModal] = useState(false);

    useEffect(() => {
        fetchDocumentData()

    }, [employeeId]);


    useEffect(() => {
        fetchData()
    }, [employeeId]);

    async function fetchDocumentData() {

        try {
            const deadlineResponse = await getEmployeeDeadlines(employeeId)
            const documentResponse = await getEmployeeDocuments(employeeId)
            setDocuments(documentResponse);
            setDeadlines(deadlineResponse);
        } catch (err) {
            console.error("Errore nell'ottenere i documenti del dipendente")
        }
    }

    async function fetchData() {
        if (!employeeId) return;
        try {
            const employeeData = await getEmployeeById(employeeId)
            const isAtWorkData = await getWorkStatus(employeeId)
            const balance = await getDetailedEmployeeBalance(employeeId);
            const cardAssignments = await getEmployeeAssignments(employeeId);
            const employeeFutureAbsences = await getNext3MonthEmployeeAbsences(employeeId);
            const cardsData = await Promise.all(
                cardAssignments.map(async ass => {
                    const card = await getCardById(ass.cardId);
                    return {
                        ...ass,
                        ...card
                    };
                })
            );
            const todayLogs = await getTodaysEmployeeLogs(employeeId)
            const contracts = await getEmployeeContracts(employeeId)
            const groups = await getEmployeeGroups(employeeId)


            const fetchedEmployeeData = {
                id: employeeId,
                name: employeeData.name,
                surname: employeeData.surname,
                taxCode: employeeData.taxCode,
                birthday: employeeData.birthday,
                companyId: employeeData.companyId,
                address: employeeData.address,
                city: employeeData.city,
                email: employeeData.email,
                phone: employeeData.phone,
                note: employeeData.note,
                deleted: employeeData.deleted,
                isWorking: isAtWorkData.isWorking,
                contracts: contracts,
                cards: cardsData,
                absences: employeeFutureAbsences,
                leaveBalance: balance,
                groups: groups,
                recentAccess: todayLogs
            }
            setEmployee(fetchedEmployeeData)
            setLoading(false)
            console.log(fetchedEmployeeData)
        } catch (error) {
            console.error("Errore nel recupero delle informazione del dipendente: " + error)
        }
    }

    const handleUpdateEmployee = async (employeeId, updatedData) => {
        await updateEmployee(employeeId, updatedData);
        await fetchData();
    };

    const handleUpdateBalance = async (employeeId, newBalance) => {
        await updateEmployeeBalance(employeeId, newBalance);
        await fetchData();
    };

    const handleEmployeeDelete = async () => {
        const confirmDelete = confirm(
            "Confermi di voler eliminare il dipendente?"
        );
        if (!confirmDelete) { return; }
        try {
            await deleteEmployee(employee.id)
            toast.success("Dipendente eliminato con successo!");
            onNavigateBack()
        } catch (error) {
            toast.error("Errore nell'eliminazione del dipendente");
            console.error(error)
        }
    }

    const handleFileDelete = async (documentId) => {
        const confirmDelete = confirm(
            "Confermi di voler eliminare il file?"
        );
        if (!confirmDelete) { return; }
        try {
            await deleteDocument(documentId)
            toast.success("Documento eliminato con successo!");
            fetchDocumentData()
        } catch (error) {
            toast.error("Errore nell'eliminazione del documento");
            console.error(error)
        }
    }

    const getAbsenceTypeLabel = (type) => {
        const types = {
            'VACATION': 'Ferie',
            'SICK_LEAVE': 'Malattia',
            'PERMIT': 'Permesso',
            'ROL': 'ROL'
        };
        return types[type] || type;
    };

    const getAbsenceTypeBadgeClass = (type) => {
        const classes = {
            'VACATION': 'bg-primary',
            'SICK_LEAVE': 'bg-warning',
            'PERMIT': 'bg-info',
            'ROL': 'bg-success'
        };
        return classes[type] || 'bg-secondary';
    };

    // Funzioni helper
    const getDeadlineTypeLabel = (type) => {
        const types = {
            'CONTRATTO': 'Contratto',
            'CERTIFICATO_MEDICO': 'Certificato Medico',
            'FORMAZIONE': 'Formazione',
            'DOCUMENTO': 'Documento',
            'ALTRO': 'Altro'
        };
        return types[type] || type;
    };

    const getDeadlineTypeBadgeClass = (type) => {
        const classes = {
            'CONTRATTO': 'bg-primary',
            'CERTIFICATO_MEDICO': 'bg-warning',
            'FORMAZIONE': 'bg-info',
            'DOCUMENTO': 'bg-secondary',
            'ALTRO': 'bg-dark'
        };
        return classes[type] || 'bg-secondary';
    };

    const isDeadlineExpiring = (expirationDate) => {
        const today = new Date();
        const expDate = new Date(expirationDate);
        const daysUntilExpiration = Math.ceil((expDate - today) / (1000 * 60 * 60 * 24));
        return daysUntilExpiration <= 30 && daysUntilExpiration >= 0;
    };

    const isDeadlineExpired = (expirationDate) => {
        const today = new Date();
        const expDate = new Date(expirationDate);
        return expDate < today;
    };

    const getFileIcon = (mimeType) => {
        if (mimeType.includes('pdf')) return '📄';
        if (mimeType.includes('image')) return '🖼️';
        if (mimeType.includes('word') || mimeType.includes('document')) return '📝';
        if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return '📊';
        return '📎';
    };

    const formatFileSize = (bytes) => {
        if (!bytes) return '0 KB';
        const kb = bytes / 1024;
        if (kb < 1024) return `${kb.toFixed(0)} KB`;
        return `${(kb / 1024).toFixed(1)} MB`;
    };

    if (loading) {
        return (
            <div className="text-center py-5">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Caricamento...</span>
                </div>
            </div>
        );
    }

    if (!employee) {
        return (
            <div className="text-center py-5">
                <User size={48} className="text-muted mb-3" />
                <p className="text-muted">Dipendente non trovato</p>
                <button className="btn btn-primary" onClick={onNavigateBack}>
                    Torna all'elenco
                </button>
            </div>
        );
    }

    return (
        <div>
            {/* Back Button */}
            <div className="mb-3">
                <button
                    className="btn btn-outline-secondary d-inline-flex align-items-center gap-2"
                    onClick={onNavigateBack}
                >
                    <ArrowLeft size={18} />
                    <span>Torna all'elenco</span>
                </button>
            </div>

            {/* Employee Header Card */}
            <div className="card border-0 shadow-sm mb-4">
                <div className="card-body p-4">
                    <div className="row align-items-center">
                        <div className="col-auto">
                            <div
                                className="bg-primary bg-opacity-10 rounded-circle d-flex align-items-center justify-content-center"
                                style={{ width: '80px', height: '80px' }}
                            >
                                <span className="text-primary fw-bold" style={{ fontSize: '2rem' }}>
                                    {employee.name.charAt(0)}{employee.surname.charAt(0)}
                                </span>
                            </div>
                        </div>
                        <div className="col">
                            <div className="d-flex align-items-center gap-3 mb-2">
                                <h3 className="fw-bold mb-0">{employee.name} {employee.surname}</h3>
                                {employee.isWorking ? (
                                    <span className="badge bg-success bg-opacity-10 text-success border border-success d-inline-flex align-items-center gap-1 px-2 py-2">
                                        <CheckCircle size={16} />
                                        <span>Al lavoro</span>
                                    </span>
                                ) : (
                                    <span className="badge bg-secondary bg-opacity-10 text-secondary border border-secondary d-inline-flex align-items-center gap-1 px-2 py-2">
                                        <XCircle size={16} />
                                        <span>Non al lavoro</span>
                                    </span>
                                )}
                            </div>
                            <div className="row g-3 text-muted">
                                <div className="col-auto d-flex align-items-center gap-2">
                                    <Mail size={16} />
                                    <span>{employee.email}</span>
                                </div>
                                <div className="col-auto d-flex align-items-center gap-2">
                                    <Phone size={16} />
                                    <span>{employee.phone}</span>
                                </div>
                                <div className="col-auto d-flex align-items-center gap-2">
                                    <MapPin size={16} />
                                    <span>{employee.city}</span>
                                </div>
                            </div>
                        </div>
                        <div className="col-auto">
                            <button
                                className="btn btn-outline-primary me-2"
                                onClick={() => setShowEditModal(true)}
                            >
                                <Edit size={18} />
                            </button>
                            <button className="btn btn-outline-danger"
                                onClick={handleEmployeeDelete}
                            >
                                <Trash2 size={18} />
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Tabs Navigation */}
            <div className="card border-0 shadow-sm mb-4">
                <div className="card-body p-0">
                    <ul className="nav nav-tabs border-0" style={{ padding: '0 1rem' }}>
                        <li className="nav-item">
                            <button
                                className={`nav-link ${activeTab === 'info' ? 'active' : ''} border-0 d-flex align-items-center gap-2`}
                                onClick={() => setActiveTab('info')}
                            >
                                <User size={18} />
                                <span>Informazioni</span>
                            </button>
                        </li>
                        <li className="nav-item">
                            <button
                                className={`nav-link ${activeTab === 'contracts' ? 'active' : ''} border-0 d-flex align-items-center gap-2`}
                                onClick={() => setActiveTab('contracts')}
                            >
                                <Briefcase size={18} />
                                <span>Contratti</span>
                            </button>
                        </li>
                        <li className="nav-item">
                            <button
                                className={`nav-link ${activeTab === 'cards' ? 'active' : ''} border-0 d-flex align-items-center gap-2`}
                                onClick={() => setActiveTab('cards')}
                            >
                                <CreditCard size={18} />
                                <span>Badge</span>
                            </button>
                        </li>
                        <li className="nav-item">
                            <button
                                className={`nav-link ${activeTab === 'absences' ? 'active' : ''} border-0 d-flex align-items-center gap-2`}
                                onClick={() => setActiveTab('absences')}
                            >
                                <Calendar size={18} />
                                <span>Assenze</span>
                            </button>
                        </li>
                        <li className="nav-item">
                            <button
                                className={`nav-link ${activeTab === 'groups' ? 'active' : ''} border-0 d-flex align-items-center gap-2`}
                                onClick={() => setActiveTab('groups')}
                            >
                                <Users size={18} />
                                <span>Gruppi</span>
                            </button>
                        </li>
                        <li className="nav-item">
                            <button
                                className={`nav-link ${activeTab === 'documents' ? 'active' : ''} border-0 d-flex align-items-center gap-2`}
                                onClick={() => setActiveTab('documents')}
                            >
                                <FileText size={18} />
                                <span>Documenti</span>
                                {documents.length > 0 && (
                                    <span className="badge bg-primary rounded-pill">{documents.length}</span>
                                )}
                            </button>
                        </li>
                        <li className="nav-item">
                            <button
                                className={`nav-link ${activeTab === 'deadlines' ? 'active' : ''} border-0 d-flex align-items-center gap-2`}
                                onClick={() => setActiveTab('deadlines')}
                            >
                                <CalendarCheck size={18} />
                                <span>Scadenze</span>
                                {deadlines.filter(d => isDeadlineExpiring(d.expirationDate) || isDeadlineExpired(d.expirationDate)).length > 0 && (
                                    <span className="badge bg-danger rounded-pill">
                                        {deadlines.filter(d => isDeadlineExpiring(d.expirationDate) || isDeadlineExpired(d.expirationDate)).length}
                                    </span>
                                )}
                            </button>
                        </li>
                    </ul>
                </div>
            </div>

            {/* Tab Content */}
            <div className="row g-4">
                {/* Main Content */}
                <div className="col-lg-8">
                    {activeTab === 'info' && (
                        <EmployeeInfoSection
                            employee={employee}
                        />
                    )}
                    {activeTab === 'contracts' && (
                        <div className="card border-0 shadow-sm mb-4">
                            <div className="card-header bg-white border-bottom">
                                <h5 className="mb-0 fw-semibold">Contratti</h5>
                            </div>
                            <div className="card-body">
                                {employee.contracts.length === 0 ? (
                                    <div className="text-center py-4 text-muted">
                                        <p>Nessun contratto disponibile</p>
                                    </div>
                                ) : (
                                    <div className="table-responsive">
                                        <table className="table table-hover mb-0">
                                            <thead className="bg-light">
                                                <tr>
                                                    <th className="border-0">Data Inizio</th>
                                                    <th className="border-0">Data Fine</th>
                                                    <th className="border-0">Ore Mensili</th>
                                                    <th className="border-0">Stato</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {employee.contracts
                                                    .sort((a, b) => new Date(a.startDate) - new Date(b.startDate))
                                                    .map((contract) => (
                                                        <tr key={contract.id}>
                                                            <td>{formatDate(contract.startDate)}</td>
                                                            <td>{formatDate(contract.endDate) || '-'}</td>
                                                            <td>{contract.monthlyWorkingHours}</td>
                                                            <td>
                                                                {contract.valid ? (
                                                                    <span className="badge bg-success">Attivo</span>
                                                                ) : (
                                                                    <span className="badge bg-secondary">Scaduto</span>
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

                    )}
                    {activeTab === 'cards' && (
                        <div className="card border-0 shadow-sm">
                            <div className="card-body">
                                {employee.cards.length === 0 ? (
                                    <div className="text-center py-4 text-muted">
                                        <CreditCard size={48} className="mb-2" />
                                        <p>Nessun badge assegnato</p>
                                    </div>
                                ) : (
                                    employee.cards.map((card, index) => (
                                        <div key={card.id} className={`${index > 0 ? 'border-top pt-3 mt-3' : ''}`}>
                                            <div className="d-flex justify-content-between align-items-start">
                                                <div>
                                                    <div className="d-flex align-items-center gap-2 mb-2">
                                                        <CreditCard size={20} className="text-primary" />
                                                        <span className="fw-semibold font-monospace">{card.uid}</span>
                                                        {!card.endDate && <span className="badge bg-success">Attivo</span>}
                                                    </div>
                                                    <div className="row g-2 text-muted small">
                                                        <div className="col-auto">
                                                            <strong>Assegnato:</strong> {formatDate(card.startDate)}
                                                        </div>
                                                        {card.endDate && (
                                                            <div className="col-auto">
                                                                <strong>Scadenza:</strong> {formatDate(card.endDate)}
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    )}

                    {activeTab === 'absences' && (
                        <div className="card border-0 shadow-sm">
                            <div className="card-header bg-white border-bottom d-flex justify-content-between align-items-center">
                                <h5 className="mb-0 fw-semibold">Assenze Programmate</h5>
                            </div>
                            <div className="card-body">
                                {employee.absences.length === 0 ? (
                                    <div className="text-center py-4 text-muted">
                                        <Calendar size={48} className="mb-2" />
                                        <p>Nessuna assenza programmata</p>
                                    </div>
                                ) : (
                                    employee.absences.map((absence, index) => (
                                        <div key={absence.id} className={`${index > 0 ? 'border-top pt-3 mt-3' : ''}`}>
                                            <div className="d-flex justify-content-between align-items-start mb-2">
                                                <div className="d-flex align-items-center gap-2">
                                                    <span className={`badge ${getAbsenceTypeBadgeClass(absence.type)}`}>
                                                        {getAbsenceTypeLabel(absence.type)}
                                                    </span>
                                                    <span className="badge bg-success">
                                                        {absence.status}
                                                    </span>
                                                </div>
                                                <span className="text-muted small">{absence.hoursCount} ore</span>
                                            </div>
                                            <div className="d-flex align-items-center gap-2 text-muted">
                                                <Calendar size={16} />
                                                <span>
                                                    {formatDate(absence.startDate)}
                                                    {absence.startDate !== absence.endDate && ` - ${formatDate(absence.endDate)}`}
                                                </span>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    )}

                    {activeTab === 'groups' && (
                        <div className="card border-0 shadow-sm">
                            <div className="card-header bg-white border-bottom d-flex justify-content-between align-items-center">
                                <h5 className="mb-0 fw-semibold">Gruppi di Appartenenza</h5>
                                <button
                                    className="btn btn-sm btn-primary"
                                    onClick={() => setShowGroupModal(true)}
                                >
                                    <span>+ Aggiungi a Gruppo</span>
                                </button>
                            </div>
                            <div className="card-body">
                                {employee.groups.length === 0 ? (
                                    <div className="text-center py-4 text-muted">
                                        <Users size={48} className="mb-2" />
                                        <p>Non appartiene a nessun gruppo</p>
                                    </div>
                                ) : (
                                    <div className="row g-3">
                                        {employee.groups.map((group) => (
                                            <div key={group.id} className="col-md-6">
                                                <div className="border rounded p-3 d-flex justify-content-between align-items-center">
                                                    <div className="d-flex align-items-center gap-2">
                                                        <Users size={20} className="text-primary" />
                                                        <span className="fw-semibold">{group.name}</span>
                                                    </div>
                                                    <button 
                                                        className="btn btn-sm btn-outline-danger"
                                                        onClick={() => {
                                                            removeMemberFromGroup(employee.id, group.id)
                                                            fetchData()
                                                        }}
                                                        >
                                                        <X size={16} />
                                                    </button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {activeTab === 'documents' && (
                        <div className="card border-0 shadow-sm">
                            <div className="card-header bg-white border-bottom d-flex justify-content-between align-items-center">
                                <h5 className="mb-0 fw-semibold">Documenti</h5>
                                <button
                                    className="btn btn-sm btn-primary d-flex align-items-center gap-2"
                                    onClick={() => setShowDocumentModal(true)}
                                >
                                    <Upload size={16} />
                                    <span>Carica Documento</span>
                                </button>
                            </div>
                            <div className="card-body">
                                {documents.length === 0 ? (
                                    <div className="text-center py-4 text-muted">
                                        <FileText size={48} className="mb-2" />
                                        <p>Nessun documento caricato</p>
                                    </div>
                                ) : (
                                    <div className="table-responsive">
                                        <table className="table table-hover mb-0">
                                            <thead className="bg-light">
                                                <tr>
                                                    <th className="border-0" style={{ width: '50px' }}></th>
                                                    <th className="border-0">Nome File</th>
                                                    <th className="border-0">Descrizione</th>
                                                    <th className="border-0">Data Caricamento</th>
                                                    <th className="border-0" style={{ width: '100px' }}>Azioni</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {documents
                                                    .sort((a, b) => new Date(b.uploadedAt) - new Date(a.uploadedAt))
                                                    .map((doc) => (
                                                        <tr key={doc.id}>
                                                            <td>
                                                                <span style={{ fontSize: '1.5rem' }}>
                                                                    {getFileIcon(doc.mimeType)}
                                                                </span>
                                                            </td>
                                                            <td>
                                                                <div className="fw-semibold">{doc.fileName}</div>
                                                                <small className="text-muted">{doc.mimeType}</small>
                                                            </td>
                                                            <td>
                                                                <span className="text-muted">{doc.description || '-'}</span>
                                                            </td>
                                                            <td>
                                                                <span className="text-muted">
                                                                    {formatDate(doc.uploadedAt)}
                                                                </span>
                                                            </td>
                                                            <td>
                                                                <div className="d-flex gap-2">
                                                                    <button
                                                                        className="btn btn-sm btn-outline-primary"
                                                                        title="Scarica"
                                                                        onClick={async () => {
                                                                            try {
                                                                                const blob = await downloadDocument(doc.id);
                                                                                const url = URL.createObjectURL(blob);
                                                                                const a = document.createElement('a');
                                                                                a.href = url;
                                                                                a.download = doc.fileName;
                                                                                a.click();
                                                                                URL.revokeObjectURL(url);
                                                                            } catch (err) {
                                                                                console.error('Errore nel download del file', err);
                                                                            }
                                                                        }}

                                                                    >
                                                                        <Download size={14} />
                                                                    </button>
                                                                    <button
                                                                        className="btn btn-sm btn-outline-danger"
                                                                        title="Elimina"
                                                                        onClick={() => handleFileDelete(doc.id)}
                                                                    >
                                                                        <Trash2 size={14} />
                                                                    </button>
                                                                </div>
                                                            </td>
                                                        </tr>
                                                    ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {activeTab === 'deadlines' && (
                        <div className="card border-0 shadow-sm">
                            <div className="card-header bg-white border-bottom d-flex justify-content-between align-items-center">
                                <h5 className="mb-0 fw-semibold">Scadenze</h5>
                                <button
                                    className="btn btn-sm btn-primary d-flex align-items-center gap-2"
                                    onClick={() => setShowDeadlineModal(true)}
                                >
                                    <CalendarCheck size={16} />
                                    <span>Nuova Scadenza</span>
                                </button>
                            </div>
                            <div className="card-body">
                                {deadlines.length === 0 ? (
                                    <div className="text-center py-4 text-muted">
                                        <CalendarCheck size={48} className="mb-2" />
                                        <p>Nessuna scadenza registrata</p>
                                    </div>
                                ) : (
                                    <div className="list-group list-group-flush">
                                        {deadlines
                                            .sort((a, b) => new Date(a.expirationDate) - new Date(b.expirationDate))
                                            .map((deadline, index) => {
                                                const expired = isDeadlineExpired(deadline.expirationDate);
                                                const expiring = isDeadlineExpiring(deadline.expirationDate);

                                                return (
                                                    <div
                                                        key={deadline.id}
                                                        className={`list-group-item ${expired ? 'border-danger' : expiring ? 'border-warning' : ''} ${index === 0 ? 'border-top-0' : ''}`}
                                                    >
                                                        <div className="d-flex justify-content-between align-items-start mb-2">
                                                            <div className="d-flex align-items-center gap-2 flex-wrap">
                                                                <span className={`badge ${getDeadlineTypeBadgeClass(deadline.type)}`}>
                                                                    {getDeadlineTypeLabel(deadline.type)}
                                                                </span>
                                                                {expired && (
                                                                    <span className="badge bg-danger d-flex align-items-center gap-1">
                                                                        <AlertCircle size={12} />
                                                                        Scaduta
                                                                    </span>
                                                                )}
                                                                {expiring && !expired && (
                                                                    <span className="badge bg-warning d-flex align-items-center gap-1">
                                                                        <AlertCircle size={12} />
                                                                        In scadenza
                                                                    </span>
                                                                )}
                                                                {deadline.notified && (
                                                                    <span className="badge bg-info d-flex align-items-center gap-1">
                                                                        <Bell size={12} />
                                                                        Notificata
                                                                    </span>
                                                                )}
                                                            </div>
                                                            <button
                                                                className="btn btn-sm btn-outline-danger"
                                                                onClick={() => {
                                                                    if (confirm('Confermi di voler eliminare questa scadenza?')) {
                                                                        setDeadlines(deadlines.filter(d => d.id !== deadline.id));
                                                                    }
                                                                }}
                                                            >
                                                                <Trash2 size={14} />
                                                            </button>
                                                        </div>

                                                        <div className="mb-2">
                                                            <div className="d-flex align-items-center gap-2 mb-1">
                                                                <CalendarCheck size={16} className="text-muted" />
                                                                <span className="fw-semibold">
                                                                    Scadenza: {formatDate(deadline.expirationDate)}
                                                                </span>
                                                            </div>
                                                            {deadline.note && (
                                                                <p className="text-muted mb-1 ms-4">{deadline.note}</p>
                                                            )}
                                                        </div>

                                                        <div className="d-flex gap-3 text-muted small ms-4">
                                                            {deadline.reminderDays && (
                                                                <span>
                                                                    <Bell size={14} className="me-1" />
                                                                    Promemoria: {deadline.reminderDays} giorni prima
                                                                </span>
                                                            )}
                                                            {deadline.recipientEmail && (
                                                                <span>
                                                                    <Mail size={14} className="me-1" />
                                                                    {deadline.recipientEmail}
                                                                </span>
                                                            )}
                                                        </div>
                                                    </div>
                                                );
                                            })}
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>

                {/* Sidebar */}
                <div className="col-lg-4">
                    {/* Leave Balance Card */}
                    <div className="card border-0 shadow-sm mb-4">
                        <div className="card-header bg-white border-bottom d-flex justify-content-between align-items-center">
                            <h6 className="mb-0 fw-semibold">Saldo Permessi</h6>
                            <button
                                className="btn btn-sm btn-outline-primary"
                                onClick={() => setShowBalanceModal(true)}
                            >
                                <Edit size={14} />
                            </button>
                        </div>
                        <div className="card-body">
                            <div className="mb-3 pb-3 border-bottom">
                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <span className="text-muted small">Ferie Disponibili</span>
                                    <span className="fw-bold text-primary">{employee.leaveBalance.vacationAvailable} ore</span>
                                </div>
                                <div className="progress" style={{ height: '8px' }}>
                                    <div
                                        className="progress-bar bg-primary"
                                        style={{ width: `${(employee.leaveBalance.vacationAvailable / (employee.leaveBalance.vacationToMature + employee.leaveBalance.vacationUsed)) * 100}%` }}
                                    ></div>
                                </div>
                            </div>
                            <div>
                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <span className="text-muted small">ROL Disponibili</span>
                                    <span className="fw-bold text-success">{employee.leaveBalance.rolAvailable} ore</span>
                                </div>
                                <div className="progress" style={{ height: '8px' }}>
                                    <div
                                        className="progress-bar bg-success"
                                        style={{ width: `${(employee.leaveBalance.rolAvailable / (employee.leaveBalance.rolToMature + employee.leaveBalance.rolUsed)) * 100}%` }}
                                    ></div>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Recent Access Card */}
                    <div className="card border-0 shadow-sm">
                        <div className="card-header bg-white border-bottom">
                            <h6 className="mb-0 fw-semibold">Accessi di oggi</h6>
                        </div>
                        <div className="card-body">
                            {employee.recentAccess.length === 0 ? (
                                <div className="text-center py-3 text-muted small">
                                    <p>Nessun accesso registrato</p>
                                </div>
                            ) : (
                                <div className="list-group list-group-flush">
                                    {employee.recentAccess.map((access, index) => (
                                        <div key={index} className={`py-2 ${index > 0 ? 'border-top' : ''}`}>
                                            <div className="d-flex align-items-center gap-2 mb-1">
                                                {access.type === 'IN' ? (
                                                    <div className="bg-success bg-opacity-10 rounded p-1">
                                                        <CheckCircle size={14} className="text-success" />
                                                    </div>
                                                ) : (
                                                    <div className="bg-secondary bg-opacity-10 rounded p-1">
                                                        <XCircle size={14} className="text-secondary" />
                                                    </div>
                                                )}
                                                <span className="small fw-semibold">
                                                    {access.type === 'IN' ? 'Ingresso' : 'Uscita'}
                                                </span>
                                            </div>
                                            <div className="text-muted small ms-4">
                                                {formatTime(access.timestamp)}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
            {showDocumentModal && (
                <UploadDocumentModal
                    employeeId={employeeId}
                    onClose={() => setShowDocumentModal(false)}
                    onDocumentUploaded={async () => {
                        await fetchDocumentData()
                    }}
                />
            )}

            {showDeadlineModal && (
                <AddDeadlineModal
                    employeeId={employeeId}
                    onClose={() => setShowDeadlineModal(false)}
                    onDeadlineAdded={async () => {
                        await fetchDocumentData()
                    }}
                />
            )}

            <EditEmployeeModal
                isOpen={showEditModal}
                onClose={() => setShowEditModal(false)}
                employee={employee}
                onSave={handleUpdateEmployee}
            />
            <EditBalanceModal
                isOpen={showBalanceModal}
                onClose={() => setShowBalanceModal(false)}
                employee={employee}
                onSave={handleUpdateBalance}
            />
            {showGroupModal && (
                <ManageEmployeeGroupsModal
                    isOpen={showGroupModal}
                    onClose={() => setShowGroupModal(false)}
                    employeeId={employeeId}
                    employeeName={`${employee.name} ${employee.surname}`}
                    currentGroups={employee.groups}
                    onSave={async () => {
                        await fetchData();
                    }}
                />
            )}
        </div>
    );
};


export default EmployeeDetailPage;