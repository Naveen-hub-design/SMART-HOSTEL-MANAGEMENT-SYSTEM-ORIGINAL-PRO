import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { FaStore, FaSearch, FaClipboardList } from 'react-icons/fa';
import ProtectedRoute from '../components/common/ProtectedRoute';
import Navbar from '../components/common/Navbar';
import Sidebar from '../components/common/Sidebar';
import WardenHome from './warden/WardenHome';
import WardenStudents from './warden/WardenStudents';
import WardenRooms from './warden/WardenRooms';
import WardenLeaves from './warden/WardenLeaves';
import WardenComplaints from './warden/WardenComplaints';
import WardenNotices from './warden/WardenNotices';
import WardenReports from './warden/WardenReports';
import WardenProfile from './warden/WardenProfile';

const WardenDashboard = () => (
  <ProtectedRoute allowedRoles={['warden']}>
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <Sidebar />
      <div className="ml-64 mt-[70px] transition-all duration-300">
        <Routes>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<WardenHome />} />
          <Route path="students" element={<WardenStudents />} />
          <Route path="rooms" element={<WardenRooms />} />
          <Route path="leaves" element={<WardenLeaves />} />
          <Route path="complaints" element={<WardenComplaints />} />
          <Route path="notices" element={<WardenNotices />} />
          <Route path="reports" element={<WardenReports />} />
          <Route path="profile" element={<WardenProfile />} />
          <Route path="marketplace" element={<div className="p-6"><div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaStore size={32} className="mb-2" /><p className="text-sm">Marketplace management coming soon</p></div></div>} />
          <Route path="lost-found" element={<div className="p-6"><div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaSearch size={32} className="mb-2" /><p className="text-sm">Lost & Found management coming soon</p></div></div>} />
          <Route path="mess-feedback" element={<div className="p-6"><div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaClipboardList size={32} className="mb-2" /><p className="text-sm">Mess Feedback management coming soon</p></div></div>} />
          <Route path="*" element={<Navigate to="dashboard" replace />} />
        </Routes>
      </div>
    </div>
  </ProtectedRoute>
);

export default WardenDashboard;
