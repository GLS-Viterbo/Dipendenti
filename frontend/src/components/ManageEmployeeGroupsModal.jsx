import React, { useState, useEffect } from 'react';
import { Users, X, Plus, Search, CheckCircle } from 'lucide-react';
import { addMemberToGroup, getAllGroups } from '../api/employees';
import { toast } from 'react-toastify';
import { createGroup, addEmployeeToGroup } from '../api/employees';

const ManageEmployeeGroupsModal = ({ isOpen, onClose, employeeId, employeeName, currentGroups = [], onSave }) => {
    const [activeTab, setActiveTab] = useState('add');
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedGroups, setSelectedGroups] = useState([]);
    const [newGroupName, setNewGroupName] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [allGroups, setAllGroups] = useState([]);

    useEffect(() => {
        async function fetchGroups() {
            try {
                const response = await getAllGroups()
                setAllGroups(response)
            } catch (error) {
                console.error(error)
            }
        }
        fetchGroups()
    }, [])

    // Filtra i gruppi già assegnati
    const availableGroups = allGroups.filter(
        group => !currentGroups.some(cg => cg.id === group.id)
    );

    const filteredGroups = availableGroups.filter(group =>
        group.name.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const handleGroupToggle = (groupId) => {
        setSelectedGroups(prev =>
            prev.includes(groupId)
                ? prev.filter(id => id !== groupId)
                : [...prev, groupId]
        );
    };

    const handleAddToGroups = async () => {
        setIsSubmitting(true);
        try {
            // Qui chiamerai la tua API per aggiungere il dipendente ai gruppi selezionati
            await addMemberToGroup(employeeId, selectedGroups);

            console.log('Adding employee to groups:', { employeeId, groupIds: selectedGroups });

            if (onSave) {
                await onSave();
            }

            onClose();
        } catch (error) {
            console.error('Errore nell\'aggiunta ai gruppi:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleCreateGroup = async () => {
        if (!newGroupName.trim()) return;

        setIsSubmitting(true);
        try {
            const newGroup = await createGroup({ name: newGroupName });
            toast.success("Gruppo creato con successo")
            console.log(newGroup)
            await addEmployeeToGroup(employeeId, newGroup.id);
            toast.success("Dipendente aggiunto al gruppo")


            if (onSave) {
                await onSave();
            }

            onClose();
        } catch (error) {
            toast.error("Errore nella creazione del gruppo")
            console.error('Errore nella creazione del gruppo:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            setSelectedGroups([]);
            setNewGroupName('');
            setSearchTerm('');
            setActiveTab('add');
        }
    }, [isOpen]);

    if (!isOpen) return null;

    return (
        <div
            className="modal fade show"
            style={{ display: 'block', backgroundColor: 'rgba(0,0,0,0.5)' }}
            onClick={onClose}
        >
            <div
                className="modal-dialog modal-dialog-centered modal-lg"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="modal-content border-0 shadow-lg">
                    {/* Header */}
                    <div className="modal-header border-bottom">
                        <div>
                            <h5 className="modal-title fw-bold mb-1">Gestisci Gruppi</h5>
                            <p className="text-muted small mb-0">
                                Dipendente: <span className="fw-semibold">{employeeName}</span>
                            </p>
                        </div>
                        <button
                            type="button"
                            className="btn-close"
                            onClick={onClose}
                        ></button>
                    </div>

                    {/* Tabs */}
                    <div className="border-bottom">
                        <ul className="nav nav-tabs border-0" style={{ padding: '0 1.5rem' }}>
                            <li className="nav-item">
                                <button
                                    className={`nav-link ${activeTab === 'add' ? 'active' : ''} border-0 d-flex align-items-center gap-2`}
                                    onClick={() => setActiveTab('add')}
                                >
                                    <Users size={18} />
                                    <span>Aggiungi a Gruppo</span>
                                </button>
                            </li>
                            <li className="nav-item">
                                <button
                                    className={`nav-link ${activeTab === 'create' ? 'active' : ''} border-0 d-flex align-items-center gap-2`}
                                    onClick={() => setActiveTab('create')}
                                >
                                    <Plus size={18} />
                                    <span>Crea Nuovo Gruppo</span>
                                </button>
                            </li>
                        </ul>
                    </div>

                    {/* Body */}
                    <div className="modal-body p-4">
                        {activeTab === 'add' ? (
                            <>
                                {/* Search */}
                                <div className="mb-4">
                                    <div className="input-group">
                                        <span className="input-group-text bg-light border-end-0">
                                            <Search size={18} className="text-muted" />
                                        </span>
                                        <input
                                            type="text"
                                            className="form-control border-start-0 bg-light"
                                            placeholder="Cerca gruppo..."
                                            value={searchTerm}
                                            onChange={(e) => setSearchTerm(e.target.value)}
                                        />
                                    </div>
                                </div>

                                {/* Groups List */}
                                {filteredGroups.length === 0 ? (
                                    <div className="text-center py-5 text-muted">
                                        <Users size={48} className="mb-3" />
                                        <p className="mb-0">
                                            {availableGroups.length === 0
                                                ? 'Il dipendente è già presente in tutti i gruppi disponibili'
                                                : 'Nessun gruppo trovato'}
                                        </p>
                                    </div>
                                ) : (
                                    <div className="row g-3" style={{ maxHeight: '400px', overflowY: 'auto' }}>
                                        {filteredGroups.map((group) => (
                                            <div key={group.id} className="col-12">
                                                <div
                                                    className={`border rounded p-3 cursor-pointer transition ${selectedGroups.includes(group.id)
                                                        ? 'border-primary bg-primary bg-opacity-10'
                                                        : 'border-secondary bg-light'
                                                        }`}
                                                    style={{ cursor: 'pointer' }}
                                                    onClick={() => handleGroupToggle(group.id)}
                                                >
                                                    <div className="d-flex justify-content-between align-items-center">
                                                        <div className="d-flex align-items-center gap-3">
                                                            <div className={`rounded-circle p-2 ${selectedGroups.includes(group.id)
                                                                ? 'bg-primary bg-opacity-10'
                                                                : 'bg-secondary bg-opacity-10'
                                                                }`}>
                                                                <Users size={20} className={
                                                                    selectedGroups.includes(group.id)
                                                                        ? 'text-primary'
                                                                        : 'text-secondary'
                                                                } />
                                                            </div>
                                                            <div>
                                                                <div className="fw-semibold">{group.name}</div>
                                                            </div>
                                                        </div>
                                                        {selectedGroups.includes(group.id) && (
                                                            <CheckCircle size={24} className="text-primary" />
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}

                                {/* Selected Count */}
                                {selectedGroups.length > 0 && (
                                    <div className="alert alert-info d-flex align-items-center gap-2 mt-4 mb-0">
                                        <CheckCircle size={18} />
                                        <span>
                                            {selectedGroups.length} gruppo{selectedGroups.length > 1 ? 'i' : ''} selezionato{selectedGroups.length > 1 ? 'i' : ''}
                                        </span>
                                    </div>
                                )}
                            </>
                        ) : (
                            <>
                                {/* Create New Group Form */}
                                <div className="mb-4">
                                    <label className="form-label fw-semibold">
                                        Nome Gruppo
                                        <span className="text-danger">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        placeholder="Es. Reparto Vendite"
                                        value={newGroupName}
                                        onChange={(e) => setNewGroupName(e.target.value)}
                                        maxLength={50}
                                    />
                                    <small className="text-muted">
                                        Il dipendente verrà automaticamente aggiunto al nuovo gruppo
                                    </small>
                                </div>

                                {/* Info Box */}
                                <div className="alert alert-light border d-flex gap-3">
                                    <div className="bg-primary bg-opacity-10 rounded p-2 d-flex align-items-center justify-content-center" style={{ width: '40px', height: '40px' }}>
                                        <Users size={20} className="text-primary" />
                                    </div>
                                    <div>
                                        <div className="fw-semibold mb-1">Crea un nuovo gruppo</div>
                                        <small className="text-muted">
                                            Dopo la creazione, potrai gestire i membri del gruppo dalla sezione dedicata.
                                        </small>
                                    </div>
                                </div>
                            </>
                        )}
                    </div>

                    {/* Footer */}
                    <div className="modal-footer border-top bg-light">
                        <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={onClose}
                            disabled={isSubmitting}
                        >
                            Annulla
                        </button>
                        {activeTab === 'add' ? (
                            <button
                                type="button"
                                className="btn btn-primary d-flex align-items-center gap-2"
                                onClick={handleAddToGroups}
                                disabled={selectedGroups.length === 0 || isSubmitting}
                            >
                                {isSubmitting ? (
                                    <>
                                        <span className="spinner-border spinner-border-sm" role="status"></span>
                                        <span>Aggiunta in corso...</span>
                                    </>
                                ) : (
                                    <>
                                        <CheckCircle size={18} />
                                        <span>Aggiungi ai Gruppi</span>
                                    </>
                                )}
                            </button>
                        ) : (
                            <button
                                type="button"
                                className="btn btn-primary d-flex align-items-center gap-2"
                                onClick={handleCreateGroup}
                                disabled={!newGroupName.trim() || isSubmitting}
                            >
                                {isSubmitting ? (
                                    <>
                                        <span className="spinner-border spinner-border-sm" role="status"></span>
                                        <span>Creazione in corso...</span>
                                    </>
                                ) : (
                                    <>
                                        <Plus size={18} />
                                        <span>Crea e Aggiungi</span>
                                    </>
                                )}
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ManageEmployeeGroupsModal;