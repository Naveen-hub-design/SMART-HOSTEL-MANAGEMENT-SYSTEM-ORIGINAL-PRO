import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import attendanceService from '../../services/attendanceService';
import wardenService from '../../services/wardenService';
import {
  FaClipboardList, FaCheck, FaTimes, FaSave,
} from 'react-icons/fa';

const STATUS_OPTIONS = [
  { value: 'PRESENT', label: 'Present' },
  { value: 'ABSENT', label: 'Absent' },
  { value: 'LATE', label: 'Late' },
  { value: 'EXCUSED', label: 'Excused' },
];

const todayString = () => new Date().toISOString().slice(0, 10);

const statusBadge = (status) => {
  const colors = status === 'PRESENT' ? 'bg-green-100 text-green-700'
    : status === 'ABSENT' ? 'bg-red-100 text-red-700'
    : status === 'LATE' ? 'bg-amber-100 text-amber-700'
    : 'bg-blue-100 text-blue-700';
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colors}`}>
      {status}
    </span>
  );
};

const WardenAttendance = () => {
  const [date, setDate] = useState(todayString());
  const [students, setStudents] = useState([]);
  const [records, setRecords] = useState({});
  const [drafts, setDrafts] = useState({});
  const [editId, setEditId] = useState(null);
  const [editStatus, setEditStatus] = useState('PRESENT');
  const [editRemarks, setEditRemarks] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const fetchStudents = useCallback(async () => {
    try {
      const data = await wardenService.getStudents({ page: 0, size: 50 });
      const list = Array.isArray(data) ? data : (data?.content || []);
      setStudents(list);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to load students');
    }
  }, []);

  const fetchRecords = useCallback(async (forDate) => {
    setLoading(true);
    try {
      const data = await attendanceService.getAttendance({
        date: forDate, size: 50,
      });
      const list = Array.isArray(data) ? data : (data?.content || []);
      const mapped = {};
      list.forEach((r) => { mapped[r.studentId] = r; });
      setRecords(mapped);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to load attendance');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchStudents(); }, [fetchStudents]);
  useEffect(() => {
    fetchRecords(date);
    setDrafts({});
    setEditId(null);
  }, [date, fetchRecords]);

  const setDraft = (studentId, patch) => {
    setDrafts((prev) => ({
      ...prev,
      [studentId]: { status: 'PRESENT', remarks: '', ...(prev[studentId] || {}), ...patch },
    }));
  };

  const handleSave = async () => {
    if (saving) return;
    const entries = students
      .filter((s) => !records[s.id])
      .map((s) => ({
        studentId: s.id,
        status: drafts[s.id]?.status || 'PRESENT',
        remarks: drafts[s.id]?.remarks || '',
      }));
    if (entries.length === 0) {
      toast.info('Nothing new to save. Existing records can be corrected below.');
      return;
    }
    setSaving(true);
    try {
      const res = await attendanceService.markBulkAttendance({ date, entries });
      const data = res.data || res;
      if (data.failureCount > 0) {
        toast.info(`Attendance saved: ${data.successCount} created, ${data.failureCount} failed`);
      } else {
        toast.success(`Attendance saved: ${data.successCount} records created`);
      }
      setDrafts({});
      fetchRecords(date);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save attendance.');
    } finally {
      setSaving(false);
    }
  };

  const openCorrect = (record) => {
    setEditId(record.id);
    setEditStatus(record.status || 'PRESENT');
    setEditRemarks(record.remarks || '');
  };

  const handleCorrect = async () => {
    if (!editId || saving) return;
    setSaving(true);
    try {
      await attendanceService.updateAttendance(editId, {
        status: editStatus,
        remarks: editRemarks,
      });
      toast.success('Attendance corrected');
      setEditId(null);
      fetchRecords(date);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to correct attendance.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaClipboardList /> Attendance</h1>
        <p className="text-sm text-gray-500 mb-3">Mark and manage daily student attendance</p>
        <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-gray-700">Date</label>
            <input type="date"
              className="px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
              value={date} onChange={(e) => e.target.value && setDate(e.target.value)} />
          </div>
          <button
            className="flex items-center gap-1 bg-[#1a237e] text-white px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer whitespace-nowrap"
            onClick={handleSave} disabled={saving}>
            <FaCheck /> {saving ? 'Saving...' : 'Save Attendance'}
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-6">
          <div className="w-8 h-8 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
        </div>
      ) : students.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400">
          <FaClipboardList size={32} className="mb-2" />
          <p className="text-sm">No students found</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Student</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Room</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Date</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Status</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Remarks</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Action</th>
                </tr>
              </thead>
              <tbody>
                {students.map((s) => {
                  const record = records[s.id];
                  const draft = drafts[s.id] || {};
                  return (
                    <tr key={s.id} className="border-b border-gray-50 hover:bg-gray-50">
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <div className="w-8 h-8 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-xs font-bold">{s.name?.charAt(0)}</div>
                          <span className="text-gray-900 font-medium">{s.name}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-gray-700">{s.roomNo || '\u2014'}</td>
                      <td className="py-3 px-4 text-gray-700">{date}</td>
                      <td className="py-3 px-4">
                        {record ? (
                          statusBadge(record.status)
                        ) : (
                          <select
                            className="border border-gray-300 rounded-lg text-sm px-2 py-1.5 outline-none focus:ring-2 focus:ring-[#1a237e] bg-white cursor-pointer"
                            value={draft.status || 'PRESENT'}
                            onChange={(e) => setDraft(s.id, { status: e.target.value })}>
                            {STATUS_OPTIONS.map((o) => (
                              <option key={o.value} value={o.value}>{o.label}</option>
                            ))}
                          </select>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        {record ? (
                          <span className="text-gray-700">{record.remarks || '\u2014'}</span>
                        ) : (
                          <input type="text" placeholder="Optional remark"
                            className="w-full max-w-[180px] px-2 py-1.5 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-[#1a237e]"
                            value={draft.remarks || ''}
                            onChange={(e) => setDraft(s.id, { remarks: e.target.value })} />
                        )}
                      </td>
                      <td className="py-3 px-4">
                        {record && editId !== record.id && (
                          <button className="text-[#1a237e] hover:underline text-sm cursor-pointer"
                            onClick={() => openCorrect(record)}>Correct</button>
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

      {editId && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => { if (!saving) setEditId(null); }}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-lg mx-4" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Correct Attendance</h3>
              <button className="text-gray-400 hover:text-gray-600 cursor-pointer" disabled={saving} onClick={() => setEditId(null)}><FaTimes /></button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
                <select
                  className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none bg-white cursor-pointer"
                  value={editStatus} onChange={(e) => setEditStatus(e.target.value)}>
                  {STATUS_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Remarks</label>
                <textarea
                  className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  rows={3} value={editRemarks} onChange={(e) => setEditRemarks(e.target.value)}
                  placeholder="Reason for correction..." />
              </div>
              <div className="flex justify-end gap-2">
                <button className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-gray-400 cursor-pointer"
                  onClick={() => setEditId(null)} disabled={saving}>Cancel</button>
                <button className="flex items-center gap-1 bg-[#1a237e] text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                  onClick={handleCorrect} disabled={saving}>
                  {saving ? 'Saving...' : (<><FaSave /> Save Correction</>)}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default WardenAttendance;
