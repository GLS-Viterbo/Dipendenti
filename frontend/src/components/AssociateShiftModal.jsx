import React, { useState, useMemo, useEffect } from 'react';
import {
  Clock,
  AlertCircle,
  AlertTriangle,
  List,
} from 'lucide-react';
import ShiftAssociationTable from './ShiftAssociationTabel';
import { createShiftAssociation, deleteShiftAssociation } from '../api/shifts';
import { toast } from "react-toastify";

const AssociateShiftModal = ({ show, onClose, updateData, employee, shifts, associations, setAssociations }) => {
  const [selectedShift, setSelectedShift] = useState('');
  const [selectedDays, setSelectedDays] = useState([]);

  const weekDays = [
    { value: 1, label: 'Lunedì' },
    { value: 2, label: 'Martedì' },
    { value: 3, label: 'Mercoledì' },
    { value: 4, label: 'Giovedì' },
    { value: 5, label: 'Venerdì' },
    { value: 6, label: 'Sabato' },
    { value: 7, label: 'Domenica' }
  ];

  // Calcola le associazioni esistenti per il dipendente
  const employeeAssociations = useMemo(() => {
    if (!employee) return [];
    return associations
      .filter(a => a.employeeId === employee.id)
      .map(a => {
        const shift = shifts.find(s => s.id === a.shiftId);
        const day = weekDays.find(d => d.value === a.dayOfWeek);
        return {
          ...a,
          shiftName: shift?.name || 'N/A',
          shiftTime: shift ? `${shift.startTime.replace(/:\d{2}$/, '')} - ${shift.endTime.replace(/:\d{2}$/, '')}` : '',
          dayLabel: day?.label || 'N/A'
        };
      })
      .sort((a, b) => a.dayOfWeek - b.dayOfWeek);
  }, [employee, associations, shifts]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const newAssociations = selectedDays.map(day => ({
        employeeId: employee.id,
        shiftId: parseInt(selectedShift),
        dayOfWeek: day
      }));

      await Promise.all(
        newAssociations.map(assoc => createShiftAssociation(assoc))
      );

      setAssociations([...associations, ...newAssociations]);
      setSelectedShift('');
      setSelectedDays([]);
      updateData();
    } catch (err) {
      console.error('Errore durante l\'associazione:', err);
      if (err.status === 409) {
        toast.error('Impossibile associare il turno: il dipendente ha già un turno assegnato in uno o più giorni selezionati.');
        console.error(err)
      } else {
        toast.error('Si è verificato un errore durante l\'associazione del turno.');
        console.error(err)
      }
    }
  };

  async function handleDeleteTurn(associationId) {
    try {
      await deleteShiftAssociation(associationId)
      toast.success("Associazione turno eliminata con successo")
      updateData()
    } catch (error) {
    toast.error("Errore nella cancellazione dell'associazione");
}
  }

  const toggleDay = (dayValue) => {
    setSelectedDays(prev =>
      prev.includes(dayValue)
        ? prev.filter(d => d !== dayValue)
        : [...prev, dayValue]
    );
  };

  const handleClose = () => {
    setSelectedShift('');
    setSelectedDays([]);
    onClose();
  };



  if (!show || !employee) return null;

  return (
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content">
          <div className="modal-header">
            <h5 className="modal-title">
              <Clock size={20} className="me-2" />
              Associa Turno Ricorrente
            </h5>
            <button type="button" className="btn-close" onClick={handleClose} />
          </div>
          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              <div className="alert alert-info d-flex align-items-center gap-2 mb-4">
                <AlertCircle size={18} />
                <small>
                  Associazione per <strong>{employee.name} {employee.surname}</strong>
                </small>
              </div>

              {/* Turni già associati */}
              {employeeAssociations.length > 0 &&
                <ShiftAssociationTable
                  employeeAssociations={employeeAssociations}
                  handleDeleteTurn={handleDeleteTurn}
                />
              }

              <div className="mb-3">
                <label className="form-label fw-semibold">
                  Turno <span className="text-danger">*</span>
                </label>
                <select
                  className="form-select"
                  value={selectedShift}
                  onChange={(e) => {
                    setSelectedShift(e.target.value);
                  }}
                  required
                >
                  <option value="">Seleziona turno...</option>
                  {shifts.filter(s => s.active).map(shift => (
                    <option key={shift.id} value={shift.id}>
                      {shift.name} ({shift.startTime.replace(/:\d{2}$/, '')} - {shift.endTime.replace(/:\d{2}$/, '')})
                    </option>
                  ))}
                </select>
              </div>

              <div className="mb-3">
                <label className="form-label fw-semibold">
                  Giorni della Settimana <span className="text-danger">*</span>
                </label>
                <div className="d-flex flex-wrap gap-2">
                  {weekDays.map(day => {
                    const hasAssociation = employeeAssociations.some(a => a.dayOfWeek === day.value);
                    return (
                      <button
                        key={day.value}
                        type="button"
                        className={`btn ${selectedDays.includes(day.value) ? 'btn-primary' : 'btn-outline-primary'} ${hasAssociation ? 'position-relative' : ''}`}
                        onClick={() => toggleDay(day.value)}
                      >
                        {day.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={handleClose}>
                Annulla
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={!selectedShift || selectedDays.length === 0}
              >
                Associa Turno
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default AssociateShiftModal;