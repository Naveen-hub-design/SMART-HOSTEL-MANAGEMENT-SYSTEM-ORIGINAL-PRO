import React, { createContext, useState, useEffect, useContext, useCallback } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = authService.getCurrentUser();
    if (storedUser) {
      if (storedUser.role) {
        storedUser.role = storedUser.role.toLowerCase().replace(/^role_/, '');
      }
      setUser(storedUser);
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (email, password) => {
    const data = await authService.login(email, password);
    const userData = {
      id: data.userId,
      name: data.name,
      email: data.email,
      role: (data.role || '').toLowerCase().replace(/^role_/, ''),
    };
    setUser(userData);
    setToken(data.token);
    return { ...data, user: userData };
  }, []);

  const register = useCallback(async (userData) => {
    const data = await authService.register(userData);
    const userDataObj = {
      id: data.userId,
      name: data.name,
      email: data.email,
      role: (data.role || '').toLowerCase().replace(/^role_/, ''),
    };
    setUser(userDataObj);
    setToken(data.token);
    return { ...data, user: userDataObj };
  }, []);

  const logout = useCallback(() => {
    authService.logout();
    setUser(null);
    setToken(null);
  }, []);

  const isAuthenticated = !!token && !!user;

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        loading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
