import {Trash2, AlertTriangle } from 'lucide-react';

const DeleteLogModal = ({ log, onClose, onConfirm }) => {
  return (
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          <div className="modal-header bg-danger text-white">
            <h5 className="modal-title">
              <Trash2 size={20} className="me-2" />
              Conferma Eliminazione
            </h5>
            <button type="button" className="btn-close btn-close-white" onClick={onClose}></button>
          </div>
          
          <div className="modal-body">
            <div className="alert alert-danger d-flex align-items-start gap-2">
              <AlertTriangle size={20} className="mt-1" />
              <div>
                <strong>Attenzione!</strong> Stai per eliminare definitivamente questa lettura badge.
              </div>
            </div>

            <div className="card bg-light">
              <div className="card-body">
                <h6 className="card-title mb-3">Dettagli Lettura:</h6>
                <dl className="row mb-0">
                  <dt className="col-sm-4">Dipendente:</dt>
                  <dd className="col-sm-8">{log?.employeeName} {log?.employeeSurname}</dd>
                  
                  <dt className="col-sm-4">Badge UID:</dt>
                  <dd className="col-sm-8"><code>{log?.cardUid}</code></dd>
                  
                  <dt className="col-sm-4">Data e Ora:</dt>
                  <dd className="col-sm-8">{new Date(log?.timestamp).toLocaleString('it-IT')}</dd>
                  
                  <dt className="col-sm-4">Tipo:</dt>
                  <dd className="col-sm-8">
                    <span className={`badge ${log?.type === 'IN' ? 'bg-success' : 'bg-danger'}`}>
                      {log?.type}
                    </span>
                  </dd>
                </dl>
              </div>
            </div>

            <p className="mt-3 mb-0 text-muted">
              Questa operazione non può essere annullata. Sei sicuro di voler procedere?
            </p>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Annulla
            </button>
            <button type="button" className="btn btn-danger" onClick={() => onConfirm(log.id)}>
              <Trash2 size={16} className="me-1" />
              Elimina Definitivamente
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DeleteLogModal;