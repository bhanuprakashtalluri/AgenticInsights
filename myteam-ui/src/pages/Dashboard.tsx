import React from 'react';
import Sidebar from '../components/Sidebar';

const SIDEBAR_WIDTH = 180;

const Dashboard: React.FC = () => {
  return (
    <div style={{ width: '100vw', minHeight: '100vh', display: 'flex', background: '#f5f7fa' }}>
      <div style={{ width: SIDEBAR_WIDTH, minWidth: SIDEBAR_WIDTH, height: '100vh', position: 'relative', zIndex: 2 }}>
        <Sidebar />
      </div>
      <div style={{ flex: 1, padding: 32, display: 'flex', flexDirection: 'column', alignItems: 'center', background: '#f5f7fa', borderRadius: 12, boxShadow: '0 4px 16px #e0e0e0', minHeight: '100vh' }}>
        <h2 style={{ textAlign: 'center', marginBottom: 18, fontSize: '1.3rem', fontWeight: 600 }}>Dashboard</h2>
        <p style={{ fontSize: '0.7rem' }}>Welcome to the dashboard. Here you can view summary information and quick links.</p>
        <ul style={{ fontSize: '0.7rem', width: '100%', padding: 0, margin: 0, listStyle: 'none' }}>
          <li style={{ padding: '6px 0', borderBottom: '1px solid #e0e0e0', color: '#333' }}>Employees</li>
          <li style={{ padding: '6px 0', borderBottom: '1px solid #e0e0e0', color: '#333' }}>Recognitions</li>
          <li style={{ padding: '6px 0', borderBottom: '1px solid #e0e0e0', color: '#333' }}>Metrics & Graphs</li>
          <li style={{ padding: '6px 0', borderBottom: '1px solid #e0e0e0', color: '#333' }}>Leaderboard</li>
        </ul>
      </div>
    </div>
  );
};

export default Dashboard;
