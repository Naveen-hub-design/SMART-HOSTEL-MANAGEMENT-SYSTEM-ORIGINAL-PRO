import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from './LoadingSpinner';

const ProtectedRoute = ({ allowedRoles, children }) => {
  const { isAuthenticated, user, loading } = useAuth();

  if (loading) {
    return <LoadingSpinner text="Verifying access..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const userRole = (user?.role || '').toLowerCase().replace(/^role_/, '');
  const normalizedAllowed = allowedRoles?.map((r) => r.toLowerCase().replace(/^role_/, ''));

  if (allowedRoles && (!userRole || !normalizedAllowed.includes(userRole))) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default ProtectedRoute;
