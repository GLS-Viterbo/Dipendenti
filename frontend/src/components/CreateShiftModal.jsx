import React, { useState } from 'react';
import {
  Plus,
  Calendar as CalendarIcon,
} from 'lucide-react';
import { createShift } from '../api/shifts';
import { toast } from "react-toastify";

const CreateShiftModal = ({ show, onClose, shifts, setShifts }) => {
  const [formData, setFormData] = useState({
    name: '',
    startTime: '',
    endTime: ''
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const newShift = {
        ...formData,
        active: true
      };
      await createShift(newShift);
      toast.success("Turno creato con successo!");
      setFormData({ name: '', startTime: '', endTime: '' });
      onClose();
    } catch (error) {
      toast.error("Errore nella creazione del turno");
      console.error(error)
    }
  };

  if (!show) return null;

  return (
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          <div className="modal-header">
            <h5 className="modal-title">
              <Plus size={20} className="me-2" />
              Crea Nuovo Turno
            </h5>
            <button type="button" className="btn-close" onClick={onClose} />
          </div>
          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              <div className="mb-3">
                <label className="form-label fw-semibold">
                  Nome Turno <span className="text-danger">*</span>
                </label>
                <input
                  type="text"
                  className="form-control"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Es. Turno Mattina"
                  required
                />
              </div>
              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label fw-semibold">
                    Ora Inizio <span className="text-danger">*</span>
                  </label>
                  <input
                    type="time"
                    className="form-control"
                    value={formData.startTime}
                    onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                    required
                  />
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label fw-semibold">
                    Ora Fine <span className="text-danger">*</span>
                  </label>
                  <input
                    type="time"
                    className="form-control"
                    value={formData.endTime}
                    onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                    required
                  />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={onClose}>
                Annulla
              </button>
              <button type="submit" className="btn btn-primary">
                Crea Turno
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
export default CreateShiftModal;