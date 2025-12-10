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
      <div style={{ flex: 1, padding: 24, display: 'flex', flexDirection: 'column', alignItems: 'center', background: '#f5f7fa', minHeight: '100vh' }}>
        <RecognitionManagement showTable={true} />
      </div>
    </div>
  );
};

export default RecognitionsPage;
