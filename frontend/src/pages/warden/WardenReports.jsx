import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import reportService from '../../services/reportService';
import wardenService from '../../services/wardenService';
import {
  FaFileAlt, FaDownload, FaChartBar, FaCalendarAlt, FaExclamationTriangle, FaDoorOpen,
} from 'react-icons/fa';

const WardenReports = () => {
  const [stats, setStats] = useState(null);
  const [blockName, setBlockName] = useState('');
  const [reportType, setReportType] = useState('dashboard-summary');
  const [downloadFormat, setDownloadFormat] = useState('pdf');
  const [downloading, setDownloading] = useState(false);
  const [loading, setLoading] = useState(true);

  const reportDownloaders = {
    'dashboard-summary': reportService.downloadDashboardSummaryReport,
    students: reportService.downloadStudentsReport,
    rooms: reportService.downloadRoomsReport,
    leaves: reportService.downloadLeavesReport,
    complaints: reportService.downloadComplaintsReport,
  };

  useEffect(() => {
    Promise.all([
      wardenService.getDashboard(),
      wardenService.getStudents({ page: 0, size: 1 }),
    ]).then(([dash, studs]) => {
      setStats(dash || null);
      const list = Array.isArray(studs) ? studs : (studs?.content || []);
      setBlockName(list[0]?.blockName || '');
    }).catch(() => toast.error('Failed to load reports'))
      .finally(() => setLoading(false));
  }, []);

  const handleExport = async () => {
    if (downloading) return;
    setDownloading(true);
    try {
      const download = reportDownloaders[reportType]
        || reportService.downloadDashboardSummaryReport;
      await download(downloadFormat);
      toast.success('Report downloaded successfully.');
    } catch (err) {
      toast.error(err.message || 'Unable to download the report. Please try again.');
    } finally {
      setDownloading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="w-10 h-10 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
      </div>
    );
  }

  const occupancyRate = stats != null && stats.totalRooms > 0
    ? Math.round((stats.occupiedRooms / stats.totalRooms) * 100)
    : null;
  const rejectedLeaves = stats != null
    ? (stats.totalLeaves ?? 0) - (stats.pendingLeaves ?? 0) - (stats.approvedLeaves ?? 0)
    : null;

  const reportCards = [
    {
      title: 'Block Overview', icon: <FaChartBar />,
      items: [
        { label: 'Total Students', value: stats?.totalStudents ?? '\u2014' },
        { label: 'Total Rooms', value: stats?.totalRooms ?? '\u2014' },
        { label: 'Occupied Rooms', value: stats?.occupiedRooms ?? '\u2014' },
        { label: 'Occupancy Rate', value: occupancyRate != null ? `${occupancyRate}%` : '\u2014' },
      ],
    },
    {
      title: 'Leave Statistics', icon: <FaCalendarAlt />,
      items: [
        { label: 'Total Leaves', value: stats?.totalLeaves ?? '\u2014' },
        { label: 'Approved', value: stats?.approvedLeaves ?? '\u2014' },
        { label: 'Pending', value: stats?.pendingLeaves ?? '\u2014' },
        { label: 'Rejected', value: rejectedLeaves ?? '\u2014' },
      ],
    },
    {
      title: 'Complaint Statistics', icon: <FaExclamationTriangle />,
      items: [
        { label: 'Total Complaints', value: stats?.totalComplaints ?? '\u2014' },
        { label: 'Resolved', value: stats?.resolvedComplaints ?? '\u2014' },
        { label: 'Pending', value: stats?.pendingComplaints ?? '\u2014' },
      ],
    },
    {
      title: 'Rooms & Occupancy', icon: <FaDoorOpen />,
      items: [
        { label: 'Total Rooms', value: stats?.totalRooms ?? '\u2014' },
        { label: 'Occupied', value: stats?.occupiedRooms ?? '\u2014' },
        { label: 'Available', value: stats?.availableRooms ?? '\u2014' },
        { label: 'Occupancy Rate', value: occupancyRate != null ? `${occupancyRate}%` : '\u2014' },
      ],
    },
  ];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between flex-wrap gap-3 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><FaFileAlt /> Reports</h1>
          <p className="text-sm text-gray-500">Block-specific statistics and reports</p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <select className="px-3 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium outline-none cursor-pointer"
            value={reportType} onChange={(e) => setReportType(e.target.value)} disabled={downloading}>
            <option value="dashboard-summary">Block Overview</option>
            <option value="students">Student Report</option>
            <option value="rooms">Room &amp; Occupancy Report</option>
            <option value="leaves">Leave Report</option>
            <option value="complaints">Complaint Report</option>
            <option value="dashboard-summary">Monthly Summary</option>
          </select>
          <select className="px-3 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium outline-none cursor-pointer"
            value={downloadFormat} onChange={(e) => setDownloadFormat(e.target.value)} disabled={downloading}>
            <option value="pdf">PDF</option>
            <option value="xlsx">Excel</option>
          </select>
          <button className="flex items-center gap-2 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:border-[#1a237e] hover:text-[#1a237e] disabled:opacity-50 disabled:cursor-not-allowed" onClick={handleExport} disabled={downloading}>
            <FaDownload /> {downloading ? 'Exporting...' : 'Export Report'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {reportCards.map((section, si) => (
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
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2"><FaChartBar /> Block Occupancy</h3>
        {stats != null && stats.totalRooms > 0 ? (
          <div className="space-y-4">
            <div>
              <div className="flex items-center justify-between text-sm mb-1">
                <span className="font-medium text-gray-700">{blockName || 'Assigned Block'}</span>
                <span className="text-gray-500">{stats.occupiedRooms}/{stats.totalRooms} ({occupancyRate}%)</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div className="bg-[#1a237e] h-2 rounded-full" style={{ width: `${occupancyRate}%` }} />
              </div>
            </div>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-8 text-gray-400"><FaChartBar size={32} className="mb-2" /><p className="text-sm">No occupancy data available</p></div>
        )}
      </div>
    </div>
  );
};

export default WardenReports;
