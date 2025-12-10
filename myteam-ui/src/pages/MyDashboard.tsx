import React from 'react';
import Dashboard from './Dashboard';

const MyDashboard: React.FC = () => {
  // Reuse the original Dashboard component, but pass a prop to indicate self context
  return <Dashboard context="self" />;
};

export default MyDashboard;
