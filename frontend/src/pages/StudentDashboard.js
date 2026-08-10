import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { Pie } from 'react-chartjs-2';
import ProtectedRoute from '../components/common/ProtectedRoute';
import Navbar from '../components/common/Navbar';
import Sidebar from '../components/common/Sidebar';
import { useAuth } from '../context/AuthContext';
import studentService from '../services/studentService';
import leaveService from '../services/leaveService';
import complaintService from '../services/complaintService';
import noticeService from '../services/noticeService';
import marketplaceService from '../services/marketplaceService';
import lostFoundService from '../services/lostFoundService';
import messFeedbackService from '../services/messFeedbackService';
import {
  FaUser, FaDoorOpen, FaCalendarAlt, FaExclamationTriangle, FaBullhorn,
  FaStore, FaSearch, FaUtensils, FaHome, FaEdit, FaSave, FaTimes,
  FaPlus, FaTrash, FaCheck, FaUpload, FaStar, FaArrowLeft,
  FaPhone, FaEnvelope, FaIdCard, FaMapMarkerAlt, FaUserFriends,
  FaVenusMars, FaCalendar, FaLock, FaEye, FaEyeSlash,
  FaImage, FaBox, FaShoppingCart, FaStarHalfAlt, FaMapMarkerAlt as FaMapPin
} from 'react-icons/fa';

ChartJS.register(ArcElement, Tooltip, Legend);

const DashboardHome = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [myRoom, setMyRoom] = useState(null);
  const [leaves, setLeaves] = useState([]);
  const [complaints, setComplaints] = useState([]);
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [prof, room, lvs, comps, nots] = await Promise.all([
          studentService.getProfile(),
          studentService.getMyRoom().catch(() => null),
          leaveService.getMyLeaves(),
          complaintService.getMyComplaints(),
          noticeService.getAllNotices(),
        ]);
        setProfile(prof);
        setMyRoom(room);
        setLeaves(Array.isArray(lvs) ? lvs : []);
        setComplaints(Array.isArray(comps) ? comps : []);
        setNotices(Array.isArray(nots) ? nots : []);
      } catch (err) {
        toast.error('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const pendingLeaves = leaves.filter(l => l.status === 'PENDING').length;
  const approvedLeaves = leaves.filter(l => l.status === 'APPROVED').length;
  const rejectedLeaves = leaves.filter(l => l.status === 'REJECTED').length;
  const pendingComplaints = complaints.filter(c => c.status === 'PENDING' || c.status === 'OPEN').length;

  const pieData = {
    labels: ['Approved', 'Pending', 'Rejected'],
    datasets: [{
      data: [approvedLeaves, pendingLeaves, rejectedLeaves],
      backgroundColor: ['#22c55e', '#eab308', '#ef4444'],
      borderWidth: 0,
    }],
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="w-10 h-10 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
      </div>
    );
  }

  const cards = [
    { icon: <FaDoorOpen />, title: 'My Room', value: myRoom?.roomNo || 'Not Allocated', color: '#1a237e', onClick: () => navigate('/student/my-room') },
    { icon: <FaCalendarAlt />, title: 'Leave Applications', value: `${leaves.length} Total (${pendingLeaves} Pending)`, color: '#f57f17', onClick: () => navigate('/student/leaves') },
    { icon: <FaExclamationTriangle />, title: 'Complaints', value: `${complaints.length} Total (${pendingComplaints} Open)`, color: '#c62828', onClick: () => navigate('/student/complaints') },
    { icon: <FaBullhorn />, title: 'Recent Notices', value: `${notices.length} Notices`, color: '#0d47a1', onClick: () => navigate('/student/notices') },
  ];

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Welcome, {profile?.name || user?.name || 'Student'}!</h1>
        <p className="text-sm text-gray-500">Here's your hostel overview</p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
        {cards.map((card, i) => (
          <div
            key={i}
            className="bg-white rounded-xl shadow-sm p-6 cursor-pointer hover:shadow-md transition-shadow"
            style={{ borderTop: `4px solid ${card.color}` }}
            onClick={card.onClick}
          >
            <div className="flex items-center gap-4">
              <div className="text-2xl" style={{ color: card.color }}>{card.icon}</div>
              <div>
                <p className="text-sm text-gray-500">{card.title}</p>
                <h3 className="text-lg font-semibold text-gray-900">{card.value}</h3>
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Recent Leaves</h3>
            </div>
            {leaves.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-8 text-gray-400">
                <FaCalendarAlt size={32} className="mb-2" />
                <p className="text-sm">No leave applications</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100">
                      <th className="text-left py-2 text-gray-500 font-medium">Dates</th>
                      <th className="text-left py-2 text-gray-500 font-medium">Reason</th>
                      <th className="text-left py-2 text-gray-500 font-medium">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {leaves.slice(0, 5).map((l, i) => (
                      <tr key={i} className="border-b border-gray-50">
                        <td className="py-2 text-gray-700">{l.fromDate?.slice(0, 10)} - {l.toDate?.slice(0, 10)}</td>
                        <td className="py-2 text-gray-700 max-w-[120px] truncate">{l.reason}</td>
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
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Recent Complaints</h3>
            </div>
            {complaints.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-8 text-gray-400">
                <FaExclamationTriangle size={32} className="mb-2" />
                <p className="text-sm">No complaints filed</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100">
                      <th className="text-left py-2 text-gray-500 font-medium">Title</th>
                      <th className="text-left py-2 text-gray-500 font-medium">Date</th>
                      <th className="text-left py-2 text-gray-500 font-medium">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {complaints.slice(0, 5).map((c, i) => (
                      <tr key={i} className="border-b border-gray-50">
                        <td className="py-2 text-gray-700 max-w-[120px] truncate">{c.title}</td>
                        <td className="py-2 text-gray-700">{c.createdAt?.slice(0, 10)}</td>
                        <td className="py-2">
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            c.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                            c.status === 'REJECTED' ? 'bg-red-100 text-red-700' :
                            'bg-yellow-100 text-yellow-700'
                          }`}>{c.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Leave Status</h3>
          </div>
          {leaves.length > 0 ? (
            <div className="flex justify-center">
              <div className="w-48 h-48">
                <Pie data={pieData} options={{ responsive: true, maintainAspectRatio: true, plugins: { legend: { position: 'bottom' } } }} />
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-8 text-gray-400">
              <FaCalendarAlt size={32} className="mb-2" />
              <p className="text-sm">No leave data</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

const StudentProfile = () => {
  const [profile, setProfile] = useState(null);
  const [editData, setEditData] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [pwData, setPwData] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [showPw, setShowPw] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    studentService.getProfile()
      .then(data => { setProfile(data); setEditData(data); })
      .catch(() => toast.error('Failed to load profile'))
      .finally(() => setLoading(false));
  }, []);

  const handleUpdate = async () => {
    setSubmitting(true);
    try {
      const res = await studentService.updateProfile(editData);
      setProfile(res.user || res);
      setIsEditing(false);
      toast.success('Profile updated successfully');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Update failed');
    } finally {
      setSubmitting(false);
    }
  };

  const handleChangePassword = async () => {
    if (pwData.newPassword !== pwData.confirmPassword) return toast.error('Passwords do not match');
    if (pwData.newPassword.length < 6) return toast.error('Password must be at least 6 characters');
    setSubmitting(true);
    try {
      await studentService.changePassword(pwData);
      toast.success('Password changed successfully');
      setPwData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setShowPw(false);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Password change failed');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="w-10 h-10 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
      </div>
    );
  }
  if (!profile) {
    return <div className="p-6"><div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaUser size={32} /><p>Failed to load profile</p></div></div>;
  }

  const fields = [
    { label: 'Name', key: 'name', icon: <FaUser /> },
    { label: 'Email', key: 'email', icon: <FaEnvelope /> },
    { label: 'Phone', key: 'phone', icon: <FaPhone /> },
    { label: 'Enrollment No.', key: 'enrollmentNo', icon: <FaIdCard /> },
    { label: 'Parent Contact', key: 'parentContact', icon: <FaUserFriends /> },
    { label: 'Address', key: 'address', icon: <FaMapMarkerAlt /> },
    { label: 'Date of Birth', key: 'dateOfBirth', icon: <FaCalendar /> },
    { label: 'Gender', key: 'gender', icon: <FaVenusMars /> },
  ];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Profile</h1>
          <p className="text-sm text-gray-500">Manage your personal information</p>
        </div>
        <button
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            isEditing ? 'bg-green-600 text-white hover:bg-green-700' : 'bg-[#1a237e] text-white hover:bg-[#0d47a1]'
          }`}
          onClick={() => isEditing ? handleUpdate() : setIsEditing(true)}
          disabled={submitting}
        >
          {isEditing ? <><FaSave /> Save Changes</> : <><FaEdit /> Edit Profile</>}
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
        <div className="flex flex-col items-center mb-6">
          <div className="w-20 h-20 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-3xl font-bold mb-2">
            {profile.name?.charAt(0).toUpperCase() || <FaUser />}
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {fields.map((f, i) => (
            <div key={i}>
              <span className="flex items-center gap-2 text-xs text-gray-500 uppercase tracking-wide mb-1">
                {f.icon} {f.label}
              </span>
              {isEditing ? (
                <input
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  value={editData?.[f.key] || ''}
                  onChange={(e) => setEditData({ ...editData, [f.key]: e.target.value })}
                />
              ) : (
                <p className="text-sm font-medium text-gray-900">{profile[f.key] || '\u2014'}</p>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2"><FaLock /> Change Password</h3>
          <button
            className="flex items-center gap-1 px-3 py-1.5 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:border-[#1a237e] hover:text-[#1a237e] transition-colors"
            onClick={() => setShowPw(!showPw)}
          >
            {showPw ? <><FaTimes /> Cancel</> : <><FaLock /> Change</>}
          </button>
        </div>
        {showPw && (
          <div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Current Password</label>
                <input
                  type="password"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  value={pwData.currentPassword}
                  onChange={(e) => setPwData({ ...pwData, currentPassword: e.target.value })}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                <input
                  type="password"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  value={pwData.newPassword}
                  onChange={(e) => setPwData({ ...pwData, newPassword: e.target.value })}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Confirm Password</label>
                <input
                  type="password"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  value={pwData.confirmPassword}
                  onChange={(e) => setPwData({ ...pwData, confirmPassword: e.target.value })}
                />
              </div>
            </div>
            <button
              className="bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50"
              onClick={handleChangePassword}
              disabled={submitting}
            >
              Update Password
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

const StudentMyRoom = () => {
  const [room, setRoom] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    studentService.getMyRoom()
      .then(data => setRoom(data))
      .catch(() => setRoom(null))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="w-10 h-10 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
      </div>
    );
  }

  if (!room) {
    return (
      <div className="p-6">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">My Room</h1>
          <p className="text-sm text-gray-500">Room allocation details</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-12 text-center">
          <FaDoorOpen size={64} className="mx-auto text-gray-300 mb-4" />
          <h3 className="text-lg font-semibold text-gray-600 mb-2">No Room Allocated</h3>
          <p className="text-sm text-gray-400">You have not been assigned a room yet. Please contact the warden for allocation.</p>
        </div>
      </div>
    );
  }

  const details = [
    { label: 'Room Number', value: room.roomNumber || room.roomNo },
    { label: 'Block', value: room.blockName || room.block?.name || '\u2014' },
    { label: 'Floor', value: room.floor || '\u2014' },
    { label: 'Capacity', value: room.capacity || '\u2014' },
    { label: 'Rent', value: room.rent ? `₹${room.rent}/month` : '\u2014' },
  ];

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">My Room</h1>
        <p className="text-sm text-gray-500">Your assigned room details</p>
      </div>
      <div className="bg-white rounded-xl shadow-sm p-6 max-w-2xl">
        <div className="mb-4">
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">
            <FaCheck className="mr-1" /> Currently Occupied
          </span>
        </div>
        <div className="flex items-center gap-4 mb-6">
          <FaDoorOpen className="text-4xl text-[#1a237e]" />
          <h2 className="text-3xl font-bold text-gray-900">{room.roomNumber || room.roomNo}</h2>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {details.map((d, i) => (
            <div key={i}>
              <span className="text-xs text-gray-500 uppercase tracking-wide">{d.label}</span>
              <p className="text-sm font-medium text-gray-900">{d.value}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

const StudentLeave = () => {
  const [leaves, setLeaves] = useState([]);
  const [form, setForm] = useState({ fromDate: '', toDate: '', reason: '' });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    leaveService.getMyLeaves()
      .then(data => setLeaves(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load leaves'))
      .finally(() => setLoading(false));
  }, []);

  const validate = () => {
    const e = {};
    if (!form.fromDate) e.fromDate = 'From date is required';
    if (!form.toDate) e.toDate = 'To date is required';
    else if (form.fromDate && form.toDate < form.fromDate) e.toDate = 'To date must be after from date';
    if (!form.reason.trim()) e.reason = 'Reason is required';
    else if (form.reason.length < 10) e.reason = 'Please provide a detailed reason';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    try {
      const res = await leaveService.applyLeave(form);
      setLeaves([res, ...leaves]);
      setForm({ fromDate: '', toDate: '', reason: '' });
      toast.success('Leave application submitted');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Application failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Leave Management</h1>
        <p className="text-sm text-gray-500">Apply for leave and track status</p>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaCalendarAlt /> Apply for Leave</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">From Date</label>
              <input
                type="date"
                className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.fromDate ? 'border-red-400' : 'border-gray-300'}`}
                value={form.fromDate}
                onChange={(e) => setForm({ ...form, fromDate: e.target.value })}
              />
              {errors.fromDate && <p className="text-red-500 text-xs mt-1">{errors.fromDate}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">To Date</label>
              <input
                type="date"
                className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.toDate ? 'border-red-400' : 'border-gray-300'}`}
                value={form.toDate}
                onChange={(e) => setForm({ ...form, toDate: e.target.value })}
              />
              {errors.toDate && <p className="text-red-500 text-xs mt-1">{errors.toDate}</p>}
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Reason</label>
            <textarea
              className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.reason ? 'border-red-400' : 'border-gray-300'}`}
              value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })}
              placeholder="Describe the reason for your leave..."
              rows={4}
            />
            {errors.reason && <p className="text-red-500 text-xs mt-1">{errors.reason}</p>}
          </div>
          <button
            type="submit"
            className="bg-[#1a237e] text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50"
            disabled={submitting}
          >
            {submitting ? 'Submitting...' : 'Apply for Leave'}
          </button>
        </form>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Leave History</h3>
        {loading ? (
          <div className="flex items-center justify-center py-6">
            <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
          </div>
        ) : leaves.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-gray-400">
            <FaCalendarAlt size={32} className="mb-2" />
            <p className="text-sm">No leave applications yet</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100">
                  <th className="text-left py-2 text-gray-500 font-medium">From</th>
                  <th className="text-left py-2 text-gray-500 font-medium">To</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Reason</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {leaves.map((l, i) => (
                  <tr key={i} className="border-b border-gray-50">
                    <td className="py-2 text-gray-700">{l.fromDate?.slice(0, 10)}</td>
                    <td className="py-2 text-gray-700">{l.toDate?.slice(0, 10)}</td>
                    <td className="py-2 text-gray-700 max-w-[200px] truncate">{l.reason}</td>
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
    </div>
  );
};

const StudentComplaint = () => {
  const [complaints, setComplaints] = useState([]);
  const [form, setForm] = useState({ title: '', description: '' });
  const [errors, setErrors] = useState({});
  const [image, setImage] = useState(null);
  const [preview, setPreview] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    complaintService.getMyComplaints()
      .then(data => setComplaints(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load complaints'))
      .finally(() => setLoading(false));
  }, []);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImage(file);
      setPreview(URL.createObjectURL(file));
    }
  };

  const validate = () => {
    const newErrors = {};
    if (!form.title.trim()) newErrors.title = 'Title is required';
    else if (form.title.trim().length < 5) newErrors.title = 'Title must be at least 5 characters';
    if (!form.description.trim()) newErrors.description = 'Description is required';
    else if (form.description.trim().length < 10) newErrors.description = 'Please provide a detailed description (min 10 characters)';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return toast.error('Please fix the errors in the form');
    setSubmitting(true);
    try {
      const fd = new FormData();
      fd.append('title', form.title);
      fd.append('description', form.description);
      if (image) fd.append('image', image);
      const res = await complaintService.createComplaint(fd);
      setComplaints([res, ...complaints]);
      setForm({ title: '', description: '' });
      setErrors({});
      setImage(null);
      setPreview('');
      toast.success('Complaint filed successfully');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit complaint');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Complaints</h1>
        <p className="text-sm text-gray-500">File and track your complaints</p>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaExclamationTriangle /> File a Complaint</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
            <input
              type="text"
              className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.title ? 'border-red-400' : 'border-gray-300'}`}
              value={form.title}
              onChange={(e) => { setForm({ ...form, title: e.target.value }); if (errors.title) setErrors({ ...errors, title: '' }); }}
              placeholder="Brief title of your complaint"
            />
            {errors.title && <p className="text-red-500 text-xs mt-1">{errors.title}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea
              className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.description ? 'border-red-400' : 'border-gray-300'}`}
              value={form.description}
              onChange={(e) => { setForm({ ...form, description: e.target.value }); if (errors.description) setErrors({ ...errors, description: '' }); }}
              placeholder="Describe your complaint in detail..."
              rows={4}
            />
            {errors.description && <p className="text-red-500 text-xs mt-1">{errors.description}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Attach Image (optional)</label>
            <div className="flex items-center gap-4">
              <label className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-600 cursor-pointer hover:bg-gray-50">
                <FaUpload /> Choose Image
                <input type="file" accept="image/*" onChange={handleImageChange} className="hidden" />
              </label>
              {preview && <img src={preview} alt="Preview" className="w-20 h-20 object-cover rounded-lg" />}
            </div>
          </div>
          <button
            type="submit"
            className="bg-[#1a237e] text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50"
            disabled={submitting}
          >
            {submitting ? 'Submitting...' : 'Submit Complaint'}
          </button>
        </form>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">My Complaints</h3>
        {loading ? (
          <div className="flex items-center justify-center py-6">
            <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
          </div>
        ) : complaints.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-gray-400">
            <FaExclamationTriangle size={32} className="mb-2" />
            <p className="text-sm">No complaints filed</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100">
                  <th className="text-left py-2 text-gray-500 font-medium">Title</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Date</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Status</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Response</th>
                </tr>
              </thead>
              <tbody>
                {complaints.map((c, i) => (
                  <tr key={i} className="border-b border-gray-50">
                    <td className="py-2 text-gray-700 max-w-[150px] truncate">{c.title}</td>
                    <td className="py-2 text-gray-700">{c.createdAt?.slice(0, 10)}</td>
                    <td className="py-2">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        c.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                        c.status === 'REJECTED' ? 'bg-red-100 text-red-700' :
                        'bg-yellow-100 text-yellow-700'
                      }`}>{c.status}</span>
                    </td>
                    <td className="py-2 text-gray-500">{c.response || '\u2014'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

const StudentNotices = () => {
  const [notices, setNotices] = useState([]);
  const [search, setSearch] = useState('');
  const [expanded, setExpanded] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    noticeService.getAllNotices()
      .then(data => setNotices(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load notices'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = notices.filter(n =>
    n.title?.toLowerCase().includes(search.toLowerCase()) ||
    n.content?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Notice Board</h1>
        <p className="text-sm text-gray-500 mb-3">Latest announcements from administration</p>
        <div className="relative max-w-md">
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
            placeholder="Search notices..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6">
          <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400">
          <FaBullhorn size={32} className="mb-2" />
          <p className="text-sm">No notices found</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((n, i) => (
            <div
              key={i}
              className={`bg-white rounded-xl shadow-sm p-6 cursor-pointer transition-all hover:shadow-md ${expanded === i ? 'md:col-span-2 lg:col-span-3' : ''}`}
              onClick={() => setExpanded(expanded === i ? null : i)}
            >
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 bg-blue-100 text-[#1a237e] rounded-lg flex items-center justify-center shrink-0">
                  <FaBullhorn />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-sm font-semibold text-gray-900 mb-1">{n.title}</h3>
                  <p className="text-xs text-gray-500">
                    {expanded === i ? n.content : n.content?.substring(0, 100) + (n.content?.length > 100 ? '...' : '')}
                  </p>
                  <div className="flex items-center gap-4 mt-2 text-xs text-gray-400">
                    <span className="flex items-center gap-1"><FaUser /> {n.postedBy?.name || n.postedBy || 'Admin'}</span>
                    <span className="flex items-center gap-1"><FaCalendarAlt /> {n.createdAt?.slice(0, 10) || n.date?.slice(0, 10)}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const StudentLostFound = () => {
  const [items, setItems] = useState([]);
  const [tab, setTab] = useState('browse');
  const [form, setForm] = useState({ title: '', description: '', category: 'lost', location: '', contactInfo: '' });
  const [errors, setErrors] = useState({});
  const [image, setImage] = useState(null);
  const [preview, setPreview] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    lostFoundService.getAllItems()
      .then(data => setItems(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load items'))
      .finally(() => setLoading(false));
  }, []);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) { setImage(file); setPreview(URL.createObjectURL(file)); }
  };

  const validate = () => {
    const newErrors = {};
    if (!form.title.trim()) newErrors.title = 'Title is required';
    if (!form.description.trim()) newErrors.description = 'Description is required';
    if (!form.location.trim()) newErrors.location = 'Location is required';
    if (!form.contactInfo.trim()) newErrors.contactInfo = 'Contact info is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleReport = async (e) => {
    e.preventDefault();
    if (!validate()) return toast.error('Please fix the errors in the form');
    setSubmitting(true);
    try {
      const fd = new FormData();
      Object.entries(form).forEach(([k, v]) => fd.append(k, v));
      if (image) fd.append('image', image);
      const res = await lostFoundService.reportItem(fd);
      setItems([res, ...items]);
      setForm({ title: '', description: '', category: 'lost', location: '', contactInfo: '' });
      setErrors({});
      setImage(null);
      setPreview('');
      setTab('browse');
      toast.success('Item reported successfully');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to report item');
    } finally {
      setSubmitting(false);
    }
  };

  const filtered = items.filter(i => !statusFilter || i.status === statusFilter);

  return (
    <div className="p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Lost & Found</h1>
          <p className="text-sm text-gray-500">Report lost items or browse found items</p>
        </div>
        <div className="flex gap-2">
          <button
            className={`px-4 py-2 rounded-lg text-sm font-medium border transition-all ${tab === 'browse' ? 'bg-[#1a237e] text-white border-[#1a237e]' : 'border-gray-300 text-gray-600 hover:border-gray-400'}`}
            onClick={() => setTab('browse')}
          ><FaSearch className="inline mr-1" /> Browse Items</button>
          <button
            className={`px-4 py-2 rounded-lg text-sm font-medium border transition-all ${tab === 'report' ? 'bg-[#1a237e] text-white border-[#1a237e]' : 'border-gray-300 text-gray-600 hover:border-gray-400'}`}
            onClick={() => setTab('report')}
          ><FaPlus className="inline mr-1" /> Report Item</button>
        </div>
      </div>

      {tab === 'report' ? (
        <div className="bg-white rounded-xl shadow-sm p-6 max-w-2xl">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Report an Item</h3>
          <form onSubmit={handleReport} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                <select className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                  <option value="lost">Lost Item</option>
                  <option value="found">Found Item</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Location</label>
                <input type="text" className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.location ? 'border-red-400' : 'border-gray-300'}`}
                  value={form.location} onChange={(e) => { setForm({ ...form, location: e.target.value }); if (errors.location) setErrors({ ...errors, location: '' }); }} placeholder="Where was it lost/found?" />
                {errors.location && <p className="text-red-500 text-xs mt-1">{errors.location}</p>}
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
              <input type="text" className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.title ? 'border-red-400' : 'border-gray-300'}`}
                value={form.title} onChange={(e) => { setForm({ ...form, title: e.target.value }); if (errors.title) setErrors({ ...errors, title: '' }); }} placeholder="Item name/title" />
              {errors.title && <p className="text-red-500 text-xs mt-1">{errors.title}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.description ? 'border-red-400' : 'border-gray-300'}`}
                value={form.description} onChange={(e) => { setForm({ ...form, description: e.target.value }); if (errors.description) setErrors({ ...errors, description: '' }); }} placeholder="Describe the item..." rows={3} />
              {errors.description && <p className="text-red-500 text-xs mt-1">{errors.description}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Contact Info</label>
              <input type="text" className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.contactInfo ? 'border-red-400' : 'border-gray-300'}`}
                value={form.contactInfo} onChange={(e) => { setForm({ ...form, contactInfo: e.target.value }); if (errors.contactInfo) setErrors({ ...errors, contactInfo: '' }); }} placeholder="Phone or email" />
              {errors.contactInfo && <p className="text-red-500 text-xs mt-1">{errors.contactInfo}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Image (optional)</label>
              <div className="flex items-center gap-4">
                <label className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-600 cursor-pointer hover:bg-gray-50">
                  <FaUpload /> Choose Image
                  <input type="file" accept="image/*" onChange={handleImageChange} className="hidden" />
                </label>
                {preview && <img src={preview} alt="Preview" className="w-20 h-20 object-cover rounded-lg" />}
              </div>
            </div>
            <button type="submit" className="bg-[#1a237e] text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50" disabled={submitting}>
              {submitting ? 'Submitting...' : 'Report Item'}
            </button>
          </form>
        </div>
      ) : (
        <>
          <div className="flex gap-2 mb-4 flex-wrap">
            {['', 'OPEN', 'RESOLVED'].map(s => (
              <button
                key={s}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${statusFilter === s ? 'bg-[#1a237e] text-white border-[#1a237e]' : 'border-gray-300 text-gray-600 hover:border-gray-400'}`}
                onClick={() => setStatusFilter(s)}
              >{s || 'All'}</button>
            ))}
          </div>
          {loading ? (
            <div className="flex items-center justify-center py-6">
              <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
            </div>
          ) : filtered.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-gray-400">
              <FaSearch size={32} className="mb-2" />
              <p className="text-sm">No items reported</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filtered.map((item, i) => (
                <div key={i} className="bg-white rounded-xl shadow-sm overflow-hidden">
                  {item.image && <img src={item.image} alt={item.title} className="w-full h-40 object-cover" />}
                  <div className="p-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium mb-2 ${item.category === 'lost' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'}`}>
                      {item.category?.toUpperCase()}
                    </span>
                    <h3 className="text-sm font-semibold text-gray-900">{item.title}</h3>
                    <p className="text-xs text-gray-500 mt-1">{item.description}</p>
                    <p className="text-xs text-gray-400 mt-1"><FaMapPin className="inline mr-1" />{item.location}</p>
                    <div className="flex items-center justify-between mt-3">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${item.status === 'RESOLVED' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}>
                        {item.status}
                      </span>
                      {item.contactInfo && <span className="text-xs text-gray-500"><FaPhone className="inline mr-1" />{item.contactInfo}</span>}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

const StudentMarketplace = () => {
  const [items, setItems] = useState([]);
  const [myItems, setMyItems] = useState([]);
  const [tab, setTab] = useState('browse');
  const [form, setForm] = useState({ title: '', description: '', price: '', category: 'books' });
  const [errors, setErrors] = useState({});
  const [image, setImage] = useState(null);
  const [preview, setPreview] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      marketplaceService.getAllItems(),
      marketplaceService.getMyItems().catch(() => [])
    ]).then(([all, mine]) => {
      setItems(Array.isArray(all) ? all : []);
      setMyItems(Array.isArray(mine) ? mine : []);
    }).catch(() => toast.error('Failed to load marketplace'))
      .finally(() => setLoading(false));
  }, []);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) { setImage(file); setPreview(URL.createObjectURL(file)); }
  };

  const validate = () => {
    const newErrors = {};
    if (!form.title.trim()) newErrors.title = 'Title is required';
    if (!form.price || parseFloat(form.price) <= 0) newErrors.price = 'Enter a valid price';
    if (!form.description.trim()) newErrors.description = 'Description is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleAddItem = async (e) => {
    e.preventDefault();
    if (!validate()) return toast.error('Please fix the errors in the form');
    setSubmitting(true);
    try {
      const fd = new FormData();
      Object.entries(form).forEach(([k, v]) => fd.append(k, v));
      if (image) fd.append('image', image);
      const res = await marketplaceService.addItem(fd);
      setItems([res, ...items]);
      setMyItems([res, ...myItems]);
      setForm({ title: '', description: '', price: '', category: 'books' });
      setErrors({});
      setImage(null);
      setPreview('');
      toast.success('Item listed successfully');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to list item');
    } finally {
      setSubmitting(false);
    }
  };

  const handleMarkSold = async (id) => {
    try {
      await marketplaceService.markAsSold(id);
      setItems(items.map(i => i.id === id ? { ...i, status: 'SOLD' } : i));
      setMyItems(myItems.map(i => i.id === id ? { ...i, status: 'SOLD' } : i));
      toast.success('Item marked as sold');
    } catch (err) {
      toast.error('Failed to update item');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this item?')) return;
    try {
      await marketplaceService.deleteItem(id);
      setItems(items.filter(i => i.id !== id));
      setMyItems(myItems.filter(i => i.id !== id));
      toast.success('Item deleted');
    } catch (err) {
      toast.error('Failed to delete item');
    }
  };

  const displayItems = tab === 'my' ? myItems : items;

  return (
    <div className="p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Marketplace</h1>
          <p className="text-sm text-gray-500">Buy, sell, and trade within the hostel</p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <button className={`px-4 py-2 rounded-lg text-sm font-medium border transition-all ${tab === 'browse' ? 'bg-[#1a237e] text-white border-[#1a237e]' : 'border-gray-300 text-gray-600 hover:border-gray-400'}`}
            onClick={() => setTab('browse')}><FaStore className="inline mr-1" /> Browse</button>
          <button className={`px-4 py-2 rounded-lg text-sm font-medium border transition-all ${tab === 'my' ? 'bg-[#1a237e] text-white border-[#1a237e]' : 'border-gray-300 text-gray-600 hover:border-gray-400'}`}
            onClick={() => setTab('my')}><FaBox className="inline mr-1" /> My Items</button>
          <button className={`px-4 py-2 rounded-lg text-sm font-medium border transition-all ${tab === 'add' ? 'bg-[#1a237e] text-white border-[#1a237e]' : 'border-gray-300 text-gray-600 hover:border-gray-400'}`}
            onClick={() => setTab('add')}><FaPlus className="inline mr-1" /> Add Item</button>
        </div>
      </div>

      {tab === 'add' ? (
        <div className="bg-white rounded-xl shadow-sm p-6 max-w-2xl">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaShoppingCart /> List New Item</h3>
          <form onSubmit={handleAddItem} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
                <input type="text" className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.title ? 'border-red-400' : 'border-gray-300'}`}
                  value={form.title} onChange={(e) => { setForm({ ...form, title: e.target.value }); if (errors.title) setErrors({ ...errors, title: '' }); }} placeholder="Item name" />
                {errors.title && <p className="text-red-500 text-xs mt-1">{errors.title}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Price ($)</label>
                <input type="number" className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.price ? 'border-red-400' : 'border-gray-300'}`}
                  value={form.price} onChange={(e) => { setForm({ ...form, price: e.target.value }); if (errors.price) setErrors({ ...errors, price: '' }); }} placeholder="0.00" step="0.01" />
                {errors.price && <p className="text-red-500 text-xs mt-1">{errors.price}</p>}
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
              <select className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                {['books', 'electronics', 'furniture', 'clothing', 'sports', 'other'].map(c => (
                  <option key={c} value={c}>{c.charAt(0).toUpperCase() + c.slice(1)}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea className={`w-full px-3 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors.description ? 'border-red-400' : 'border-gray-300'}`}
                value={form.description} onChange={(e) => { setForm({ ...form, description: e.target.value }); if (errors.description) setErrors({ ...errors, description: '' }); }} placeholder="Describe your item..." rows={3} />
              {errors.description && <p className="text-red-500 text-xs mt-1">{errors.description}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Image (optional)</label>
              <div className="flex items-center gap-4">
                <label className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-600 cursor-pointer hover:bg-gray-50">
                  <FaUpload /> Choose Image
                  <input type="file" accept="image/*" onChange={handleImageChange} className="hidden" />
                </label>
                {preview && <img src={preview} alt="Preview" className="w-20 h-20 object-cover rounded-lg" />}
              </div>
            </div>
            <button type="submit" className="bg-[#1a237e] text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50" disabled={submitting}>
              {submitting ? 'Listing...' : 'List Item'}
            </button>
          </form>
        </div>
      ) : (
        loading ? (
          <div className="flex items-center justify-center py-6">
            <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
          </div>
        ) : displayItems.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-gray-400">
            <FaStore size={32} className="mb-2" />
            <p className="text-sm">{tab === 'my' ? 'You have no items listed' : 'No items in marketplace'}</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {displayItems.map((item, i) => (
              <div key={i} className="bg-white rounded-xl shadow-sm overflow-hidden">
                {item.image ? (
                  <img src={item.image} alt={item.title} className="w-full h-40 object-cover" />
                ) : (
                  <div className="w-full h-40 bg-gray-100 flex items-center justify-center"><FaStore size={40} className="text-gray-300" /></div>
                )}
                <div className="p-4">
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700 mb-2">{item.category?.toUpperCase()}</span>
                  <h3 className="text-sm font-semibold text-gray-900">{item.title}</h3>
                  <p className="text-lg font-bold text-[#1a237e]">₹{item.price}</p>
                  <p className="text-xs text-gray-500 mt-1">{item.description?.substring(0, 60)}</p>
                  <p className="text-xs text-gray-400 mt-1">Sold by: {item.seller?.name || 'Unknown'}</p>
                  {tab === 'my' && (
                    <div className="flex gap-2 mt-3">
                      {item.status !== 'SOLD' && (
                        <button className="flex items-center gap-1 bg-green-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-green-700"
                          onClick={() => handleMarkSold(item.id)}><FaCheck /> Mark Sold</button>
                      )}
                      <button className="flex items-center gap-1 bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700"
                        onClick={() => handleDelete(item.id)}><FaTrash /> Delete</button>
                    </div>
                  )}
                  {item.status === 'SOLD' && (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700 mt-2">SOLD</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )
      )}
    </div>
  );
};

const StudentMessFeedback = () => {
  const [feedback, setFeedback] = useState([]);
  const [averages, setAverages] = useState(null);
  const [form, setForm] = useState({ foodQuality: 0, taste: 0, cleanliness: 0, comments: '' });
  const [hover, setHover] = useState({ foodQuality: 0, taste: 0, cleanliness: 0 });
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      messFeedbackService.getMyFeedback(),
      messFeedbackService.getAverages()
    ]).then(([fb, avg]) => {
      setFeedback(Array.isArray(fb) ? fb : []);
      setAverages(avg);
    }).catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const StarRating = ({ name, value, onHover, onChange }) => (
    <div className="flex justify-center gap-1">
      {[1, 2, 3, 4, 5].map(star => (
        <span
          key={star}
          className={`cursor-pointer text-2xl transition-colors ${star <= (hover[name] || value) ? 'text-yellow-400' : 'text-gray-300'}`}
          onMouseEnter={() => onHover(star)}
          onMouseLeave={() => onHover(0)}
          onClick={() => onChange(star)}
        >
          {star <= (hover[name] || value) ? '\u2605' : '\u2606'}
        </span>
      ))}
    </div>
  );

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.foodQuality || !form.taste || !form.cleanliness) return toast.error('Please rate all categories');
    setSubmitting(true);
    try {
      const res = await messFeedbackService.submitFeedback(form);
      setFeedback([res, ...feedback]);
      setForm({ foodQuality: 0, taste: 0, cleanliness: 0, comments: '' });
      const avg = await messFeedbackService.getAverages();
      setAverages(avg);
      toast.success('Feedback submitted');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit feedback');
    } finally {
      setSubmitting(false);
    }
  };

  const avgRatings = averages || {};
  const avgCategories = [
    { label: 'Food Quality', value: avgRatings.foodQuality },
    { label: 'Taste', value: avgRatings.taste },
    { label: 'Cleanliness', value: avgRatings.cleanliness },
  ];

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Mess Feedback</h1>
        <p className="text-sm text-gray-500">Rate the hostel mess and share your thoughts</p>
      </div>

      {avgRatings && Object.keys(avgRatings).length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          {avgCategories.map((c, i) => (
            <div key={i} className="bg-white rounded-xl shadow-sm p-6 text-center">
              <h3 className="text-sm text-gray-500 mb-1">{c.label}</h3>
              <p className="text-3xl font-bold text-[#1a237e] mb-2">{c.value ? Number(c.value).toFixed(1) : '\u2014'}</p>
              <div className="flex justify-center gap-1">
                {[1, 2, 3, 4, 5].map(s => (
                  <span key={s} className={`text-xl ${s <= Math.round(c.value || 0) ? 'text-yellow-400' : 'text-gray-300'}`}>\u2605</span>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaStar /> Submit Feedback</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {['foodQuality', 'taste', 'cleanliness'].map(cat => (
              <div key={cat} className="text-center">
                <label className="block text-sm font-medium text-gray-700 mb-2 capitalize">
                  {cat === 'foodQuality' ? 'Food Quality' : cat}
                </label>
                <StarRating
                  name={cat}
                  value={form[cat]}
                  hover={hover}
                  onHover={(v) => setHover({ ...hover, [cat]: v })}
                  onChange={(v) => setForm({ ...form, [cat]: v })}
                />
              </div>
            ))}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Comments (optional)</label>
            <textarea className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
              value={form.comments} onChange={(e) => setForm({ ...form, comments: e.target.value })} placeholder="Share your experience..." rows={3} />
          </div>
          <button type="submit" className="bg-[#1a237e] text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50" disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit Feedback'}
          </button>
        </form>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">My Feedback History</h3>
        {loading ? (
          <div className="flex items-center justify-center py-6">
            <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
          </div>
        ) : feedback.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-gray-400">
            <FaStar size={32} className="mb-2" />
            <p className="text-sm">No feedback submitted yet</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100">
                  <th className="text-left py-2 text-gray-500 font-medium">Date</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Food</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Taste</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Cleanliness</th>
                  <th className="text-left py-2 text-gray-500 font-medium">Comments</th>
                </tr>
              </thead>
              <tbody>
                {feedback.map((f, i) => {
                  const fq = f.foodQualityRating || f.foodQuality || 0;
                  const t = f.tasteRating || f.taste || 0;
                  const c = f.cleanlinessRating || f.cleanliness || 0;
                  return (
                    <tr key={i} className="border-b border-gray-50">
                      <td className="py-2 text-gray-700">{(f.createdAt || f.date)?.slice(0, 10)}</td>
                      <td className="py-2">{'\u2605'.repeat(fq)}{'\u2606'.repeat(Math.max(0, 5 - fq))}</td>
                      <td className="py-2">{'\u2605'.repeat(t)}{'\u2606'.repeat(Math.max(0, 5 - t))}</td>
                      <td className="py-2">{'\u2605'.repeat(c)}{'\u2606'.repeat(Math.max(0, 5 - c))}</td>
                      <td className="py-2 text-gray-500 max-w-[150px] truncate">{f.comments || '\u2014'}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

const StudentDashboard = () => (
  <ProtectedRoute allowedRoles={['student']}>
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <Sidebar />
      <div className="ml-64 mt-[70px] transition-all duration-300">
        <Routes>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<DashboardHome />} />
          <Route path="profile" element={<StudentProfile />} />
          <Route path="my-room" element={<StudentMyRoom />} />
          <Route path="leaves" element={<StudentLeave />} />
          <Route path="complaints" element={<StudentComplaint />} />
          <Route path="notices" element={<StudentNotices />} />
          <Route path="marketplace" element={<StudentMarketplace />} />
          <Route path="lost-found" element={<StudentLostFound />} />
          <Route path="mess-feedback" element={<StudentMessFeedback />} />
          <Route path="*" element={<Navigate to="dashboard" replace />} />
        </Routes>
      </div>
    </div>
  </ProtectedRoute>
);

export default StudentDashboard;
