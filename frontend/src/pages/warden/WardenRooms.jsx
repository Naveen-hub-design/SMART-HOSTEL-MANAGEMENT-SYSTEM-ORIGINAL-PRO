import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import wardenService from '../../services/wardenService';
import roomService from '../../services/roomService';
import {
  FaDoorOpen, FaCheck, FaTimes, FaSearch, FaPlus,
} from 'react-icons/fa';

const WardenRooms = () => {
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
        wardenService.getStudents({ page: 0, size: 50 }),
      ]);
      setRooms(Array.isArray(rms) ? rms : []);
      setStudents(Array.isArray(studs) ? studs : (studs?.content || []));
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

export default WardenRooms;
