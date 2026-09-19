import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import complaintService from '../../services/complaintService';
import {
  FaExclamationTriangle, FaTimes, FaEye, FaSearch,
} from 'react-icons/fa';

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
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Category</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Priority</th>
                  <th className="text-left py-3 px-4 text-gray-500 font-medium">Sentiment</th>
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
                    <td className="py-3 px-4">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">{c.aiCategory || c.category || '\u2014'}</span>
                    </td>
                    <td className="py-3 px-4">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        c.priority === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                        c.priority === 'HIGH' ? 'bg-orange-100 text-orange-700' :
                        c.priority === 'MEDIUM' ? 'bg-amber-100 text-amber-700' :
                        c.priority === 'LOW' ? 'bg-gray-100 text-gray-600' :
                        'bg-gray-100 text-gray-500'
                      }`}>{c.priority || '\u2014'}</span>
                    </td>
                    <td className="py-3 px-4">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        c.sentiment === 'POSITIVE' ? 'bg-green-100 text-green-700' :
                        c.sentiment === 'NEGATIVE' ? 'bg-red-100 text-red-700' :
                        c.sentiment === 'NEUTRAL' ? 'bg-gray-100 text-gray-600' :
                        'bg-gray-100 text-gray-500'
                      }`}>{c.sentiment || '\u2014'}</span>
                    </td>
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
              <div className="flex flex-wrap gap-2">
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">{viewModal.aiCategory || viewModal.category || '\u2014'}</span>
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  viewModal.priority === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                  viewModal.priority === 'HIGH' ? 'bg-orange-100 text-orange-700' :
                  viewModal.priority === 'MEDIUM' ? 'bg-amber-100 text-amber-700' :
                  viewModal.priority === 'LOW' ? 'bg-gray-100 text-gray-600' :
                  'bg-gray-100 text-gray-500'
                }`}>{viewModal.priority ? `Priority: ${viewModal.priority}` : '\u2014'}</span>
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  viewModal.sentiment === 'POSITIVE' ? 'bg-green-100 text-green-700' :
                  viewModal.sentiment === 'NEGATIVE' ? 'bg-red-100 text-red-700' :
                  viewModal.sentiment === 'NEUTRAL' ? 'bg-gray-100 text-gray-600' :
                  'bg-gray-100 text-gray-500'
                }`}>{viewModal.sentiment ? `Sentiment: ${viewModal.sentiment}` : '\u2014'}</span>
              </div>
              <p className="text-sm"><strong className="text-gray-700">Status:</strong> <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ml-1 ${
                viewModal.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                viewModal.status === 'REJECTED' ? 'bg-red-100 text-red-700' :
                'bg-yellow-100 text-yellow-700'
              }`}>{viewModal.status}</span></p>
              <p className="text-sm"><strong className="text-gray-700">Description:</strong></p>
              <p className="text-sm text-gray-600">{viewModal.description}</p>
              {viewModal.image && <img src={viewModal.image} alt="Complaint" className="w-full rounded-lg mt-2" />}
              {viewModal.aiRecommendation && (
                <div className="p-3 bg-blue-50 rounded-lg">
                  <strong className="text-sm text-[#1a237e]">AI Recommendation:</strong>
                  <p className="text-sm text-gray-600 mt-1">{viewModal.aiRecommendation}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default WardenComplaints;
