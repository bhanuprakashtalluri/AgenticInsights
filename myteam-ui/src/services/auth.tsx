import React, { createContext, useContext, useEffect, useState } from 'react';
import axios from 'axios';

export type AuthUser = {
  email: string;
  role: string;
  isAuthenticated: boolean;
  unitId?: string; // Add this if your backend provides it
  teamleadId?: string; // Add this if your backend provides it
};

const AuthContext = createContext<{
  user: AuthUser | null;
  setUser: (user: AuthUser | null) => void;
  logout: () => void;
}>({ user: null, setUser: () => {}, logout: () => {} });

export const useAuth = () => useContext(AuthContext);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    // Try to fetch user info on mount (session or token based)
    axios.get('/api/auth/me').then(res => {
      setUser({
        email: res.data.email,
        role: (res.data.role || '').toLowerCase(), // normalize to lowercase
        isAuthenticated: true,
        unitId: res.data.unitId, // if available
        teamleadId: res.data.teamleadId, // if available
      });
    }).catch(() => {
      setUser(null);
    });
  }, []);

  const logout = () => {
    setUser(null);
    // Optionally call backend to clear session
    window.location.href = '/login';
  };

  return (
    <AuthContext.Provider value={{ user, setUser, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
