import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  FaHome, FaUser, FaDoorOpen, FaCalendarAlt, FaExclamationTriangle,
  FaBullhorn, FaStore, FaSearch, FaSignOutAlt,
  FaUtensils, FaChevronLeft, FaChevronRight, FaBuilding,
  FaChartBar, FaUsers, FaFileAlt, FaUserShield, FaRobot, FaClipboardList
} from 'react-icons/fa';

const menuConfig = {
  student: [
    { path: '/student/dashboard', label: 'Dashboard', icon: <FaHome /> },
    { path: '/student/profile', label: 'Profile', icon: <FaUser /> },
    { path: '/student/my-room', label: 'My Room', icon: <FaDoorOpen /> },
    { path: '/student/leaves', label: 'Leave Requests', icon: <FaCalendarAlt /> },
    { path: '/student/complaints', label: 'Complaints', icon: <FaExclamationTriangle /> },
    { path: '/student/notices', label: 'Notices', icon: <FaBullhorn /> },
    { path: '/student/marketplace', label: 'Marketplace', icon: <FaStore /> },
    { path: '/student/lost-found', label: 'Lost & Found', icon: <FaSearch /> },
    { path: '/student/mess-feedback', label: 'Mess Feedback', icon: <FaUtensils /> },
  ],
  warden: [
    { path: '/warden/dashboard', label: 'Dashboard', icon: <FaChartBar /> },
    { path: '/warden/students', label: 'Students', icon: <FaUsers /> },
    { path: '/warden/rooms', label: 'Rooms', icon: <FaDoorOpen /> },
    { path: '/warden/leaves', label: 'Leave Requests', icon: <FaCalendarAlt /> },
    { path: '/warden/complaints', label: 'Complaints', icon: <FaExclamationTriangle /> },
    { path: '/warden/notices', label: 'Notices', icon: <FaBullhorn /> },
  ],
  admin: [
    { path: '/admin/dashboard', label: 'Dashboard', icon: <FaChartBar /> },
    { path: '/admin/students', label: 'Students', icon: <FaUsers /> },
    { path: '/admin/wardens', label: 'Wardens', icon: <FaUserShield /> },
    { path: '/admin/hostel-blocks', label: 'Hostel Blocks', icon: <FaBuilding /> },
    { path: '/admin/rooms', label: 'Rooms', icon: <FaDoorOpen /> },
    { path: '/admin/leaves', label: 'Leaves', icon: <FaCalendarAlt /> },
    { path: '/admin/complaints', label: 'Complaints', icon: <FaExclamationTriangle /> },
    { path: '/admin/notices', label: 'Notices', icon: <FaBullhorn /> },
    { path: '/admin/marketplace', label: 'Marketplace', icon: <FaStore /> },
    { path: '/admin/lost-found', label: 'Lost & Found', icon: <FaSearch /> },
    { path: '/admin/mess-feedback', label: 'Mess Feedback', icon: <FaUtensils /> },
    { path: '/admin/audit', label: 'Audit Logs', icon: <FaClipboardList /> },
    { path: '/admin/reports', label: 'Reports', icon: <FaFileAlt /> },
    { path: '/admin/ai-analytics', label: 'AI Analytics', icon: <FaRobot /> },
  ],
};

const Sidebar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);

  const menuItems = menuConfig[user?.role] || [];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside
      className={`fixed top-[70px] left-0 bottom-0 ${
        collapsed ? 'w-[70px]' : 'w-[260px]'
      } bg-gradient-to-b from-[#0f172a] via-[#1e1b4b] to-[#0f172a] text-white overflow-y-auto transition-all duration-300 z-40 sidebar-scroll border-r border-white/10 shadow-2xl flex flex-col justify-between`}
    >
      <div>
        <div className={`${collapsed ? 'py-4 px-0' : 'p-4'} text-center border-b border-white/10 mb-2`}>
          {!collapsed && (
            <div className="mb-2">
              <div className="w-12 h-12 rounded-2xl bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center mx-auto shadow-inner text-indigo-300">
                <FaBuilding size={24} />
              </div>
              <h3 className="text-xs font-bold tracking-wider mt-2.5 capitalize text-indigo-200 uppercase">
                {user?.role} Portal
              </h3>
            </div>
          )}
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="w-8 h-8 rounded-xl bg-white/10 text-white flex items-center justify-center cursor-pointer mx-auto border border-white/20 hover:bg-indigo-600 transition-all duration-200 shadow-sm active:scale-95"
          >
            {collapsed ? <FaChevronRight size={14} /> : <FaChevronLeft size={14} />}
          </button>
        </div>

        <nav className={collapsed ? 'py-2 px-1' : 'px-3'}>
          {menuItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center ${
                  collapsed ? 'justify-center py-3 px-0' : 'justify-start py-3 px-3.5'
                } mb-1.5 rounded-xl text-sm no-underline transition-all duration-200 whitespace-nowrap overflow-hidden ${
                  isActive
                    ? 'bg-gradient-to-r from-indigo-600/50 to-purple-600/50 text-white font-bold border-l-4 border-indigo-400 shadow-md backdrop-blur-sm'
                    : 'text-white/70 hover:bg-white/10 hover:text-white font-medium hover:translate-x-1'
                }`
              }
            >
              <span className="text-lg min-w-[22px] flex justify-center text-indigo-300">
                {item.icon}
              </span>
              {!collapsed && <span className="ml-3 tracking-wide">{item.label}</span>}
            </NavLink>
          ))}
        </nav>
      </div>

      <div className={`border-t border-white/10 ${collapsed ? 'py-3 px-1' : 'py-3 px-3'} mt-auto mb-2`}>
        <button
          onClick={handleLogout}
          className="flex items-center w-full py-2.5 px-3.5 rounded-xl text-rose-300/80 bg-rose-500/10 cursor-pointer text-sm font-semibold transition-all duration-200 hover:bg-rose-600 hover:text-white border border-rose-500/20 active:scale-95"
        >
          <FaSignOutAlt size={16} />
          {!collapsed && <span className="ml-3">Logout</span>}
        </button>
      </div>

      <style>{`
        .sidebar-scroll::-webkit-scrollbar { width: 4px; }
        .sidebar-scroll::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.15); border-radius: 4px; }
        @media (max-width: 1023px) {
          .sidebar-scroll { transform: translateX(-100%); }
          .sidebar-scroll.open { transform: translateX(0); }
        }
      `}</style>
    </aside>
  );
};

export default Sidebar;
