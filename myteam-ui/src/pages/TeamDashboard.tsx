import React from 'react';
import Dashboard from './Dashboard';

const TeamDashboard: React.FC = () => {
  // Reuse the original Dashboard component, but pass a prop to indicate team context
  return <Dashboard context="team" />;
};

export default TeamDashboard;
