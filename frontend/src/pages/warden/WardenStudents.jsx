import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import wardenService from '../../services/wardenService';
import {
  FaUsers, FaTimes, FaEye, FaSearch, FaUser, FaEnvelope, FaPhone, FaIdCard,
  FaFilter, FaUserPlus, FaLock, FaVenusMars, FaMapMarkerAlt, FaUserFriends,
  FaUpload, FaFileCsv, FaFileDownload,
} from 'react-icons/fa';

const WardenStudents = () => {
  const PAGE_SIZE = 10;
  const [students, setStudents] = useState([]);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [genderFilter, setGenderFilter] = useState('');
  const [roomStatusFilter, setRoomStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [creating, setCreating] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  const [showImport, setShowImport] = useState(false);
  const [importing, setImporting] = useState(false);
  const [bulkFile, setBulkFile] = useState(null);
  const [bulkResult, setBulkResult] = useState(null);
  const [detailsId, setDetailsId] = useState(null);
  const [details, setDetails] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const emptyForm = {
    name: '', email: '', enrollmentNo: '', password: '', confirmPassword: '',
    phone: '', parentContact: '', address: '', dateOfBirth: '', gender: '',
  };
  const [form, setForm] = useState(emptyForm);

  // Debounce search input before querying the server
  useEffect(() => {
    const t = setTimeout(() => { setSearch(searchInput); setPage(0); }, 400);
    return () => clearTimeout(t);
  }, [searchInput]);

  const fetchStudents = useCallback(async () => {
    setLoading(true);
    try {
      const data = await wardenService.getStudents({
        search: search || undefined,
        gender: genderFilter || undefined,
        roomStatus: roomStatusFilter || undefined,
        page,
        size: PAGE_SIZE,
      });
      const list = Array.isArray(data) ? data : (data?.content || []);
      setStudents(list);
      setTotalPages(data?.totalPages || 0);
      setTotalElements(data?.totalElements ?? list.length);
      return list;
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to load students');
      return [];
    } finally {
      setLoading(false);
    }
  }, [search, genderFilter, roomStatusFilter, page]);

  useEffect(() => { fetchStudents(); }, [fetchStudents]);

  const resetFilters = () => {
    setSearchInput('');
    setSearch('');
    setGenderFilter('');
    setRoomStatusFilter('');
    setPage(0);
  };

  const openDetails = async (id) => {
    setDetailsId(id);
    setDetails(null);
    setDetailsLoading(true);
    try {
      const data = await wardenService.getStudentDetails(id);
      setDetails(data);
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to load student details.';
      toast.error(message);
      setDetailsId(null);
    } finally {
      setDetailsLoading(false);
    }
  };

  const openCreate = () => {
    setForm(emptyForm);
    setFormErrors({});
    setShowCreate(true);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    if (formErrors[e.target.name]) {
      setFormErrors({ ...formErrors, [e.target.name]: '' });
    }
  };

  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = 'Name is required';
    if (!form.email.trim()) errs.email = 'Email is required';
    else if (!/\S+@\S+\.\S+/.test(form.email)) errs.email = 'Invalid email format';
    if (!form.enrollmentNo.trim()) errs.enrollmentNo = 'Enrollment number is required';
    if (!form.password) errs.password = 'Password is required';
    else if (form.password.length < 6) errs.password = 'Password must be at least 6 characters';
    if (form.password !== form.confirmPassword) errs.confirmPassword = 'Passwords do not match';
    setFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setCreating(true);
    try {
      const { confirmPassword, ...payload } = form;
      const authData = await wardenService.createStudent(payload);
      setShowCreate(false);
      setForm(emptyForm);
      const list = await fetchStudents();
      const created = list.find(s => s.email === authData.email);
      const room = created?.roomNo || created?.roomNumber || 'Auto-assigned';
      toast.success(
        `Student account created successfully. ${authData.name || ''} (${authData.email}) - Enrollment: ${payload.enrollmentNo} - Room: ${room}`
      );
    } catch (err) {
      const message = err.response?.data?.message
        || (err.message === 'Network Error'
          ? 'Cannot connect to backend server. Make sure Spring Boot is running on port 8080.'
          : 'Failed to create student.');
      toast.error(message);
    } finally {
      setCreating(false);
    }
  };

  // NOTE: filtering, search and pagination are server-side.
  // The `students` array below is exactly the current page from the server.

  const openImport = () => {
    setBulkFile(null);
    setBulkResult(null);
    setShowImport(true);
  };

  const handleDownloadTemplate = () => {
    const header = 'name,email,enrollmentNo,password,phone,parentContact,gender,dateOfBirth,address';
    const example = 'John Doe,john@example.com,ENR001,password123,9876543210,9876500000,MALE,2005-06-15,Coimbatore';
    const blob = new Blob(['\uFEFF' + header + '\n' + example + '\n'], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'student_import_template.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    toast.success('CSV template downloaded');
  };

  const handleBulkFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) {
      setBulkFile(null);
      return;
    }
    if (!file.name.toLowerCase().endsWith('.csv')) {
      toast.error('Only CSV files are supported. Please select a .csv file.');
      setBulkFile(null);
      e.target.value = '';
      return;
    }
    if (file.size === 0) {
      toast.error('Selected file is empty.');
      setBulkFile(null);
      e.target.value = '';
      return;
    }
    setBulkFile(file);
  };

  const handleBulkImport = async () => {
    if (!bulkFile) {
      toast.error('Please select a CSV file first.');
      return;
    }
    setImporting(true);
    try {
      const result = await wardenService.bulkImportStudents(bulkFile);
      setBulkResult(result);
      fetchStudents();
      if (result.failureCount > 0) {
        toast.info(`Bulk import completed: ${result.successCount} created, ${result.failureCount} failed`);
      } else {
        toast.success(`Bulk import completed: ${result.successCount} students created`);
      }
    } catch (err) {
      const message = err.response?.data?.message
        || (err.message === 'Network Error'
          ? 'Cannot connect to backend server. Make sure Spring Boot is running on port 8080.'
          : 'Bulk import failed.');
      toast.error(message);
    } finally {
      setImporting(false);
    }
  };

  const closeImport = () => {
    if (importing) return;
    setShowImport(false);
    setBulkFile(null);
    setBulkResult(null);
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaUsers /> Manage Students</h1>
        <p className="text-sm text-gray-500 mb-3">View all students under your supervision and create new student accounts</p>
        <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
          <div className="relative max-w-md flex-1 w-full">
            <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input type="text" className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
              placeholder="Search by name, email, or enrollment..."
              value={searchInput} onChange={(e) => setSearchInput(e.target.value)} />
          </div>
          <div className="flex gap-3">
            <button className="flex items-center gap-1 bg-[#1a237e] text-white px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors cursor-pointer whitespace-nowrap"
              onClick={openCreate}><FaUserPlus /> Create Student</button>
            <button className="flex items-center gap-1 border border-[#1a237e] text-[#1a237e] px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-[#1a237e] hover:text-white transition-colors cursor-pointer whitespace-nowrap"
              onClick={openImport}><FaUpload /> Import Students</button>
          </div>
        </div>
        <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center mt-3">
          <div className="flex items-center gap-2">
            <FaFilter className="text-gray-400" />
            <select className="border border-gray-300 rounded-lg text-sm px-3 py-2.5 outline-none focus:ring-2 focus:ring-[#1a237e] bg-white cursor-pointer"
              value={genderFilter} onChange={(e) => { setGenderFilter(e.target.value); setPage(0); }}>
              <option value="">All genders</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
            <select className="border border-gray-300 rounded-lg text-sm px-3 py-2.5 outline-none focus:ring-2 focus:ring-[#1a237e] bg-white cursor-pointer"
              value={roomStatusFilter} onChange={(e) => { setRoomStatusFilter(e.target.value); setPage(0); }}>
              <option value="">All room statuses</option>
              <option value="AVAILABLE">Available</option>
              <option value="OCCUPIED">Occupied</option>
              <option value="MAINTENANCE">Maintenance</option>
            </select>
            <button className="text-sm text-[#1a237e] hover:underline cursor-pointer px-2 py-2.5"
              onClick={resetFilters}>Reset filters</button>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6">
          <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
        </div>
      ) : students.length === 0 ? (
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
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Details</th>
                </tr>
              </thead>
              <tbody>
                {students.map((s) => (
                  <tr key={s.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-xs font-bold">{s.name?.charAt(0)}</div>
                        <span className="text-gray-900 font-medium">{s.name}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-gray-700">{s.email}</td>
                    <td className="py-3 px-4 text-gray-700">{s.enrollmentNo || '\u2014'}</td>
                    <td className="py-3 px-4 text-gray-700">{s.roomNo || 'Not allocated'}</td>
                    <td className="py-3 px-4 text-gray-700">{s.phone || '\u2014'}</td>
                    <td className="py-3 px-4">
                      <button className="flex items-center gap-1 text-[#1a237e] hover:underline text-sm cursor-pointer"
                        onClick={() => openDetails(s.id)}><FaEye /> View</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-4 py-3 border-t border-gray-100">
            <p className="text-xs text-gray-500">
              Showing {totalElements === 0 ? 0 : page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, totalElements)} of {totalElements} students
            </p>
            <div className="flex items-center gap-3">
              <button disabled={page <= 0}
                className="px-4 py-2 rounded-lg border text-sm font-medium cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed border-gray-300 text-gray-700 hover:bg-gray-50"
                onClick={() => setPage(page - 1)}>Previous</button>
              <span className="text-xs text-gray-500">Page {totalPages === 0 ? 0 : page + 1} of {totalPages}</span>
              <button disabled={page + 1 >= totalPages}
                className="px-4 py-2 rounded-lg border text-sm font-medium cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed border-gray-300 text-gray-700 hover:bg-gray-50"
                onClick={() => setPage(page + 1)}>Next</button>
            </div>
          </div>
        </div>
      )}

      {detailsId && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => { if (!detailsLoading) { setDetailsId(null); setDetails(null); } }}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-2xl mx-4 max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2"><FaEye className="text-[#1a237e]" /> Student Details</h3>
              <button className="text-gray-400 hover:text-gray-600 cursor-pointer" disabled={detailsLoading} onClick={() => { setDetailsId(null); setDetails(null); }}><FaTimes /></button>
            </div>
            {detailsLoading || !details ? (
              <div className="flex items-center justify-center py-6">
                <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
              </div>
            ) : (
              <div className="space-y-5">
                <div>
                  <h4 className="text-sm font-bold text-[#1a237e] uppercase tracking-wide mb-2">Profile</h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-2 text-sm">
                    <p><strong className="text-gray-700">Name:</strong> <span className="text-gray-900">{details.name}</span></p>
                    <p><strong className="text-gray-700">Email:</strong> <span className="text-gray-900">{details.email}</span></p>
                    <p><strong className="text-gray-700">Phone:</strong> <span className="text-gray-900">{details.phone || '\u2014'}</span></p>
                    <p><strong className="text-gray-700">Enrollment No:</strong> <span className="text-gray-900">{details.enrollmentNo}</span></p>
                    <p><strong className="text-gray-700">Gender:</strong> <span className="text-gray-900">{details.gender || '\u2014'}</span></p>
                    <p><strong className="text-gray-700">Date of Birth:</strong> <span className="text-gray-900">{details.dateOfBirth || '\u2014'}</span></p>
                    <p><strong className="text-gray-700">Parent Contact:</strong> <span className="text-gray-900">{details.parentContact || '\u2014'}</span></p>
                    <p className="md:col-span-2"><strong className="text-gray-700">Address:</strong> <span className="text-gray-900">{details.address || '\u2014'}</span></p>
                  </div>
                </div>
                <div>
                  <h4 className="text-sm font-bold text-[#1a237e] uppercase tracking-wide mb-2">Room</h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-2 text-sm">
                    <p><strong className="text-gray-700">Block:</strong> <span className="text-gray-900">{details.blockName || '\u2014'}</span></p>
                    <p><strong className="text-gray-700">Room:</strong> <span className="text-gray-900">{details.roomNo || '\u2014'}</span></p>
                    <p><strong className="text-gray-700">Floor:</strong> <span className="text-gray-900">{details.floor ?? '\u2014'}</span></p>
                    <p><strong className="text-gray-700">Capacity:</strong> <span className="text-gray-900">{details.capacity ?? '\u2014'}</span></p>
                    <p><strong className="text-gray-700">Occupants:</strong> <span className="text-gray-900">{details.occupants ?? '\u2014'}</span></p>
                    <p><strong className="text-gray-700">Status:</strong> <span className="text-gray-900">{details.status || '\u2014'}</span></p>
                  </div>
                </div>
                <div>
                  <h4 className="text-sm font-bold text-[#1a237e] uppercase tracking-wide mb-2">Recent Leaves</h4>
                  {(!details.recentLeaves || details.recentLeaves.length === 0) ? (
                    <p className="text-sm text-gray-400">No recent leaves</p>
                  ) : (
                    <div className="space-y-2">
                      {details.recentLeaves.map((l) => (
                        <div key={l.id} className="border border-gray-100 rounded-lg px-3 py-2 text-sm flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-4">
                          <span className="text-gray-900 font-medium">{l.fromDate} → {l.toDate}</span>
                          <span className="text-gray-500 flex-1">{l.reason}</span>
                          <span className="text-xs font-bold text-gray-700">{l.status}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
                <div>
                  <h4 className="text-sm font-bold text-[#1a237e] uppercase tracking-wide mb-2">Recent Complaints</h4>
                  {(!details.recentComplaints || details.recentComplaints.length === 0) ? (
                    <p className="text-sm text-gray-400">No recent complaints</p>
                  ) : (
                    <div className="space-y-2">
                      {details.recentComplaints.map((c) => (
                        <div key={c.id} className="border border-gray-100 rounded-lg px-3 py-2 text-sm flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-4">
                          <span className="text-gray-900 font-medium">{c.title}</span>
                          <span className="text-gray-500 flex-1">{c.category || ''}</span>
                          <span className="text-xs font-bold text-gray-700">{c.status}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => { if (!creating) setShowCreate(false); }}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-2xl mx-4 max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <div>
                <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2"><FaUserPlus className="text-[#1a237e]" /> Create Student</h3>
                <p className="text-sm text-gray-500 mt-1">A room from your assigned block will be allocated automatically</p>
              </div>
              <button className="text-gray-400 hover:text-gray-600 cursor-pointer" disabled={creating} onClick={() => setShowCreate(false)}><FaTimes /></button>
            </div>
            <form onSubmit={handleCreate} className="space-y-4" noValidate>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Student Name *</label>
                  <div className="relative">
                    <FaUser className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input type="text" name="name" value={form.name} onChange={handleChange}
                      className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none" placeholder="Full name" />
                  </div>
                  {formErrors.name && <p className="text-xs text-red-600 mt-1">{formErrors.name}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Email *</label>
                  <div className="relative">
                    <FaEnvelope className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input type="email" name="email" value={form.email} onChange={handleChange}
                      className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none" placeholder="student@hostel.com" />
                  </div>
                  {formErrors.email && <p className="text-xs text-red-600 mt-1">{formErrors.email}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Enrollment Number *</label>
                  <div className="relative">
                    <FaIdCard className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input type="text" name="enrollmentNo" value={form.enrollmentNo} onChange={handleChange}
                      className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none" placeholder="ENR2025xxx" />
                  </div>
                  {formErrors.enrollmentNo && <p className="text-xs text-red-600 mt-1">{formErrors.enrollmentNo}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
                  <div className="relative">
                    <FaPhone className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input type="tel" name="phone" value={form.phone} onChange={handleChange}
                      className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none" placeholder="9876543210" />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Password *</label>
                  <div className="relative">
                    <FaLock className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input type="password" name="password" value={form.password} onChange={handleChange}
                      className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none" placeholder="Min 6 characters" />
                  </div>
                  {formErrors.password && <p className="text-xs text-red-600 mt-1">{formErrors.password}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Confirm Password *</label>
                  <div className="relative">
                    <FaLock className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input type="password" name="confirmPassword" value={form.confirmPassword} onChange={handleChange}
                      className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none" placeholder="Re-enter password" />
                  </div>
                  {formErrors.confirmPassword && <p className="text-xs text-red-600 mt-1">{formErrors.confirmPassword}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Parent Contact</label>
                  <div className="relative">
                    <FaUserFriends className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input type="tel" name="parentContact" value={form.parentContact} onChange={handleChange}
                      className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none" placeholder="Parent phone number" />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Gender</label>
                  <div className="relative">
                    <FaVenusMars className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <select name="gender" value={form.gender} onChange={handleChange}
                      className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none bg-white">
                      <option value="">Select gender</option>
                      <option value="MALE">Male</option>
                      <option value="FEMALE">Female</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
                  <input type="date" name="dateOfBirth" value={form.dateOfBirth} onChange={handleChange}
                    className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none" />
                </div>
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                  <div className="relative">
                    <FaMapMarkerAlt className="absolute left-3 top-3 text-gray-400" />
                    <textarea name="address" value={form.address} onChange={handleChange} rows={2}
                      className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none" placeholder="Home address" />
                  </div>
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-gray-400 cursor-pointer"
                  onClick={() => setShowCreate(false)} disabled={creating}>Cancel</button>
                <button type="submit" className="flex items-center gap-1 bg-[#1a237e] text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                  disabled={creating}>
                  {creating ? (
                    <>
                      <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                      </svg>
                      Creating Student...
                    </>
                  ) : (<><FaUserPlus /> Create Student</>)}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showImport && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={closeImport}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-3xl mx-4 max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <div>
                <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2"><FaFileCsv className="text-[#1a237e]" /> Bulk Import Students</h3>
                <p className="text-sm text-gray-500 mt-1">Upload a CSV containing student details. Students will be automatically assigned to available rooms in your hostel block.</p>
              </div>
              <button className="text-gray-400 hover:text-gray-600 cursor-pointer" disabled={importing} onClick={closeImport}><FaTimes /></button>
            </div>

            {!bulkResult ? (
              <div className="space-y-5">
                <div className="p-4 bg-blue-50 rounded-lg">
                  <p className="text-sm text-blue-800"><strong>CSV format:</strong></p>
                  <code className="text-xs text-blue-700 block mt-1 break-all">name,email,enrollmentNo,password,phone,parentContact,gender,dateOfBirth,address</code>
                  <p className="text-xs text-blue-600 mt-2">Required: name, email, enrollmentNo, password (min 6 characters). Gender: MALE / FEMALE / OTHER. Date: YYYY-MM-DD. Quoted values and commas inside quotes are supported.</p>
                </div>

                <div className="flex items-center gap-3">
                  <button className="flex items-center gap-1 bg-[#1a237e] text-white px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors cursor-pointer"
                    onClick={handleDownloadTemplate}><FaFileDownload /> Download CSV Template</button>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Select CSV File</label>
                  <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center">
                    <label className="flex items-center gap-2 px-4 py-2.5 border-2 border-dashed border-gray-300 rounded-lg text-sm text-gray-600 hover:border-[#1a237e] hover:text-[#1a237e] transition-colors cursor-pointer">
                      <FaFileCsv />
                      <span>{bulkFile ? bulkFile.name : 'Choose file...'}</span>
                      <input type="file" accept=".csv" className="hidden" onChange={handleBulkFileChange} />
                    </label>
                    {bulkFile && (
                      <span className="text-xs text-gray-500">{(bulkFile.size / 1024).toFixed(1)} KB selected</span>
                    )}
                  </div>
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <button type="button" className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-gray-400 cursor-pointer"
                    onClick={closeImport} disabled={importing}>Cancel</button>
                  <button type="button" className="flex items-center gap-1 bg-green-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                    onClick={handleBulkImport} disabled={importing || !bulkFile}>
                    {importing ? (
                      <>
                        <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                        </svg>
                        Importing...
                      </>
                    ) : (<><FaUpload /> Import Students</>)}
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-5">
                <div className="grid grid-cols-3 gap-3">
                  <div className="p-4 bg-gray-50 rounded-lg text-center">
                    <p className="text-2xl font-bold text-gray-900">{bulkResult.totalRows}</p>
                    <p className="text-xs text-gray-500 mt-1">Total Students</p>
                  </div>
                  <div className="p-4 bg-green-50 rounded-lg text-center">
                    <p className="text-2xl font-bold text-green-700">{bulkResult.successCount}</p>
                    <p className="text-xs text-green-600 mt-1">Successfully Created</p>
                  </div>
                  <div className="p-4 bg-red-50 rounded-lg text-center">
                    <p className="text-2xl font-bold text-red-700">{bulkResult.failureCount}</p>
                    <p className="text-xs text-red-600 mt-1">Failed</p>
                  </div>
                </div>

                {bulkResult.results?.length > 0 ? (
                  <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-100">
                    <div className="overflow-x-auto">
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="border-b border-gray-100 bg-gray-50">
                            <th className="text-left py-3 px-4 text-gray-500 font-medium">Row</th>
                            <th className="text-left py-3 px-4 text-gray-500 font-medium">Student</th>
                            <th className="text-left py-3 px-4 text-gray-500 font-medium">Email</th>
                            <th className="text-left py-3 px-4 text-gray-500 font-medium">Status</th>
                            <th className="text-left py-3 px-4 text-gray-500 font-medium">Room</th>
                            <th className="text-left py-3 px-4 text-gray-500 font-medium">Message</th>
                          </tr>
                        </thead>
                        <tbody>
                          {bulkResult.results.map((r, i) => (
                            <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                              <td className="py-3 px-4 text-gray-500">{r.rowNumber}</td>
                              <td className="py-3 px-4 font-medium text-gray-900">{r.name || '\u2014'}</td>
                              <td className="py-3 px-4 text-gray-700">{r.email || '\u2014'}</td>
                              <td className="py-3 px-4">
                                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${r.status === 'SUCCESS' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                                  {r.status === 'SUCCESS' ? 'Success' : 'Failed'}
                                </span>
                              </td>
                              <td className="py-3 px-4 text-gray-700">{r.room || '\u2014'}</td>
                              <td className="py-3 px-4 text-gray-600 max-w-[220px]">{r.message}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-8 text-gray-400">
                    <FaFileCsv size={32} className="mb-2" />
                    <p className="text-sm">No rows processed</p>
                  </div>
                )}

                <div className="flex justify-end gap-2">
                  <button type="button" className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-gray-400 cursor-pointer"
                    onClick={() => { setBulkResult(null); setBulkFile(null); }}>Import Another</button>
                  <button type="button" className="bg-[#1a237e] text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors cursor-pointer"
                    onClick={closeImport}>Close</button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default WardenStudents;
