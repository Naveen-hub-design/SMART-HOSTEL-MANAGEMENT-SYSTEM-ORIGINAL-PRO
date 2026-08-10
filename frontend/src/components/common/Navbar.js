import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { FaBars, FaTimes, FaUserCircle, FaSignOutAlt, FaSun, FaMoon } from 'react-icons/fa';

const roleBadgeClass = {
  student: 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30',
  warden: 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30',
  admin: 'bg-amber-500/20 text-amber-300 border border-amber-500/30',
};

const Navbar = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="fixed top-0 left-0 right-0 h-[70px] bg-gradient-to-r from-[#0f172a] via-[#1e1b4b] to-[#312e81] text-white flex items-center justify-between px-6 z-50 shadow-xl border-b border-white/10 backdrop-blur-md">
      <div className="flex items-center gap-3">
        <button
          className="lg:hidden text-white text-xl bg-transparent border-none cursor-pointer"
          onClick={() => setMobileOpen(!mobileOpen)}
        >
          {mobileOpen ? <FaTimes /> : <FaBars />}
        </button>
        <Link to="/" className="text-[22px] font-extrabold text-white no-underline flex items-center gap-2.5 tracking-tight group">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center shadow-lg group-hover:scale-105 transition-transform duration-300">
            <span className="text-xl">🏠</span>
          </div>
          <span className="bg-gradient-to-r from-white via-indigo-100 to-indigo-300 bg-clip-text text-transparent">
            Smart Hostel
          </span>
        </Link>
      </div>

      <div className="flex items-center gap-3">
        {/* Theme Toggle Button */}
        <button
          onClick={toggleTheme}
          title={`Switch to ${theme === 'light' ? 'Dark' : 'Light'} Mode`}
          className="w-10 h-10 rounded-xl bg-white/10 hover:bg-white/20 text-amber-300 flex items-center justify-center border border-white/20 transition-all duration-300 cursor-pointer shadow-md hover:scale-105 active:scale-95"
        >
          {theme === 'dark' ? <FaSun size={18} className="text-amber-400" /> : <FaMoon size={18} className="text-indigo-200" />}
        </button>

        {isAuthenticated && (
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2.5 bg-white/5 border border-white/10 px-3.5 py-1.5 rounded-full shadow-inner">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              <FaUserCircle size={22} className="text-indigo-200" />
              <span className="text-sm font-semibold tracking-wide hidden sm:inline text-white/90">
                {user?.name || user?.email}
              </span>
              <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full capitalize ${roleBadgeClass[user?.role] || roleBadgeClass.student}`}>
                {user?.role}
              </span>
            </div>
            <button
              onClick={handleLogout}
              className="flex items-center gap-2 bg-gradient-to-r from-rose-600 to-red-600 hover:from-rose-500 hover:to-red-500 text-white px-3.5 py-2 rounded-xl text-sm font-semibold border border-rose-400/30 shadow-md hover:shadow-rose-900/30 transition-all duration-300 cursor-pointer active:scale-95"
            >
              <FaSignOutAlt /> Logout
            </button>
          </div>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
