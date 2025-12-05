import React from 'react';
import { Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import NotFound from './pages/NotFound';
import RecognitionsPage from './pages/RecognitionsPage';
import EmployeesPage from './pages/EmployeesPage';
import LeaderboardPage from './pages/LeaderboardPage';
import RecognitionTypesPage from './pages/RecognitionTypesPage';

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<Dashboard />} />
      <Route path="/recognitions" element={<RecognitionsPage />} />
      <Route path="/employees" element={<EmployeesPage />} />
      <Route path="/leaderboard" element={<LeaderboardPage />} />
      <Route path="/recognition-types" element={<RecognitionTypesPage />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

export default App;