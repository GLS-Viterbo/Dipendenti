import React, { useState, useEffect } from 'react';
import { X, Clock, Plane, Coffee, AlertCircle, TrendingUp, TrendingDown } from 'lucide-react';
import { toast } from "react-toastify";

const EditBalanceModal = ({ isOpen, onClose, employee, onSave }) => {
    const [formData, setFormData] = useState({
        vacationAvailable: '',
        rolAvailable: ''
    });

    const [errors, setErrors] = useState({});
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        if (employee?.leaveBalance && isOpen) {
            setFormData({
                vacationAvailable: employee.leaveBalance.vacationAvailable || 0,
                rolAvailable: employee.leaveBalance.rolAvailable || 0
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

        if (formData.vacationAvailable === '' || isNaN(formData.vacationAvailable)) {
            newErrors.vacationAvailable = 'Inserire un valore valido';
        }
        if (formData.rolAvailable === '' || isNaN(formData.rolAvailable)) {
            newErrors.rolAvailable = 'Inserire un valore valido';
        }

        if (parseFloat(formData.vacationAvailable) < 0) {
            newErrors.vacationAvailable = 'Il valore non può essere negativo';
        }
        if (parseFloat(formData.rolAvailable) < 0) {
            newErrors.rolAvailable = 'Il valore non può essere negativo';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const calculateNewBalance = () => {
        return {
            vacationAvailable: parseFloat(formData.vacationAvailable),
            rolAvailable: parseFloat(formData.rolAvailable)
        };
    };

    const handleSubmit = async () => {
        if (validateForm()) {
            setIsSaving(true);
            try {
                const newBalance = calculateNewBalance();
                await onSave(employee.id, newBalance);
                toast.success('Saldo permessi aggiornato con successo!');
                handleClose();
            } catch (error) {
                toast.error('Errore nell\'aggiornamento del saldo');
                console.error(error);
            } finally {
                setIsSaving(false);
            }
        }
    };

    const handleClose = () => {
        setFormData({
            vacationAvailable: '',
            rolAvailable: ''
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
                <div className="modal-dialog modal-dialog-centered modal-lg">
                    <div className="modal-content border-0 shadow-lg">
                        {/* Header */}
                        <div className="modal-header bg-primary text-white border-0">
                            <div className="d-flex align-items-center gap-2">
                                <div className="bg-white bg-opacity-25 rounded p-2">
                                    <Clock size={24} />
                                </div>
                                <div>
                                    <h5 className="modal-title mb-0 fw-bold">
                                        Modifica Saldo Permessi
                                    </h5>
                                    <small className="opacity-75">
                                        {employee?.name} {employee?.surname}
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
                            {/* Alert Info */}
                            <div className="alert alert-info d-flex align-items-start gap-2 mb-4" role="alert">
                                <AlertCircle size={20} className="mt-1 flex-shrink-0" />
                                <div className="small">
                                    <strong>Attenzione:</strong> La modifica del saldo permessi è un'operazione delicata.
                                    Assicurati di inserire i valori corretti.
                                </div>
                            </div>

                            {/* Saldo Attuale */}
                            <div className="card bg-light border-0 mb-4">
                                <div className="card-body">
                                    <h6 className="fw-semibold mb-3">Saldo Attuale</h6>
                                    <div className="row g-3">
                                        <div className="col-md-6">
                                            <div className="d-flex align-items-center gap-2 mb-1">
                                                <Plane size={16} className="text-primary" />
                                                <span className="text-muted small">Ferie</span>
                                            </div>
                                            <div className="h4 fw-bold text-primary mb-0">
                                                {employee?.leaveBalance?.vacationAvailable || 0} ore
                                            </div>
                                        </div>
                                        <div className="col-md-6">
                                            <div className="d-flex align-items-center gap-2 mb-1">
                                                <Coffee size={16} className="text-success" />
                                                <span className="text-muted small">ROL</span>
                                            </div>
                                            <div className="h4 fw-bold text-success mb-0">
                                                {employee?.leaveBalance?.rolAvailable || 0} ore
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>


                            {/* Form Ferie e ROL in un'unica row */}
                            <div className="mb-4">
                                <div className="row g-3">
                                    {/* Ferie */}
                                    <div className="col">
                                        <h6 className="fw-semibold text-primary mb-3 d-flex align-items-center gap-2">
                                            <Plane size={18} />
                                            Ferie
                                        </h6>
                                        <div className="input-group">
                                            <span className="input-group-text bg-light border-end-0">
                                                <Plane size={16} className="text-muted" />
                                            </span>
                                            <input
                                                type="number"
                                                className={`form-control border-start-0 ${errors.vacationAvailable ? 'is-invalid' : ''}`}
                                                name="vacationAvailable"
                                                value={formData.vacationAvailable}
                                                onChange={handleChange}
                                                placeholder="0"
                                                step="0.01"
                                                min="0"
                                                disabled={isSaving}
                                            />
                                            <span className="input-group-text bg-light border-start-0">ore</span>
                                            {errors.vacationAvailable && (
                                                <div className="invalid-feedback">{errors.vacationAvailable}</div>
                                            )}
                                        </div>
                                    </div>

                                    {/* ROL */}
                                    <div className="col">
                                        <h6 className="fw-semibold text-success mb-3 d-flex align-items-center gap-2">
                                            <Coffee size={18} />
                                            ROL
                                        </h6>
                                        <div className="input-group">
                                            <span className="input-group-text bg-light border-end-0">
                                                <Coffee size={16} className="text-muted" />
                                            </span>
                                            <input
                                                type="number"
                                                className={`form-control border-start-0 ${errors.rolAvailable ? 'is-invalid' : ''}`}
                                                name="rolAvailable"
                                                value={formData.rolAvailable}
                                                onChange={handleChange}
                                                placeholder="0"
                                                step="0.01"
                                                min="0"
                                                disabled={isSaving}
                                            />
                                            <span className="input-group-text bg-light border-start-0">ore</span>
                                            {errors.rolAvailable && (
                                                <div className="invalid-feedback">{errors.rolAvailable}</div>
                                            )}
                                        </div>
                                    </div>
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
                                <Clock size={18} />
                                {isSaving ? 'Salvataggio...' : 'Salva Modifiche'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
};

export default EditBalanceModal;