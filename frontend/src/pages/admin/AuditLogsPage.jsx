import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import adminService from '../../services/adminService';
import {
  FaClipboardList, FaSearch, FaFilter, FaRedo, FaShieldAlt,
  FaUserShield, FaUsers, FaChevronLeft, FaChevronRight, FaCalendarAlt
} from 'react-icons/fa';

const AuditLogsPage = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('ALL');
  const [severity, setSeverity] = useState('ALL');
  const [action, setAction] = useState('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size,
        ...(search.trim() && { search: search.trim() }),
        ...(role !== 'ALL' && { role }),
        ...(severity !== 'ALL' && { severity }),
        ...(action !== 'ALL' && { action }),
        ...(startDate && { startDate }),
        ...(endDate && { endDate }),
      };

      const res = await adminService.getAuditLogs(params);
      if (res && res.success && res.data) {
        const pageData = res.data;
        setLogs(pageData.content || []);
        setTotalPages(pageData.totalPages || 0);
        setTotalElements(pageData.totalElements || 0);
      } else {
        setLogs([]);
      }
    } catch (err) {
      toast.error('Failed to load audit logs');
      setLogs([]);
    } finally {
      setLoading(false);
    }
  }, [page, size, search, role, severity, action, startDate, endDate]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleResetFilters = () => {
    setSearch('');
    setRole('ALL');
    setSeverity('ALL');
    setAction('ALL');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  // Badge Color Helper
  const getActionBadgeClass = (actStr = '') => {
    const act = actStr.toUpperCase();
    if (act.includes('CREATED') || act.includes('APPROVED') || act.includes('ALLOCATED') || act.includes('SUCCESS')) {
      return 'bg-emerald-50 text-emerald-700 border border-emerald-200';
    }
    if (act.includes('DELETED') || act.includes('REJECTED') || act.includes('DEACTIVATED') || act.includes('FAILED')) {
      return 'bg-rose-50 text-rose-700 border border-rose-200';
    }
    if (act.includes('UPDATED') || act.includes('VACATED') || act.includes('TRANSFERRED')) {
      return 'bg-blue-50 text-blue-700 border border-blue-200';
    }
    return 'bg-indigo-50 text-indigo-700 border border-indigo-200';
  };

  const getSeverityBadgeClass = (sevStr = 'INFO') => {
    const sev = sevStr.toUpperCase();
    if (sev === 'CRITICAL') return 'bg-red-100 text-red-800 font-bold border border-red-300';
    if (sev === 'WARNING') return 'bg-amber-100 text-amber-800 font-semibold border border-amber-300';
    return 'bg-sky-100 text-sky-800 font-medium border border-sky-200';
  };

  // Metrics
  const securityCount = logs.filter(l => (l.action || '').includes('LOGIN') || (l.severity || '') === 'CRITICAL').length;
  const adminCount = logs.filter(l => (l.performedByRole || '').toUpperCase() === 'ADMIN').length;
  const studentCount = logs.filter(l => (l.performedByRole || '').toUpperCase() === 'STUDENT').length;

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 text-[#1a237e] flex items-center justify-center text-xl shadow-inner">
              <FaClipboardList />
            </div>
            <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">System Audit Logs</h1>
          </div>
          <p className="text-sm text-gray-500">Track user activities, security logs, and administrative changes across the platform</p>
        </div>
        <button
          onClick={fetchLogs}
          className="flex items-center gap-2 bg-[#1a237e] text-white px-4 py-2.5 rounded-xl text-sm font-semibold hover:bg-[#0d47a1] transition-all cursor-pointer shadow-md hover:shadow-lg active:scale-95 self-start md:self-auto"
        >
          <FaRedo className={loading ? 'animate-spin' : ''} /> Refresh Logs
        </button>
      </div>

      {/* Summary Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase text-gray-400">Total Audit Logs</p>
            <h3 className="text-2xl font-bold text-gray-900 mt-1">{totalElements}</h3>
          </div>
          <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center text-xl">
            <FaClipboardList />
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase text-gray-400">Security Events</p>
            <h3 className="text-2xl font-bold text-gray-900 mt-1">{securityCount}</h3>
          </div>
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center text-xl">
            <FaShieldAlt />
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase text-gray-400">Admin Actions</p>
            <h3 className="text-2xl font-bold text-gray-900 mt-1">{adminCount}</h3>
          </div>
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-xl">
            <FaUserShield />
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase text-gray-400">Student Events</p>
            <h3 className="text-2xl font-bold text-gray-900 mt-1">{studentCount}</h3>
          </div>
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center text-xl">
            <FaUsers />
          </div>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-gray-800 uppercase tracking-wider flex items-center gap-2">
            <FaFilter className="text-indigo-600" /> Server-Side Search & Filters
          </h3>
          <button
            onClick={handleResetFilters}
            className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 cursor-pointer hover:underline"
          >
            Clear Filters
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Search */}
          <div className="relative">
            <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search action, email, target..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          {/* Role Filter */}
          <div>
            <select
              value={role}
              onChange={(e) => { setRole(e.target.value); setPage(0); }}
              className="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ALL">All Roles</option>
              <option value="ADMIN">ADMIN</option>
              <option value="WARDEN">WARDEN</option>
              <option value="STUDENT">STUDENT</option>
            </select>
          </div>

          {/* Severity Filter */}
          <div>
            <select
              value={severity}
              onChange={(e) => { setSeverity(e.target.value); setPage(0); }}
              className="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ALL">All Severities</option>
              <option value="INFO">INFO</option>
              <option value="WARNING">WARNING</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>

          {/* Per Page Size */}
          <div>
            <select
              value={size}
              onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
              className="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value={10}>10 per page</option>
              <option value={20}>20 per page</option>
              <option value={50}>50 per page</option>
            </select>
          </div>
        </div>

        {/* Date Range Row */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1 border-t border-gray-100">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-gray-500 whitespace-nowrap flex items-center gap-1">
              <FaCalendarAlt /> Start Date:
            </span>
            <input
              type="date"
              value={startDate}
              onChange={(e) => { setStartDate(e.target.value); setPage(0); }}
              className="w-full px-3 py-1.5 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-gray-500 whitespace-nowrap flex items-center gap-1">
              <FaCalendarAlt /> End Date:
            </span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => { setEndDate(e.target.value); setPage(0); }}
              className="w-full px-3 py-1.5 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </div>
      </div>

      {/* Audit Log Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16">
            <div className="w-10 h-10 border-4 border-gray-200 border-t-indigo-600 rounded-full animate-spin mb-3" />
            <p className="text-sm font-semibold text-gray-500">Loading audit records from server...</p>
          </div>
        ) : logs.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-gray-400">
            <FaClipboardList size={40} className="mb-3 text-gray-300" />
            <p className="text-base font-semibold text-gray-600">No audit logs found</p>
            <p className="text-xs text-gray-400 mt-1">Try resetting search parameters or date filters</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100 text-xs font-bold uppercase tracking-wider text-gray-500">
                  <th className="py-3.5 px-4">Timestamp</th>
                  <th className="py-3.5 px-4">Action</th>
                  <th className="py-3.5 px-4">Severity</th>
                  <th className="py-3.5 px-4">Performed By</th>
                  <th className="py-3.5 px-4">Role</th>
                  <th className="py-3.5 px-4">Target</th>
                  <th className="py-3.5 px-4">Description</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {logs.map((log) => {
                  const formattedDate = log.timestamp
                    ? new Date(log.timestamp).toLocaleString('en-US', {
                        year: 'numeric', month: 'short', day: '2-digit',
                        hour: '2-digit', minute: '2-digit', second: '2-digit'
                      })
                    : 'N/A';

                  return (
                    <tr key={log.id} className="hover:bg-gray-50/80 transition-colors">
                      <td className="py-3 px-4 text-xs font-mono text-gray-500 whitespace-nowrap">{formattedDate}</td>
                      <td className="py-3 px-4">
                        <span className={`inline-block px-2.5 py-1 rounded-full text-xs font-semibold ${getActionBadgeClass(log.action)}`}>
                          {log.action}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className={`inline-block px-2 py-0.5 rounded text-xs ${getSeverityBadgeClass(log.severity)}`}>
                          {log.severity || 'INFO'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-xs font-semibold text-gray-800">{log.performedByEmail || 'SYSTEM'}</td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded-full text-[11px] font-bold uppercase bg-slate-100 text-slate-700">
                          {log.performedByRole || 'SYSTEM'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-xs text-gray-600 whitespace-nowrap">
                        {log.targetType ? `${log.targetType} #${log.targetId || ''}` : '-'}
                      </td>
                      <td className="py-3 px-4 text-xs text-gray-600 max-w-xs truncate" title={log.description || ''}>
                        {log.description || '-'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {!loading && totalPages > 0 && (
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 p-4 bg-gray-50 border-t border-gray-100">
            <p className="text-xs text-gray-500">
              Showing page <span className="font-bold text-gray-800">{page + 1}</span> of{' '}
              <span className="font-bold text-gray-800">{totalPages}</span> ({totalElements} total entries)
            </p>
            <div className="flex items-center gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(p - 1, 0))}
                className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold rounded-lg border border-gray-200 bg-white hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
              >
                <FaChevronLeft size={10} /> Prev
              </button>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold rounded-lg border border-gray-200 bg-white hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
              >
                Next <FaChevronRight size={10} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AuditLogsPage;
