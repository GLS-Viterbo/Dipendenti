import { Menu, Users, UserPlus, Calendar, ClipboardList, CreditCard, Clock, BarChart3, Settings, LogOut, MessageCircleWarningIcon } from 'lucide-react';
import React, { useState, useEffect } from 'react';
import { getEmployeeCount } from '../api/employees';
import { getAtWrokCount, getAllAnomalies } from '../api/access';
import { getTodayApprovedCount, getFutureToApproveCount } from '../api/absence';
import { getAssignedCardCount, getNotDeletedCardCount } from '../api/card';
import EmployeeListPage from './EmployeeListPage';
import EmployeeDetailPage from './EmployeeDetailPage';
import CardManagementPage from './CardManagementPage';
import AccessLogsPage from './AccessLogsPage';
import AbsenceManagementPage from './AbsenceManagementPage';
import ContractManagementPage from './ContractManagementPage';
import AdminPanel from './AdminPanel';
import { formatDate } from '../utils/utils';
import { useAuth } from '../utils/AuthContext';
import { apiFetchFile } from '../api/client';
import ShiftCalendarPage from './ShiftCalendarPage'

const HomePage = ({ onLogout, user }) => {
  const { hasRole } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [activeSection, setActiveSection] = useState('dashboard');
  const [selectedEmployeeId, setSelectedEmployeeId] = useState(null);
  // Counts
  const [employeeCount, setEmployeeCount] = useState(0)
  const [atWorkCount, setAtWorkCount] = useState(0)
  const [todaysCount, setTodaysCount] = useState(0)
  const [futureCount, setFutureCount] = useState(0)
  const [assignedCards, setAssignedCards] = useState(0)
  const [cardCount, setCardCount] = useState(0)
  const [anomalies, setAnomalies] = useState([])


  useEffect(() => {
    async function fetchCounts() {
      try {
        const employeeResults = await getEmployeeCount();
        const atWorkResults = await getAtWrokCount();
        const todaysResult = await getTodayApprovedCount();
        const futureResult = await getFutureToApproveCount();
        const assignedResult = await getAssignedCardCount();
        const cardCountResult = await getNotDeletedCardCount();
        // Past 3 month anomalies
        const allAnomalies = await getAllAnomalies();

        setTodaysCount(todaysResult.count);
        setFutureCount(futureResult.count);
        setEmployeeCount(employeeResults.count);
        setAtWorkCount(atWorkResults.count);
        setAssignedCards(assignedResult.count);
        setCardCount(cardCountResult.count);
        setAnomalies(allAnomalies);
      } catch (error) {
        console.error('Errore nel recupero dei contatori:', error.message);
      }
    }

    fetchCounts();
  }, [activeSection]);

  const showEmployeeDetailPage = (id) => {
    setSelectedEmployeeId(id);
  };

  const backToList = () => {
    setSelectedEmployeeId(null);
  };

  async function downloadMonthlyReport() {
    try {
      const blob = await apiFetchFile(`/api/reports/monthly/current`);

      // Crea un URL temporaneo per il blob
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `report_dipendenti.xlsx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url); // pulizia
    } catch (err) {
      console.error("Errore durante il download del file:", err);
    }
  }

  const menuItems = [
    { id: 'dashboard', icon: BarChart3, label: 'Dashboard', badge: null },
    { id: 'employees', icon: Users, label: 'Elenco Dipendenti', badge: null },
    { id: 'absences', icon: Calendar, label: 'Gestione Assenze', badge: null },
    { id: 'access-logs', icon: ClipboardList, label: 'Letture Accessi', badge: null },
    { id: 'cards', icon: CreditCard, label: 'Gestione Badge', badge: null },
    { id: 'shifts', icon: Clock, label: 'Turni & Orari', badge: null },
    { id: 'contracts', icon: ClipboardList, label: 'Contratti', badge: null },
  ];

  return (
    <div className="d-flex vh-100 overflow-hidden">
      <aside
        className={`bg-dark text-white ${sidebarOpen ? '' : 'd-none d-lg-block'}`}
        style={{
          width: sidebarOpen ? '280px' : '0',
          transition: 'width 0.3s ease',
          minWidth: sidebarOpen ? '280px' : '0'
        }}
      >
        <div className="p-4 border-bottom border-secondary">
          <div className="d-flex align-items-center gap-2">
            <div className="bg-primary rounded p-2">
              <Users size={24} />
            </div>
            <div>
              <h5 className="mb-0 fw-bold">Gestionale</h5>
              <small className="text-white-50">Amministrazione</small>
            </div>
          </div>
        </div>

        <nav className="p-3">
          <ul className="list-unstyled">
            {menuItems.map((item) => {
              const Icon = item.icon;
              return (
                <li key={item.id} className="mb-1">
                  <button
                    onClick={() => setActiveSection(item.id)}
                    className={`btn w-100 text-start d-flex align-items-center gap-3 px-3 py-2 rounded ${activeSection === item.id
                      ? 'bg-primary text-white'
                      : 'text-white-50 hover-bg-secondary'
                      }`}
                    style={{
                      border: 'none',
                      transition: 'all 0.2s'
                    }}
                  >
                    <Icon size={20} />
                    <span className="flex-grow-1">{item.label}</span>
                    {item.badge && (
                      <span className="badge bg-danger rounded-pill">{item.badge}</span>
                    )}
                  </button>
                </li>
              );
            })}
          </ul>
        </nav>

        <div className="position-absolute bottom-0 w-100 p-3">
          <button
            onClick={onLogout}
            className="btn btn-outline-light d-flex align-items-center justify-content-center gap-2"
          >
            <LogOut size={18} />
            <span>Esci</span>
          </button>
        </div>
      </aside>

      <main className="flex-grow-1 overflow-auto bg-light">
        <header className="bg-white border-bottom sticky-top">
          <div className="d-flex align-items-center justify-content-between p-3">
            <div className="d-flex align-items-center gap-3">
              <button
                className="btn btn-outline-secondary d-lg-none"
                onClick={() => setSidebarOpen(!sidebarOpen)}
              >
                <Menu size={20} />
              </button>
              <h4 className="mb-0 fw-bold text-dark">
                {menuItems.find(item => item.id === activeSection)?.label || 'Dashboard'}
              </h4>
            </div>

            <div className="d-flex align-items-center gap-3">
              {hasRole('ADMIN') && (
                <button
                  className="btn btn-outline-secondary"
                  onClick={() => setActiveSection("admin")}
                  title="Pannello Amministrazione"
                >
                  <Settings size={18} />
                </button>
              )}
              <div className="d-flex align-items-center gap-2">
                <div className="bg-primary rounded-circle d-flex align-items-center justify-content-center" style={{ width: '40px', height: '40px' }}>
                  <span className="text-white fw-bold">AM</span>
                </div>
                <div className="d-none d-md-block">
                  <div className="fw-semibold small">{user.username}</div>
                  <div className="text-muted" style={{ fontSize: '0.75rem' }}>{user.email}</div>
                </div>
              </div>
            </div>
          </div>
        </header>

        <div className="p-4">
          <div className="container-fluid">
            {activeSection === 'dashboard' && (
              <div>
                <div className="row g-4 mb-4">
                  <div className="col-md-6 col-xl-3">
                    <div className="card border-0 shadow-sm h-100">
                      <div className="card-body">
                        <div className="d-flex justify-content-between align-items-start">
                          <div>
                            <p className="text-muted mb-1 small">Dipendenti Totali</p>
                            <h3 className="fw-bold mb-0">{employeeCount}</h3>
                          </div>
                          <div className="bg-primary bg-opacity-10 rounded p-2">
                            <Users size={24} className="text-primary" />
                          </div>
                        </div>
                        <div className="mt-3">
                          <small className="text-success"></small>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="col-md-6 col-xl-3">
                    <div className="card border-0 shadow-sm h-100">
                      <div className="card-body">
                        <div className="d-flex justify-content-between align-items-start">
                          <div>
                            <p className="text-muted mb-1 small">Al lavoro</p>
                            <h3 className="fw-bold mb-0">{atWorkCount}</h3>
                          </div>
                          <div className="bg-success bg-opacity-10 rounded p-2">
                            <ClipboardList size={24} className="text-success" />
                          </div>
                        </div>
                        <div className="mt-3">
                          <small className="text-muted">{(atWorkCount * 100) / employeeCount}% presenza</small>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="col-md-6 col-xl-3">
                    <div className="card border-0 shadow-sm h-100">
                      <div className="card-body">
                        <div className="d-flex justify-content-between align-items-start">
                          <div>
                            <p className="text-muted mb-1 small">Assenti Oggi</p>
                            <h3 className="fw-bold mb-0">{todaysCount}</h3>
                          </div>
                          <div className="bg-warning bg-opacity-10 rounded p-2">
                            <Calendar size={24} className="text-warning" />
                          </div>
                        </div>
                        <div className="mt-3">
                          <small className="text-muted">{futureCount} richieste pendenti</small>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="col-md-6 col-xl-3">
                    <div className="card border-0 shadow-sm h-100">
                      <div className="card-body">
                        <div className="d-flex justify-content-between align-items-start">
                          <div>
                            <p className="text-muted mb-1 small">Badge Attivi</p>
                            <h3 className="fw-bold mb-0">{assignedCards}</h3>
                          </div>
                          <div className="bg-info bg-opacity-10 rounded p-2">
                            <CreditCard size={24} className="text-info" />
                          </div>
                        </div>
                        <div className="mt-3">
                          <small className="text-muted">{cardCount - assignedCards} non assegnati</small>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="row g-4">
                  <div className="col-lg-8">
                    <div className="card border-0 shadow-sm">
                      <div className="card-header bg-white border-bottom">
                        <h5 className="mb-0 fw-semibold">Attività Recenti</h5>
                      </div>
                      <div className="card-body">
                        <div className="list-group list-group-flush">
                          {/* <div className="list-group-item px-0">
                            <div className="d-flex gap-3">
                              <div className="bg-success bg-opacity-10 rounded p-2 align-self-start">
                                <UserPlus size={20} className="text-success" />
                              </div>
                              <div className="flex-grow-1">
                                <div className="fw-semibold">Nuovo dipendente aggiunto</div>
                                <p className="text-muted mb-1 small">Mario Rossi è stato inserito nel sistema</p>
                                <small className="text-muted">2 ore fa</small>
                              </div>
                            </div>
                          </div> */}
                          {anomalies.map((item, index) => (
                            <div key={index} className="list-group-item px-0">
                              <div className="d-flex gap-3">
                                <div className="bg-warning bg-opacity-10 rounded p-2 align-self-start">
                                  <MessageCircleWarningIcon size={20} className="text-warning" />
                                </div>
                                <div className="flex-grow-1">
                                  <div className="fw-semibold">Anomalia lettura badge di {item.employeeName}</div>
                                  <p className="text-muted mb-1 small">{item.description}</p>
                                  <small className="text-muted">{formatDate(item.date)}</small>
                                </div>
                              </div>
                            </div>
                          ))}
                          {/* <div className="list-group-item px-0">
                            <div className="d-flex gap-3">
                              <div className="bg-primary bg-opacity-10 rounded p-2 align-self-start">
                                <Clock size={20} className="text-primary" />
                              </div>
                              <div className="flex-grow-1">
                                <div className="fw-semibold">Turni aggiornati</div>
                                <p className="text-muted mb-1 small">Pianificazione turni settimana prossima completata</p>
                                <small className="text-muted">Ieri</small>
                              </div>
                            </div>
                          </div> */}
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="col-lg-4">
                    <div className="card border-0 shadow-sm">
                      <div className="card-header bg-white border-bottom">
                        <h5 className="mb-0 fw-semibold">Azioni Rapide</h5>
                      </div>
                      <div className="card-body">
                        <div className="d-grid gap-2">
                          <button className="btn btn-outline-success" onClick={async () => await downloadMonthlyReport()}>
                            <ClipboardList size={18} className="me-2" />
                            Scarica Report
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
            {activeSection === "employees" && !selectedEmployeeId && (
              <EmployeeListPage
                onNavigateToDetails={showEmployeeDetailPage}
                onAction1={() => console.log("Ciao2")}
              />
            )}

            {activeSection === "employees" && selectedEmployeeId && (
              <EmployeeDetailPage
                employeeId={selectedEmployeeId}
                onNavigateBack={backToList}
              />
            )}
            {activeSection === 'cards' && (
              <CardManagementPage />
            )}
            {activeSection === 'access-logs' && (
              <AccessLogsPage />
            )}
            {activeSection === 'absences' && (
              <AbsenceManagementPage />
            )}
            {activeSection === 'contracts' && (
              <ContractManagementPage />
            )}
            {activeSection === 'shifts' && (
              <ShiftCalendarPage />
            )}
            {activeSection === 'admin' && hasRole('ADMIN') && (
              <AdminPanel currentUser={user} />
            )}
          </div>
        </div>
      </main>
    </div>
  );
};

export default HomePage;