import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../services/auth';

const Sidebar: React.FC = () => {
  const { user } = useAuth();
  const name = user?.email?.split('@')[0] || 'User';
  const role = user?.role ? user.role.charAt(0).toUpperCase() + user.role.slice(1) : '';

  let navLinks: { to: string; label: string }[] = [];

  if (!user) {
    // Not authenticated; minimal links
    navLinks = [{ to: '/login', label: 'Login' }];
  } else if (user.role === 'employee') {
    navLinks = [
      { to: '/my-dashboard', label: 'My Dashboard' },
      { to: '/leaderboard', label: 'Leaderboard' },
      { to: '/recognitions', label: 'Recognitions' },
    ];
  } else if (user.role === 'teamlead') {
    navLinks = [
      { to: '/my-dashboard', label: 'My Dashboard' },
      { to: '/team-dashboard', label: 'Team Dashboard' },
      { to: '/leaderboard', label: 'Leaderboard' },
      { to: '/recognitions', label: 'Recognitions' },
    ];
  } else if (user.role === 'manager') {
    navLinks = [
      { to: '/my-dashboard', label: 'My Dashboard' },
      { to: '/unit-dashboard', label: 'Unit Dashboard' },
      { to: '/leaderboard', label: 'Leaderboard' },
      { to: '/recognitions', label: 'Recognitions' },
      { to: '/employees', label: 'Employees' },
      { to: '/recognition-types', label: 'Recognition Types' },
    ];
  } else if (user.role === 'admin') {
    // Admin sees the generic Dashboard and all management pages
    navLinks = [
      { to: '/', label: 'Dashboard' },
      { to: '/leaderboard', label: 'Leaderboard' },
      { to: '/employees', label: 'Employees' },
      { to: '/recognitions', label: 'Recognitions' },
      { to: '/recognition-types', label: 'Recognition Types' },
    ];
  }

  return (
    <div style={{ width: 180, minHeight: '100vh', background: '#8da1bd', color: '#fff', display: 'flex', flexDirection: 'column', alignItems: 'flex-start', padding: '24px 0 0 16px', position: 'fixed', left: 0, top: 0, zIndex: 100 }}>
      <div style={{ marginBottom: 28, width: '100%' }}>
        <div style={{ fontWeight: 700, fontSize: '1.05rem', marginBottom: 2 }}>{name}</div>
        <div style={{ fontSize: '0.95rem', color: '#e0e0e0' }}>{role}</div>
        <hr style={{ border: 0, borderTop: '1px solid #bfcbe3', margin: '16px 0 0 0', width: '90%' }} />
      </div>
      <h3 style={{ marginBottom: 24, fontSize: '1.1rem', fontWeight: 700 }}>Navigation</h3>
      <nav style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        {navLinks.map(link => (
          <Link key={link.to} to={link.to} style={{ color: '#fff', textDecoration: 'none', fontSize: '0.95rem', fontWeight: 500 }}>{link.label}</Link>
        ))}
      </nav>
    </div>
  );
};

export default Sidebar;
