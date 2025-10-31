import React, { useState, useMemo, useEffect } from 'react';
import { Search, Download, Filter, X, Edit2, Trash2, AlertTriangle, Clock, Calendar } from 'lucide-react';
import { getTodayDate, formatTime } from '../utils/utils';
import { deleteLog, getAllDetailedLogs, getDetailedLogs } from '../api/access';
import DeleteLogModal from '../components/DeleteLogModal'
import EditLogModal from '../components/EditLogModal'
import AddLogModal from '../components/AddLogModal';
import { updateAccessLog, addManualLog } from '../api/access';
import { toast } from "react-toastify";

const AccessLogsPage = () => {

  // States
  const [selectedDate, setSelectedDate] = useState(getTodayDate);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [showModified, setShowModified] = useState('ALL');
  const [showDeleted, setShowDeleted] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [showFilters, setShowFilters] = useState(false);
  const [logs, setLogs] = useState([])
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [selectedLog, setSelectedLog] = useState(null);

  useEffect(() => {
    async function loadBadgeReads() {
      if (!selectedDate) return;
      const logResponse = await getAllDetailedLogs(selectedDate)
      setLogs(logResponse)
    }
    loadBadgeReads()
  }, [selectedDate])

  // Filtered data
  const filteredLogs = useMemo(() => {
    return logs.filter(log => {
      // Search filter
      const searchMatch = searchTerm === '' ||
        log.employeeName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        log.employeeSurname.toLowerCase().includes(searchTerm.toLowerCase()) ||
        log.cardUid.toLowerCase().includes(searchTerm.toLowerCase()) ||
        `${log.employeeName} ${log.employeeSurname}`.toLowerCase().includes(searchTerm.toLowerCase());

      // Type filter
      const typeMatch = filterType === 'ALL' || log.type === filterType;

      // Modified filter
      const modifiedMatch = showModified === 'ALL' ||
        (showModified === 'MODIFIED' && log.modified) ||
        (showModified === 'NOT_MODIFIED' && !log.modified);

      // Deleted filter
      const deletedMatch = showDeleted || !log.deleted;

      return searchMatch && typeMatch && modifiedMatch && deletedMatch;
    });
  }, [logs, searchTerm, filterType, showModified, showDeleted]);

  // Pagination
  const totalPages = Math.ceil(filteredLogs.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentLogs = filteredLogs.slice(startIndex, endIndex);

  // Stats
  const stats = useMemo(() => {
    const total = filteredLogs.length;
    const inCount = filteredLogs.filter(l => l.type === 'IN').length;
    const outCount = filteredLogs.filter(l => l.type === 'OUT').length;
    const modifiedCount = filteredLogs.filter(l => l.modified).length;
    return { total, inCount, outCount, modifiedCount };
  }, [filteredLogs]);



  const handleExport = () => {
    // Placeholder per export Excel
    console.log('Esportazione Excel...');
  };

  const resetFilters = () => {
    setSearchTerm('');
    setFilterType('ALL');
    setShowModified('ALL');
    setShowDeleted(false);
  };

  const activeFiltersCount = [
    filterType !== 'ALL',
    showModified !== 'ALL',
    showDeleted,
    searchTerm !== ''
  ].filter(Boolean).length;

  const handleEdit = (log) => {
    setSelectedLog(log);
    setShowEditModal(true);
  };

  const handleDelete = (log) => {
    setSelectedLog(log);
    setShowDeleteModal(true);
  };

  const handleSaveEdit = async (updatedLog) => {
    try {
      await updateAccessLog(updatedLog.id, updatedLog);

      // Ricarica i dati
      const logResponse = await getAllDetailedLogs(selectedDate);
      setLogs(logResponse);

      setShowEditModal(false);
      setSelectedLog(null);
    } catch (error) {
      toast.error('Errore durante la modifica');
      console.error(error)
    }
  };

  const handleSaveManualLog = async (manualLog) => {
    try {
      await addManualLog(manualLog);
      toast.success("Lettura aggiunta con successo")

      // Ricarica i dati
      const logResponse = await getAllDetailedLogs(selectedDate);
      setLogs(logResponse);

      setShowEditModal(false);
      setSelectedLog(null);
    } catch (error) {
      toast.error("Errore durante l'aggiunta della lettura manuale");
      console.error(error)
    }
  };

  const handleConfirmDelete = async (logId) => {
    try {
      await deleteLog(logId);

      // Ricarica i dati
      const logResponse = await getAllDetailedLogs(selectedDate);
      setLogs(logResponse);

      setShowDeleteModal(false);
      setSelectedLog(null);
    } catch (error) {
      toast.error("Errore durante l'eliminazione");
      console.error(error)
    }
  };

  return (
    <div>
      {/* Stats Cards */}
      <div className="row g-3 mb-4">
        <div className="col-md-3">
          <div className="card border-0 shadow-sm">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start">
                <div>
                  <p className="text-muted mb-1 small">Totale Letture</p>
                  <h4 className="fw-bold mb-0">{stats.total}</h4>
                </div>
                <div className="bg-primary bg-opacity-10 rounded p-2">
                  <Clock size={20} className="text-primary" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="col-md-3">
          <div className="card border-0 shadow-sm">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start">
                <div>
                  <p className="text-muted mb-1 small">Entrate (IN)</p>
                  <h4 className="fw-bold mb-0 text-success">{stats.inCount}</h4>
                </div>
                <div className="bg-success bg-opacity-10 rounded p-2">
                  <Clock size={20} className="text-success" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="col-md-3">
          <div className="card border-0 shadow-sm">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start">
                <div>
                  <p className="text-muted mb-1 small">Uscite (OUT)</p>
                  <h4 className="fw-bold mb-0 text-danger">{stats.outCount}</h4>
                </div>
                <div className="bg-danger bg-opacity-10 rounded p-2">
                  <Clock size={20} className="text-danger" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="col-md-3">
          <div className="card border-0 shadow-sm">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start">
                <div>
                  <p className="text-muted mb-1 small">Modificate</p>
                  <h4 className="fw-bold mb-0 text-warning">{stats.modifiedCount}</h4>
                </div>
                <div className="bg-warning bg-opacity-10 rounded p-2">
                  <AlertTriangle size={20} className="text-warning" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content Card */}
      <div className="card border-0 shadow-sm">
        {/* Header */}
        <div className="card-header bg-white border-bottom">
          <div className="row align-items-center g-3">
            {/* Date Picker */}
            <div className="col-md-3">
              <label className="form-label small text-muted mb-1">Data</label>
              <div className="input-group">
                <span className="input-group-text bg-white">
                  <Calendar size={18} />
                </span>
                <input
                  type="date"
                  className="form-control"
                  value={selectedDate}
                  onChange={(e) => {
                    setSelectedDate(e.target.value)
                  }}
                />
              </div>
            </div>

            {/* Search */}
            <div className="col-md-5">
              <label className="form-label small text-muted mb-1">Ricerca</label>
              <div className="input-group">
                <span className="input-group-text bg-white">
                  <Search size={18} />
                </span>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Cerca per nome, cognome o UID badge..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
                {searchTerm && (
                  <button
                    className="btn btn-outline-secondary"
                    onClick={() => setSearchTerm('')}
                  >
                    <X size={18} />
                  </button>
                )}
              </div>
            </div>

            {/* Actions */}
            <div className="col-md-4">
              <label className="form-label small text-muted mb-1 d-block">&nbsp;</label>
              <div className="d-flex gap-2">
                <button
                  className={`btn ${showFilters ? 'btn-primary' : 'btn-outline-primary'} position-relative`}
                  onClick={() => setShowFilters(!showFilters)}
                >
                  <Filter size={18} className="me-1" />
                  Filtri
                  {activeFiltersCount > 0 && (
                    <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
                      {activeFiltersCount}
                    </span>
                  )}
                </button>
                <button
                  onClick={() => setShowAddModal(true)}
                  className="btn btn-primary flex-grow-1"
                >
                  <span>Aggiungi</span>
                </button>
                <button
                  className="btn btn-success flex-grow-1"
                  onClick={handleExport}
                >
                  <Download size={18} className="me-1" />
                  Esporta Excel
                </button>
              </div>
            </div>
          </div>

          {/* Filters Panel */}
          {showFilters && (
            <div className="mt-3 p-3 bg-light rounded">
              <div className="row g-3 align-items-end">
                <div className="col-md-3">
                  <label className="form-label small fw-semibold">Tipo Accesso</label>
                  <select
                    className="form-select"
                    value={filterType}
                    onChange={(e) => setFilterType(e.target.value)}
                  >
                    <option value="ALL">Tutti</option>
                    <option value="IN">Solo Entrate</option>
                    <option value="OUT">Solo Uscite</option>
                  </select>
                </div>

                <div className="col-md-3">
                  <label className="form-label small fw-semibold">Stato Modifica</label>
                  <select
                    className="form-select"
                    value={showModified}
                    onChange={(e) => setShowModified(e.target.value)}
                  >
                    <option value="ALL">Tutte</option>
                    <option value="MODIFIED">Solo Modificate</option>
                    <option value="NOT_MODIFIED">Solo Originali</option>
                  </select>
                </div>

                <div className="col-md-3">
                  <div className="form-check">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="showDeleted"
                      checked={showDeleted}
                      onChange={(e) => setShowDeleted(e.target.checked)}
                    />
                    <label className="form-check-label" htmlFor="showDeleted">
                      Mostra letture eliminate
                    </label>
                  </div>
                </div>

                <div className="col-md-3">
                  <button
                    className="btn btn-outline-secondary w-100"
                    onClick={resetFilters}
                  >
                    <X size={18} className="me-1" />
                    Reset Filtri
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Table */}
        <div className="card-body p-0">
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead className="table-light">
                <tr>
                  <th className="px-4 py-3">Orario</th>
                  <th className="py-3">Dipendente</th>
                  <th className="py-3">Badge UID</th>
                  <th className="py-3 text-center">Tipo</th>
                  <th className="py-3 text-center">Stato</th>
                  <th className="py-3 text-end px-4">Azioni</th>
                </tr>
              </thead>
              <tbody>
                {currentLogs.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="text-center py-5 text-muted">
                      <Clock size={48} className="mb-3 opacity-25" />
                      <p className="mb-0">Nessuna lettura trovata per i filtri selezionati</p>
                    </td>
                  </tr>
                ) : (
                  currentLogs.map((log) => (
                    <tr
                      key={log.id}
                      className={log.deleted ? 'table-secondary text-decoration-line-through' : ''}
                    >
                      <td className="px-4 py-3">
                        <span className="fw-semibold">{formatTime(log.timestamp)}</span>
                      </td>
                      <td className="py-3">
                        <div>
                          <div className="fw-semibold">{log.employeeName} {log.employeeSurname}</div>
                          <small className="text-muted">ID: {log.employeeId}</small>
                        </div>
                      </td>
                      <td className="py-3">
                        <code className="bg-light px-2 py-1 rounded">{log.cardUid}</code>
                      </td>
                      <td className="py-3 text-center">
                        <span className={`badge ${log.type === 'IN' ? 'bg-success' : 'bg-danger'}`}>
                          {log.type}
                        </span>
                      </td>
                      <td className="py-3 text-center">
                        <div className="d-flex gap-1 justify-content-center">
                          {log.modified && (
                            <span
                              className="badge bg-warning text-dark"
                              title="Lettura modificata manualmente"
                            >
                              <Edit2 size={12} className="me-1" />
                              Modificata
                            </span>
                          )}
                          {log.deleted && (
                            <span
                              className="badge bg-secondary"
                              title="Lettura eliminata"
                            >
                              <Trash2 size={12} className="me-1" />
                              Eliminata
                            </span>
                          )}
                          {!log.modified && !log.deleted && (
                            <span className="text-muted small">-</span>
                          )}
                        </div>
                      </td>
                      <td className="py-3 text-end px-4">
                        <div className="btn-group btn-group-sm">
                          <button
                            className="btn btn-outline-primary"
                            title="Modifica"
                            onClick={() => handleEdit(log)}
                          >
                            <Edit2 size={14} />
                          </button>
                          <button
                            className="btn btn-outline-danger"
                            title="Elimina"
                            onClick={() => handleDelete(log)}
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Footer with Pagination */}
        <div className="card-footer bg-white border-top">
          <div className="row align-items-center">
            <div className="col-md-4">
              <div className="d-flex align-items-center gap-2">
                <label className="small text-muted mb-0">Mostra:</label>
                <select
                  className="form-select form-select-sm w-auto"
                  value={itemsPerPage}
                  onChange={(e) => {
                    setItemsPerPage(Number(e.target.value));
                    setCurrentPage(1);
                  }}
                >
                  <option value="10">10</option>
                  <option value="25">25</option>
                  <option value="50">50</option>
                  <option value="100">100</option>
                </select>
                <span className="small text-muted">
                  Risultati {startIndex + 1}-{Math.min(endIndex, filteredLogs.length)} di {filteredLogs.length}
                </span>
              </div>
            </div>

            <div className="col-md-8">
              <nav>
                <ul className="pagination pagination-sm justify-content-end mb-0">
                  <li className={`page-item ${currentPage === 1 ? 'disabled' : ''}`}>
                    <button
                      className="page-link"
                      onClick={() => setCurrentPage(1)}
                      disabled={currentPage === 1}
                    >
                      Prima
                    </button>
                  </li>
                  <li className={`page-item ${currentPage === 1 ? 'disabled' : ''}`}>
                    <button
                      className="page-link"
                      onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                      disabled={currentPage === 1}
                    >
                      Precedente
                    </button>
                  </li>

                  {[...Array(Math.min(5, totalPages))].map((_, idx) => {
                    let pageNumber;
                    if (totalPages <= 5) {
                      pageNumber = idx + 1;
                    } else if (currentPage <= 3) {
                      pageNumber = idx + 1;
                    } else if (currentPage >= totalPages - 2) {
                      pageNumber = totalPages - 4 + idx;
                    } else {
                      pageNumber = currentPage - 2 + idx;
                    }

                    return (
                      <li
                        key={pageNumber}
                        className={`page-item ${currentPage === pageNumber ? 'active' : ''}`}
                      >
                        <button
                          className="page-link"
                          onClick={() => setCurrentPage(pageNumber)}
                        >
                          {pageNumber}
                        </button>
                      </li>
                    );
                  })}

                  <li className={`page-item ${currentPage === totalPages ? 'disabled' : ''}`}>
                    <button
                      className="page-link"
                      onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                      disabled={currentPage === totalPages}
                    >
                      Successiva
                    </button>
                  </li>
                  <li className={`page-item ${currentPage === totalPages ? 'disabled' : ''}`}>
                    <button
                      className="page-link"
                      onClick={() => setCurrentPage(totalPages)}
                      disabled={currentPage === totalPages}
                    >
                      Ultima
                    </button>
                  </li>
                </ul>
              </nav>
            </div>
          </div>
        </div>
      </div>
      {showEditModal && (
        <EditLogModal
          log={selectedLog}
          onClose={() => {
            setShowEditModal(false);
            setSelectedLog(null);
          }}
          onSave={handleSaveEdit}
        />
      )}

      {showDeleteModal && (
        <DeleteLogModal
          log={selectedLog}
          onClose={() => {
            setShowDeleteModal(false);
            setSelectedLog(null);
          }}
          onConfirm={handleConfirmDelete}
        />
      )}

      {showAddModal && (
        <AddLogModal
          onClose={() => {
            setShowAddModal(false);
          }}
          onSave={handleSaveManualLog}
          selectedDate={selectedDate}
        />
      )}
    </div>
  );
};

export default AccessLogsPage;