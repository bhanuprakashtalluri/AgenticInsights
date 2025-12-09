import React from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import NotFound from './pages/NotFound';
import RecognitionsPage from './pages/RecognitionsPage';
import EmployeesPage from './pages/EmployeesPage';
import LeaderboardPage from './pages/LeaderboardPage';
import RecognitionTypesPage from './pages/RecognitionTypesPage';
import PasswordUpdatePage from './pages/PasswordUpdatePage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import { useAuth, AuthProvider } from './services/auth';

const ProtectedRoute: React.FC<{ children: React.ReactNode; roles?: string[] }> = ({ children, roles }) => {
  const { user } = useAuth();
  const location = useLocation();
  if (!user || !user.isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/notfound" replace />;
  }
  return <>{children}</>;
};

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/update-password" element={<PasswordUpdatePage />} />
      <Route path="/" element={
        <ProtectedRoute>
          <Dashboard />
        </ProtectedRoute>
      } />
      <Route path="/recognitions" element={
        <ProtectedRoute roles={["admin", "manager", "teamlead"]}>
          <RecognitionsPage />
        </ProtectedRoute>
      } />
      <Route path="/employees" element={
        <ProtectedRoute roles={["admin", "manager"]}>
          <EmployeesPage />
        </ProtectedRoute>
      } />
      <Route path="/leaderboard" element={
        <ProtectedRoute roles={["admin", "manager", "teamlead", "employee"]}>
          <LeaderboardPage />
        </ProtectedRoute>
      } />
      <Route path="/recognition-types" element={
        <ProtectedRoute roles={["admin"]}>
          <RecognitionTypesPage />
        </ProtectedRoute>
      } />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}

export default App;