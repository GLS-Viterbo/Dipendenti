import React, { useState, useMemo, useEffect } from 'react';
import { Calendar, momentLocalizer } from 'react-big-calendar';
import { startOfMonth, endOfMonth, subMonths, addMonths, format } from 'date-fns';
import moment from 'moment';
import 'moment/locale/it';
import { Calendar as CalendarIcon, Plus, Trash2, Filter, Users, Clock } from 'lucide-react';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import { addHoliday, createApprovedAbsence, deleteAbsence, deleteHoliday, getAbsencesInRange, getHolidaysInRange } from '../api/absence';
import AbsenceModal from '../components/AbsenceModal';
import HolidayModal from '../components/HolidayModal';
import { getAllEmployees } from '../api/employees';
import { toast } from "react-toastify";

// IMPORTANTE: Inizializza moment e localizer FUORI dal componente
moment.locale('it');
const localizer = momentLocalizer(moment);

// Messaggi italiani per il calendario
const messages = {
    next: 'Successivo',
    previous: 'Precedente',
    today: 'Oggi',
    month: 'Mese',
    week: 'Settimana',
    day: 'Giorno',
    agenda: 'Agenda',
    date: 'Data',
    time: 'Ora',
    event: 'Evento',
    noEventsInRange: 'Nessuna assenza in questo periodo'
};

const ABSENCE_TYPES = {
    VACATION: { label: 'Ferie', color: '#3b82f6', bgColor: '#dbeafe' },
    ROL: { label: 'ROL', color: '#8b5cf6', bgColor: '#ede9fe' },
    SICK_LEAVE: { label: 'Malattia', color: '#ef4444', bgColor: '#fee2e2' },
    PERMIT: { label: 'Permesso', color: '#f59e0b', bgColor: '#fef3c7' },
    HOLIDAY: { label: 'Festivo', color: '#10b981', bgColor: '#d1fae5' }
};


const AbsenceManagementPage = () => {
    const [absences, setAbsences] = useState([]);
    const [holidays, setHolidays] = useState([]);
    const [showModal, setShowModal] = useState(false);
    const [showHolidayModal, setShowHolidayModal] = useState(false);
    const [showDayModal, setShowDayModal] = useState(false);
    const [selectedDay, setSelectedDay] = useState(null);
    const [dayAbsences, setDayAbsences] = useState([]);
    const [selectedAbsence, setSelectedAbsence] = useState(null);
    const [filterEmployee, setFilterEmployee] = useState('');
    const [filterType, setFilterType] = useState('');
    const [currentDate, setCurrentDate] = useState(new Date());
    const [currentView, setCurrentView] = useState('month');
    const [employees, setEmployees] = useState([])

    useEffect(() => {
        fetchMonthAbsences()
    }, [currentDate])

    useEffect(() => {
        async function fetchEmployees() {
            try {
                const employeeResults = await getAllEmployees();
                setEmployees(employeeResults)
            } catch (error) {
                console.error('Errore nel recupero dei dipendenti:', error.message);
            }
        }
        fetchEmployees()
    }, [])

    // Form state
    const [formData, setFormData] = useState({
        employeeId: '',
        type: 'VACATION',
        startDate: '',
        endDate: '',
        note: '',
        hoursCount: ''
    });

    async function fetchMonthAbsences() {
        // Inizio dello scorso mese
        const start = format(startOfMonth(subMonths(currentDate, 1)), 'yyyy-MM-dd');

        // Fine del prossimo mese
        const end = format(endOfMonth(addMonths(currentDate, 1)), 'yyyy-MM-dd');

        const absenceData = await getAbsencesInRange(start, end);
        const holidayData = await getHolidaysInRange(start, end);

        setHolidays(holidayData);
        setAbsences(absenceData);
    }
    const handleNavigate = (date) => {
        setCurrentDate(date);
    };

    // Converti festivi in eventi per il calendario
    const holidayEvents = useMemo(() => {
        const year = currentDate.getFullYear();

        return holidays
            .filter(h => h.recurring || h.year === year)
            .map(holiday => {
                const holidayYear = holiday.recurring ? year : holiday.year;
                const date = moment(`${holidayYear}-${holiday.month}-${holiday.day}`, 'YYYY-M-D');

                return {
                    id: `holiday-${holiday.id}`,
                    title: `🎉 ${holiday.name}`,
                    start: date.toDate(),
                    end: date.endOf('day').toDate(),
                    resource: {
                        ...holiday,
                        type: 'HOLIDAY',
                        isHoliday: true
                    }
                };
            });
    }, [holidays, currentDate]);

    // Converti assenze in eventi per il calendario
    const absenceEvents = useMemo(() => {
        let filtered = absences;

        if (filterEmployee) {
            filtered = filtered.filter(a => a.employeeId === parseInt(filterEmployee));
        }

        if (filterType) {
            filtered = filtered.filter(a => a.type === filterType);
        }

        return filtered.map(absence => ({
            id: absence.id,
            title: `${absence.employeeName} - ${ABSENCE_TYPES[absence.type].label}`,
            start: moment(absence.startDate, 'YYYY-MM-DD').toDate(),
            end: moment(absence.endDate, 'YYYY-MM-DD').endOf('day').toDate(),
            resource: absence
        }));
    }, [absences, filterEmployee, filterType]);

    // Combina assenze e festivi
    const events = useMemo(() => {
        return [...absenceEvents, ...holidayEvents];
    }, [absenceEvents, holidayEvents]);

    const handleSelectSlot = ({ start, end }) => {
        setSelectedAbsence(null);
        setFormData({
            employeeId: '',
            type: 'VACATION',
            startDate: moment(start).format('YYYY-MM-DD'),
            endDate: moment(end).subtract(1, 'day').format('YYYY-MM-DD'),
            note: ''
        });
        setShowModal(true);
    };

    const handleShowMore = (events, date) => {
        const dayAbsencesList = events.map(e => e.resource);
        setDayAbsences(dayAbsencesList);
        setSelectedDay(date);
        setShowDayModal(true);
    };

    const handleSelectEvent = async (event) => {
        // Se è un festivo, non aprire il modal di dettaglio assenza
        if (event.resource.isHoliday) {
            const delHoliday = confirm("Vuoi eliminare il giorno festivo selezionato?")
            if (delHoliday) {
                await deleteHoliday(event.resource.id)
                await fetchMonthAbsences()
            }
            return;
        }

        const absence = event.resource;
        setSelectedAbsence(absence);
        setFormData({
            employeeId: absence.employeeId,
            type: absence.type,
            startDate: absence.startDate,
            endDate: absence.endDate,
            note: absence.note || ''
        });
        setShowModal(true);
    };

    const handleSubmit = async (formData) => {
        console.log("Nuova assenza:", formData);
        try {
            await createApprovedAbsence(formData)
            toast.success("Assenza inserita con successo")
            fetchMonthAbsences()
        } catch (error) {
            if (error.status == 409) {
                toast.error("Esiste già un'assenza per uno o più giorni selezionati")
                console.error()
            } else {
                toast.error("Errore nell'inserimento dell'assenza")
                console.error()
            }
        }
    };

    const handleHolidaySubmit = async (holidayData) => {
        console.log("Nuovo festivo:", holidayData);
        try {
            await addHoliday(holidayData);
            fetchMonthAbsences()
            toast.success("Giorno festivo aggiunto con successo");
        } catch (error) {
            toast.error("Errore nell'inserimento del giorno festivo");
            console.error(error);
        }
    };

    const handleDelete = async () => {
        if (selectedAbsence && window.confirm('Sei sicuro di voler eliminare questa assenza?')) {
            try {
                await deleteAbsence(selectedAbsence.id)
                toast.success("Assenza eliminata e saldo ripristinato")
                setShowModal(false);
                resetForm();
                await fetchMonthAbsences()
            } catch (error) {
                toast.error("Impossibile eliminare l'assenza")
                console.error(error)
            }
        }
    };

    const resetForm = () => {
        setSelectedAbsence(null);
        setFormData({
            employeeId: '',
            type: 'VACATION',
            startDate: '',
            endDate: '',
            note: '',
            hoursCount: ''
        });
    };

    const eventStyleGetter = (event) => {
        const type = event.resource.type;
        return {
            style: {
                backgroundColor: ABSENCE_TYPES[type].bgColor,
                color: ABSENCE_TYPES[type].color,
                border: `2px solid ${ABSENCE_TYPES[type].color}`,
                borderRadius: '4px',
                fontSize: '0.85rem',
                fontWeight: '600'
            }
        };
    };

    return (
        <div>
            {/* Header con filtri */}
            <div className="row g-3 mb-4">
                <div className="col-md-4">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <label className="form-label small text-muted mb-2">
                                <Users size={16} className="me-1" />
                                Filtra per Dipendente
                            </label>
                            <select
                                className="form-select"
                                value={filterEmployee}
                                onChange={(e) => setFilterEmployee(e.target.value)}
                            >
                                <option value="">Tutti i dipendenti</option>
                                {employees.map(emp => (
                                    <option key={emp.id} value={emp.id}>{emp.name} {emp.surname}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                </div>

                <div className="col-md-4">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <label className="form-label small text-muted mb-2">
                                <Filter size={16} className="me-1" />
                                Filtra per Tipo
                            </label>
                            <select
                                className="form-select"
                                value={filterType}
                                onChange={(e) => setFilterType(e.target.value)}
                            >
                                <option value="">Tutti i tipi</option>
                                {Object.entries(ABSENCE_TYPES)
                                    .filter(([key]) => key !== 'HOLIDAY')
                                    .map(([key, value]) => (
                                        <option key={key} value={key}>{value.label}</option>
                                    ))}
                            </select>
                        </div>
                    </div>
                </div>

                <div className="col-md-4">
                    <div className="card border-0 shadow-sm">
                        <div className="card-body">
                            <label className="form-label small text-muted mb-2">Azioni Rapide</label>
                            <button
                                className="btn btn-primary w-100"
                                onClick={() => {
                                    resetForm();
                                    setShowModal(true);
                                }}
                            >
                                <Plus size={18} className="me-2" />
                                Nuova Assenza
                            </button>
                            <button
                                className="btn btn-success w-100 mt-2"
                                onClick={() => setShowHolidayModal(true)}
                            >
                                <CalendarIcon size={18} className="me-2" />
                                Giorno Festivo
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Legenda */}
            <div className="card border-0 shadow-sm mb-4">
                <div className="card-body">
                    <div className="d-flex flex-wrap gap-3 align-items-center">
                        <small className="text-muted fw-semibold">Legenda:</small>
                        {Object.entries(ABSENCE_TYPES).map(([key, value]) => (
                            <div key={key} className="d-flex align-items-center gap-2">
                                <div
                                    style={{
                                        width: '20px',
                                        height: '20px',
                                        backgroundColor: value.bgColor,
                                        border: `2px solid ${value.color}`,
                                        borderRadius: '4px'
                                    }}
                                />
                                <small>{value.label}</small>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Calendario */}
            <div className="card border-0 shadow-sm">
                <div className="card-body" style={{ height: '800px' }}>
                    <Calendar
                        localizer={localizer}
                        events={events}
                        startAccessor="start"
                        endAccessor="end"
                        style={{ height: '100%' }}
                        selectable
                        onSelectSlot={handleSelectSlot}
                        onSelectEvent={handleSelectEvent}
                        onShowMore={handleShowMore}
                        eventPropGetter={eventStyleGetter}
                        date={currentDate}
                        onNavigate={handleNavigate}
                        messages={messages}
                        view={currentView}
                        onView={(view) => setCurrentView(view)}
                        views={['month', 'week', 'agenda']}
                        defaultView="month"
                    />
                </div>
            </div>

            {/* Modal per dettaglio assenza */}
            {showModal && selectedAbsence && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">
                                    <CalendarIcon size={20} className="me-2" />
                                    Dettaglio Assenza
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

                            <div className="modal-body">
                                <div className="mb-4">
                                    <label className="text-muted small mb-1">Dipendente</label>
                                    <h6 className="fw-semibold">{selectedAbsence.employeeName}</h6>
                                </div>

                                <div className="mb-4">
                                    <label className="text-muted small mb-1">Tipo Assenza</label>
                                    <div className="d-flex align-items-center gap-2">
                                        <div
                                            style={{
                                                width: '20px',
                                                height: '20px',
                                                backgroundColor: ABSENCE_TYPES[selectedAbsence.type].bgColor,
                                                border: `2px solid ${ABSENCE_TYPES[selectedAbsence.type].color}`,
                                                borderRadius: '4px'
                                            }}
                                        />
                                        <span className="fw-semibold">{ABSENCE_TYPES[selectedAbsence.type].label}</span>
                                    </div>
                                </div>

                                <div className="row mb-4">
                                    <div className="col-6">
                                        <label className="text-muted small mb-1">Data Inizio</label>
                                        <div className="fw-semibold">{moment(selectedAbsence.startDate).format('DD/MM/YYYY')}</div>
                                    </div>
                                    <div className="col-6">
                                        <label className="text-muted small mb-1">Data Fine</label>
                                        <div className="fw-semibold">{moment(selectedAbsence.endDate).format('DD/MM/YYYY')}</div>
                                    </div>
                                </div>

                                <div className="mb-4">
                                    <label className="text-muted small mb-1">Durata</label>
                                    <div className="d-flex align-items-center gap-3">
                                        <span className="badge bg-primary px-3 py-2">
                                            {selectedAbsence.hoursCount} ore
                                        </span>
                                        <span className="text-muted">
                                            ({moment(selectedAbsence.endDate).diff(moment(selectedAbsence.startDate), 'days') + 1} giorni)
                                        </span>
                                    </div>
                                </div>

                                {(selectedAbsence.type === 'VACATION' || selectedAbsence.type === 'ROL') && (
                                    <div className="alert alert-info mb-4">
                                        <div className="d-flex justify-content-between align-items-center mb-2">
                                            <strong>{selectedAbsence.type === 'VACATION' ? 'Ferie' : 'Permessi (ROL)'} utilizzate</strong>
                                            <strong>{selectedAbsence.hoursCount} ore</strong>
                                        </div>
                                        <small className="text-muted">
                                            Queste ore sono state detratte dal saldo disponibile del dipendente
                                        </small>
                                    </div>
                                )}

                                {selectedAbsence.note && (
                                    <div className="mb-4">
                                        <label className="text-muted small mb-1">Note</label>
                                        <div className="p-3 bg-light rounded">
                                            {selectedAbsence.note}
                                        </div>
                                    </div>
                                )}

                                <div>
                                    <label className="text-muted small mb-1">Data Creazione</label>
                                    <div className="small">{moment(selectedAbsence.createdAt).format('DD/MM/YYYY HH:mm')}</div>
                                </div>
                            </div>

                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-danger"
                                    onClick={handleDelete}
                                >
                                    <Trash2 size={16} className="me-2" />
                                    Elimina Assenza
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => {
                                        setShowModal(false);
                                        resetForm();
                                    }}
                                >
                                    Chiudi
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {showDayModal && (
                <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered modal-lg">
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title">
                                    <CalendarIcon size={20} className="me-2" />
                                    Assenze del {moment(selectedDay).format('DD MMMM YYYY')}
                                </h5>
                                <button
                                    type="button"
                                    className="btn-close"
                                    onClick={() => setShowDayModal(false)}
                                />
                            </div>
                            <div className="modal-body">
                                {dayAbsences.length === 0 ? (
                                    <div className="text-center py-5">
                                        <CalendarIcon size={48} className="text-muted mb-3" />
                                        <p className="text-muted">Nessuna assenza registrata per questo giorno</p>
                                    </div>
                                ) : (
                                    <div className="list-group">
                                        {dayAbsences.map((absence) => (
                                            <div key={absence.id} className="list-group-item">
                                                <div className="d-flex justify-content-between align-items-start mb-2">
                                                    <div className="d-flex align-items-center gap-3 flex-grow-1">
                                                        <div
                                                            style={{
                                                                width: '4px',
                                                                height: '60px',
                                                                backgroundColor: ABSENCE_TYPES[absence.type].color,
                                                                borderRadius: '2px'
                                                            }}
                                                        />
                                                        <div className="flex-grow-1">
                                                            <h6 className="mb-1 fw-bold">
                                                                {absence.isHoliday ? `🎉 ${absence.name}` : absence.employeeName}
                                                            </h6>
                                                            <div className="d-flex align-items-center gap-2 mb-1">
                                                                <div
                                                                    style={{
                                                                        width: '16px',
                                                                        height: '16px',
                                                                        backgroundColor: ABSENCE_TYPES[absence.type].bgColor,
                                                                        border: `2px solid ${ABSENCE_TYPES[absence.type].color}`,
                                                                        borderRadius: '3px'
                                                                    }}
                                                                />
                                                                <span className="small fw-semibold">{ABSENCE_TYPES[absence.type].label}</span>
                                                            </div>
                                                            {!absence.isHoliday && (
                                                                <>
                                                                    <div className="text-muted small">
                                                                        {moment(absence.startDate).format('DD/MM/YYYY')} - {moment(absence.endDate).format('DD/MM/YYYY')}
                                                                        <span className="ms-2">({absence.hoursCount} ore)</span>
                                                                    </div>
                                                                    {absence.note && (
                                                                        <div className="mt-2 small text-muted">
                                                                            <strong>Note:</strong> {absence.note}
                                                                        </div>
                                                                    )}
                                                                </>
                                                            )}
                                                            {absence.isHoliday && (
                                                                <div className="text-muted small">
                                                                    {absence.recurring ? 'Festività ricorrente' : `Anno ${absence.year}`}
                                                                </div>
                                                            )}
                                                        </div>
                                                    </div>
                                                    {!absence.isHoliday && (
                                                        <button
                                                            className="btn btn-sm btn-outline-primary"
                                                            onClick={() => {
                                                                setShowDayModal(false);
                                                                handleSelectEvent({ resource: absence });
                                                            }}
                                                        >
                                                            Dettagli
                                                        </button>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                            <div className="modal-footer">
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={() => setShowDayModal(false)}
                                >
                                    Chiudi
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Modal per inserimento assenza */}
            {showModal && !selectedAbsence && (
                <AbsenceModal
                    showModal={showModal}
                    setShowModal={setShowModal}
                    handleSubmit={handleSubmit}
                    ABSENCE_TYPES={ABSENCE_TYPES}
                    employees={employees}
                    formData={formData}
                    setFormData={setFormData}
                    selectedEmployee={filterEmployee}
                />
            )}

            {/* Modal per inserimento giorno festivo */}
            <HolidayModal
                showModal={showHolidayModal}
                setShowModal={setShowHolidayModal}
                onSubmit={handleHolidaySubmit}
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
        .rbc-today {
          background-color: #fef3c7;
        }
        .rbc-off-range-bg {
          background-color: #f9fafb;
        }
        .rbc-event {
          padding: 2px 6px;
        }
        .rbc-event:focus {
          outline: 2px solid #3b82f6;
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
        .rbc-toolbar button.rbc-active:hover {
          background-color: #2563eb;
        }
        .rbc-month-view {
          border: 1px solid #e5e7eb;
          border-radius: 8px;
          overflow: hidden;
        }
        .rbc-month-row {
            min-height: 110px;
        }
        .rbc-date-cell {
          padding: 8px;
        }
        .rbc-day-bg {
          cursor: pointer;
        }
        .rbc-day-bg:hover {
          background-color: #f3f4f6;
        }
      `}</style>
        </div>
    );
};

export default AbsenceManagementPage;