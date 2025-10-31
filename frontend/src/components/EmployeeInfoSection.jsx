import React, { useEffect, useState } from 'react';
import {
    User, Mail, Phone, MapPin, Calendar, Hash, CreditCard,
    Clock, Briefcase, FileText, CheckCircle, XCircle,
    TrendingUp, Award, AlertCircle
} from 'lucide-react';
import { hasAnomalies } from '../api/access';
import { getEmployeeStatsForYearMonth } from '../api/stats';

const EmployeeInfoSection = ({ employee }) => {

    const [employeeData, setEmployeeData] = useState({})
    const [anomalies, setAnomalies] = useState(false)

    useEffect(() => {
        async function fetchData() {

            const anomaliesResponse = await hasAnomalies(employee.id)
            const statsResponse = await getEmployeeStatsForYearMonth(employee.id)

            setEmployeeData(
                {
                    // Dati anagrafici
                    name: employee.name,
                    surname: employee.surname,
                    taxCode: employee.taxCode,
                    birthday: employee.birthday,
                    address: employee.address,
                    city: employee.city,
                    email: employee.email,
                    phone: employee.phone,
                    note: employee.note,

                    // Contratto attuale
                    currentContract: employee.contracts.find(c => c.valid),

                    // Statistiche
                    stats: statsResponse,
                }
            )
            setAnomalies(anomaliesResponse.hasAnomalies)
        }

        fetchData()
    }, [employee])


    // Calcola età
    const calculateAge = (birthday) => {
        const today = new Date();
        const birthDate = new Date(birthday);
        let age = today.getFullYear() - birthDate.getFullYear();
        const monthDiff = today.getMonth() - birthDate.getMonth();
        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
            age--;
        }
        return age;
    };

    // Calcola anzianità
    const calculateSeniority = (startDate) => {
        const today = new Date();
        const start = new Date(startDate);
        const years = today.getFullYear() - start.getFullYear();
        const months = today.getMonth() - start.getMonth();

        let totalMonths = years * 12 + months;
        if (today.getDate() < start.getDate()) {
            totalMonths--;
        }

        const finalYears = Math.floor(totalMonths / 12);
        const finalMonths = totalMonths % 12;

        if (finalYears === 0) {
            return `${finalMonths} ${finalMonths === 1 ? 'mese' : 'mesi'}`;
        } else if (finalMonths === 0) {
            return `${finalYears} ${finalYears === 1 ? 'anno' : 'anni'}`;
        } else {
            return `${finalYears} ${finalYears === 1 ? 'anno' : 'anni'} e ${finalMonths} ${finalMonths === 1 ? 'mese' : 'mesi'}`;
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('it-IT', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    };

    return (
        <div className="row g-4">
            {/* Colonna Sinistra */}
            <div className="col-lg-6">
                {/* Dati Anagrafici */}
                <div className="card border-0 shadow-sm mb-4">
                    <div className="card-header bg-white border-bottom">
                        <h6 className="mb-0 fw-semibold d-flex align-items-center gap-2">
                            <User size={18} className="text-primary" />
                            Dati Anagrafici
                        </h6>
                    </div>
                    <div className="card-body">
                        <div className="row g-3">
                            <div className="col-md-6">
                                <label className="text-muted small mb-1">Nome</label>
                                <div className="fw-semibold">{employeeData.name}</div>
                            </div>
                            <div className="col-md-6">
                                <label className="text-muted small mb-1">Cognome</label>
                                <div className="fw-semibold">{employeeData.surname}</div>
                            </div>
                            <div className="col-md-6">
                                <label className="text-muted small mb-1 d-flex align-items-center gap-1">
                                    <Hash size={14} />
                                    Codice Fiscale
                                </label>
                                <div className="font-monospace fw-semibold">{employeeData.taxCode}</div>
                            </div>
                            <div className="col-md-6">
                                <label className="text-muted small mb-1 d-flex align-items-center gap-1">
                                    <Calendar size={14} />
                                    Data di Nascita
                                </label>
                                <div className="fw-semibold">
                                    {formatDate(employeeData.birthday)}
                                    <span className="text-muted ms-2">({calculateAge(employeeData.birthday)} anni)</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Contatti */}
                <div className="card border-0 shadow-sm mb-4">
                    <div className="card-header bg-white border-bottom">
                        <h6 className="mb-0 fw-semibold d-flex align-items-center gap-2">
                            <Mail size={18} className="text-primary" />
                            Informazioni di Contatto
                        </h6>
                    </div>
                    <div className="card-body">
                        <div className="mb-3">
                            <label className="text-muted small mb-1 d-flex align-items-center gap-1">
                                <Mail size={14} />
                                Email
                            </label>
                            <div className="d-flex align-items-center gap-2">
                                <span className="fw-semibold">{employeeData.email}</span>
                                <button className="btn btn-sm btn-outline-primary" title="Invia email">
                                    <Mail size={14} />
                                </button>
                            </div>
                        </div>
                        <div className="mb-3">
                            <label className="text-muted small mb-1 d-flex align-items-center gap-1">
                                <Phone size={14} />
                                Telefono
                            </label>
                            <div className="d-flex align-items-center gap-2">
                                <span className="fw-semibold">{employeeData.phone}</span>
                                <button className="btn btn-sm btn-outline-primary" title="Chiama">
                                    <Phone size={14} />
                                </button>
                            </div>
                        </div>
                        <div>
                            <label className="text-muted small mb-1 d-flex align-items-center gap-1">
                                <MapPin size={14} />
                                Indirizzo
                            </label>
                            <div className="fw-semibold">{employeeData.address}</div>
                            <div className="text-muted small">{employeeData.city}</div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Colonna Destra */}
            <div className="col-lg-6">
                {/* Informazioni Contrattuali */}
                <div className="card border-0 shadow-sm mb-4">
                    <div className="card-header bg-white border-bottom">
                        <h6 className="mb-0 fw-semibold d-flex align-items-center gap-2">
                            <Briefcase size={18} className="text-primary" />
                            Informazioni Contrattuali
                        </h6>
                    </div>
                    <div className="card-body">
                        {!employeeData.currentContract ? (
                            <div className="p-4 bg-red-100 border border-red-300 rounded-lg text-center">
                                <div className="flex flex-col items-center">
                                    <XCircle size={48} className="text-red-500 mb-3" />
                                    <h3 className="text-xl font-bold text-red-700 mb-1">Nessun contratto attivo</h3>

                                </div>
                            </div>
                        ) : (
                            <>
                                <div className="row g-3">
                                    <div className="col-md-6">
                                        <label className="text-muted small mb-1">Data Assunzione</label>
                                        <div className="fw-semibold">{formatDate(employeeData?.currentContract?.startDate)}</div>
                                    </div>
                                    <div className="col-md-6">
                                        <label className="text-muted small mb-1">Anzianità di Servizio</label>
                                        <div className="fw-semibold d-flex align-items-center gap-1">
                                            <Award size={16} className="text-warning" />
                                            {calculateSeniority(employeeData?.currentContract?.startDate)}
                                        </div>
                                    </div>
                                    <div className="col-md-6">
                                        <label className="text-muted small mb-1">Tipo Contratto</label>
                                        <div className="fw-semibold">
                                            {employeeData?.currentContract?.endDate ? (
                                                <>
                                                    Tempo Determinato
                                                    <small className="d-block text-muted">Scadenza: {formatDate(employeeData?.currentContract?.endDate)}</small>
                                                </>
                                            ) : (
                                                'Tempo Indeterminato'
                                            )}
                                        </div>
                                    </div>
                                    <div className="col-md-6">
                                        <label className="text-muted small mb-1">Ore Mensili Previste</label>
                                        <div className="fw-semibold d-flex align-items-center gap-1">
                                            <Clock size={16} className="text-primary" />
                                            {employeeData?.currentContract?.monthlyWorkingHours} ore
                                        </div>
                                    </div>
                                </div>
                            </>
                        )
                        }
                    </div >
                </div >

                {/* Statistiche del Mese */}
                < div className="card border-0 shadow-sm mb-4" >
                    <div className="card-header bg-white border-bottom">
                        <h6 className="mb-0 fw-semibold d-flex align-items-center gap-2">
                            <TrendingUp size={18} className="text-primary" />
                            Statistiche Correnti
                        </h6>
                    </div>
                    {
                        anomalies ? (
                            <div className="p-4 bg-red-100 border border-red-300 rounded-lg text-center">
                                <div className="flex flex-col items-center">
                                    <XCircle size={48} className="text-red-500 mb-3" />
                                    <h3 className="text-xl font-bold text-red-700 mb-1">Anomalie rilevate</h3>
                                    <p className="text-red-600">
                                        Non è possibile calcolare le statistiche perché sono state rilevate anomalie nei dati.
                                    </p>
                                </div>
                            </div>
                        ) : (
                            <div className="card-body">
                                <div className="row g-3">
                                    <div className="col-md-4">
                                        <div className="text-center p-3 bg-light rounded">
                                            <Clock size={24} className="text-primary mb-2" />
                                            <div className="h4 fw-bold mb-0">{employeeData?.stats?.hoursWorkedThisMonth}</div>
                                            <small className="text-muted">Ore questo mese</small>
                                        </div>
                                    </div>
                                    <div className="col-md-4">
                                        <div className="text-center p-3 bg-light rounded">
                                            <Calendar size={24} className="text-warning mb-2" />
                                            <div className="h4 fw-bold mb-0">{employeeData?.stats?.absencesThisYear}</div>
                                            <small className="text-muted">Assenze quest'anno</small>
                                        </div>
                                    </div>
                                    <div className="col-md-4">
                                        <div className="text-center p-3 bg-light rounded">
                                            <CheckCircle size={24} className="text-success mb-2" />
                                            <div className="h4 fw-bold mb-0">{employeeData?.stats?.attendanceRate}%</div>
                                            <small className="text-muted">Tasso presenza</small>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )
                    }
                </div >

                {/* Note */}
                {
                    employeeData?.note && (
                        <div className="card border-0 shadow-sm">
                            <div className="card-header bg-white border-bottom">
                                <h6 className="mb-0 fw-semibold d-flex align-items-center gap-2">
                                    <FileText size={18} className="text-primary" />
                                    Note
                                </h6>
                            </div>
                            <div className="card-body">
                                <p className="mb-0 text-muted">{employeeData?.note}</p>
                            </div>
                        </div>
                    )
                }
            </div >
        </div >
    );
};

export default EmployeeInfoSection;