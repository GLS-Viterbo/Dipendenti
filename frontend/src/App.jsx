import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import LoginPage from './layouts/LoginPage'
import HomePage from './layouts/HomePage'
import { useAuth } from './utils/AuthContext';

function App() {
  const { user, logout, isAuthenticated, isLoading } = useAuth();

  // Mostra un loader durante il controllo iniziale dell'autenticazione
  if (isLoading) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Caricamento...</span>
        </div>
      </div>
    );
  }

  return (
    <>
      {!isAuthenticated() ? (
        <LoginPage />
      ) : (
        <HomePage
          onLogout={logout}
          user={user}
        />
      )}

      <ToastContainer position="top-right" autoClose={2000} />
    </>
  );
}

export default App;