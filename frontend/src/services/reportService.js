import API from './api';

const REPORT_TYPES = [
  'students',
  'rooms',
  'leaves',
  'complaints',
  'dashboard-summary',
];

const REPORT_FORMATS = ['pdf', 'xlsx'];

const safeFilename = (name, fallback) => {
  if (!name) return fallback;
  const cleaned = String(name).replace(/["\\/:*?<>|]/g, '').trim();
  return cleaned || fallback;
};

const filenameFromDisposition = (disposition, fallback) => {
  if (!disposition) return fallback;
  const quoted = /filename="([^"]+)"/.exec(disposition);
  if (quoted && quoted[1]) return safeFilename(quoted[1], fallback);
  const bare = /filename=([^;]+)/.exec(disposition);
  if (bare && bare[1]) return safeFilename(bare[1], fallback);
  return fallback;
};

const downloadBlob = (response, fallbackName) => {
  const contentType =
    response.headers && response.headers['content-type']
      ? response.headers['content-type']
      : 'application/octet-stream';
  const blob =
    response.data instanceof Blob
      ? response.data
      : new Blob([response.data], { type: contentType });
  const filename = filenameFromDisposition(
    response.headers && response.headers['content-disposition'],
    fallbackName
  );
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
  return filename;
};

const errorMessage = async (err) => {
  if (err && err.response && err.response.status === 403) {
    return 'You are not authorized to download this report.';
  }
  if (err && err.response && err.response.data instanceof Blob) {
    try {
      const text = await err.response.data.text();
      const parsed = JSON.parse(text);
      if (parsed && typeof parsed.message === 'string' && parsed.message) {
        if (/access denied|not authorized|forbidden/i.test(parsed.message)) {
          return 'You are not authorized to download this report.';
        }
        return 'Unable to download the report. Please try again.';
      }
    } catch (ignored) {
      // fall through to generic message below
    }
  }
  if (err && err.message === 'Network Error') {
    return 'Cannot connect to backend server. Please try again later.';
  }
  return 'Unable to download the report. Please try again.';
};

/**
 * Download a server-generated report. Type and format are validated
 * against fixed allowlists — arbitrary paths can never be requested.
 * The backend derives ADMIN-global vs WARDEN own-block scope from the
 * JWT; the frontend never sends block identifiers.
 */
const downloadReport = async (type, format) => {
  if (!REPORT_TYPES.includes(type) || !REPORT_FORMATS.includes(format)) {
    throw new Error('Unable to download the report. Please try again.');
  }
  const fallbackName = `hostel-${type}-report.${format}`;
  try {
    const response = await API.get(`/reports/${type}.${format}`, {
      responseType: 'blob',
    });
    return downloadBlob(response, fallbackName);
  } catch (err) {
    throw new Error(await errorMessage(err));
  }
};

const reportService = {
  downloadReport,
  downloadStudentsReport: (format) => downloadReport('students', format),
  downloadRoomsReport: (format) => downloadReport('rooms', format),
  downloadLeavesReport: (format) => downloadReport('leaves', format),
  downloadComplaintsReport: (format) => downloadReport('complaints', format),
  downloadDashboardSummaryReport: (format) =>
    downloadReport('dashboard-summary', format),
};

export default reportService;
