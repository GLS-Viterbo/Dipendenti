import React, { useState, useEffect } from 'react';
import { 
  Settings, Building2, Users, Shield, Plus, Edit, Trash2, 
  X, Save, Search, Filter, CheckCircle, XCircle 
} from 'lucide-react';
import { addCompany, addUser, assignRole, delComapny, deleteUser, getActiveCompanies, getActiveUsers, getAllRoles, getUserRoles, revokeRole, updateCompany, updateUser } from '../api/auth';

const AdminPanel = ({ currentUser }) => {
  const [activeTab, setActiveTab] = useState('companies');
  const [companies, setCompanies] = useState([]);
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterActive, setFilterActive] = useState('all');
  const [showCompanyModal, setShowCompanyModal] = useState(false);
  const [showUserModal, setShowUserModal] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [selectedUserForRoles, setSelectedUserForRoles] = useState(null);
  const [userRoles, setUserRoles] = useState([]);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [companiesData, usersData, rolesData] = await Promise.all([
        getActiveCompanies(),
        getActiveUsers(),
        getAllRoles()
      ]);
      setCompanies(companiesData);
      setUsers(usersData);
      setRoles(rolesData);
    } catch (error) {
      console.error('Errore nel caricamento dei dati:', error);
    }
  };

  // Company Modal Component
  const CompanyModal = ({ isOpen, onClose, company, onSave }) => {
    const [formData, setFormData] = useState({
      name: company?.name || '',
      active: company?.active ?? true
    });

    const handleSubmit = async (e) => {
      e.preventDefault();
      try {
        if (company) {
          await updateCompany(company.id, formData);
        } else {
          await addCompany(formData);
        }
        await loadData();
        onClose();
      } catch (error) {
        console.error('Errore nel salvataggio:', error);
      }
    };

    if (!isOpen) return null;

    return (
      <>
        <div className="modal-backdrop fade show" style={{ zIndex: 1040 }} onClick={onClose} />
        <div className="modal fade show d-block" style={{ zIndex: 1050 }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg">
              <div className="modal-header bg-primary text-white">
                <h5 className="modal-title d-flex align-items-center gap-2">
                  <Building2 size={20} />
                  {company ? 'Modifica Azienda' : 'Nuova Azienda'}
                </h5>
                <button className="btn-close btn-close-white" onClick={onClose} />
              </div>
              <form onSubmit={handleSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label fw-semibold">
                      Nome Azienda <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      required
                    />
                  </div>
                  <div className="form-check">
                    <input
                      type="checkbox"
                      className="form-check-input"
                      id="companyActive"
                      checked={formData.active}
                      onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                    />
                    <label className="form-check-label" htmlFor="companyActive">
                      Azienda Attiva
                    </label>
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-outline-secondary" onClick={onClose}>
                    Annulla
                  </button>
                  <button type="submit" className="btn btn-primary">
                    <Save size={18} className="me-2" />
                    Salva
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </>
    );
  };

  // User Modal Component
  const UserModal = ({ isOpen, onClose, user, onSave }) => {
    const [formData, setFormData] = useState({
      username: user?.username || '',
      email: user?.email || '',
      password: '',
      companyId: user?.companyId || '',
      active: user?.active ?? true
    });

    const handleSubmit = async (e) => {
      e.preventDefault();
      try {
        if (user) {
          await updateUser(user.id, formData);
        } else {
          await addUser(formData);
        }
        await loadData();
        onClose();
      } catch (error) {
        console.error('Errore nel salvataggio:', error);
      }
    };

    if (!isOpen) return null;

    return (
      <>
        <div className="modal-backdrop fade show" style={{ zIndex: 1040 }} onClick={onClose} />
        <div className="modal fade show d-block" style={{ zIndex: 1050 }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg">
              <div className="modal-header bg-primary text-white">
                <h5 className="modal-title d-flex align-items-center gap-2">
                  <Users size={20} />
                  {user ? 'Modifica Utente' : 'Nuovo Utente'}
                </h5>
                <button className="btn-close btn-close-white" onClick={onClose} />
              </div>
              <form onSubmit={handleSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label fw-semibold">
                      Username <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      value={formData.username}
                      onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                      required
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label fw-semibold">
                      Email <span className="text-danger">*</span>
                    </label>
                    <input
                      type="email"
                      className="form-control"
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      required
                    />
                  </div>
                  {!user && (
                    <div className="mb-3">
                      <label className="form-label fw-semibold">
                        Password <span className="text-danger">*</span>
                      </label>
                      <input
                        type="password"
                        className="form-control"
                        value={formData.password}
                        onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                        required={!user}
                      />
                    </div>
                  )}
                  <div className="mb-3">
                    <label className="form-label fw-semibold">
                      Azienda <span className="text-danger">*</span>
                    </label>
                    <select
                      className="form-select"
                      value={formData.companyId}
                      onChange={(e) => setFormData({ ...formData, companyId: parseInt(e.target.value) })}
                      required
                    >
                      <option value="">Seleziona un'azienda</option>
                      {companies.map(company => (
                        <option key={company.id} value={company.id}>
                          {company.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-check">
                    <input
                      type="checkbox"
                      className="form-check-input"
                      id="userActive"
                      checked={formData.active}
                      onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                    />
                    <label className="form-check-label" htmlFor="userActive">
                      Utente Attivo
                    </label>
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-outline-secondary" onClick={onClose}>
                    Annulla
                  </button>
                  <button type="submit" className="btn btn-primary">
                    <Save size={18} className="me-2" />
                    Salva
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </>
    );
  };

  // Role Management Modal
  const RoleModal = ({ isOpen, onClose, user }) => {
    const [availableRoles, setAvailableRoles] = useState([]);
    const [assignedRoles, setAssignedRoles] = useState([]);

    useEffect(() => {
      if (user && isOpen) {
        loadUserRoles();
      }
    }, [user, isOpen]);

    const loadUserRoles = async () => {
      try {
        const userRolesData = await getUserRoles(user.id);
        console.log(userRolesData)
        setAssignedRoles(userRolesData);
        setAvailableRoles(roles.filter(r => !userRolesData.find(ur => ur.id === r.id)));
      } catch (error) {
        console.error('Errore nel caricamento dei ruoli:', error);
      }
    };

    const handleAssignRole = async (roleId) => {
      try {
        await assignRole(user.id, roleId);
        await loadUserRoles();
      } catch (error) {
        console.error('Errore nell\'assegnazione del ruolo:', error);
      }
    };

    const handleRemoveRole = async (roleId) => {
      try {
        await revokeRole(user.id, roleId);
        await loadUserRoles();
      } catch (error) {
        console.error('Errore nella rimozione del ruolo:', error);
      }
    };

    if (!isOpen || !user) return null;

    return (
      <>
        <div className="modal-backdrop fade show" style={{ zIndex: 1040 }} onClick={onClose} />
        <div className="modal fade show d-block" style={{ zIndex: 1050 }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg">
              <div className="modal-header bg-primary text-white">
                <h5 className="modal-title d-flex align-items-center gap-2">
                  <Shield size={20} />
                  Gestione Permessi - {user.username}
                </h5>
                <button className="btn-close btn-close-white" onClick={onClose} />
              </div>
              <div className="modal-body">
                <div className="mb-4">
                  <h6 className="fw-semibold mb-3">Ruoli Assegnati</h6>
                  {assignedRoles.length === 0 ? (
                    <p className="text-muted">Nessun ruolo assegnato</p>
                  ) : (
                    <div className="d-flex flex-wrap gap-2">
                      {assignedRoles.map(role => (
                        <span key={role.id} className="badge bg-primary d-flex align-items-center gap-2">
                          {role.name}
                          <button
                            className="btn-close btn-close-white"
                            style={{ fontSize: '0.6rem' }}
                            onClick={() => handleRemoveRole(role.id)}
                          />
                        </span>
                      ))}
                    </div>
                  )}
                </div>
                <div>
                  <h6 className="fw-semibold mb-3">Ruoli Disponibili</h6>
                  {availableRoles.length === 0 ? (
                    <p className="text-muted">Tutti i ruoli sono già assegnati</p>
                  ) : (
                    <div className="d-flex flex-wrap gap-2">
                      {availableRoles.map(role => (
                        <button
                          key={role.id}
                          className="btn btn-outline-primary btn-sm"
                          onClick={() => handleAssignRole(role.id)}
                        >
                          <Plus size={14} className="me-1" />
                          {role.name}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={onClose}>
                  Chiudi
                </button>
              </div>
            </div>
          </div>
        </div>
      </>
    );
  };

  const filteredCompanies = companies.filter(c => {
    const matchesSearch = c.name.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filterActive === 'all' || 
      (filterActive === 'active' && c.active) || 
      (filterActive === 'inactive' && !c.active);
    return matchesSearch && matchesFilter;
  });

  const filteredUsers = users.filter(u => {
    const matchesSearch = u.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      u.email.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filterActive === 'all' || 
      (filterActive === 'active' && u.active) || 
      (filterActive === 'inactive' && !u.active);
    return matchesSearch && matchesFilter;
  });

  return (
    <div className="container-fluid p-4">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h3 className="fw-bold d-flex align-items-center gap-2">
            <Settings size={28} className="text-primary" />
            Pannello di Amministrazione
          </h3>
          <p className="text-muted mb-0">Gestione aziende, utenti e permessi</p>
        </div>
      </div>

      {/* Tabs */}
      <ul className="nav nav-tabs mb-4">
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'companies' ? 'active' : ''}`}
            onClick={() => setActiveTab('companies')}
          >
            <Building2 size={18} className="me-2" />
            Aziende
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link ${activeTab === 'users' ? 'active' : ''}`}
            onClick={() => setActiveTab('users')}
          >
            <Users size={18} className="me-2" />
            Utenti
          </button>
        </li>
      </ul>

      {/* Filters and Search */}
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body">
          <div className="row g-3">
            <div className="col-md-6">
              <div className="input-group">
                <span className="input-group-text bg-light border-end-0">
                  <Search size={18} className="text-muted" />
                </span>
                <input
                  type="text"
                  className="form-control border-start-0"
                  placeholder="Cerca..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>
            <div className="col-md-3">
              <select
                className="form-select"
                value={filterActive}
                onChange={(e) => setFilterActive(e.target.value)}
              >
                <option value="all">Tutti</option>
                <option value="active">Attivi</option>
                <option value="inactive">Non Attivi</option>
              </select>
            </div>
            <div className="col-md-3">
              <button
                className="btn btn-primary w-100"
                onClick={() => activeTab === 'companies' ? setShowCompanyModal(true) : setShowUserModal(true)}
              >
                <Plus size={18} className="me-2" />
                {activeTab === 'companies' ? 'Nuova Azienda' : 'Nuovo Utente'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Companies Tab */}
      {activeTab === 'companies' && (
        <div className="card border-0 shadow-sm">
          <div className="card-body p-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead className="bg-light">
                  <tr>
                    <th>ID</th>
                    <th>Nome Azienda</th>
                    <th>Stato</th>
                    <th className="text-end">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredCompanies.map(company => (
                    <tr key={company.id}>
                      <td className="align-middle">{company.id}</td>
                      <td className="align-middle fw-semibold">{company.name}</td>
                      <td className="align-middle">
                        {company.active ? (
                          <span className="badge bg-success">
                            <CheckCircle size={14} className="me-1" />
                            Attiva
                          </span>
                        ) : (
                          <span className="badge bg-secondary">
                            <XCircle size={14} className="me-1" />
                            Non Attiva
                          </span>
                        )}
                      </td>
                      <td className="align-middle text-end">
                        <button
                          className="btn btn-sm btn-outline-primary me-2"
                          onClick={() => {
                            setEditingItem(company);
                            setShowCompanyModal(true);
                          }}
                        >
                          <Edit size={14} />
                        </button>
                        <button
                          className="btn btn-sm btn-outline-danger"
                          onClick={async () => {
                            if (confirm('Sei sicuro di voler eliminare questa azienda?')) {
                              await delComapny(company.id);
                              await loadData();
                            }
                          }}
                        >
                          <Trash2 size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Users Tab */}
      {activeTab === 'users' && (
        <div className="card border-0 shadow-sm">
          <div className="card-body p-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead className="bg-light">
                  <tr>
                    <th>ID</th>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Azienda</th>
                    <th>Stato</th>
                    <th className="text-end">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map(user => {
                    const company = companies.find(c => c.id === user.companyId);
                    return (
                      <tr key={user.id}>
                        <td className="align-middle">{user.id}</td>
                        <td className="align-middle fw-semibold">{user.username}</td>
                        <td className="align-middle">{user.email}</td>
                        <td className="align-middle">{company?.name || '-'}</td>
                        <td className="align-middle">
                          {user.active ? (
                            <span className="badge bg-success">
                              <CheckCircle size={14} className="me-1" />
                              Attivo
                            </span>
                          ) : (
                            <span className="badge bg-secondary">
                              <XCircle size={14} className="me-1" />
                              Non Attivo
                            </span>
                          )}
                        </td>
                        <td className="align-middle text-end">
                          <button
                            className="btn btn-sm btn-outline-primary me-2"
                            onClick={() => {
                              setSelectedUserForRoles(user);
                              setShowRoleModal(true);
                            }}
                            title="Gestisci Permessi"
                          >
                            <Shield size={14} />
                          </button>
                          <button
                            className="btn btn-sm btn-outline-primary me-2"
                            onClick={() => {
                              setEditingItem(user);
                              setShowUserModal(true);
                            }}
                          >
                            <Edit size={14} />
                          </button>
                          <button
                            className="btn btn-sm btn-outline-danger"
                            onClick={async () => {
                              if (confirm('Sei sicuro di voler eliminare questo utente?')) {
                                await deleteUser(user.id);
                                await loadData();
                              }
                            }}
                          >
                            <Trash2 size={14} />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Modals */}
      <CompanyModal
        isOpen={showCompanyModal}
        onClose={() => {
          setShowCompanyModal(false);
          setEditingItem(null);
        }}
        company={editingItem}
      />
      
      <UserModal
        isOpen={showUserModal}
        onClose={() => {
          setShowUserModal(false);
          setEditingItem(null);
        }}
        user={editingItem}
      />

      <RoleModal
        isOpen={showRoleModal}
        onClose={() => {
          setShowRoleModal(false);
          setSelectedUserForRoles(null);
        }}
        user={selectedUserForRoles}
      />
    </div>
  );
};

export default AdminPanel;