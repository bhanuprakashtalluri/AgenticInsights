import React from 'react';
import Dashboard from './Dashboard';

const UnitDashboard: React.FC = () => {
  // Reuse the original Dashboard component, but pass a prop to indicate unit context
  return <Dashboard context="unit" />;
};

export default UnitDashboard;
