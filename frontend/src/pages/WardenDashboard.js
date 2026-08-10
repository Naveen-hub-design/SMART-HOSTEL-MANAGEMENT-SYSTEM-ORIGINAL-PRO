import React, { useState, useEffect, useCallback } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import ProtectedRoute from '../components/common/ProtectedRoute';
import Navbar from '../components/common/Navbar';
import Sidebar from '../components/common/Sidebar';
import wardenService from '../services/wardenService';
import roomService from '../services/roomService';
import leaveService from '../services/leaveService';
import complaintService from '../services/complaintService';
import noticeService from '../services/noticeService';
import {
  FaUsers, FaDoorOpen, FaCalendarAlt, FaExclamationTriangle, FaBullhorn,
  FaCheck, FaTimes, FaEye, FaSearch, FaUser, FaEnvelope, FaPhone,
  FaIdCard, FaFilter, FaPlus, FaTrash, FaEdit, FaSave, FaBan,
  FaArrowLeft, FaHome, FaChartBar, FaClipboardList, FaStore, FaUtensils
} from 'react-icons/fa';

const DashboardHome = () => {
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

const WardenManageStudents = () => {
  const [students, setStudents] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    wardenService.getStudents()
      .then(data => setStudents(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load students'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = students.filter(s =>
    s.name?.toLowerCase().includes(search.toLowerCase()) ||
    s.email?.toLowerCase().includes(search.toLowerCase()) ||
    s.enrollmentNo?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaUsers /> Manage Students</h1>
        <p className="text-sm text-gray-500 mb-3">View all students under your supervision</p>
        <div className="relative max-w-md">
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
            placeholder="Search by name, email, or enrollment..."
            value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6">
          <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400">
          <FaUsers size={32} className="mb-2" />
          <p className="text-sm">No students found</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Name</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Email</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Enrollment No.</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Room</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Phone</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((s, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-xs font-bold">{s.name?.charAt(0)}</div>
                        <span className="text-gray-900 font-medium">{s.name}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-gray-700">{s.email}</td>
                    <td className="py-3 px-4 text-gray-700">{s.enrollmentNo || '\u2014'}</td>
                    <td className="py-3 px-4 text-gray-700">{s.room?.roomNumber || s.roomNumber || 'Not allocated'}</td>
                    <td className="py-3 px-4 text-gray-700">{s.phone || '\u2014'}</td>
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

const WardenManageRooms = () => {
  const [rooms, setRooms] = useState([]);
  const [students, setStudents] = useState([]);
  const [search, setSearch] = useState('');
  const [blockFilter, setBlockFilter] = useState('');
  const [showAllocate, setShowAllocate] = useState(null);
  const [selectedStudent, setSelectedStudent] = useState('');
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    try {
      const [rms, studs] = await Promise.all([
        roomService.getAllRooms(),
        wardenService.getStudents(),
      ]);
      setRooms(Array.isArray(rms) ? rms : []);
      setStudents(Array.isArray(studs) ? studs : []);
    } catch (err) {
      toast.error('Failed to load rooms');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleAllocate = async (roomId) => {
    if (!selectedStudent) return toast.error('Please select a student');
    try {
      await roomService.allocateRoom(roomId, selectedStudent);
      toast.success('Room allocated successfully');
      setShowAllocate(null);
      setSelectedStudent('');
      fetchData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Allocation failed');
    }
  };

  const handleVacate = async (studentId) => {
    if (!window.confirm('Vacate this room?')) return;
    try {
      await roomService.vacateRoom(studentId);
      toast.success('Room vacated');
      fetchData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to vacate room');
    }
  };

  const unallocatedStudents = students.filter(s => !s.room && !s.roomNumber);
  const blocks = [...new Set(rooms.map(r => r.block?.name || r.blockName).filter(Boolean))];

  const filtered = rooms.filter(r => {
    const matchSearch = !search || r.roomNumber?.toString().includes(search) || r.block?.name?.toLowerCase().includes(search.toLowerCase());
    const matchBlock = !blockFilter || r.block?.id === blockFilter || r.blockName === blockFilter;
    return matchSearch && matchBlock;
  });

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaDoorOpen /> Room Management</h1>
        <p className="text-sm text-gray-500 mb-3">Manage room allocation and occupancy</p>
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
              placeholder="Search rooms..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none sm:w-48"
            value={blockFilter} onChange={(e) => setBlockFilter(e.target.value)}>
            <option value="">All Blocks</option>
            {blocks.map((b, i) => <option key={i} value={b}>{b}</option>)}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6">
          <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400">
          <FaDoorOpen size={32} className="mb-2" />
          <p className="text-sm">No rooms found</p>
        </div>
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
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Status</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((r, i) => {
                  const occ = r.occupants?.length || r.currentOccupancy || 0;
                  const cap = r.capacity || 999;
                  return (
                    <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                      <td className="py-3 px-4 font-semibold text-gray-900">{r.roomNumber || r.roomNo}</td>
                      <td className="py-3 px-4 text-gray-700">{r.block?.name || r.blockName || '\u2014'}</td>
                      <td className="py-3 px-4 text-gray-700">{r.floor || '\u2014'}</td>
                      <td className="py-3 px-4 text-gray-700">{r.capacity}</td>
                      <td className="py-3 px-4 text-gray-700">{occ}</td>
                      <td className="py-3 px-4">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${occ >= cap ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                          {occ >= cap ? 'FULL' : 'AVAILABLE'}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex gap-2">
                          {occ < cap && (
                            <button className="flex items-center gap-1 bg-[#1a237e] text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-[#0d47a1]"
                              onClick={() => setShowAllocate(showAllocate === r.id ? null : r.id)}><FaPlus /> Allocate</button>
                          )}
                          {occ > 0 && (
                            <button className="flex items-center gap-1 bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700"
                              onClick={() => { const sid = r.occupants?.[0]?.id || r.occupants?.[0]; if (sid) handleVacate(sid); }}><FaTimes /> Vacate</button>
                          )}
                        </div>
                        {showAllocate === r.id && (
                          <div className="mt-2 p-3 bg-gray-50 rounded-lg">
                            <select className="w-full px-3 py-2 border border-gray-300 rounded-lg text-xs mb-2"
                              value={selectedStudent} onChange={(e) => setSelectedStudent(e.target.value)}>
                              <option value="">Select student</option>
                              {unallocatedStudents.map((s, si) => (
                                <option key={si} value={s.id}>{s.name} ({s.enrollmentNo})</option>
                              ))}
                            </select>
                            <div className="flex gap-2">
                              <button className="bg-green-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-green-700"
                                onClick={() => handleAllocate(r.id)}><FaCheck /> Confirm</button>
                              <button className="border border-gray-300 text-gray-700 px-3 py-1.5 rounded-lg text-xs font-medium hover:border-gray-400"
                                onClick={() => { setShowAllocate(null); setSelectedStudent(''); }}>Cancel</button>
                            </div>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

const WardenLeaves = () => {
  const [leaves, setLeaves] = useState([]);
  const [tab, setTab] = useState('pending');
  const [rejectModal, setRejectModal] = useState(null);
  const [remarks, setRemarks] = useState('');
  const [loading, setLoading] = useState(true);

  const fetchLeaves = useCallback(async () => {
    try {
      const data = tab === 'pending' ? await leaveService.getPendingLeaves() : await leaveService.getAllLeaves();
      setLeaves(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error('Failed to load leaves');
    } finally {
      setLoading(false);
    }
  }, [tab]);

  useEffect(() => { fetchLeaves(); }, [fetchLeaves]);

  const handleApprove = async (id) => {
    try {
      await leaveService.approveLeave(id);
      toast.success('Leave approved');
      setLeaves(leaves.filter(l => l.id !== id));
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to approve');
    }
  };

  const handleReject = async () => {
    if (!rejectModal) return;
    try {
      await leaveService.rejectLeave(rejectModal.id, { remarks });
      toast.success('Leave rejected');
      setLeaves(leaves.filter(l => l.id !== rejectModal.id));
      setRejectModal(null);
      setRemarks('');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to reject');
    }
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaCalendarAlt /> Leave Requests</h1>
        <p className="text-sm text-gray-500 mb-3">Manage student leave applications</p>
        <div className="flex gap-2">
          <button className={`px-4 py-2 rounded-lg text-sm font-medium border transition-all ${tab === 'pending' ? 'bg-[#1a237e] text-white border-[#1a237e]' : 'border-gray-300 text-gray-600 hover:border-gray-400'}`}
            onClick={() => setTab('pending')}>Pending</button>
          <button className={`px-4 py-2 rounded-lg text-sm font-medium border transition-all ${tab === 'all' ? 'bg-[#1a237e] text-white border-[#1a237e]' : 'border-gray-300 text-gray-600 hover:border-gray-400'}`}
            onClick={() => setTab('all')}>All Leaves</button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6">
          <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
        </div>
      ) : leaves.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400">
          <FaCalendarAlt size={32} className="mb-2" />
          <p className="text-sm">No leave requests found</p>
        </div>
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
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {leaves.map((l, i) => (
                  <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-xs font-bold">{l.student?.name?.charAt(0) || '?'}</div>
                        <span className="text-gray-900">{l.student?.name || l.studentName}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-gray-700">{l.fromDate?.slice(0, 10)}</td>
                    <td className="py-3 px-4 text-gray-700">{l.toDate?.slice(0, 10)}</td>
                    <td className="py-3 px-4 text-gray-700 max-w-[200px] truncate">{l.reason}</td>
                    <td className="py-3 px-4">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        l.status === 'APPROVED' ? 'bg-green-100 text-green-700' :
                        l.status === 'REJECTED' ? 'bg-red-100 text-red-700' :
                        'bg-yellow-100 text-yellow-700'
                      }`}>{l.status}</span>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex gap-2 items-center">
                        {l.status === 'PENDING' && (
                          <>
                            <button className="flex items-center gap-1 bg-green-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-green-700"
                              onClick={() => handleApprove(l.id)}><FaCheck /> Approve</button>
                            <button className="flex items-center gap-1 bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700"
                              onClick={() => setRejectModal(l)}><FaTimes /> Reject</button>
                          </>
                        )}
                        {l.remarks && <span className="text-xs text-gray-400">Remarks: {l.remarks}</span>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {rejectModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setRejectModal(null)}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-lg mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Reject Leave</h3>
              <button className="text-gray-400 hover:text-gray-600" onClick={() => setRejectModal(null)}><FaTimes /></button>
            </div>
            <p className="text-sm text-gray-600 mb-4">
              Provide remarks for rejecting <strong>{rejectModal.student?.name || rejectModal.studentName}</strong>'s leave:
            </p>
            <textarea className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
              value={remarks} onChange={(e) => setRemarks(e.target.value)} placeholder="Enter rejection reason..." rows={3} />
            <div className="flex justify-end gap-2 mt-4">
              <button className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-gray-400"
                onClick={() => setRejectModal(null)}>Cancel</button>
              <button className="flex items-center gap-1 bg-red-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-red-700"
                onClick={handleReject}><FaBan /> Reject Leave</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const WardenComplaints = () => {
  const [complaints, setComplaints] = useState([]);
  const [search, setSearch] = useState('');
  const [viewModal, setViewModal] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    complaintService.getAllComplaints()
      .then(data => setComplaints(Array.isArray(data) ? data : []))
      .catch(() => toast.error('Failed to load complaints'))
      .finally(() => setLoading(false));
  }, []);

  const handleStatusUpdate = async (id, status) => {
    try {
      await complaintService.updateStatus(id, status);
      setComplaints(complaints.map(c => c.id === id ? { ...c, status } : c));
      toast.success(`Complaint ${status.toLowerCase()}`);
    } catch (err) {
      toast.error('Failed to update status');
    }
  };

  const filtered = complaints.filter(c =>
    c.title?.toLowerCase().includes(search.toLowerCase()) ||
    c.student?.name?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaExclamationTriangle /> Complaints</h1>
        <p className="text-sm text-gray-500 mb-3">Manage student complaints</p>
        <div className="relative max-w-md">
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
            placeholder="Search complaints..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6">
          <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400">
          <FaExclamationTriangle size={32} className="mb-2" />
          <p className="text-sm">No complaints found</p>
        </div>
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
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        c.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                        c.status === 'REJECTED' ? 'bg-red-100 text-red-700' :
                        'bg-yellow-100 text-yellow-700'
                      }`}>{c.status}</span>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex gap-2">
                        <button className="flex items-center gap-1 bg-blue-500 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-blue-600"
                          onClick={() => setViewModal(c)}><FaEye /> View</button>
                        <select className="px-3 py-1.5 border border-gray-300 rounded-lg text-xs"
                          value={c.status} onChange={(e) => handleStatusUpdate(c.id, e.target.value)}>
                          <option value="PENDING">Pending</option>
                          <option value="IN_PROGRESS">In Progress</option>
                          <option value="RESOLVED">Resolved</option>
                          <option value="REJECTED">Rejected</option>
                        </select>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {viewModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setViewModal(null)}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">{viewModal.title}</h3>
              <button className="text-gray-400 hover:text-gray-600" onClick={() => setViewModal(null)}><FaTimes /></button>
            </div>
            <div className="space-y-3">
              <p className="text-sm"><strong className="text-gray-700">Student:</strong> {viewModal.student?.name || viewModal.studentName}</p>
              <p className="text-sm"><strong className="text-gray-700">Date:</strong> {viewModal.createdAt?.slice(0, 10)}</p>
              <p className="text-sm"><strong className="text-gray-700">Status:</strong> <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ml-1 ${
                viewModal.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                viewModal.status === 'REJECTED' ? 'bg-red-100 text-red-700' :
                'bg-yellow-100 text-yellow-700'
              }`}>{viewModal.status}</span></p>
              <p className="text-sm"><strong className="text-gray-700">Description:</strong></p>
              <p className="text-sm text-gray-600">{viewModal.description}</p>
              {viewModal.image && <img src={viewModal.image} alt="Complaint" className="w-full rounded-lg mt-2" />}
              {viewModal.response && (
                <div className="p-3 bg-gray-50 rounded-lg">
                  <strong className="text-sm text-gray-700">Response:</strong>
                  <p className="text-sm text-gray-600 mt-1">{viewModal.response}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const WardenNotices = () => {
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
        <p className="text-sm text-gray-500">Create and manage announcements</p>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">{editing ? 'Edit Notice' : 'Create New Notice'}</h3>
          {editing && (
            <button className="flex items-center gap-1 px-3 py-1.5 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:border-gray-400"
              onClick={() => { setEditing(null); setForm({ title: '', content: '', targetRole: 'ALL', expiryDate: '' }); }}><FaTimes /> Cancel</button>
          )}
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
              <input type="text" className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="Notice title" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Target Audience</label>
              <select className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                value={form.targetRole} onChange={(e) => setForm({ ...form, targetRole: e.target.value })}>
                <option value="ALL">All</option>
                <option value="STUDENT">Students Only</option>
                <option value="WARDEN">Wardens Only</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Content</label>
            <textarea className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
              value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} placeholder="Notice content..." rows={4} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Expiry Date (optional)</label>
            <input type="date" className="w-full max-w-xs px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
              value={form.expiryDate} onChange={(e) => setForm({ ...form, expiryDate: e.target.value })} />
          </div>
          <button type="submit" className="bg-[#1a237e] text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50" disabled={submitting}>
            {submitting ? 'Saving...' : editing ? 'Update Notice' : 'Create Notice'}
          </button>
        </form>
      </div>

      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <div className="p-4 border-b border-gray-100">
          <h3 className="text-lg font-semibold text-gray-900">All Notices</h3>
        </div>
        {loading ? (
          <div className="flex items-center justify-center py-6">
            <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
          </div>
        ) : notices.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-gray-400">
            <FaBullhorn size={32} className="mb-2" />
            <p className="text-sm">No notices created</p>
          </div>
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
                    <td className="py-3 px-4">
                      <div className="flex gap-2">
                        <button className="flex items-center gap-1 bg-[#1a237e] text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-[#0d47a1]"
                          onClick={() => startEdit(n)}><FaEdit /> Edit</button>
                        <button className="flex items-center gap-1 bg-red-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-red-700"
                          onClick={() => handleDelete(n.id)}><FaTrash /> Delete</button>
                      </div>
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

const WardenDashboard = () => (
  <ProtectedRoute allowedRoles={['warden']}>
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <Sidebar />
      <div className="ml-64 mt-[70px] transition-all duration-300">
        <Routes>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<DashboardHome />} />
          <Route path="students" element={<WardenManageStudents />} />
          <Route path="rooms" element={<WardenManageRooms />} />
          <Route path="leaves" element={<WardenLeaves />} />
          <Route path="complaints" element={<WardenComplaints />} />
          <Route path="notices" element={<WardenNotices />} />
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
