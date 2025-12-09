import React from 'react';
import RecognitionManagement from '../features/RecognitionManagement';
import Sidebar from '../components/Sidebar';

const SIDEBAR_WIDTH = 180;

const RecognitionsPage: React.FC = () => {
  return (
    <div style={{ width: '100vw', minHeight: '100vh', display: 'flex', background: '#f5f7fa' }}>
      <div style={{ width: SIDEBAR_WIDTH, minWidth: SIDEBAR_WIDTH, height: '100vh', position: 'relative', zIndex: 2 }}>
        <Sidebar />
      </div>
      <div style={{ flex: 1, padding: 32, display: 'flex', flexDirection: 'column', alignItems: 'center', background: '#f5f7fa', borderRadius: 12, boxShadow: '0 4px 16px #e0e0e0', minHeight: '100vh' }}>
        <h2 style={{ textAlign: 'center', marginBottom: 18, fontSize: '1.3rem', fontWeight: 600 }}>Recognitions</h2>
        <RecognitionManagement />
      </div>
    </div>
  );
};

export default RecognitionsPage;
