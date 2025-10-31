import React, { useState, useEffect } from 'react';
import { CreditCard, Users, Search, Plus, X, Trash2, History, Calendar, CheckCircle, AlertCircle } from 'lucide-react';
import { assignCard, getAssignedCardWithDetails, getCardHistory, getUnassignedCards, revokeAssignment } from '../api/card';
import { formatDate, getTodayDate } from '../utils/utils';
import { getEmployeesWithoutCard } from '../api/employees';
import { toast } from "react-toastify";

const CardManagementPage = () => {
    const [activeTab, setActiveTab] = useState('assigned');
    const [searchTerm, setSearchTerm] = useState('');
    const [showAssignModal, setShowAssignModal] = useState(false);
    const [showRevokeModal, setShowRevokeModal] = useState(false);
    const [showHistoryModal, setShowHistoryModal] = useState(false);
    const [selectedCard, setSelectedCard] = useState(null);
    const [selectedAssignment, setSelectedAssignment] = useState(null);
    const [assignedCards, setAssignedCards] = useState([]);
    const [unassignedCards, setUnassignedCards] = useState([]);
    const [filterStatus, setFilterStatus] = useState('all');
    const [selectedCardHistory, setSelectedCardHistory] = useState([])
    const [employeesWithoutCard, setEmployeesWithoutCard] = useState([])


    useEffect(() => {
        async function getAssignedCards() {
            try {
                const assignedCardData = await getAssignedCardWithDetails();
                const unassignedCardData = await getUnassignedCards();
                const withoutCardData = await getEmployeesWithoutCard();

                setAssignedCards(assignedCardData)
                setUnassignedCards(unassignedCardData)
                setEmployeesWithoutCard(withoutCardData)
            } catch (error) {
                console.error('Errore nel recupero delle card assegnate:', error.message);
            }
        }
        getAssignedCards();
    }, [])

    useEffect(() => {
        async function fetchSelectedCardData() {
            try {
                if (selectedCard !== null) {
                    const cardHistoryData = await getCardHistory(selectedCard.id)
                    setSelectedCardHistory(cardHistoryData)
                }
            } catch (error) {
                console.error("Impossibile ottenere i dati della card selezionata")
            }
        }
        fetchSelectedCardData();
    }, [selectedCard])

    const refreshCards = async () => {
        try {
            const assignedCardData = await getAssignedCardWithDetails();
            const unassignedCardData = await getUnassignedCards();
            const withoutCardData = await getEmployeesWithoutCard();

            setAssignedCards(assignedCardData);
            setUnassignedCards(unassignedCardData);
            setEmployeesWithoutCard(withoutCardData);
        } catch (error) {
            console.error('Errore nel recupero delle card:', error);
        }
    };


    // Form states
    const [selectedEmployee, setSelectedEmployee] = useState('');

    const filteredAssignedCards = assignedCards.filter(card => {
        const matchesSearch =
            card.cardUid.toLowerCase().includes(searchTerm.toLowerCase()) ||
            card.employeeName.toLowerCase().includes(searchTerm.toLowerCase()) ||
            card.employeeSurname.toLowerCase().includes(searchTerm.toLowerCase());

        const matchesStatus =
            filterStatus === 'all' ||
            (filterStatus === 'active' && card.active) ||
            (filterStatus === 'expired' && !card.active);

        return matchesSearch && matchesStatus;
    });

    const filteredUnassignedCards = unassignedCards.filter(card =>
        card.uid.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const handleAssignCard = async () => {
        if (!selectedEmployee) {
            alert('Seleziona un dipendente e una data di inizio');
            return;
        }

        const employee = employeesWithoutCard.find(e => e.id === parseInt(selectedEmployee));
        const newAssignment = {
            cardId: selectedCard.id,
            employeeId: employee.id,
        };

        try {
            await assignCard(newAssignment);
            await refreshCards();
            setShowAssignModal(false);
            setSelectedCard(null);
            setSelectedEmployee('');
            toast.success("Card assegnata con successo")
        } catch (error) {
            toast.error("Errore nell'assegnazione della card");
            console.error(error)
        }
    };


    const handleRevokeAssignment = async () => {
        try {
            const today = getTodayDate();
            await revokeAssignment(selectedAssignment.assignmentId);
            await refreshCards();

            setShowRevokeModal(false);
            setSelectedAssignment(null);
            toast.success("Card revocata con successo")
        } catch (error) {
            toast.error("Errore nella revoca della card");
            console.error(error)
        }
    };


    const openAssignModal = (card) => {
        setSelectedCard(card);
        setShowAssignModal(true);
    };

    const openRevokeModal = (card) => {
        setSelectedAssignment(card);
        setShowRevokeModal(true);
    };

    const openHistoryModal = (card) => {
        setSelectedCard(card);
        setShowHistoryModal(true);
    };

    return (
        <div>
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 className="mb-1">Gestione Badge</h2>
                    <p className="text-muted mb-0">Gestisci le assegnazioni dei badge aziendali</p>
                </div>
            </div>

            {/* Tabs */}
            <ul className="nav nav-tabs mb-4">
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'assigned' ? 'active' : ''}`}
                        onClick={() => {
                            setActiveTab('assigned');
                            setSearchTerm('');
                        }}
                    >
                        <CheckCircle size={18} className="me-2" />
                        Badge Assegnati
                        <span className="badge bg-primary ms-2">{assignedCards.length}</span>
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'unassigned' ? 'active' : ''}`}
                        onClick={() => {
                            setActiveTab('unassigned');
                            setSearchTerm('');
                        }}
                    >
                        <AlertCircle size={18} className="me-2" />
                        Badge Non Assegnati
                        <span className="badge bg-warning ms-2">{unassignedCards.length}</span>
                    </button>
                </li>
            </ul>

            {/* Search and Filter */}
            <div className="card border-0 shadow-sm mb-4">
                <div className="card-body">
                    <div className="row g-3">
                        <div className="col-md-6">
                            <div className="input-group">
                                <span className="input-group-text bg-white">
                                    <Search size={18} />
                                </span>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder={activeTab === 'assigned' ? 'Cerca per badge o dipendente...' : 'Cerca per UID badge...'}
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>
                        </div>
                        {activeTab === 'assigned' && (
                            <div className="col-md-6">
                                <select
                                    className="form-select"
                                    value={filterStatus}
                                    onChange={(e) => setFilterStatus(e.target.value)}
                                >
                                    <option value="all">Tutti gli stati</option>
                                    <option value="active">Solo attivi</option>
                                    <option value="expired">Solo scaduti</option>
                                </select>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Assigned Cards Tab */}
            {activeTab === 'assigned' && (
                <div className="card border-0 shadow-sm">
                    <div className="card-body">
                        {filteredAssignedCards.length === 0 ? (
                            <div className="text-center py-5">
                                <CreditCard size={48} className="text-muted mb-3" />
                                <p className="text-muted">Nessun badge assegnato trovato</p>
                            </div>
                        ) : (
                            <div className="table-responsive">
                                <table className="table table-hover align-middle">
                                    <thead>
                                        <tr>
                                            <th>Badge UID</th>
                                            <th>Dipendente</th>
                                            <th>Data Inizio</th>
                                            <th>Data Fine</th>
                                            <th>Stato</th>
                                            <th className="text-end">Azioni</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {filteredAssignedCards.map((card) => (
                                            <tr key={card.id}>
                                                <td>
                                                    <div className="d-flex align-items-center gap-2">
                                                        <div className="bg-primary bg-opacity-10 rounded p-2">
                                                            <CreditCard size={18} className="text-primary" />
                                                        </div>
                                                        <span className="fw-semibold">{card.cardUid}</span>
                                                    </div>
                                                </td>
                                                <td>
                                                    <div className="d-flex align-items-center gap-2">
                                                        <Users size={16} className="text-muted" />
                                                        {card.employeeName} {card.employeeSurname}
                                                    </div>
                                                </td>
                                                <td>
                                                    <small className="text-muted">
                                                        {formatDate(card.assignmentStartDate)}
                                                    </small>
                                                </td>
                                                <td>
                                                    <small className="text-muted">
                                                        {card.endDate
                                                            ? formatDate(card.assignmentEndDate)
                                                            : 'N/A'}
                                                    </small>
                                                </td>
                                                <td>
                                                    {card.active ? (
                                                        <span className="badge bg-success">Attivo</span>
                                                    ) : (
                                                        <span className="badge bg-secondary">Scaduto</span>
                                                    )}
                                                </td>
                                                <td>
                                                    <div className="d-flex gap-2 justify-content-end">
                                                        <button
                                                            className="btn btn-sm btn-outline-primary"
                                                            onClick={() => openHistoryModal(card)}
                                                            title="Visualizza storico"
                                                        >
                                                            <History size={16} />
                                                        </button>
                                                        {card.active && (
                                                            <button
                                                                className="btn btn-sm btn-outline-danger"
                                                                onClick={() => openRevokeModal(card)}
                                                                title="Revoca assegnazione"
                                                            >
                                                                <Trash2 size={16} />
                                                            </button>
                                                        )}
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

            {/* Unassigned Cards Tab */}
            {activeTab === 'unassigned' && (
                <div className="card border-0 shadow-sm">
                    <div className="card-body">
                        {filteredUnassignedCards.length === 0 ? (
                            <div className="text-center py-5">
                                <CreditCard size={48} className="text-muted mb-3" />
                                <p className="text-muted">Nessun badge disponibile</p>
                            </div>
                        ) : (
                            <div className="row g-3">
                                {filteredUnassignedCards.map((card) => (
                                    <div key={card.id} className="col-md-6 col-lg-4">
                                        <div className="card border h-100">
                                            <div className="card-body">
                                                <div className="d-flex justify-content-between align-items-start mb-3">
                                                    <div className="bg-warning bg-opacity-10 rounded p-2">
                                                        <CreditCard size={24} className="text-warning" />
                                                    </div>
                                                    <span className="badge bg-warning">Disponibile</span>
                                                </div>
                                                <h6 className="fw-bold mb-3">{card.uid}</h6>
                                                <button
                                                    className="btn btn-primary w-100"
                                                    onClick={() => openAssignModal(card)}
                                                >
                                                    <Plus size={16} className="me-2" />
                                                    Assegna Badge
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Assign Modal */}
            {showAssignModal && (
                <div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Assegna Badge</h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => {
                                        setShowAssignModal(false);
                                        setSelectedCard(null);
                                        setSelectedEmployee('');
                                    }}
                                ></button>
                            </div>
                            <div className="modal-body">
                                <div className="mb-3">
                                    <label className="form-label fw-semibold">Badge UID</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={selectedCard?.uid || ''}
                                        disabled
                                    />
                                </div>
                                <div className="mb-3">
                                    <label className="form-label fw-semibold">Dipendente *</label>
                                    <select
                                        className="form-select"
                                        value={selectedEmployee}
                                        onChange={(e) => setSelectedEmployee(e.target.value)}
                                    >
                                        <option value="">Seleziona un dipendente</option>
                                        {employeesWithoutCard.map((emp) => (
                                            <option key={emp.id} value={emp.id}>
                                                {emp.name} {emp.surname}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => {
                                        setShowAssignModal(false);
                                        setSelectedCard(null);
                                        setSelectedEmployee('');
                                    }}
                                >
                                    Annulla
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-primary"
                                    onClick={handleAssignCard}
                                >
                                    Assegna Badge
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Revoke Modal */}
            {showRevokeModal && (
                <div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Conferma Revoca</h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => {
                                        setShowRevokeModal(false);
                                        setSelectedAssignment(null);
                                    }}
                                ></button>
                            </div>
                            <div className="modal-body">
                                <div className="alert alert-warning">
                                    <strong>Attenzione!</strong> Stai per revocare l'assegnazione del badge.
                                </div>
                                <p>
                                    <strong>Badge:</strong> {selectedAssignment?.cardUid}<br />
                                    <strong>Dipendente:</strong> {selectedAssignment?.employeeName} {selectedAssignment?.employeeSurname}<br />
                                    <strong>Data Inizio:</strong> {selectedAssignment && formatDate(selectedAssignment.assignmentStartDate)}
                                </p>
                                <p className="text-muted">
                                    La data fine verrà impostata a oggi e il badge tornerà disponibile.
                                </p>
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => {
                                        setShowRevokeModal(false);
                                        setSelectedAssignment(null);
                                    }}
                                >
                                    Annulla
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-danger"
                                    onClick={handleRevokeAssignment}
                                >
                                    Revoca Assegnazione
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* History Modal */}
            {showHistoryModal && (
                <div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered modal-lg">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">Storico Assegnazioni</h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => {
                                        setShowHistoryModal(false);
                                        setSelectedCard(null);
                                    }}
                                ></button>
                            </div>
                            <div className="modal-body">
                                <div className="mb-3">
                                    <strong>Badge UID:</strong> {selectedCard?.cardUid}
                                </div>
                                <div className="table-responsive">
                                    <table className="table">
                                        <thead>
                                            <tr>
                                                <th>Dipendente</th>
                                                <th>Data Inizio</th>
                                                <th>Data Fine</th>
                                                <th>Stato</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {selectedCardHistory.map((item) => (
                                                <tr key={item.id}>
                                                    <td>{item.employeeName} {item.employeeSurname}</td>
                                                    <td>{formatDate(item.startDate)}</td>
                                                    <td>
                                                        {item.endDate
                                                            ? formatDate(item.endDate)
                                                            : 'In corso'}
                                                    </td>
                                                    <td>
                                                        {item.endDate ? (
                                                            <span className="badge bg-secondary">Terminato</span>
                                                        ) : (
                                                            <span className="badge bg-success">Attivo</span>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => {
                                        setShowHistoryModal(false);
                                        setSelectedCard(null);
                                    }}
                                >
                                    Chiudi
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CardManagementPage;