import React, { useState, useEffect, useCallback } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, PointElement, LineElement, Title } from 'chart.js';
import { Pie, Bar, Line } from 'react-chartjs-2';
import ProtectedRoute from '../components/common/ProtectedRoute';
import Navbar from '../components/common/Navbar';
import Sidebar from '../components/common/Sidebar';
import adminService from '../services/adminService';
import wardenService from '../services/wardenService';
import roomService from '../services/roomService';
import leaveService from '../services/leaveService';
import complaintService from '../services/complaintService';
import noticeService from '../services/noticeService';
import marketplaceService from '../services/marketplaceService';
import lostFoundService from '../services/lostFoundService';
import messFeedbackService from '../services/messFeedbackService';
import aiService from '../services/aiService';
import AuditLogsPage from './admin/AuditLogsPage';
import {
  FaUsers, FaDoorOpen, FaUserShield, FaBuilding, FaChartBar, FaCalendarAlt,
  FaExclamationTriangle, FaBullhorn, FaStore, FaPlus, FaTrash, FaEdit,
  FaCheck, FaTimes, FaEye, FaSearch, FaUser, FaEnvelope, FaPhone,
  FaIdCard, FaMapMarkerAlt, FaFileAlt, FaDownload, FaBan, FaLink,
  FaHome, FaBed, FaStar, FaShoppingCart, FaBox, FaCog, FaClipboardList,
  FaRobot, FaSmile, FaFrown, FaLightbulb
} from 'react-icons/fa';

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, PointElement, LineElement, Title);

const chartOptions = { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } };

const DashboardHome = () => {
  const [stats, setStats] = useState(null);
  const [leaveStats, setLeaveStats] = useState(null);
  const [complaintStats, setComplaintStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      adminService.getDashboard(),
      leaveService.getLeaveStats().catch(() => null),
      complaintService.getComplaintStats().catch(() => null),
    ]).then(([dash, lstats, cstats]) => {
      setStats(dash);
      setLeaveStats(lstats);
      setComplaintStats(cstats);
    }).catch(() => toast.error('Failed to load dashboard'))
      .finally(() => setLoading(false));
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
    { icon: <FaUserShield />, label: 'Wardens', value: stats?.totalWardens || 0, color: '#2e7d32' },
    { icon: <FaBuilding />, label: 'Hostel Blocks', value: stats?.totalBlocks || 0, color: '#f57f17' },
    { icon: <FaCalendarAlt />, label: 'Leaves This Month', value: stats?.monthlyLeaves || 0, color: '#1565c0' },
    { icon: <FaExclamationTriangle />, label: 'Pending Complaints', value: stats?.pendingComplaints || 0, color: '#c62828' },
  ];

  const occupancyData = {
    labels: stats?.blockStats?.map(b => b.name) || [],
    datasets: [{ data: stats?.blockStats?.map(b => b.capacity > 0 ? Math.round((b.occupied / b.capacity) * 100) : 0) || [], backgroundColor: ['#1a237e', '#0d47a1', '#1565c0', '#1976d2', '#1e88e5', '#2196f3'] }],
  };

  const barData = {
    labels: ['Pending', 'In Progress', 'Resolved', 'Rejected'],
    datasets: [{ label: 'Complaints', data: [complaintStats?.pending || 0, complaintStats?.inProgress || 0, complaintStats?.resolved || 0, complaintStats?.rejected || 0], backgroundColor: ['#eab308', '#3b82f6', '#22c55e', '#ef4444'] }],
  };

  const lineData = {
    labels: ['Approved', 'Rejected', 'Pending'],
    datasets: [{ label: 'Leaves', data: [leaveStats?.approved || 0, leaveStats?.rejected || 0, leaveStats?.pending || 0], borderColor: '#1a237e', backgroundColor: 'rgba(26,35,126,0.1)', fill: true }],
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
        <p className="text-sm text-gray-500">Full system administration and management</p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4 mb-6">
        {statCards.map((c, i) => (
          <div key={i} className="bg-white rounded-xl shadow-sm p-4" style={{ borderTop: `4px solid ${c.color}` }}>
            <div className="flex items-center gap-3">
              <div className="text-xl" style={{ color: c.color }}>{c.icon}</div>
              <div>
                <p className="text-xs text-gray-500">{c.label}</p>
                <h3 className="text-lg font-semibold text-gray-900">{c.value}</h3>
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaChartBar /> Occupancy</h3>
          <div className="h-64">{stats?.blockStats?.length > 0 ? <Pie data={occupancyData} options={chartOptions} /> : <div className="flex flex-col items-center justify-center py-8 text-gray-400"><FaChartBar size={32} className="mb-2" /><p className="text-sm">No data</p></div>}</div>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaExclamationTriangle /> Complaints</h3>
          <div className="h-64"><Bar data={barData} options={chartOptions} /></div>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaCalendarAlt /> Leaves</h3>
          <div className="h-64"><Line data={lineData} options={chartOptions} /></div>
        </div>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Occupancy Overview</h3>
          {stats?.blockStats?.length > 0 ? (
            <div className="space-y-4">
              {stats.blockStats.map((b, i) => (
                <div key={i}>
                  <div className="flex items-center justify-between text-sm mb-1">
                    <span className="font-medium text-gray-700">{b.name}</span>
                    <span className="text-gray-500">{b.occupied}/{b.capacity} rooms</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div className="bg-[#1a237e] h-2 rounded-full" style={{ width: `${b.capacity > 0 ? (b.occupied / b.capacity) * 100 : 0}%` }} />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-8 text-gray-400"><FaChartBar size={32} className="mb-2" /><p className="text-sm">No block data</p></div>
          )}
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h3>
          {stats?.recentActivities?.length > 0 ? (
            <div className="space-y-3">
              {stats.recentActivities.map((a, i) => (
                <div key={i} className="flex items-start gap-3">
                  <div className="w-2 h-2 mt-2 rounded-full bg-[#1a237e] shrink-0" />
                  <div>
                    <p className="text-sm text-gray-700">{a.message || a.description}</p>
                    <span className="text-xs text-gray-400">{a.date?.slice(0, 10) || a.createdAt?.slice(0, 10)}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-8 text-gray-400"><FaChartBar size={32} className="mb-2" /><p className="text-sm">No recent activity</p></div>
          )}
        </div>
      </div>
    </div>
  );
};

const AdminManageStudents = () => {
  const [students, setStudents] = useState([]);
  const [search, setSearch] = useState('');
  const [viewStudent, setViewStudent] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchStudents = useCallback(async () => {
    try {
      const data = await adminService.getStudents();
      setStudents(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error('Failed to load students');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchStudents(); }, [fetchStudents]);

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this student? This action cannot be undone.')) return;
    try {
      await adminService.deleteStudent(id);
      setStudents(students.filter(s => s.id !== id));
      toast.success('Student deleted');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to delete student');
    }
  };

  const filtered = students.filter(s =>
    s.name?.toLowerCase().includes(search.toLowerCase()) ||
    s.email?.toLowerCase().includes(search.toLowerCase()) ||
    s.enrollmentNo?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaUsers /> Manage Students</h1>
        <p className="text-sm text-gray-500 mb-3">View and manage all registered students</p>
        <div className="relative max-w-md">
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
            placeholder="Search by name, email, or enrollment..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaUsers size={32} className="mb-2" /><p className="text-sm">No students found</p></div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Name</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Email</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Enrollment</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Phone</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Room</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((s, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4"><div className="flex items-center gap-2"><div className="w-8 h-8 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-xs font-bold">{s.name?.charAt(0)}</div><span className="font-medium text-gray-900">{s.name}</span></div></td>
                    <td className="py-3 px-4 text-gray-700">{s.email}</td>
                    <td className="py-3 px-4 text-gray-700">{s.enrollmentNo || '\u2014'}</td>
                    <td className="py-3 px-4 text-gray-700">{s.phone || '\u2014'}</td>
                    <td className="py-3 px-4 text-gray-700">{s.room?.roomNumber || s.roomNumber || 'Not allocated'}</td>
                    <td className="py-3 px-4"><div className="flex gap-2"><button className="flex items-center gap-1 bg-blue-500 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-blue-600" onClick={() => setViewStudent(s)}><FaEye /> View</button><button className="flex items-center gap-1 bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700" onClick={() => handleDelete(s.id)}><FaTrash /> Delete</button></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {viewStudent && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setViewStudent(null)}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-lg mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Student Details</h3>
              <button className="text-gray-400 hover:text-gray-600" onClick={() => setViewStudent(null)}><FaTimes /></button>
            </div>
            <div className="flex justify-center mb-4">
              <div className="w-16 h-16 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-2xl font-bold">{viewStudent.name?.charAt(0)}</div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              {[
                { label: 'Name', value: viewStudent.name }, { label: 'Email', value: viewStudent.email },
                { label: 'Phone', value: viewStudent.phone || '\u2014' }, { label: 'Enrollment No.', value: viewStudent.enrollmentNo || '\u2014' },
                { label: 'Gender', value: viewStudent.gender || '\u2014' }, { label: 'Date of Birth', value: viewStudent.dateOfBirth?.slice(0, 10) || '\u2014' },
                { label: 'Room', value: viewStudent.room?.roomNumber || viewStudent.roomNumber || 'Not allocated' }, { label: 'Parent Contact', value: viewStudent.parentContact || '\u2014' },
              ].map((f, i) => (
                <div key={i}>
                  <p className="text-xs text-gray-500 uppercase tracking-wide">{f.label}</p>
                  <p className="text-sm font-medium text-gray-900">{f.value}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const AdminManageWardens = () => {
  const [wardens, setWardens] = useState([]);
  const [blocks, setBlocks] = useState([]);
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '' });
  const [assignData, setAssignData] = useState({ wardenId: '', blockId: '' });
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    try {
      const [wardenData, blks] = await Promise.all([adminService.getAllWardens(), adminService.getHostelBlocks()]);
      const wardenList = Array.isArray(wardenData) ? wardenData : [];
      setWardens(wardenList);
      setBlocks(Array.isArray(blks) ? blks : []);
    } catch (err) {
      toast.error('Failed to load data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleCreateWarden = async (e) => {
    e.preventDefault();
    if (!form.name || !form.email || !form.password) return toast.error('Please fill required fields');
    setSubmitting(true);
    try {
      await adminService.createWarden(form);
      toast.success('Warden created successfully');
      setShowCreate(false);
      setForm({ name: '', email: '', password: '', phone: '' });
      fetchData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create warden');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteWarden = async (id) => {
    if (!window.confirm('Delete this warden?')) return;
    try {
      await adminService.deleteWarden(id);
      setWardens(wardens.filter(w => w.id !== id));
      toast.success('Warden deleted');
    } catch (err) {
      toast.error('Failed to delete warden');
    }
  };

  const handleAssignBlock = async () => {
    if (!assignData.wardenId || !assignData.blockId) return toast.error('Select warden and block');
    try {
      await wardenService.assignBlock(assignData.wardenId, assignData.blockId);
      toast.success('Block assigned');
      setAssignData({ wardenId: '', blockId: '' });
      fetchData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to assign block');
    }
  };

  const filtered = wardens.filter(w => w.name?.toLowerCase().includes(search.toLowerCase()) || w.email?.toLowerCase().includes(search.toLowerCase()));

  return (
    <div className="p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaUserShield /> Manage Wardens</h1>
          <p className="text-sm text-gray-500">Create and manage warden accounts</p>
        </div>
        <button className="flex items-center gap-2 bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1]" onClick={() => setShowCreate(true)}><FaPlus /> Create Warden</button>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaLink /> Assign Block to Warden</h3>
        <div className="flex flex-col sm:flex-row gap-4 items-end">
          <div className="flex-1 w-full">
            <label className="block text-sm font-medium text-gray-700 mb-1">Select Warden</label>
            <select className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={assignData.wardenId} onChange={(e) => setAssignData({ ...assignData, wardenId: e.target.value })}>
              <option value="">Choose warden</option>
              {wardens.map((w, i) => <option key={i} value={w.id}>{w.name} ({w.email})</option>)}
            </select>
          </div>
          <div className="flex-1 w-full">
            <label className="block text-sm font-medium text-gray-700 mb-1">Select Block</label>
            <select className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={assignData.blockId} onChange={(e) => setAssignData({ ...assignData, blockId: e.target.value })}>
              <option value="">Choose block</option>
              {blocks.map((b, i) => <option key={i} value={b.id}>{b.name}</option>)}
            </select>
          </div>
          <button className="flex items-center gap-1 bg-[#1a237e] text-white px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] whitespace-nowrap" onClick={handleAssignBlock}><FaLink /> Assign</button>
        </div>
      </div>

      <div className="relative max-w-md mb-4">
        <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
        <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm" placeholder="Search wardens..." value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaUserShield size={32} className="mb-2" /><p className="text-sm">No wardens found</p></div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Name</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Email</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Phone</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Assigned Block</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((w, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4"><div className="flex items-center gap-2"><div className="w-8 h-8 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-xs font-bold">{w.name?.charAt(0)}</div><span className="font-medium text-gray-900">{w.name}</span></div></td>
                    <td className="py-3 px-4 text-gray-700">{w.email}</td>
                    <td className="py-3 px-4 text-gray-700">{w.phone || '\u2014'}</td>
                    <td className="py-3 px-4 text-gray-700">{w.assignedBlock?.name || w.blockName || 'Not assigned'}</td>
                    <td className="py-3 px-4"><button className="flex items-center gap-1 bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700" onClick={() => handleDeleteWarden(w.id)}><FaTrash /> Delete</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setShowCreate(false)}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Create Warden</h3>
              <button className="text-gray-400 hover:text-gray-600" onClick={() => setShowCreate(false)}><FaTimes /></button>
            </div>
            <form onSubmit={handleCreateWarden} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                <input type="text" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <input type="email" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                <input type="password" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Phone (optional)</label>
                <input type="text" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              </div>
              <div className="flex justify-end gap-2">
                <button type="button" className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-gray-400" onClick={() => setShowCreate(false)}>Cancel</button>
                <button type="submit" className="bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] disabled:opacity-50" disabled={submitting}>{submitting ? 'Creating...' : 'Create Warden'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

const AdminManageBlocks = () => {
  const [blocks, setBlocks] = useState([]);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ name: '', code: '', address: '' });
  const [expandedBlock, setExpandedBlock] = useState(null);
  const [blockRooms, setBlockRooms] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchBlocks = useCallback(async () => {
    try {
      const data = await adminService.getHostelBlocks();
      setBlocks(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error('Failed to load blocks');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchBlocks(); }, [fetchBlocks]);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!form.name || !form.code) return toast.error('Name and code are required');
    setSubmitting(true);
    try {
      await adminService.createHostelBlock(form);
      toast.success('Block created');
      setShowCreate(false);
      setForm({ name: '', code: '', address: '' });
      fetchBlocks();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create block');
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewRooms = async (block) => {
    if (expandedBlock === block.id) { setExpandedBlock(null); setBlockRooms([]); return; }
    setExpandedBlock(block.id);
    try {
      const rooms = await roomService.getRoomsByBlock(block.id);
      setBlockRooms(Array.isArray(rooms) ? rooms : []);
    } catch (err) {
      toast.error('Failed to load rooms');
      setBlockRooms([]);
    }
  };

  return (
    <div className="p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaBuilding /> Hostel Blocks</h1>
          <p className="text-sm text-gray-500">Manage hostel blocks and their rooms</p>
        </div>
        <button className="flex items-center gap-2 bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1]" onClick={() => setShowCreate(true)}><FaPlus /> Add Block</button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
      ) : blocks.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaBuilding size={32} className="mb-2" /><p className="text-sm">No blocks created</p></div>
      ) : (
        <div className="space-y-4">
          {blocks.map((b, i) => (
            <div key={i} className="bg-white rounded-xl shadow-sm p-6">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">{b.name}</h3>
                  <p className="text-sm text-gray-500">Code: {b.code} {b.address && `| ${b.address}`}</p>
                </div>
                <button className="flex items-center gap-1 px-3 py-1.5 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:border-[#1a237e] hover:text-[#1a237e]" onClick={() => handleViewRooms(b)}>
                  <FaEye /> {expandedBlock === b.id ? 'Hide Rooms' : 'View Rooms'}
                </button>
              </div>
              {expandedBlock === b.id && (
                <div className="mt-4">
                  <h4 className="text-sm font-semibold text-gray-700 mb-2">Rooms in {b.name}</h4>
                  {blockRooms.length === 0 ? (
                    <p className="text-sm text-gray-400">No rooms in this block</p>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="border-b border-gray-100">
                            <th className="text-left py-2 text-gray-500 font-medium">Room No.</th>
                            <th className="text-left py-2 text-gray-500 font-medium">Floor</th>
                            <th className="text-left py-2 text-gray-500 font-medium">Capacity</th>
                            <th className="text-left py-2 text-gray-500 font-medium">Occupants</th>
                          </tr>
                        </thead>
                        <tbody>
                          {blockRooms.map((r, ri) => (
                            <tr key={ri} className="border-b border-gray-50">
                              <td className="py-2 text-gray-700">{r.roomNumber || r.roomNo}</td>
                              <td className="py-2 text-gray-700">{r.floor}</td>
                              <td className="py-2 text-gray-700">{r.capacity}</td>
                              <td className="py-2 text-gray-700">{r.occupants?.length || r.currentOccupancy || 0}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setShowCreate(false)}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Create Hostel Block</h3>
              <button className="text-gray-400 hover:text-gray-600" onClick={() => setShowCreate(false)}><FaTimes /></button>
            </div>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Block Name</label>
                <input type="text" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="e.g., A-Block" required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Block Code</label>
                <input type="text" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="e.g., BLK-A" required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Address (optional)</label>
                <textarea className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} placeholder="Block address/location" rows={2} />
              </div>
              <div className="flex justify-end gap-2">
                <button type="button" className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-gray-400" onClick={() => setShowCreate(false)}>Cancel</button>
                <button type="submit" className="bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] disabled:opacity-50" disabled={submitting}>{submitting ? 'Creating...' : 'Create Block'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

const AdminManageRooms = () => {
  const [rooms, setRooms] = useState([]);
  const [blocks, setBlocks] = useState([]);
  const [search, setSearch] = useState('');
  const [blockFilter, setBlockFilter] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ roomNo: '', block: '', floor: '', capacity: '', rent: '' });
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    try {
      const [rms, blks] = await Promise.all([roomService.getAllRooms(), adminService.getHostelBlocks()]);
      setRooms(Array.isArray(rms) ? rms : []);
      setBlocks(Array.isArray(blks) ? blks : []);
    } catch (err) {
      toast.error('Failed to load data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const openForm = (room = null) => {
    if (room) {
      setEditing(room);
      setForm({ roomNo: room.roomNumber || room.roomNo || '', block: room.block?.id || room.blockId || '', floor: room.floor || '', capacity: room.capacity || '', rent: room.rent || '' });
    } else {
      setEditing(null);
      setForm({ roomNo: '', block: '', floor: '', capacity: '', rent: '' });
    }
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.roomNo || !form.block || !form.capacity) return toast.error('Please fill required fields');
    setSubmitting(true);
    try {
      const payload = { roomNumber: form.roomNo, block: form.block, floor: form.floor, capacity: parseInt(form.capacity), rent: form.rent ? parseFloat(form.rent) : undefined };
      if (editing) {
        const res = await roomService.updateRoom(editing.id, payload);
        setRooms(rooms.map(r => r.id === editing.id ? res : r));
        toast.success('Room updated');
      } else {
        const res = await roomService.addRoom(payload);
        setRooms([res, ...rooms]);
        toast.success('Room created');
      }
      setShowForm(false);
      setEditing(null);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save room');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this room?')) return;
    try {
      await roomService.deleteRoom(id);
      setRooms(rooms.filter(r => r.id !== id));
      toast.success('Room deleted');
    } catch (err) {
      toast.error('Failed to delete room');
    }
  };

  const filtered = rooms.filter(r => {
    const matchSearch = !search || r.roomNumber?.toString().includes(search) || r.block?.name?.toLowerCase().includes(search.toLowerCase());
    const matchBlock = !blockFilter || r.block?.id === blockFilter || r.blockId === blockFilter;
    return matchSearch && matchBlock;
  });

  return (
    <div className="p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaDoorOpen /> Manage Rooms</h1>
          <p className="text-sm text-gray-500">Create, edit, and manage hostel rooms</p>
        </div>
        <button className="flex items-center gap-2 bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1]" onClick={() => openForm()}><FaPlus /> Add Room</button>
      </div>

      <div className="flex flex-col sm:flex-row gap-3 mb-4">
        <div className="relative flex-1">
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm" placeholder="Search rooms..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <select className="px-3 py-2.5 border border-gray-300 rounded-lg text-sm sm:w-48" value={blockFilter} onChange={(e) => setBlockFilter(e.target.value)}>
          <option value="">All Blocks</option>
          {blocks.map((b, i) => <option key={i} value={b.id}>{b.name}</option>)}
        </select>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaDoorOpen size={32} className="mb-2" /><p className="text-sm">No rooms found</p></div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Room No.</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Block</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Floor</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Capacity</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Occupants</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Rent</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((r, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4 font-semibold text-gray-900">{r.roomNumber || r.roomNo}</td>
                    <td className="py-3 px-4 text-gray-700">{r.block?.name || '\u2014'}</td>
                    <td className="py-3 px-4 text-gray-700">{r.floor || '\u2014'}</td>
                    <td className="py-3 px-4 text-gray-700">{r.capacity}</td>
                    <td className="py-3 px-4 text-gray-700">{r.occupants?.length || r.currentOccupancy || 0}</td>
                    <td className="py-3 px-4 text-gray-700">{r.rent ? `₹${r.rent}` : '\u2014'}</td>
                    <td className="py-3 px-4"><div className="flex gap-2"><button className="flex items-center gap-1 bg-[#1a237e] text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-[#0d47a1]" onClick={() => openForm(r)}><FaEdit /> Edit</button><button className="flex items-center gap-1 bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700" onClick={() => handleDelete(r.id)}><FaTrash /> Delete</button></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showForm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setShowForm(false)}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-lg mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">{editing ? 'Edit Room' : 'Add New Room'}</h3>
              <button className="text-gray-400 hover:text-gray-600" onClick={() => setShowForm(false)}><FaTimes /></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Room Number</label>
                  <input type="text" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.roomNo} onChange={(e) => setForm({ ...form, roomNo: e.target.value })} required />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Block</label>
                  <select className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.block} onChange={(e) => setForm({ ...form, block: e.target.value })} required>
                    <option value="">Select block</option>
                    {blocks.map((b, i) => <option key={i} value={b.id}>{b.name}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Floor</label>
                  <input type="number" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.floor} onChange={(e) => setForm({ ...form, floor: e.target.value })} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Capacity</label>
                  <input type="number" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} required />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Rent ($/month)</label>
                  <input type="number" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.rent} onChange={(e) => setForm({ ...form, rent: e.target.value })} step="0.01" />
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <button type="button" className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-gray-400" onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" className="bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] disabled:opacity-50" disabled={submitting}>{submitting ? 'Saving...' : editing ? 'Update Room' : 'Add Room'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

const AdminLeaves = () => {
  const [leaves, setLeaves] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    leaveService.getAllLeaves()
      .then(data => setLeaves(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load leaves'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = leaves.filter(l => {
    const matchSearch = !search || l.student?.name?.toLowerCase().includes(search.toLowerCase()) || l.reason?.toLowerCase().includes(search.toLowerCase());
    const matchStatus = !statusFilter || l.status === statusFilter;
    return matchSearch && matchStatus;
  });

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaCalendarAlt /> Leave Management</h1>
        <p className="text-sm text-gray-500 mb-3">View all leave requests across the hostel</p>
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm" placeholder="Search leaves..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="px-3 py-2.5 border border-gray-300 rounded-lg text-sm sm:w-40" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All Status</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaCalendarAlt size={32} className="mb-2" /><p className="text-sm">No leaves found</p></div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Student</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">From</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">To</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Reason</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((l, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4"><div className="flex items-center gap-2"><div className="w-8 h-8 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-xs font-bold">{l.student?.name?.charAt(0) || '?'}</div><span className="font-medium text-gray-900">{l.student?.name || l.studentName}</span></div></td>
                    <td className="py-3 px-4 text-gray-700">{l.fromDate?.slice(0, 10)}</td>
                    <td className="py-3 px-4 text-gray-700">{l.toDate?.slice(0, 10)}</td>
                    <td className="py-3 px-4 text-gray-700 max-w-[200px] truncate">{l.reason}</td>
                    <td className="py-3 px-4">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${l.status === 'APPROVED' ? 'bg-green-100 text-green-700' : l.status === 'REJECTED' ? 'bg-red-100 text-red-700' : 'bg-yellow-100 text-yellow-700'}`}>{l.status}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

const AdminComplaints = () => {
  const [complaints, setComplaints] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    complaintService.getAllComplaints()
      .then(data => setComplaints(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load complaints'))
      .finally(() => setLoading(false));
  }, []);

  const handleUpdateStatus = async (id, status) => {
    try {
      await complaintService.updateStatus(id, status);
      setComplaints(complaints.map(c => c.id === id ? { ...c, status } : c));
      toast.success(`Complaint ${status.toLowerCase()}`);
    } catch (err) {
      toast.error('Failed to update status');
    }
  };

  const filtered = complaints.filter(c => {
    const matchSearch = !search || c.title?.toLowerCase().includes(search.toLowerCase()) || c.student?.name?.toLowerCase().includes(search.toLowerCase());
    const matchStatus = !statusFilter || c.status === statusFilter;
    return matchSearch && matchStatus;
  });

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaExclamationTriangle /> Complaints</h1>
        <p className="text-sm text-gray-500 mb-3">View and manage all complaints</p>
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm" placeholder="Search complaints..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="px-3 py-2.5 border border-gray-300 rounded-lg text-sm sm:w-40" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All Status</option>
            <option value="PENDING">Pending</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaExclamationTriangle size={32} className="mb-2" /><p className="text-sm">No complaints found</p></div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Title</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Student</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Date</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Status</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((c, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4 font-semibold text-gray-900">{c.title}</td>
                    <td className="py-3 px-4 text-gray-700">{c.student?.name || c.studentName || '\u2014'}</td>
                    <td className="py-3 px-4 text-gray-700">{c.createdAt?.slice(0, 10)}</td>
                    <td className="py-3 px-4">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${c.status === 'RESOLVED' ? 'bg-green-100 text-green-700' : c.status === 'REJECTED' ? 'bg-red-100 text-red-700' : c.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-700' : 'bg-yellow-100 text-yellow-700'}`}>{c.status}</span>
                    </td>
                    <td className="py-3 px-4">
                      <select className="px-3 py-1.5 border border-gray-300 rounded-lg text-xs" value={c.status} onChange={(e) => handleUpdateStatus(c.id, e.target.value)}>
                        <option value="PENDING">Pending</option>
                        <option value="IN_PROGRESS">In Progress</option>
                        <option value="RESOLVED">Resolved</option>
                        <option value="REJECTED">Rejected</option>
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

const AdminNotices = () => {
  const [notices, setNotices] = useState([]);
  const [form, setForm] = useState({ title: '', content: '', targetRole: 'ALL', expiryDate: '' });
  const [editing, setEditing] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    noticeService.getAllNotices()
      .then(data => setNotices(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load notices'))
      .finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title.trim() || !form.content.trim()) return toast.error('Please fill all fields');
    setSubmitting(true);
    try {
      if (editing) {
        const res = await noticeService.updateNotice(editing.id, form);
        setNotices(notices.map(n => n.id === editing.id ? res : n));
        toast.success('Notice updated');
      } else {
        const res = await noticeService.createNotice(form);
        setNotices([res, ...notices]);
        toast.success('Notice created');
      }
      setForm({ title: '', content: '', targetRole: 'ALL', expiryDate: '' });
      setEditing(null);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save notice');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this notice?')) return;
    try {
      await noticeService.deleteNotice(id);
      setNotices(notices.filter(n => n.id !== id));
      toast.success('Notice deleted');
    } catch (err) {
      toast.error('Failed to delete notice');
    }
  };

  const startEdit = (notice) => {
    setEditing(notice);
    setForm({ title: notice.title, content: notice.content, targetRole: notice.targetRole || 'ALL', expiryDate: notice.expiryDate?.slice(0, 10) || '' });
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaBullhorn /> Notices</h1>
        <p className="text-sm text-gray-500">Create and manage notices</p>
      </div>
      <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">{editing ? 'Edit Notice' : 'Create New Notice'}</h3>
          {editing && <button className="flex items-center gap-1 px-3 py-1.5 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:border-gray-400" onClick={() => { setEditing(null); setForm({ title: '', content: '', targetRole: 'ALL', expiryDate: '' }); }}><FaTimes /> Cancel</button>}
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
              <input type="text" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="Notice title" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Target Audience</label>
              <select className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.targetRole} onChange={(e) => setForm({ ...form, targetRole: e.target.value })}>
                <option value="ALL">All</option>
                <option value="STUDENT">Students Only</option>
                <option value="WARDEN">Wardens Only</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Content</label>
            <textarea className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} placeholder="Notice content..." rows={4} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Expiry Date (optional)</label>
            <input type="date" className="w-full max-w-xs px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={form.expiryDate} onChange={(e) => setForm({ ...form, expiryDate: e.target.value })} />
          </div>
          <button type="submit" className="bg-[#1a237e] text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] disabled:opacity-50" disabled={submitting}>{submitting ? 'Saving...' : editing ? 'Update Notice' : 'Create Notice'}</button>
        </form>
      </div>
      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <div className="p-4 border-b border-gray-100"><h3 className="text-lg font-semibold text-gray-900">All Notices</h3></div>
        {loading ? (
          <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
        ) : notices.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-gray-400"><FaBullhorn size={32} className="mb-2" /><p className="text-sm">No notices created</p></div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Title</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Target</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Created</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Expires</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {notices.map((n, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4 font-semibold text-gray-900">{n.title}</td>
                    <td className="py-3 px-4"><span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">{n.targetRole || n.target || 'ALL'}</span></td>
                    <td className="py-3 px-4 text-gray-700">{n.createdAt?.slice(0, 10)}</td>
                    <td className="py-3 px-4 text-gray-700">{n.expiryDate?.slice(0, 10) || '\u2014'}</td>
                    <td className="py-3 px-4"><div className="flex gap-2"><button className="flex items-center gap-1 bg-[#1a237e] text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-[#0d47a1]" onClick={() => startEdit(n)}><FaEdit /> Edit</button><button className="flex items-center gap-1 bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700" onClick={() => handleDelete(n.id)}><FaTrash /> Delete</button></div></td>
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

const AdminMarketplace = () => {
  const [items, setItems] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    marketplaceService.getAllItems()
      .then(data => setItems(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load marketplace'))
      .finally(() => setLoading(false));
  }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this item?')) return;
    try {
      await marketplaceService.deleteItem(id);
      setItems(items.filter(i => i.id !== id));
      toast.success('Item deleted');
    } catch (err) {
      toast.error('Failed to delete item');
    }
  };

  const handleMarkSold = async (id) => {
    try {
      await marketplaceService.markAsSold(id);
      setItems(items.map(i => i.id === id ? { ...i, status: 'SOLD' } : i));
      toast.success('Item marked as sold');
    } catch (err) {
      toast.error('Failed to update item');
    }
  };

  const filtered = items.filter(i => i.title?.toLowerCase().includes(search.toLowerCase()) || i.seller?.name?.toLowerCase().includes(search.toLowerCase()));

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaStore /> Marketplace</h1>
        <p className="text-sm text-gray-500 mb-3">View all marketplace items</p>
        <div className="relative max-w-md">
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm" placeholder="Search items..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaStore size={32} className="mb-2" /><p className="text-sm">No items found</p></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((item, i) => (
            <div key={i} className="bg-white rounded-xl shadow-sm overflow-hidden">
              {item.image ? <img src={item.image} alt={item.title} className="w-full h-40 object-cover" /> : <div className="w-full h-40 bg-gray-100 flex items-center justify-center"><FaStore size={40} className="text-gray-300" /></div>}
              <div className="p-4">
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700 mb-2">{item.category?.toUpperCase()}</span>
                <h3 className="text-sm font-semibold text-gray-900">{item.title}</h3>
                <p className="text-lg font-bold text-[#1a237e]">₹{item.price}</p>
                <p className="text-xs text-gray-500 mt-1">{item.description?.substring(0, 60)}</p>
                <p className="text-xs text-gray-400 mt-1">Sold by: {item.seller?.name || 'Unknown'}</p>
                <div className="flex gap-2 mt-3">
                  {item.status !== 'SOLD' && <button className="flex items-center gap-1 bg-green-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-green-700" onClick={() => handleMarkSold(item.id)}><FaCheck /> Mark Sold</button>}
                  <button className="flex items-center gap-1 bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700" onClick={() => handleDelete(item.id)}><FaTrash /> Delete</button>
                </div>
                {item.status === 'SOLD' && <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700 mt-2">SOLD</span>}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const AdminLostFound = () => {
  const [items, setItems] = useState([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    lostFoundService.getAllItems()
      .then(data => setItems(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load lost & found items'))
      .finally(() => setLoading(false));
  }, []);

  const handleUpdateStatus = async (id, status) => {
    try {
      await lostFoundService.updateStatus(id, status);
      setItems(items.map(i => i.id === id ? { ...i, status } : i));
      toast.success(`Item ${status.toLowerCase()}`);
    } catch (err) {
      toast.error('Failed to update status');
    }
  };

  const filtered = items.filter(i => {
    const matchSearch = !search || i.title?.toLowerCase().includes(search.toLowerCase()) || i.location?.toLowerCase().includes(search.toLowerCase());
    const matchStatus = !statusFilter || i.status === statusFilter;
    return matchSearch && matchStatus;
  });

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaSearch /> Lost & Found</h1>
        <p className="text-sm text-gray-500 mb-3">View all lost and found items</p>
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm" placeholder="Search items..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="px-3 py-2.5 border border-gray-300 rounded-lg text-sm sm:w-40" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All Status</option>
            <option value="OPEN">Open</option>
            <option value="RESOLVED">Resolved</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaSearch size={32} className="mb-2" /><p className="text-sm">No items found</p></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((item, i) => (
            <div key={i} className="bg-white rounded-xl shadow-sm overflow-hidden">
              {item.image && <img src={item.image} alt={item.title} className="w-full h-40 object-cover" />}
              <div className="p-4">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium mb-2 ${item.category === 'lost' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'}`}>{item.category?.toUpperCase()}</span>
                <h3 className="text-sm font-semibold text-gray-900">{item.title}</h3>
                <p className="text-xs text-gray-500 mt-1">{item.description}</p>
                <p className="text-xs text-gray-400 mt-1"><FaMapMarkerAlt className="inline mr-1" />{item.location}</p>
                <div className="flex items-center justify-between mt-3">
                  <select className="px-3 py-1.5 border border-gray-300 rounded-lg text-xs" value={item.status} onChange={(e) => handleUpdateStatus(item.id, e.target.value)}>
                    <option value="OPEN">Open</option>
                    <option value="RESOLVED">Resolved</option>
                  </select>
                  {item.contactInfo && <span className="text-xs text-gray-500"><FaPhone className="inline mr-1" />{item.contactInfo}</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const AdminMessFeedback = () => {
  const [feedback, setFeedback] = useState([]);
  const [averages, setAverages] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      messFeedbackService.getAllFeedback(),
      messFeedbackService.getAverages(),
    ]).then(([fb, avg]) => {
      setFeedback(Array.isArray(fb) ? fb : []);
      setAverages(avg);
    }).catch(() => toast.error('Failed to load feedback'))
      .finally(() => setLoading(false));
  }, []);

  const avgRatings = averages || {};
  const avgCategories = [
    { label: 'Food Quality', value: avgRatings.foodQuality },
    { label: 'Taste', value: avgRatings.taste },
    { label: 'Cleanliness', value: avgRatings.cleanliness },
  ];

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaStar /> Mess Feedback</h1>
        <p className="text-sm text-gray-500">View all mess feedback and averages</p>
      </div>

      {averages && Object.keys(averages).length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          {avgCategories.map((c, i) => (
            <div key={i} className="bg-white rounded-xl shadow-sm p-6 text-center">
              <h3 className="text-sm text-gray-500 mb-1">{c.label}</h3>
              <p className="text-3xl font-bold text-[#1a237e] mb-2">{c.value ? Number(c.value).toFixed(1) : '\u2014'}</p>
              <div className="flex justify-center gap-1">{ [1,2,3,4,5].map(s => <span key={s} className={`text-xl ${s <= Math.round(c.value || 0) ? 'text-yellow-400' : 'text-gray-300'}`}>{'\u2605'}</span>) }</div>
            </div>
          ))}
        </div>
      )}

      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <div className="p-4 border-b border-gray-100"><h3 className="text-lg font-semibold text-gray-900">All Feedback</h3></div>
        {loading ? (
          <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
        ) : feedback.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-gray-400"><FaStar size={32} className="mb-2" /><p className="text-sm">No feedback yet</p></div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Student</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Date</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Food</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Taste</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Cleanliness</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Comments</th>
                </tr>
              </thead>
              <tbody>
                {feedback.map((f, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4 font-medium text-gray-900">{f.student?.name || f.studentName || 'Unknown'}</td>
                    <td className="py-3 px-4 text-gray-700">{f.createdAt?.slice(0, 10)}</td>
                    <td className="py-3 px-4">{'\u2605'.repeat(f.foodQuality)}{'\u2606'.repeat(5 - f.foodQuality)}</td>
                    <td className="py-3 px-4">{'\u2605'.repeat(f.taste)}{'\u2606'.repeat(5 - f.taste)}</td>
                    <td className="py-3 px-4">{'\u2605'.repeat(f.cleanliness)}{'\u2606'.repeat(5 - f.cleanliness)}</td>
                    <td className="py-3 px-4 text-gray-500 max-w-[150px] truncate">{f.comments || '\u2014'}</td>
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

const AdminReports = () => {
  const [reports, setReports] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      adminService.getReports(),
      leaveService.getLeaveStats(),
      complaintService.getComplaintStats(),
    ]).then(([rpt, lstats, cstats]) => {
      setReports({ ...rpt, leaveStats: lstats, complaintStats: cstats });
    }).catch(() => toast.error('Failed to load reports'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="w-10 h-10 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
      </div>
    );
  }

  const reportSections = [
    {
      title: 'System Overview', icon: <FaChartBar />,
      items: [
        { label: 'Total Students', value: reports?.totalStudents || '\u2014' },
        { label: 'Total Wardens', value: reports?.totalWardens || '\u2014' },
        { label: 'Total Rooms', value: reports?.totalRooms || '\u2014' },
        { label: 'Occupancy Rate', value: reports?.occupancyRate ? `${reports.occupancyRate}%` : '\u2014' },
      ],
    },
    {
      title: 'Leave Statistics', icon: <FaCalendarAlt />,
      items: [
        { label: 'Total Leaves', value: reports?.leaveStats?.total || '\u2014' },
        { label: 'Approved', value: reports?.leaveStats?.approved || '\u2014' },
        { label: 'Rejected', value: reports?.leaveStats?.rejected || '\u2014' },
        { label: 'Pending', value: reports?.leaveStats?.pending || '\u2014' },
      ],
    },
    {
      title: 'Complaint Statistics', icon: <FaExclamationTriangle />,
      items: [
        { label: 'Total Complaints', value: reports?.complaintStats?.total || '\u2014' },
        { label: 'Resolved', value: reports?.complaintStats?.resolved || '\u2014' },
        { label: 'Pending', value: reports?.complaintStats?.pending || '\u2014' },
        { label: 'In Progress', value: reports?.complaintStats?.inProgress || '\u2014' },
      ],
    },
    {
      title: 'Financial Summary', icon: <FaShoppingCart />,
      items: [
        { label: 'Total Rooms with Rent', value: reports?.roomsWithRent || '\u2014' },
        { label: 'Average Rent', value: reports?.averageRent ? `₹${reports.averageRent}` : '\u2014' },
        { label: 'Total Blocks', value: reports?.totalBlocks || '\u2014' },
        { label: 'Active Students', value: reports?.activeStudents || '\u2014' },
      ],
    },
  ];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaFileAlt /> Reports & Analytics</h1>
          <p className="text-sm text-gray-500">System-wide statistics and insights</p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-[#1a237e] hover:text-[#1a237e]" onClick={() => toast.info('Report download initiated')}>
          <FaDownload /> Export Report
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {reportSections.map((section, si) => (
          <div key={si} className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center gap-2 mb-4">
              <span className="text-[#1a237e]">{section.icon}</span>
              <h3 className="text-sm font-semibold text-gray-900">{section.title}</h3>
            </div>
            <div className="space-y-3">
              {section.items.map((item, ii) => (
                <div key={ii} className="flex items-center justify-between">
                  <span className="text-xs text-gray-500">{item.label}</span>
                  <span className="text-sm font-bold text-[#1a237e]">{item.value}</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaChartBar /> Occupancy Chart</h3>
        {reports?.blockStats?.length > 0 ? (
          <div className="space-y-4">
            {reports.blockStats.map((b, i) => (
              <div key={i}>
                <div className="flex items-center justify-between text-sm mb-1">
                  <span className="font-medium text-gray-700">{b.name}</span>
                  <span className="text-gray-500">{b.occupied}/{b.capacity} ({b.capacity > 0 ? Math.round((b.occupied / b.capacity) * 100) : 0}%)</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div className="bg-[#1a237e] h-2 rounded-full" style={{ width: `${b.capacity > 0 ? (b.occupied / b.capacity) * 100 : 0}%` }} />
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-8 text-gray-400"><FaChartBar size={32} className="mb-2" /><p className="text-sm">No occupancy data available</p></div>
        )}
      </div>
    </div>
  );
};

const AdminAIAnalytics = () => {
  const [categorizeForm, setCategorizeForm] = useState({ title: '', description: '' });
  const [categorizeResult, setCategorizeResult] = useState(null);
  const [categorizing, setCategorizing] = useState(false);
  const [sentimentText, setSentimentText] = useState('');
  const [sentimentResult, setSentimentResult] = useState(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [recommendations, setRecommendations] = useState([]);
  const [loadingRecs, setLoadingRecs] = useState(false);

  const handleCategorize = async (e) => {
    e.preventDefault();
    if (!categorizeForm.title.trim() || !categorizeForm.description.trim()) return toast.error('Please fill all fields');
    setCategorizing(true);
    try {
      const res = await aiService.categorizeComplaint(categorizeForm.title, categorizeForm.description);
      setCategorizeResult(res);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to categorize');
    } finally {
      setCategorizing(false);
    }
  };

  const handleSentiment = async () => {
    if (!sentimentText.trim()) return toast.error('Please enter feedback text');
    setAnalyzing(true);
    try {
      const res = await aiService.analyzeSentiment(sentimentText);
      setSentimentResult(res);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to analyze sentiment');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleGetRecommendations = async () => {
    setLoadingRecs(true);
    try {
      const data = await aiService.getRecommendedRooms();
      setRecommendations(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error('Failed to get recommendations');
    } finally {
      setLoadingRecs(false);
    }
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaRobot /> AI Analytics</h1>
        <p className="text-sm text-gray-500">Leverage AI for smarter hostel management</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaLightbulb /> Categorize Complaint</h3>
          <form onSubmit={handleCategorize} className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Complaint Title</label>
              <input type="text" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={categorizeForm.title} onChange={(e) => setCategorizeForm({ ...categorizeForm, title: e.target.value })} placeholder="Enter complaint title" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={categorizeForm.description} onChange={(e) => setCategorizeForm({ ...categorizeForm, description: e.target.value })} placeholder="Describe the complaint..." rows={3} />
            </div>
            <button type="submit" className="bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] disabled:opacity-50" disabled={categorizing}>
              {categorizing ? 'Analyzing...' : 'Categorize'}
            </button>
          </form>
          {categorizeResult && (
            <div className="mt-4 p-4 bg-blue-50 rounded-lg">
              <p className="text-sm font-medium text-[#1a237e]">Category: {categorizeResult.category || categorizeResult.predictedCategory || 'N/A'}</p>
              {categorizeResult.confidence && <p className="text-xs text-gray-500 mt-1">Confidence: {categorizeResult.confidence}</p>}
              {categorizeResult.explanation && <p className="text-xs text-gray-500 mt-1">{categorizeResult.explanation}</p>}
            </div>
          )}
        </div>

        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaSmile /> Sentiment Analysis</h3>
          <div className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Feedback Text</label>
              <textarea className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm" value={sentimentText} onChange={(e) => setSentimentText(e.target.value)} placeholder="Enter feedback to analyze sentiment..." rows={3} />
            </div>
            <button className="bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] disabled:opacity-50" onClick={handleSentiment} disabled={analyzing}>
              {analyzing ? 'Analyzing...' : 'Analyze Sentiment'}
            </button>
          </div>
          {sentimentResult && (
            <div className="mt-4 p-4 bg-green-50 rounded-lg">
              <p className="text-sm font-medium text-green-700 flex items-center gap-2">
                {sentimentResult.sentiment === 'positive' ? <FaSmile className="text-green-500" /> : sentimentResult.sentiment === 'negative' ? <FaFrown className="text-red-500" /> : <FaSmile className="text-yellow-500" />}
                Sentiment: {sentimentResult.sentiment || 'N/A'}
              </p>
              {sentimentResult.score !== undefined && <p className="text-xs text-gray-500 mt-1">Score: {sentimentResult.score}</p>}
            </div>
          )}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2"><FaBed /> Room Recommendations</h3>
          <button className="bg-[#1a237e] text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] disabled:opacity-50" onClick={handleGetRecommendations} disabled={loadingRecs}>
            {loadingRecs ? 'Loading...' : 'Get Recommendations'}
          </button>
        </div>
        {loadingRecs ? (
          <div className="flex items-center justify-center py-6"><div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" /></div>
        ) : recommendations.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-gray-400"><FaBed size={32} className="mb-2" /><p className="text-sm">Click "Get Recommendations" to see AI-suggested rooms</p></div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {recommendations.map((r, i) => (
              <div key={i} className="p-4 border border-gray-200 rounded-xl">
                <h4 className="text-sm font-semibold text-gray-900">{r.roomNumber || r.roomNo || `Room #${i + 1}`}</h4>
                <p className="text-xs text-gray-500">Block: {r.block?.name || r.blockName || 'N/A'}</p>
                <p className="text-xs text-gray-500">Floor: {r.floor || 'N/A'} | Capacity: {r.capacity || 'N/A'}</p>
                {r.reason && <p className="text-xs text-gray-400 mt-2">{r.reason}</p>}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

const AdminDashboard = () => (
  <ProtectedRoute allowedRoles={['admin']}>
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <Sidebar />
      <div className="ml-64 mt-[70px] transition-all duration-300">
        <Routes>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<DashboardHome />} />
          <Route path="students" element={<AdminManageStudents />} />
          <Route path="wardens" element={<AdminManageWardens />} />
          <Route path="hostel-blocks" element={<AdminManageBlocks />} />
          <Route path="rooms" element={<AdminManageRooms />} />
          <Route path="leaves" element={<AdminLeaves />} />
          <Route path="complaints" element={<AdminComplaints />} />
          <Route path="notices" element={<AdminNotices />} />
          <Route path="marketplace" element={<AdminMarketplace />} />
          <Route path="lost-found" element={<AdminLostFound />} />
          <Route path="mess-feedback" element={<AdminMessFeedback />} />
          <Route path="audit" element={<AuditLogsPage />} />
          <Route path="reports" element={<AdminReports />} />
          <Route path="ai-analytics" element={<AdminAIAnalytics />} />
          <Route path="*" element={<Navigate to="dashboard" replace />} />
        </Routes>
      </div>
    </div>
  </ProtectedRoute>
);

export default AdminDashboard;
