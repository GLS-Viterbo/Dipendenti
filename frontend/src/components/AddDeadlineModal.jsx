import React, { useState } from 'react';
import { CalendarCheck, X, Bell, Mail, AlertCircle } from 'lucide-react';
import { toast } from 'react-toastify';
import { addDeadline } from '../api/employees';

const AddDeadlineModal = ({ employeeId, onClose, onDeadlineAdded }) => {
    const [formData, setFormData] = useState({
        type: 'CONTRATTO',
        expirationDate: '',
        note: '',
        reminderDays: 30,
        recipientEmail: '',
        notified: false
    });
    const [saving, setSaving] = useState(false);

    const deadlineTypes = [
        { value: 'CONTRATTO', label: 'Contratto' },
        { value: 'CERTIFICATO_MEDICO', label: 'Certificato Medico' },
        { value: 'FORMAZIONE', label: 'Formazione' },
        { value: 'DOCUMENTO', label: 'Documento' },
        { value: 'PATENTE', label: 'Patente' },
        { value: 'ASSICURAZIONE', label: 'Assicurazione' },
        { value: 'ALTRO', label: 'Altro' }
    ];

    const reminderOptions = [
        { value: 7, label: '7 giorni prima' },
        { value: 15, label: '15 giorni prima' },
        { value: 30, label: '30 giorni prima' },
        { value: 60, label: '60 giorni prima' },
        { value: 90, label: '90 giorni prima' }
    ];

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const validateForm = () => {
        if (!formData.expirationDate) {
            toast.error('La data di scadenza è obbligatoria');
            return false;
        }

        const selectedDate = new Date(formData.expirationDate);
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        if (selectedDate < today) {
            toast.error('La data di scadenza non può essere nel passato');
            return false;
        }

        if (formData.recipientEmail && !isValidEmail(formData.recipientEmail)) {
            toast.error('Inserisci un indirizzo email valido');
            return false;
        }

        return true;
    };

    const isValidEmail = (email) => {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        setSaving(true);

        try {
            // Prepara i dati per l'API
            const deadlineData = {
                employeeId: employeeId,
                type: formData.type,
                expirationDate: formData.expirationDate,
                note: formData.note || null,
                reminderDays: parseInt(formData.reminderDays),
                recipientEmail: formData.recipientEmail || null,
                notified: false
            };

            // Chiamata API - sostituire con la tua implementazione
            await addDeadline(deadlineData)

            toast.success('Scadenza aggiunta con successo!');
            onDeadlineAdded();
            onClose();
        } catch (error) {
            console.error('Errore salvataggio scadenza:', error);
            toast.error('Errore durante il salvataggio della scadenza');
        } finally {
            setSaving(false);
        }
    };

    const getDaysUntilExpiration = () => {
        if (!formData.expirationDate) return null;
        const today = new Date();
        const expDate = new Date(formData.expirationDate);
        const diffTime = expDate - today;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        return diffDays;
    };

    const daysUntil = getDaysUntilExpiration();

    return (
        <div 
            className="modal show d-block" 
            style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
            onClick={onClose}
        >
            <div 
                className="modal-dialog modal-dialog-centered modal-lg"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="modal-content">
                    <div className="modal-header border-bottom">
                        <h5 className="modal-title fw-semibold">
                            <CalendarCheck size={20} className="me-2" />
                            Nuova Scadenza
                        </h5>
                        <button 
                            type="button" 
                            className="btn-close"
                            onClick={onClose}
                            disabled={saving}
                        ></button>
                    </div>

                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            <div className="row g-3">
                                {/* Tipo Scadenza */}
                                <div className="col-md-6">
                                    <label htmlFor="type" className="form-label fw-semibold">
                                        Tipo Scadenza <span className="text-danger">*</span>
                                    </label>
                                    <select
                                        id="type"
                                        name="type"
                                        className="form-select"
                                        value={formData.type}
                                        onChange={handleChange}
                                        disabled={saving}
                                        required
                                    >
                                        {deadlineTypes.map(type => (
                                            <option key={type.value} value={type.value}>
                                                {type.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {/* Data Scadenza */}
                                <div className="col-md-6">
                                    <label htmlFor="expirationDate" className="form-label fw-semibold">
                                        Data Scadenza <span className="text-danger">*</span>
                                    </label>
                                    <input
                                        type="date"
                                        id="expirationDate"
                                        name="expirationDate"
                                        className="form-control"
                                        value={formData.expirationDate}
                                        onChange={handleChange}
                                        disabled={saving}
                                        required
                                    />
                                    {daysUntil !== null && daysUntil >= 0 && (
                                        <small className={`text-${daysUntil <= 30 ? 'warning' : 'muted'}`}>
                                            Tra {daysUntil} giorni
                                        </small>
                                    )}
                                </div>

                                {/* Note */}
                                <div className="col-12">
                                    <label htmlFor="note" className="form-label fw-semibold">
                                        Note <small className="text-muted">(opzionale)</small>
                                    </label>
                                    <textarea
                                        id="note"
                                        name="note"
                                        className="form-control"
                                        rows="3"
                                        placeholder="Inserisci eventuali note o dettagli aggiuntivi..."
                                        value={formData.note}
                                        onChange={handleChange}
                                        disabled={saving}
                                        maxLength={500}
                                    />
                                    <small className="text-muted">
                                        {formData.note.length}/500 caratteri
                                    </small>
                                </div>

                                {/* Promemoria */}
                                <div className="col-12">
                                    <div className="card bg-light border-0">
                                        <div className="card-body">
                                            <h6 className="fw-semibold mb-3 d-flex align-items-center gap-2">
                                                <Bell size={18} />
                                                Promemoria
                                            </h6>
                                            
                                            <div className="row g-3">
                                                <div className="col-md-6">
                                                    <label htmlFor="reminderDays" className="form-label">
                                                        Invia promemoria
                                                    </label>
                                                    <select
                                                        id="reminderDays"
                                                        name="reminderDays"
                                                        className="form-select"
                                                        value={formData.reminderDays}
                                                        onChange={handleChange}
                                                        disabled={saving}
                                                    >
                                                        {reminderOptions.map(option => (
                                                            <option key={option.value} value={option.value}>
                                                                {option.label}
                                                            </option>
                                                        ))}
                                                    </select>
                                                </div>

                                                <div className="col-md-6">
                                                    <label htmlFor="recipientEmail" className="form-label">
                                                        Email destinatario
                                                    </label>
                                                    <div className="input-group">
                                                        <span className="input-group-text">
                                                            <Mail size={16} />
                                                        </span>
                                                        <input
                                                            type="email"
                                                            id="recipientEmail"
                                                            name="recipientEmail"
                                                            className="form-control"
                                                            placeholder="email@example.com"
                                                            value={formData.recipientEmail}
                                                            onChange={handleChange}
                                                            disabled={saving}
                                                        />
                                                    </div>
                                                    <small className="text-muted">
                                                        Se non specificato, non verrà inviata alcuna notifica
                                                    </small>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* Info Alert */}
                                <div className="col-12">
                                    <div className="alert alert-info d-flex align-items-start gap-2 mb-0">
                                        <AlertCircle size={20} className="flex-shrink-0 mt-1" />
                                        <small>
                                            Il sistema invierà automaticamente una notifica email al destinatario 
                                            specificato nel periodo indicato prima della scadenza.
                                        </small>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="modal-footer border-top">
                            <button 
                                type="button" 
                                className="btn btn-outline-secondary"
                                onClick={onClose}
                                disabled={saving}
                            >
                                Annulla
                            </button>
                            <button 
                                type="submit" 
                                className="btn btn-primary d-flex align-items-center gap-2"
                                disabled={saving}
                            >
                                {saving ? (
                                    <>
                                        <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                                        <span>Salvataggio...</span>
                                    </>
                                ) : (
                                    <>
                                        <CalendarCheck size={16} />
                                        <span>Aggiungi Scadenza</span>
                                    </>
                                )}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default AddDeadlineModal;