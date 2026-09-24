import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import noticeService from '../../services/noticeService';
import {
  FaBullhorn, FaTimes, FaTrash, FaEdit,
} from 'react-icons/fa';

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
    setForm({ title: notice.title, content: notice.content, targetRole: notice.targetRole || 'ALL', expiryDate: notice.expiresAt?.slice(0, 10) || notice.expiryDate?.slice(0, 10) || '' });
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
                    <td className="py-3 px-4 text-gray-700">{n.expiresAt?.slice(0, 10) || n.expiryDate?.slice(0, 10) || '\u2014'}</td>
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

export default WardenNotices;
