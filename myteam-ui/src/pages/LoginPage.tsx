import React, { useState } from 'react';
import axios from 'axios';
import { useAuth } from '../services/auth';
import { Link, useNavigate } from 'react-router-dom';

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { setUser } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await axios.post('/api/auth/login', { username: email, password });
      // Fetch user info after login
      const me = await axios.get('/api/auth/me');
      setUser({
        email: me.data.email,
        role: me.data.role,
        isAuthenticated: true,
      });
      navigate('/');
    } catch (err: any) {
      let msg = 'Invalid email or password.';
      if (err.response?.data) {
        if (typeof err.response.data === 'string') msg = err.response.data;
        else if (err.response.data.message) msg = err.response.data.message;
        else msg = JSON.stringify(err.response.data);
      } else if (err.message) {
        msg = err.message;
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ width: '100vw', minHeight: '100vh', display: 'block' }}>
      <div style={{ maxWidth: 400, width: '100%', padding: 24, background: '#fff', borderRadius: 12, boxShadow: '0 4px 16px #e0e0e0', display: 'flex', flexDirection: 'column', alignItems: 'center', margin: '32px auto 0 auto' }}>
        <h2 style={{ textAlign: 'center', marginBottom: 18, fontSize: '1.3rem', fontWeight: 600 }}>Login</h2>
        <form onSubmit={handleSubmit} style={{ width: '100%' }}>
          <div style={{ marginBottom: 16 }}>
            <label>Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required style={{ width: '100%', padding: 8, marginTop: 4 }} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label>Password</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required style={{ width: '100%', padding: 8, marginTop: 4 }} />
          </div>
          {error && <div style={{ color: 'red', marginBottom: 12 }}>{error}</div>}
          <button type="submit" disabled={loading} style={{ width: '100%', padding: 10, background: '#1976d2', color: '#fff', border: 'none', borderRadius: 6, fontWeight: 600, fontSize: '1rem', cursor: 'pointer' }}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
        <div style={{ marginTop: 16, textAlign: 'center' }}>
          <Link to="/forgot-password" style={{ color: '#1976d2', textDecoration: 'underline', fontSize: '0.9rem' }}>
            Forgot Password?
          </Link>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
