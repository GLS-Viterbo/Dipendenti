import React, { useState } from "react";
import { Calendar, Plus } from "lucide-react";
import { toast } from "react-toastify";

const HolidayModal = ({ showModal, setShowModal, onSubmit }) => {
    const [formData, setFormData] = useState({
        name: "",
        recurring: false,
        day: "",
        month: "",
        year: new Date().getFullYear()
    });

    const resetForm = () => {
        setFormData({
            name: "",
            recurring: false,
            day: "",
            month: "",
            year: new Date().getFullYear()
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!formData.name.trim()) {
            toast.error("Inserisci il nome del giorno festivo");
            return;
        }

        if (!formData.day || !formData.month) {
            toast.error("Seleziona giorno e mese");
            return;
        }

        if (!formData.recurring && !formData.year) {
            toast.error("Seleziona l'anno per un festivo non ricorrente");
            return;
        }

        try {
            await onSubmit({
                name: formData.name.trim(),
                recurring: formData.recurring,
                day: parseInt(formData.day),
                month: parseInt(formData.month),
                year: formData.recurring ? null : parseInt(formData.year)
            });
            
            resetForm();
            setShowModal(false);
        } catch (error) {
            console.error(error);
        }
    };

    if (!showModal) return null;

    const months = [
        { value: 1, label: "Gennaio" },
        { value: 2, label: "Febbraio" },
        { value: 3, label: "Marzo" },
        { value: 4, label: "Aprile" },
        { value: 5, label: "Maggio" },
        { value: 6, label: "Giugno" },
        { value: 7, label: "Luglio" },
        { value: 8, label: "Agosto" },
        { value: 9, label: "Settembre" },
        { value: 10, label: "Ottobre" },
        { value: 11, label: "Novembre" },
        { value: 12, label: "Dicembre" }
    ];

    const getDaysInMonth = () => {
        if (!formData.month) return 31;
        const month = parseInt(formData.month);
        const year = formData.year || new Date().getFullYear();
        return new Date(year, month, 0).getDate();
    };

    const currentYear = new Date().getFullYear();
    const years = Array.from({ length: 10 }, (_, i) => currentYear + i);

    return (
        <div
            className="modal show d-block"
            style={{ backgroundColor: "rgba(0,0,0,0.5)" }}
            onClick={(e) => {
                if (e.target === e.currentTarget) {
                    setShowModal(false);
                    resetForm();
                }
            }}
        >
            <div className="modal-dialog modal-dialog-centered">
                <div className="modal-content">
                    <div className="modal-header border-bottom">
                        <h5 className="modal-title d-flex align-items-center">
                            <Calendar size={20} className="me-2 text-primary" />
                            Aggiungi Giorno Festivo
                        </h5>
                        <button
                            type="button"
                            className="btn-close"
                            onClick={() => {
                                setShowModal(false);
                                resetForm();
                            }}
                        />
                    </div>

                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            <div className="mb-4">
                                <label className="form-label fw-semibold">
                                    Nome Festività *
                                </label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={formData.name}
                                    onChange={(e) =>
                                        setFormData({ ...formData, name: e.target.value })
                                    }
                                    placeholder="Es: Natale, Capodanno, Ferragosto..."
                                    required
                                />
                                <small className="text-muted">
                                    Inserisci un nome descrittivo per il giorno festivo
                                </small>
                            </div>

                            <div className="row mb-4">
                                <div className="col-6">
                                    <label className="form-label fw-semibold">Giorno *</label>
                                    <select
                                        className="form-select"
                                        value={formData.day}
                                        onChange={(e) =>
                                            setFormData({ ...formData, day: e.target.value })
                                        }
                                        required
                                    >
                                        <option value="">Seleziona</option>
                                        {Array.from({ length: getDaysInMonth() }, (_, i) => i + 1).map(
                                            (day) => (
                                                <option key={day} value={day}>
                                                    {day}
                                                </option>
                                            )
                                        )}
                                    </select>
                                </div>
                                <div className="col-6">
                                    <label className="form-label fw-semibold">Mese *</label>
                                    <select
                                        className="form-select"
                                        value={formData.month}
                                        onChange={(e) =>
                                            setFormData({ ...formData, month: e.target.value })
                                        }
                                        required
                                    >
                                        <option value="">Seleziona</option>
                                        {months.map((month) => (
                                            <option key={month.value} value={month.value}>
                                                {month.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            <div className="mb-4">
                                <div className="form-check form-switch">
                                    <input
                                        className="form-check-input"
                                        type="checkbox"
                                        id="recurringCheck"
                                        checked={formData.recurring}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                recurring: e.target.checked
                                            })
                                        }
                                    />
                                    <label
                                        className="form-check-label fw-semibold"
                                        htmlFor="recurringCheck"
                                    >
                                        Festività Ricorrente
                                    </label>
                                </div>
                                <small className="text-muted ms-4">
                                    {formData.recurring
                                        ? "Questa festività si ripeterà ogni anno"
                                        : "Questa festività sarà valida solo per l'anno selezionato"}
                                </small>
                            </div>

                            {!formData.recurring && (
                                <div className="mb-3">
                                    <label className="form-label fw-semibold">Anno *</label>
                                    <select
                                        className="form-select"
                                        value={formData.year}
                                        onChange={(e) =>
                                            setFormData({ ...formData, year: e.target.value })
                                        }
                                        required
                                    >
                                        {years.map((year) => (
                                            <option key={year} value={year}>
                                                {year}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            )}

                            {formData.day && formData.month && (
                                <div className="alert alert-info d-flex align-items-start">
                                    <Calendar size={20} className="me-2 mt-1 flex-shrink-0" />
                                    <div>
                                        <strong className="d-block mb-1">Riepilogo:</strong>
                                        <span>
                                            {formData.name || "Festività"} -{" "}
                                            {formData.day}/{formData.month}
                                            {!formData.recurring && `/${formData.year}`}
                                        </span>
                                        <div className="small text-muted mt-1">
                                            {formData.recurring
                                                ? "Si ripeterà ogni anno"
                                                : `Valida solo per il ${formData.year}`}
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>

                        <div className="modal-footer border-top">
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => {
                                    setShowModal(false);
                                    resetForm();
                                }}
                            >
                                Annulla
                            </button>
                            <button type="submit" className="btn btn-primary">
                                <Plus size={16} className="me-2" />
                                Aggiungi Festività
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default HolidayModal;