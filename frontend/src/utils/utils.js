export const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString('it-IT');
};

export const formatTime = (timestamp) => {
  const date = new Date(formatOffsetDateTime(timestamp));
  return date.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
};

export const getDayName = (dayNum) => {
  const days = ['Domenica', 'Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato'];
  return days[dayNum];
};

export const getTodayDate = () => {
  return new Date().toISOString().split('T')[0]
}

export const formatOffsetDateTime = (offsetDateTime) => {
  const formatted = offsetDateTime.replace(/([0-9]{2}:[0-9]{2}:[0-9]{2})\.\d+\+\d{2}:\d{2}/, '$1');
  return formatted
}

export function getLocalOffsetDateTime() {
  const date = new Date();

  // offset in minuti → ore e minuti
  const offsetMinutes = date.getTimezoneOffset();
  const sign = offsetMinutes <= 0 ? "+" : "-";
  const absOffset = Math.abs(offsetMinutes);
  const hours = String(Math.floor(absOffset / 60)).padStart(2, "0");
  const minutes = String(absOffset % 60).padStart(2, "0");

  // data e ora locale in formato ISO
  const local = new Date(date.getTime() - offsetMinutes * 60_000)
    .toISOString()
    .replace("Z", `${sign}${hours}:${minutes}`);

  return local;
}

export function getOffsetDateTime(date) {
  const offsetMinutes = date.getTimezoneOffset();
  const sign = offsetMinutes <= 0 ? "+" : "-";
  const absOffset = Math.abs(offsetMinutes);
  const hours = String(Math.floor(absOffset / 60)).padStart(2, "0");
  const minutes = String(absOffset % 60).padStart(2, "0");

  // costruiamo manualmente la parte ISO locale
  return date.toISOString().replace("Z", "+02:00");
}


// Restituisce il primo giorno del mese corrente in formato YYYY-MM-DD
export const getStartOfMonth = () => {
  const now = new Date();
  const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
  return firstDay.toISOString().split('T')[0];
};

export const get3MonthsFromNow = () => {
  const now = new Date();
  const firstDay = new Date(now.getFullYear(), now.getMonth() + 3, 1);
  return firstDay.toISOString().split('T')[0];
};

export const getDateIn15Days = () => {
  const now = new Date();
  const tomorrow = new Date(now);
  tomorrow.setDate(now.getDate() + 1); // domani
  const in15Days = new Date(tomorrow);
  in15Days.setDate(tomorrow.getDate() + 14); // 14 giorni dopo domani = 15 giorni totali
  return in15Days.toISOString().split('T')[0];
};


// Restituisce l'ultimo giorno del mese corrente in formato YYYY-MM-DD
export const getEndOfMonth = () => {
  const now = new Date();
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0); // il giorno 0 del mese successivo è l'ultimo del mese corrente
  return lastDay.toISOString().split('T')[0];
};

export const getCurrentYearMonth = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0"); // i mesi partono da 0
  return `${year}-${month}`;
};

export const formatDateYYYYMMDD = (timestamp) => {
    const date = new Date(timestamp);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};

export const getTomorrow = () => {
    const today = new Date();
    today.setDate(today.getDate() + 1);
    return formatDateYYYYMMDD(today);
};
