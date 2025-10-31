import React, { useState } from 'react';
import { Users } from 'lucide-react';
import { apiFetch } from '../api/client';
import { toast } from 'react-toastify';
import { useAuth } from '../utils/AuthContext';

const LoginPage = () => {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    
    try {
      const response = await apiFetch('/api/auth/login', {
        method: 'POST',
        body: { username, password }
      });

      // Il login aggiorna automaticamente il contesto
      login(response);
      
      toast.success("Benvenuto " + response.user.username);
    } catch (error) {
      console.error(error);
      toast.error("Credenziali sbagliate");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
      <div className="container">
        <div className="row justify-content-center">
          <div className="col-md-5 col-lg-4">
            <div className="card shadow-lg border-0 rounded-4">
              <div className="card-body p-5">
                <div className="text-center mb-4">
                  <div className="bg-primary bg-opacity-10 rounded-circle d-inline-flex p-3 mb-3">
                    <Users size={40} className="text-primary" />
                  </div>
                  <h3 className="fw-bold mb-1">Gestionale Dipendenti</h3>
                  <p className="text-muted small">Portale Amministrazione</p>
                </div>

                <form onSubmit={handleSubmit}>
                  <div className="mb-3">
                    <label htmlFor="username" className="form-label fw-semibold">Username</label>
                    <input
                      type="text"
                      className="form-control form-control-lg"
                      id="username"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      placeholder="Inserisci username"
                      required
                      disabled={isLoading}
                    />
                  </div>

                  <div className="mb-4">
                    <label htmlFor="password" className="form-label fw-semibold">Password</label>
                    <input
                      type="password"
                      className="form-control form-control-lg"
                      id="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Inserisci password"
                      required
                      disabled={isLoading}
                    />
                  </div>

                  <button 
                    type="submit" 
                    className="btn btn-primary btn-lg w-100 fw-semibold"
                    disabled={isLoading}
                  >
                    {isLoading ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                        Accesso in corso...
                      </>
                    ) : (
                      'Entra'
                    )}
                  </button>
                </form>

                <div className="text-center mt-4">
                  <small className="text-muted">© 2024 Sistema Gestionale Aziendale</small>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;