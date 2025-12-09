import React from 'react';

const NotFound: React.FC = () => (
  <div style={{ width: '100vw', minHeight: '100vh', display: 'block' }}>
    <div style={{ maxWidth: 850, width: '100%', padding: 10, background: '#f5f7fa', borderRadius: 12, boxShadow: '0 4px 16px #e0e0e0', display: 'flex', flexDirection: 'column', alignItems: 'center', margin: '32px 0 0 32px' }}>
      <h2 style={{ textAlign: 'center', marginBottom: 18, fontSize: '1.3rem', fontWeight: 600, color: '#d32f2f' }}>404</h2>
      <div style={{ fontSize: '0.7rem', color: '#d32f2f', marginBottom: 8 }}>Page Not Found</div>
    </div>
  </div>
);

export default NotFound;
