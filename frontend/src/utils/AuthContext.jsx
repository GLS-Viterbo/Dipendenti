import React, { createContext, useContext, useState, useEffect } from 'react';
import { toast } from 'react-toastify';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [userRoles, setUserRoles] = useState([]);
    const [token, setToken] = useState(null);
    const [user, setUser] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        // Carica i dati di autenticazione salvati al mount
        const storedToken = localStorage.getItem('token');
        const storedRoles = localStorage.getItem('userRoles');
        const storedUser = localStorage.getItem('user');
        
        if (storedToken && storedRoles && storedUser) {
            setToken(storedToken);
            setUserRoles(JSON.parse(storedRoles));
            setUser(JSON.parse(storedUser));
        }
        setIsLoading(false);
    }, []);

    // Listener per errori di autenticazione
    useEffect(() => {
        const handleAuthError = () => {
            // Pulisci lo stato quando viene rilevato un errore di autenticazione
            setToken(null);
            setUserRoles([]);
            setUser(null);
            toast.error('Sessione scaduta. Effettua nuovamente il login.')
        };

        window.addEventListener('auth-error', handleAuthError);

        return () => {
            window.removeEventListener('auth-error', handleAuthError);
        };
    }, []);

    const login = (apiResponse) => {
        const { token, roles, user } = apiResponse;

        // Salva in localStorage
        localStorage.setItem('token', token);
        localStorage.setItem('userRoles', JSON.stringify(roles));
        localStorage.setItem('user', JSON.stringify(user));
        
        // Aggiorna lo stato
        setToken(token);
        setUserRoles(roles);
        setUser(user);
    };

    const logout = () => {
        // Rimuovi da localStorage
        localStorage.removeItem('token');
        localStorage.removeItem('userRoles');
        localStorage.removeItem('user');
        
        // Reset dello stato
        setToken(null);
        setUserRoles([]);
        setUser(null);
    };

    const hasRole = (requiredRoles) => {
        // requiredRoles può essere una singola stringa o un array di ruoli richiesti
        const rolesToCheck = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles];
        
        return userRoles.some(role => rolesToCheck.includes(role));
    };

    const isAuthenticated = () => {
        return !!token;
    };

    const value = { 
        token, 
        userRoles, 
        user,
        isLoading,
        login, 
        logout, 
        hasRole,
        isAuthenticated
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth deve essere usato all\'interno di un AuthProvider');
    }
    return context;
};