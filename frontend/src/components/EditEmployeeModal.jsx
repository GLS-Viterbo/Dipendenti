import React, { useState, useEffect } from 'react';
import { X, User, Mail, Phone, MapPin, Calendar, Hash, FileText } from 'lucide-react';
import { toast } from "react-toastify";

const EditEmployeeModal = ({ isOpen, onClose, employee, onSave }) => {
    const [formData, setFormData] = useState({
        companyId: employee.companyId,
        name: '',
        surname: '',
        taxCode: '',
        birthday: '',
        address: '',
        city: '',
        email: '',
        phone: '',
        note: ''
    });

    const [errors, setErrors] = useState({});
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        if (employee && isOpen) {
            setFormData({
                companyId: employee.companyId,
                name: employee.name || '',
                surname: employee.surname || '',
                taxCode: employee.taxCode || '',
                birthday: employee.birthday || '',
                address: employee.address || '',
                city: employee.city || '',
                email: employee.email || '',
                phone: employee.phone || '',
                note: employee.note || ''
            });
            setErrors({});
        }
    }, [employee, isOpen]);

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

    const handleSubmit = async () => {
        if (validateForm()) {
            setIsSaving(true);
            try {
                const updatedData = {
                    ...formData,
                    taxCode: formData.taxCode.toUpperCase(),
                    companyId: employee.companyId
                };
                
                await onSave(employee.id, updatedData);
                toast.success('Dipendente aggiornato con successo!');
                handleClose();
            } catch (error) {
                toast.error('Errore nell\'aggiornamento del dipendente');
                console.error(error);
            } finally {
                setIsSaving(false);
            }
        }
    };

    const handleClose = () => {
        setFormData({
            companyId: '',
            name: '',
            surname: '',
            taxCode: '',
            birthday: '',
            address: '',
            city: '',
            email: '',
            phone: '',
            note: ''
        });
        setErrors({});
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
                                        Modifica Dipendente
                                    </h5>
                                    <small className="opacity-75">
                                        Aggiorna i dati anagrafici di {employee?.name} {employee?.surname}
                                    </small>
                                </div>
                            </div>
                            <button
                                type="button"
                                className="btn-close btn-close-white"
                                onClick={handleClose}
                            />
                        </div>

                        {/* Body */}
                        <div className="modal-body p-4">
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
                                            disabled={isSaving}
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
                                            disabled={isSaving}
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
                                                disabled={isSaving}
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
                                                disabled={isSaving}
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
                                                disabled={isSaving}
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
                                                disabled={isSaving}
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
                                            disabled={isSaving}
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
                                            disabled={isSaving}
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
                                    disabled={isSaving}
                                />
                                <div className="form-text text-end">
                                    {formData.note.length}/200 caratteri
                                </div>
                            </div>
                        </div>

                        {/* Footer */}
                        <div className="modal-footer bg-light border-0">
                            <button
                                type="button"
                                className="btn btn-outline-secondary d-flex align-items-center gap-2"
                                onClick={handleClose}
                                disabled={isSaving}
                            >
                                <X size={18} />
                                Annulla
                            </button>
                            <button
                                type="button"
                                className="btn btn-primary d-flex align-items-center gap-2"
                                onClick={handleSubmit}
                                disabled={isSaving}
                            >
                                <User size={18} />
                                {isSaving ? 'Salvataggio...' : 'Salva Modifiche'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
};

export default EditEmployeeModal;