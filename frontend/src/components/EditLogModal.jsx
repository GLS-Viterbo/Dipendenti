import React, { useState } from 'react';
import { Edit2, Trash2, X, AlertTriangle } from 'lucide-react';

const EditLogModal = ({ log, onClose, onSave }) => {
  const [formData, setFormData] = useState({
    timestamp: log?.timestamp || '',
    type: log?.type || 'IN'
  });

  const handleSubmit = () => {
    onSave({
      ...log,
      timestamp: formData.timestamp,
      type: formData.type,
      modified: true,
      modifiedAt: new Date().toISOString()
    });
  };

  // Formatta la data per l'input datetime-local
  const formatDateForInput = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  return (
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          <div className="modal-header">
            <h5 className="modal-title">
              <Edit2 size={20} className="me-2" />
              Modifica Lettura Badge
            </h5>
            <button type="button" className="btn-close" onClick={onClose}></button>
          </div>
          
          <div className="modal-body">
            <div className="alert alert-warning d-flex align-items-start gap-2">
              <AlertTriangle size={20} className="mt-1" />
              <div>
                <strong>Attenzione!</strong> Stai modificando manualmente una lettura badge.
                Questa operazione verrà registrata nel sistema.
              </div>
            </div>

            <div className="mb-3">
              <label className="form-label fw-semibold">Dipendente</label>
              <input 
                type="text" 
                className="form-control" 
                value={`${log?.employeeName} ${log?.employeeSurname}`}
                disabled 
              />
            </div>

            <div className="mb-3">
              <label className="form-label fw-semibold">Badge UID</label>
              <input 
                type="text" 
                className="form-control" 
                value={log?.cardUid}
                disabled 
              />
            </div>

            <div className="mb-3">
              <label className="form-label fw-semibold">Data e Ora *</label>
              <input 
                type="datetime-local" 
                className="form-control" 
                value={formatDateForInput(formData.timestamp)}
                onChange={(e) => setFormData({...formData, timestamp: new Date(e.target.value).toISOString()})}
                required
              />
              <small className="text-muted">Originale: {new Date(log?.timestamp).toLocaleString('it-IT')}</small>
            </div>

            <div className="mb-3">
              <label className="form-label fw-semibold">Tipo Accesso *</label>
              <select 
                className="form-select" 
                value={formData.type}
                onChange={(e) => setFormData({...formData, type: e.target.value})}
                required
              >
                <option value="IN">Entrata (IN)</option>
                <option value="OUT">Uscita (OUT)</option>
              </select>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Annulla
            </button>
            <button type="button" className="btn btn-primary" onClick={handleSubmit}>
              <Edit2 size={16} className="me-1" />
              Salva Modifiche
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditLogModal;