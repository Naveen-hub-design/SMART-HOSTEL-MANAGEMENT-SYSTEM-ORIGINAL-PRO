import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import leaveService from '../../services/leaveService';
import {
  FaCalendarAlt, FaCheck, FaTimes, FaBan,
} from 'react-icons/fa';

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

export default WardenLeaves;
