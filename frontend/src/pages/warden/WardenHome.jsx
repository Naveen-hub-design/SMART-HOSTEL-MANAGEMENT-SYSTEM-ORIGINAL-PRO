import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import wardenService from '../../services/wardenService';
import leaveService from '../../services/leaveService';
import complaintService from '../../services/complaintService';
import noticeService from '../../services/noticeService';
import {
  FaUsers, FaDoorOpen, FaCalendarAlt, FaExclamationTriangle, FaBullhorn,
} from 'react-icons/fa';

const WardenHome = () => {
  const [stats, setStats] = useState(null);
  const [pendingLeaves, setPendingLeaves] = useState([]);
  const [pendingComplaints, setPendingComplaints] = useState([]);
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [dash, leaves, comps, nots] = await Promise.all([
          wardenService.getDashboard(),
          leaveService.getPendingLeaves(),
          complaintService.getAllComplaints(),
          noticeService.getAllNotices(),
        ]);
        setStats(dash);
        setPendingLeaves(Array.isArray(leaves) ? leaves : []);
        setPendingComplaints(Array.isArray(comps) ? comps.filter(c => c.status === 'PENDING' || c.status === 'OPEN') : []);
        setNotices(Array.isArray(nots) ? nots.slice(0, 3) : []);
      } catch (err) {
        toast.error('Failed to load dashboard');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="w-10 h-10 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
      </div>
    );
  }

  const statCards = [
    { icon: <FaUsers />, label: 'Total Students', value: stats?.totalStudents || 0, color: '#1a237e' },
    { icon: <FaDoorOpen />, label: 'Total Rooms', value: stats?.totalRooms || 0, color: '#0d47a1' },
    { icon: <FaCalendarAlt />, label: 'Pending Leaves', value: pendingLeaves.length, color: '#f57f17' },
    { icon: <FaExclamationTriangle />, label: 'Open Complaints', value: pendingComplaints.length, color: '#c62828' },
  ];

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Warden Dashboard</h1>
        <p className="text-sm text-gray-500">Oversee hostel operations and students</p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
        {statCards.map((c, i) => (
          <div key={i} className="bg-white rounded-xl shadow-sm p-6" style={{ borderTop: `4px solid ${c.color}` }}>
            <div className="flex items-center gap-4">
              <div className="text-2xl" style={{ color: c.color }}>{c.icon}</div>
              <div>
                <p className="text-sm text-gray-500">{c.label}</p>
                <h3 className="text-lg font-semibold text-gray-900">{c.value}</h3>
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaCalendarAlt /> Recent Leave Requests</h3>
          {pendingLeaves.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-gray-400">
              <FaCalendarAlt size={32} className="mb-2" />
              <p className="text-sm">No pending leaves</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100">
                    <th className="text-left py-2 text-gray-500 font-medium">Student</th>
                    <th className="text-left py-2 text-gray-500 font-medium">Dates</th>
                    <th className="text-left py-2 text-gray-500 font-medium">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingLeaves.slice(0, 5).map((l, i) => (
                    <tr key={i} className="border-b border-gray-50">
                      <td className="py-2 text-gray-700">{l.student?.name || l.studentName}</td>
                      <td className="py-2 text-gray-700">{l.fromDate?.slice(0, 10)} - {l.toDate?.slice(0, 10)}</td>
                      <td className="py-2">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          l.status === 'APPROVED' ? 'bg-green-100 text-green-700' :
                          l.status === 'REJECTED' ? 'bg-red-100 text-red-700' :
                          'bg-yellow-100 text-yellow-700'
                        }`}>{l.status}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaExclamationTriangle /> Open Complaints</h3>
          {pendingComplaints.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-gray-400">
              <FaExclamationTriangle size={32} className="mb-2" />
              <p className="text-sm">No open complaints</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100">
                    <th className="text-left py-2 text-gray-500 font-medium">Title</th>
                    <th className="text-left py-2 text-gray-500 font-medium">Student</th>
                    <th className="text-left py-2 text-gray-500 font-medium">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingComplaints.slice(0, 5).map((c, i) => (
                    <tr key={i} className="border-b border-gray-50">
                      <td className="py-2 text-gray-700 max-w-[120px] truncate">{c.title}</td>
                      <td className="py-2 text-gray-700">{c.student?.name || c.studentName}</td>
                      <td className="py-2">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${c.status === 'RESOLVED' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>{c.status}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
      {notices.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaBullhorn /> Recent Notices</h3>
          {notices.map((n, i) => (
            <div key={i} className={`py-3 ${i < notices.length - 1 ? 'border-b border-gray-100' : ''}`}>
              <h4 className="text-sm font-semibold text-gray-900">{n.title}</h4>
              <p className="text-xs text-gray-500 mt-1">{n.content?.substring(0, 100)}</p>
              <span className="text-xs text-gray-400 mt-1 block">{n.createdAt?.slice(0, 10)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default WardenHome;
