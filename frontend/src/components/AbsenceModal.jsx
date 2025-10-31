import React, { useEffect, useState } from "react";
import moment from "moment";
import { Plus, CalendarIcon, Clock } from "lucide-react";
import { toast } from "react-toastify";
import { getAbsenceNeeded, getDetailedEmployeeBalance } from "../api/absence";

const AbsenceModal = ({
    showModal,
    setShowModal,
    handleSubmit,
    ABSENCE_TYPES,
    employees,
    formData,
    setFormData,
    selectedEmployee
}) => {


    useEffect(() => {
        if (selectedEmployee != "" && selectedEmployee != null) {
            setFormData(prevFormData => ({
                ...prevFormData,
                employeeId: selectedEmployee,
            }));
        }
    }, [selectedEmployee]);

    const [detailedBalance, setDetailedBalance] = useState({})
    const [absenceNeeded, setAbsenceNeeded] = useState(0)


    useEffect(() => {
        async function fetchDetailedBalance() {
            try {
                if (formData.employeeId) {
                    const balance = await getDetailedEmployeeBalance(formData.employeeId);
                    setDetailedBalance({
                        vacationAvailable: balance.vacationAvailable,
                        rolAvailable: balance.rolAvailable,
                        vacationToAccrue: balance.vacationToMature,
                        rolToAccrue: balance.rolToMature
                    });
                }
            } catch (error) {
                toast.error("Impossibile ottenere il numero di ferie necessario");
                console.error(error);
            }
        }
        fetchDetailedBalance();
    }, [formData.employeeId]);

    useEffect(() => {
        async function fetchAmountNeeded() {
            try {
                if (formData.startDate && formData.endDate && formData.employeeId) {
                    const response = await getAbsenceNeeded(formData.employeeId, formData.startDate, formData.endDate)
                    setAbsenceNeeded(response.needed)
                    setFormData(prev => ({ ...prev, hoursCount: response.needed }))
                }
            } catch (error) {
                console.error("Impossibile ottenere il numero di ferie necessario")
            }
        }
        fetchAmountNeeded()
    }, [formData.startDate, formData.endDate, formData.employeeId])

    const resetForm = () => {
        setFormData({
            employeeId: "",
            type: "VACATION",
            startDate: "",
            endDate: "",
            note: "",
            hoursCount: "",
        });
        setDetailedBalance({});
        setAbsenceNeeded(0);
    };

    if (!showModal) return null;

    return (
        <div
            className="modal show d-block"
            style={{ backgroundColor: "rgba(0,0,0,0.5)" }}
        >
            <div className="modal-dialog modal-dialog-centered modal-lg">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">
                            <Plus size={20} className="me-2" />
                            Nuova Assenza
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

                    <form
                        onSubmit={(e) => {
                            e.preventDefault();
                            handleSubmit(formData);
                            resetForm();
                            setShowModal(false);
                        }}
                    >
                        <div className="modal-body">
                            <div className="mb-3">
                                <label className="form-label">Dipendente *</label>
                                <select
                                    className="form-select"
                                    value={formData.employeeId}
                                    onChange={(e) =>
                                        setFormData({ ...formData, employeeId: e.target.value })
                                    }
                                    required
                                >
                                    <option value="">Seleziona dipendente</option>
                                    {employees.map((emp) => (
                                        <option key={emp.id} value={emp.id}>
                                            {emp.name} {emp.surname}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {formData.employeeId && (
                                <div className="row g-3 mb-4">
                                    <div className="col-md-6">
                                        <div className="card border-0 bg-light">
                                            <div className="card-body">
                                                <div className="d-flex justify-content-between align-items-start mb-2">
                                                    <div>
                                                        <small className="text-muted d-block mb-1">
                                                            Ferie Disponibili
                                                        </small>
                                                        <h4 className="mb-0 text-primary fw-bold">
                                                            {detailedBalance.vacationAvailable || 0}{" "}
                                                            ore
                                                        </h4>
                                                    </div>
                                                    <CalendarIcon
                                                        size={24}
                                                        className="text-primary opacity-50"
                                                    />
                                                </div>
                                                <small className="text-muted">
                                                    Da maturare:{" "}
                                                    {detailedBalance.vacationToAccrue || 0}{" "}
                                                    ore
                                                </small>
                                            </div>
                                        </div>
                                    </div>
                                    <div className="col-md-6">
                                        <div className="card border-0 bg-light">
                                            <div className="card-body">
                                                <div className="d-flex justify-content-between align-items-start mb-2">
                                                    <div>
                                                        <small className="text-muted d-block mb-1">
                                                            Permessi (ROL) Disponibili
                                                        </small>
                                                        <h4 className="mb-0 text-success fw-bold">
                                                            {detailedBalance.rolAvailable ||
                                                                0}{" "}
                                                            ore
                                                        </h4>
                                                    </div>
                                                    <Clock size={24} className="text-success opacity-50" />
                                                </div>
                                                <small className="text-muted">
                                                    Da maturare:{" "}
                                                    {detailedBalance.rolToAccrue ||
                                                        0}{" "}
                                                    ore
                                                </small>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            )}

                            <div className="mb-3">
                                <label className="form-label">Tipo Assenza *</label>
                                <select
                                    className="form-select"
                                    value={formData.type}
                                    onChange={(e) =>
                                        setFormData({ ...formData, type: e.target.value })
                                    }
                                    required
                                >
                                    {Object.entries(ABSENCE_TYPES).map(([key, value]) => (
                                        <option key={key} value={key}>
                                            {value.label}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="row mb-3">
                                <div className="col-md-4">
                                    <label className="form-label">Data Inizio *</label>
                                    <input
                                        type="date"
                                        className="form-control"
                                        value={formData.startDate}
                                        onChange={(e) =>
                                            setFormData({ ...formData, startDate: e.target.value })
                                        }
                                        required
                                    />
                                </div>
                                <div className="col-md-4">
                                    <label className="form-label">Data Fine *</label>
                                    <input
                                        type="date"
                                        className="form-control"
                                        value={formData.endDate}
                                        onChange={(e) =>
                                            setFormData({ ...formData, endDate: e.target.value })
                                        }
                                        required
                                        min={formData.startDate}
                                    />
                                </div>
                                <div className="col-md-4">
                                    <label className="form-label">Ore da scalare *</label>
                                    <input
                                        type="number"
                                        className="form-control"
                                        value={formData.hoursCount}
                                        onChange={(e) =>
                                            setFormData({
                                                ...formData,
                                                hoursCount: parseFloat(e.target.value) || 0
                                            })
                                        }
                                        onWheel={(e) => e.preventDefault()}
                                        required
                                        min={0}
                                    />
                                </div>
                            </div>


                            {formData.startDate && formData.endDate && (
                                <div className="alert alert-info mb-3">
                                    <small>
                                        Secondo i turni impostati l'assenza corrisponde a: {absenceNeeded} ore
                                    </small>
                                </div>
                            )}

                            <div className="mb-3">
                                <label className="form-label">Note</label>
                                <textarea
                                    className="form-control"
                                    rows="3"
                                    value={formData.note}
                                    onChange={(e) =>
                                        setFormData({ ...formData, note: e.target.value })
                                    }
                                    placeholder="Note aggiuntive..."
                                />
                            </div>
                        </div>

                        <div className="modal-footer">
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
                                Crea Assenza
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default AbsenceModal;
