import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { Edit2, Trash2, X, AlertTriangle } from 'lucide-react';
import { getAllEmployees, getEmployeesWithoutCard } from '../api/employees';

const AddLogModal = ({ onClose, onSave, selectedDate }) => {
  const [employees, setEmployees] = useState([]);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState("")

  const [selectedTimestamp, setSelectedTimestamp] = useState(() => {
    if (selectedDate) {
      const now = new Date();
      const hours = now.getHours().toString().padStart(2, '0');
      const minutes = now.getMinutes().toString().padStart(2, '0');
      return new Date(`${selectedDate}T${hours}:${minutes}:00`).toISOString();
    }
    return new Date().toISOString();
  });

  const [selectedType, setSelectedType] = useState("IN")

  useEffect(() => {
    async function getEmployeesWithCard() {
      try {
        const allEmployees = await getAllEmployees();
        const employeesWithoutCard = await getEmployeesWithoutCard();
        const withoutCardIds = new Set(employeesWithoutCard.map(e => e.id));
        const filteredEmployees = allEmployees.filter(e => !withoutCardIds.has(e.id));
        setEmployees(filteredEmployees);
      } catch (error) {
        toast.error("Errore nel recupero dei dipendenti");
        console.error(error)
      }

    }
    getEmployeesWithCard();
  }, [])

  const handleSubmit = () => {
    if (selectedEmployeeId === "") {
      toast.error("Seleziona un dipendente!")
      return;
    }
    onSave({
      employeeId: selectedEmployeeId,
      timestamp: selectedTimestamp,
      type: selectedType
    })
    onClose()
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

            <div className="mb-3">
              <label className="form-label fw-semibold">Dipendente</label>
              <select
                className="form-control"
                value={selectedEmployeeId}
                onChange={(e) => setSelectedEmployeeId(e.target.value)}
                required
              >
                <option value="">Seleziona un dipendente</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.name} {emp.surname}
                  </option>
                ))}
              </select>
              <small className="text-muted">Solo dipendenti con un badge</small>
            </div>

            <div className="mb-3">
              <label className="form-label fw-semibold">Data e Ora *</label>
              <input
                type="datetime-local"
                className="form-control"
                value={formatDateForInput(selectedTimestamp)}
                onChange={(e) => setSelectedTimestamp(new Date(e.target.value).toISOString())}
                required
              />
            </div>

            <div className="mb-3">
              <label className="form-label fw-semibold">Tipo Accesso *</label>
              <select
                className="form-select"
                value={selectedType}
                defaultValue={"IN"}
                onChange={(e) => setSelectedType(e.target.value)}
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

export default AddLogModal;