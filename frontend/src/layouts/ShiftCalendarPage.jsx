import React, { useState, useEffect } from 'react';
import { Calendar, momentLocalizer } from 'react-big-calendar';
import { toast } from "react-toastify";
import moment from 'moment';
import 'moment/locale/it';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import {
    Clock,
    Users,
    Plus,
    PlayCircle,
    Calendar as CalendarIcon,
    CheckCircle
} from 'lucide-react';
import CreateShiftModal from '../components/CreateShiftModal';
import AssociateShiftModal from '../components/AssociateShiftModal';
import AssignmentModal from '../components/AssignmentModal';
import EditEventModal from '../components/EditEventModal';
import { getAllEmployees } from '../api/employees';
import { generateAssignments, getAllShifts, getEmployeeShiftAssignments, getEmployeeShiftAssociations } from '../api/shifts';
import { getEmployeeAbsencesInRange, getHolidaysInRange } from '../api/absence';
import { get3MonthsFromNow, getDateIn15Days, getTomorrow } from '../utils/utils';


// Inizializza moment e localizer
moment.updateLocale('it', {
    week: {
        dow: 1,
    },
});
moment.locale('it');
const localizer = momentLocalizer(moment);

// Messaggi italiani
const messages = {
    week: 'Settimana',
    work_week: 'Settimana Lavorativa',
    day: 'Giorno',
    month: 'Mese',
    previous: 'Precedente',
    next: 'Successivo',
    today: 'Oggi',
    agenda: 'Agenda',
    date: 'Data',
    time: 'Ora',
    event: 'Evento',
    noEventsInRange: 'Nessun turno in questo periodo',
    showMore: total => `+${total} altro/i`
};

const ShiftCalendarPage = () => {
    // Stati principali
    const [selectedEmployee, setSelectedEmployee] = useState(null);
    const [currentDate, setCurrentDate] = useState(new Date());
    const [view, setView] = useState('week');

    // Modal states
    const [showCreateShiftModal, setShowCreateShiftModal] = useState(false);
    const [showAssociateShiftModal, setShowAssociateShiftModal] = useState(false);
    const [showAssignmentModal, setShowAssignmentModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [selectedSlot, setSelectedSlot] = useState(null);

    const [employees, setEmployees] = useState([]);
    const [shifts, setShifts] = useState([]);
    const [shiftAssociations, setShiftAssociations] = useState([]);
    const [shiftAssignments, setShiftAssignments] = useState([]);
    const [absences, setAbsences] = useState([]);
    const [holidays, setHolidays] = useState([])

    function getStartOfWeek() {
        return moment(currentDate).startOf("isoWeek").format("YYYY-MM-DD");
    }

    function getEndOfWeek() {
        return moment(currentDate).endOf("isoWeek").format("YYYY-MM-DD");
    }

    useEffect(() => {
        fetchData()
    }, [])

    useEffect(() => {
        fetchEmployeeData()
    }, [selectedEmployee, currentDate])

    async function fetchData() {
        try {
            const employeeResponse = await getAllEmployees()
            const shiftResponse = await getAllShifts()

            setShifts(shiftResponse)
            setEmployees(employeeResponse)
        } catch (error) {
            console.error("Errore nell0ottenimento dei dati!")
        }
    }

    async function fetchEmployeeData() {
        try {
            if (selectedEmployee && selectedEmployee.id) {
                const response = await getEmployeeShiftAssociations(selectedEmployee.id)
                const assignmentResponse = await getEmployeeShiftAssignments(selectedEmployee.id, getStartOfWeek(), getEndOfWeek())
                const absencesResponse = await getEmployeeAbsencesInRange(getStartOfWeek(), getEndOfWeek(), selectedEmployee.id)
                const holidayResponse = await getHolidaysInRange(getStartOfWeek(), getEndOfWeek())


                setAbsences(absencesResponse)
                setShiftAssignments(assignmentResponse)
                setShiftAssociations(response)
                setHolidays(holidayResponse)
            }
        } catch (error) {
            console.error("Errore nel recupero delle associazioni", error)
        }
    }

    // Genera eventi per il calendario
    const events = React.useMemo(() => {
        if (!selectedEmployee) return [];

        const startOfWeek = moment(getStartOfWeek());
        const endOfWeek = moment(getEndOfWeek());

        const employeeAssignments = shiftAssignments.filter(
            a => a.employeeId === selectedEmployee.id
        );

        const employeeAbsences = absences.filter(
            a => a.employeeId === selectedEmployee.id
        );

        const assignmentEvents = employeeAssignments.map(assignment => {
            const start = moment(`${assignment.date} ${assignment.startTime}`, 'YYYY-MM-DD HH:mm').toDate();
            const end = moment(`${assignment.date} ${assignment.endTime}`, 'YYYY-MM-DD HH:mm').toDate();

            return {
                id: `assignment-${assignment.id}`,
                title: `${assignment.startTime.replace(/:\d{2}$/, '')} - ${assignment.endTime.replace(/:\d{2}$/, '')}`,
                start,
                end,
                resource: {
                    type: 'assignment',
                    data: assignment
                }
            };
        });

        const absenceEvents = employeeAbsences.map(absence => {
            const start = moment(absence.startDate).startOf('day').toDate();
            const end = moment(absence.endDate).endOf('day').toDate();

            const typeLabels = {
                'VACATION': 'Ferie',
                'ROL': 'ROL',
                'SICK_LEAVE': 'Malattia',
                'PERMIT': 'Permesso'
            };

            return {
                id: `absence-${absence.id}`,
                title: typeLabels[absence.type] || absence.type,
                start,
                end,
                allDay: true,
                resource: {
                    type: 'absence',
                    data: absence
                }
            };
        });
        const year = currentDate.getFullYear();
        const holidayEvents = holidays
            .filter(h => h.recurring || h.year === year)
            .map(holiday => {
                const holidayYear = holiday.recurring ? year : holiday.year;
                const date = moment(`${holidayYear}-${holiday.month}-${holiday.day}`, 'YYYY-M-D');

                return {
                    id: `holiday-${holiday.id}`,
                    title: `🎉 ${holiday.name}`,
                    start: moment(date).startOf('day').toDate(),
                    end: moment(date).endOf('day').toDate(),
                    allDay: true,
                    resource: {
                        ...holiday,
                        type: 'holiday',
                        isHoliday: true
                    }
                };
            });

        return [...assignmentEvents, ...absenceEvents, ...holidayEvents];

    }, [selectedEmployee, shiftAssignments, absences, holidays, currentDate]);


    // Stile eventi
    const eventStyleGetter = (event) => {
        if (event.resource.type === 'absence') {
            const absenceColors = {
                'VACATION': { bg: '#dbeafe', border: '#3b82f6', color: '#1e40af' },
                'ROL': { bg: '#ede9fe', border: '#8b5cf6', color: '#6b21a8' },
                'SICK_LEAVE': { bg: '#fee2e2', border: '#ef4444', color: '#991b1b' },
                'PERMIT': { bg: '#fef3c7', border: '#f59e0b', color: '#92400e' },
            };

            const colors = absenceColors[event.resource.data.type] || absenceColors['PERMIT'];

            return {
                style: {
                    backgroundColor: colors.bg,
                    borderLeft: `4px solid ${colors.border}`,
                    color: colors.color,
                    borderRadius: '4px',
                    padding: '4px 8px',
                    fontSize: '0.875rem',
                    fontWeight: '600'
                }
            };
        }

        if (event.resource.type === 'holiday') {
            return {
                style: {
                backgroundColor: '#f3fff9ff',
                color: '#6e0101ff',
                borderLeft: `2px solid #6e0101ff`,
                borderRadius: '4px',
                fontSize: '0.85rem',
                fontWeight: '600'
            }
            };
        }

        // Turno normale
        const isAutoGenerated = event.resource.data.autoGenerated;
        return {
            style: {
                backgroundColor: isAutoGenerated ? '#dcfce7' : '#e0e7ff',
                borderLeft: `4px solid ${isAutoGenerated ? '#10b981' : '#6366f1'}`,
                color: isAutoGenerated ? '#065f46' : '#3730a3',
                borderRadius: '4px',
                padding: '4px 8px',
                fontSize: '0.875rem',
                fontWeight: '600'
            }
        };
    };

    const handleSelectSlot = (slotInfo) => {
        if (!selectedEmployee) {
            alert('Seleziona prima un dipendente');
            return;
        }
        setSelectedSlot(slotInfo);
        setShowAssignmentModal(true);
    };

    const handleSelectEvent = (event) => {
        setSelectedEvent(event);
        setShowEditModal(true);
    };

    const handleGenerateAssignments = async () => {
        if (!confirm('Vuoi generare automaticamente le assegnazioni per la prossima settimana dalle associazioni ricorrenti?')) {
            return;
        }
        try {
            await generateAssignments(getTomorrow(), getDateIn15Days())
            toast.success('Assegnazioni generate con successo!');
            await fetchEmployeeData()
        } catch (error) {
            toast.error("Errore nella generazione delle assegnazioni");
            console.error(error)
        }
    };

    const stats = {
        totalShifts: shifts.length,
        activeShifts: shifts.filter(s => s.active).length,
        employeesWithShifts: new Set(shiftAssociations.map(a => a.employeeId)).size,
        assignmentsThisWeek: shiftAssignments.filter(a => {
            const assignmentDate = moment(a.date);
            const startOfWeek = moment(currentDate).startOf('week');
            const endOfWeek = moment(currentDate).endOf('week');
            return assignmentDate.isBetween(startOfWeek, endOfWeek, null, '[]');
        }).length
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
                                    <p className="text-muted mb-1 small">Turni Totali</p>
                                    <h4 className="fw-bold mb-0">{stats.totalShifts}</h4>
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
                                    <p className="text-muted mb-1 small">Turni Attivi</p>
                                    <h4 className="fw-bold mb-0 text-success">{stats.activeShifts}</h4>
                                </div>
                                <div className="bg-success bg-opacity-10 rounded p-2">
                                    <CheckCircle size={20} className="text-success" />
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
                                    <p className="text-muted mb-1 small">Dipendenti con Turni</p>
                                    <h4 className="fw-bold mb-0 text-info">{stats.employeesWithShifts}</h4>
                                </div>
                                <div className="bg-info bg-opacity-10 rounded p-2">
                                    <Users size={20} className="text-info" />
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
                                    <p className="text-muted mb-1 small">Turni Questa Settimana</p>
                                    <h4 className="fw-bold mb-0 text-warning">{stats.assignmentsThisWeek}</h4>
                                </div>
                                <div className="bg-warning bg-opacity-10 rounded p-2">
                                    <CalendarIcon size={20} className="text-warning" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Action Bar */}
            <div className="card border-0 shadow-sm mb-4">
                <div className="card-body">
                    <div className="row g-3 align-items-center">
                        <div className="col-md-4">
                            <label className="form-label small text-muted mb-1">Seleziona Dipendente</label>
                            <select
                                className="form-select"
                                value={selectedEmployee?.id || ''}
                                onChange={(e) => {
                                    const emp = employees.find(emp => emp.id === parseInt(e.target.value));
                                    setSelectedEmployee(emp);
                                }}
                            >
                                <option value="">Seleziona un dipendente...</option>
                                {employees.map(emp => (
                                    <option key={emp.id} value={emp.id}>
                                        {emp.name} {emp.surname}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="col-md-8">
                            <label className="form-label small text-muted mb-1">Azioni Rapide</label>
                            <div className="d-flex gap-2">
                                <button
                                    className="btn btn-primary"
                                    onClick={() => setShowCreateShiftModal(true)}
                                >
                                    <Plus size={18} className="me-1" />
                                    Crea Turno
                                </button>
                                <button
                                    className="btn btn-outline-primary"
                                    onClick={() => setShowAssociateShiftModal(true)}
                                    disabled={!selectedEmployee}
                                >
                                    <Clock size={18} className="me-1" />
                                    Associa Turno Ricorrente
                                </button>
                                <button
                                    className="btn btn-success"
                                    onClick={handleGenerateAssignments}
                                >
                                    <PlayCircle size={18} className="me-1" />
                                    Genera Assegnazioni
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Legenda */}
            {selectedEmployee && (
                <div className="card border-0 shadow-sm mb-4">
                    <div className="card-body">
                        <div className="d-flex flex-wrap gap-3 align-items-center">
                            <small className="text-muted fw-semibold">Legenda:</small>
                            <div className="d-flex align-items-center gap-2">
                                <div style={{
                                    width: '20px',
                                    height: '20px',
                                    backgroundColor: '#dcfce7',
                                    borderLeft: '4px solid #10b981',
                                    borderRadius: '4px'
                                }} />
                                <small>Turno Auto-generato</small>
                            </div>
                            <div className="d-flex align-items-center gap-2">
                                <div style={{
                                    width: '20px',
                                    height: '20px',
                                    backgroundColor: '#e0e7ff',
                                    borderLeft: '4px solid #6366f1',
                                    borderRadius: '4px'
                                }} />
                                <small>Turno Manuale</small>
                            </div>
                            <div className="d-flex align-items-center gap-2">
                                <div style={{
                                    width: '20px',
                                    height: '20px',
                                    backgroundColor: '#dbeafe',
                                    borderLeft: '4px solid #3b82f6',
                                    borderRadius: '4px'
                                }} />
                                <small>Ferie</small>
                            </div>
                            <div className="d-flex align-items-center gap-2">
                                <div style={{
                                    width: '20px',
                                    height: '20px',
                                    backgroundColor: '#fee2e2',
                                    borderLeft: '4px solid #ef4444',
                                    borderRadius: '4px'
                                }} />
                                <small>Malattia</small>
                            </div>
                            <div className="d-flex align-items-center gap-2">
                                <div style={{
                                    width: '20px',
                                    height: '20px',
                                    backgroundColor: '#f3fff9ff',
                                    borderLeft: '4px solid #6e0101ff',
                                    borderRadius: '4px'
                                }} />
                                <small>Giorno festivo</small>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Calendar */}
            <div className="card border-0 shadow-sm">
                <div className="card-body" style={{ height: '700px' }}>
                    {!selectedEmployee ? (
                        <div className="d-flex flex-column align-items-center justify-content-center h-100 text-muted">
                            <Users size={64} className="mb-3 opacity-25" />
                            <h5 className="mb-2">Seleziona un dipendente</h5>
                            <p className="mb-0">Scegli un dipendente per visualizzare i suoi turni</p>
                        </div>
                    ) : (
                        <Calendar
                            localizer={localizer}
                            events={events}
                            startAccessor="start"
                            endAccessor="end"
                            style={{ height: '100%' }}
                            view={view}
                            onView={setView}
                            views={['week', 'day']}
                            date={currentDate}
                            onNavigate={setCurrentDate}
                            messages={messages}
                            selectable
                            onSelectSlot={handleSelectSlot}
                            onSelectEvent={handleSelectEvent}
                            eventPropGetter={eventStyleGetter}
                            step={30}
                            timeslots={2}
                            min={new Date(0, 0, 0, 6, 0, 0)}
                            max={new Date(0, 0, 0, 23, 0, 0)}
                        />
                    )}
                </div>
            </div>

            {/* Modals verranno aggiunti nei componenti separati */}
            <CreateShiftModal
                show={showCreateShiftModal}
                onClose={() => {
                    setShowCreateShiftModal(false)
                    fetchData()
                    fetchEmployeeData()
                }}
                shifts={shifts}
                setShifts={setShifts}
            />

            <AssociateShiftModal
                show={showAssociateShiftModal}
                onClose={() => {
                    setShowAssociateShiftModal(false)
                    fetchData()
                    fetchEmployeeData()
                }}
                updateData={() => {
                    fetchData()
                    fetchEmployeeData()
                }}
                employee={selectedEmployee}
                shifts={shifts}
                associations={shiftAssociations}
                setAssociations={setShiftAssociations}
            />

            <AssignmentModal
                show={showAssignmentModal}
                onClose={() => {
                    setShowAssignmentModal(false);
                    setSelectedSlot(null);
                    fetchData()
                    fetchEmployeeData()
                }}
                employee={selectedEmployee}
                slotInfo={selectedSlot}
                shifts={shifts}
                assignments={shiftAssignments}
                setAssignments={setShiftAssignments}
            />

            <EditEventModal
                show={showEditModal}
                onClose={() => {
                    setShowEditModal(false);
                    setSelectedEvent(null);
                    fetchData()
                    fetchEmployeeData()
                }}
                event={selectedEvent}
                assignments={shiftAssignments}
                setAssignments={setShiftAssignments}
            />

            <style>{`
        .rbc-calendar {
          font-family: inherit;
        }
        .rbc-header {
          padding: 12px 6px;
          font-weight: 600;
          color: #1f2937;
          border-bottom: 2px solid #e5e7eb;
          background-color: #f9fafb;
        }
        .rbc-time-slot {
          min-height: 15px;
        }
        .rbc-time-content {
          border-top: 1px solid #e5e7eb;
        }
        .rbc-today {
          background-color: #fef3c7;
        }
        .rbc-toolbar button {
          color: #374151;
          border: 1px solid #d1d5db;
          padding: 8px 16px;
          border-radius: 6px;
          font-weight: 500;
        }
        .rbc-toolbar button:hover {
          background-color: #f3f4f6;
        }
        .rbc-toolbar button.rbc-active {
          background-color: #3b82f6;
          color: white;
          border-color: #3b82f6;
        }
        .rbc-event {
          padding: 2px 6px;
        }
        .rbc-event:focus {
          outline: 2px solid #3b82f6;
        }
      `}</style>
        </div>
    );
};
export default ShiftCalendarPage;