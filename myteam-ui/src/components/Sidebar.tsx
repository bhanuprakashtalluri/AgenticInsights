import React from 'react';
import { Link } from 'react-router-dom';

const Sidebar: React.FC = () => (
  <div style={{ width: 180, minHeight: '100vh', background: '#8da1bd', color: '#fff', display: 'flex', flexDirection: 'column', alignItems: 'flex-start', padding: '24px 0 0 16px', position: 'fixed', left: 0, top: 0, zIndex: 100 }}>
    <h3 style={{ marginBottom: 24, fontSize: '1.1rem', fontWeight: 700 }}>Navigation</h3>
    <nav style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <Link to="/" style={{ color: '#fff', textDecoration: 'none', fontSize: '0.95rem', fontWeight: 500 }}>Dashboard</Link>
        <Link to="/leaderboard" style={{ color: '#fff', textDecoration: 'none', fontSize: '0.95rem', fontWeight: 500 }}>Leaderboard</Link>
        <Link to="/employees" style={{ color: '#fff', textDecoration: 'none', fontSize: '0.95rem', fontWeight: 500 }}>Employees</Link>
      <Link to="/recognitions" style={{ color: '#fff', textDecoration: 'none', fontSize: '0.95rem', fontWeight: 500 }}>Recognitions</Link>
      <Link to="/recognition-types" style={{ color: '#fff', textDecoration: 'none', fontSize: '0.95rem', fontWeight: 500 }}>Recognition Types</Link>
    </nav>
  </div>
);

export default Sidebar;
