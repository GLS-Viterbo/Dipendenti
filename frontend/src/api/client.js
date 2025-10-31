const BASE_URL = import.meta.env.VITE_API_URL;

let isRefreshing = false;
let refreshPromise = null;

function handleAuthError() {
  localStorage.removeItem('token');
  localStorage.removeItem('userRoles');
  localStorage.removeItem('user');
  
  // Notifica l'AuthContext tramite evento personalizzato
  window.dispatchEvent(new Event('auth-error'));
}

async function refreshToken() {
  if (isRefreshing) {
    return refreshPromise;
  }

  isRefreshing = true;
  refreshPromise = (async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (res.ok) {
        const data = await res.json();
        localStorage.setItem('token', data.token);
        return data.token;
      }
      
      // Se il refresh fallisce, fai logout
      throw new Error('Refresh failed');
    } catch (error) {
      handleAuthError();
      throw error; // Rilancia l'errore per gestirlo nel chiamante
    } finally {
      isRefreshing = false;
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

export async function apiFetch(endpoint, options = {}) {
  const token = localStorage.getItem('token');

  const config = {
    method: options.method || 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
    ...(options.body ? { body: JSON.stringify(options.body) } : {}),
  };

  let res = await fetch(`${BASE_URL}${endpoint}`, config);

  // Se ricevi 401/403 e non è già un retry, prova a fare refresh
  if ((res.status === 401 || res.status === 403) && !options._retry) {
    try {
      const newToken = await refreshToken();
      
      // Riprova la richiesta con il nuovo token
      config.headers.Authorization = `Bearer ${newToken}`;
      options._retry = true; // Previeni loop infiniti
      res = await fetch(`${BASE_URL}${endpoint}`, config);
    } catch (error) {
      // Il refresh è fallito, l'utente è già stato disconnesso
      throw new Error('Session expired');
    }
  }

  // Se dopo il retry riceviamo ancora 401/403, logout definitivo
  if (res.status === 401 || res.status === 403) {
    handleAuthError();
    throw new Error('Session expired');
  }

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    const error = new Error(errorData.message || 'Errore API');
    error.status = res.status;
    throw error;
  }

  if (res.status === 204 || res.headers.get('content-length') === '0') return null;
  return res.json();
}

export async function apiFetchFile(endpoint, options = {}) {
  const token = localStorage.getItem('token');

  const config = {
    method: options.method || 'GET',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
    ...(options.body ? { body: JSON.stringify(options.body) } : {}),
  };

  let res = await fetch(`${BASE_URL}${endpoint}`, config);

  // Gestisci 401/403 con refresh
  if ((res.status === 401 || res.status === 403) && !options._retry) {
    try {
      const newToken = await refreshToken();
      
      config.headers.Authorization = `Bearer ${newToken}`;
      options._retry = true;
      res = await fetch(`${BASE_URL}${endpoint}`, config);
    } catch (error) {
      throw new Error('Session expired');
    }
  }

  if (res.status === 401 || res.status === 403) {
    handleAuthError();
    throw new Error('Session expired');
  }

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    const error = new Error(errorData.message || 'Errore API');
    error.status = res.status;
    throw error;
  }

  if (res.status === 204) return null;

  return res.blob();
}

export async function apiMultipart(endpoint, formData, options = {}) {
  const token = localStorage.getItem('token');

  const config = {
    method: options.method || 'POST',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
    body: formData,
  };

  let res = await fetch(`${BASE_URL}${endpoint}`, config);

  // Gestisci 401/403 con refresh
  if ((res.status === 401 || res.status === 403) && !options._retry) {
    try {
      const newToken = await refreshToken();
      
      config.headers.Authorization = `Bearer ${newToken}`;
      options._retry = true;
      res = await fetch(`${BASE_URL}${endpoint}`, config);
    } catch (error) {
      throw new Error('Session expired');
    }
  }

  if (res.status === 401 || res.status === 403) {
    handleAuthError();
    throw new Error('Session expired');
  }

  if (!res.ok) {
    let errorData = {};
    try {
      errorData = await res.json();
    } catch (e) { }
    const error = new Error(errorData.message || 'Errore API');
    error.status = res.status;
    throw error;
  }

  if (res.status === 204) return null;

  return res.json();
}