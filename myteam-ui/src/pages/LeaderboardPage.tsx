import React from 'react';
import Sidebar from '../components/Sidebar';
import LeaderboardManagement from '../features/LeaderboardManagement';

const SIDEBAR_WIDTH = 180;

const LeaderboardPage: React.FC = () => (
  <div style={{ width: '100vw', minHeight: '100vh', display: 'flex', background: '#f5f7fa' }}>
    <div style={{ width: SIDEBAR_WIDTH, minWidth: SIDEBAR_WIDTH, height: '100vh', position: 'relative', zIndex: 2 }}>
      <Sidebar />
    </div>
    <div style={{ flex: 1, padding: 32, display: 'flex', flexDirection: 'column', alignItems: 'center', background: '#f5f7fa', borderRadius: 12, boxShadow: '0 4px 16px #e0e0e0', minHeight: '100vh' }}>
      <LeaderboardManagement />
    </div>
  </div>
);

export default LeaderboardPage;
