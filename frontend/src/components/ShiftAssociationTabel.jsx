import React, { useEffect } from 'react';
import { List, Trash2 } from "lucide-react";


export default function ShiftAssociationTable({ employeeAssociations, handleDeleteTurn }) {
    if (!employeeAssociations || employeeAssociations.length === 0) return null;

    // Estrai giorni unici (ordine naturale)
    const days = [...new Set(employeeAssociations.map(a => a.dayLabel))];

    // Raggruppa per giorno → lista di turni
    const dayMap = {};
    employeeAssociations.forEach(a => {
        if (!dayMap[a.dayLabel]) dayMap[a.dayLabel] = [];
        dayMap[a.dayLabel].push(a);
    });

    // Ordina ciascun gruppo per ora di inizio
    Object.keys(dayMap).forEach(day => {
        dayMap[day].sort((a, b) => {
            const startA = a.shiftTime.split("-")[0].trim();
            const startB = b.shiftTime.split("-")[0].trim();
            return startA.localeCompare(startB);
        });
    });

    // Estrai tutti gli orari distinti per riga
    const allTimes = [
        ...new Set(employeeAssociations.map(a => a.shiftTime)),
    ].sort((a, b) => {
        const startA = a.split("-")[0].trim();
        const startB = b.split("-")[0].trim();
        return startA.localeCompare(startB);
    });

    return (
        <div className="card bg-light border-0 mb-4">
            <div className="card-body">
                <h6 className="fw-semibold mb-3 d-flex align-items-center gap-2">
                    <List size={18} />
                    Turni già associati
                </h6>

                <div className="table-responsive">
                    <table className="table table-bordered align-middle text-center mb-0">
                        <thead className="table-light">
                            <tr>
                                {days.map((day) => (
                                    <th key={day}>{day}</th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {allTimes.map((time) => (
                                <tr key={time}>
                                    {days.map((day) => {
                                        const turni = dayMap[day]?.filter(
                                            (a) => a.shiftTime === time
                                        );
                                        if (!turni || turni.length === 0)
                                            return (
                                                <td key={day}>
                                                    <span className="text-muted">—</span>
                                                </td>
                                            );
                                        return (

                                            <td key={day}>
                                                <div className="d-flex flex-column gap-1">
                                                    {turni.map((t, idx) => (
                                                        <span
                                                            key={idx}
                                                            className="badge bg-primary-subtle border text-primary fw-normal d-flex align-items-center justify-content-between"
                                                            style={{ gap: "4px" }}
                                                        >
                                                            {t.shiftTime}
                                                            <Trash2
                                                                size={16}
                                                                className="cursor-pointer"
                                                                onClick={() => handleDeleteTurn(t.id)}
                                                            />
                                                        </span>
                                                    ))}
                                                </div>
                                            </td>
                                        );
                                    })}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}