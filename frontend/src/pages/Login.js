import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';
import { FaEnvelope, FaLock, FaEye, FaEyeSlash, FaUserGraduate, FaBuilding, FaUserShield } from 'react-icons/fa';

const roleIcons = {
  student: <FaUserGraduate />,
  warden: <FaBuilding />,
  admin: <FaUserShield />,
};

const Login = () => {
  const [formData, setFormData] = useState({ email: '', password: '', role: 'student' });
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const { login, user, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated && user) {
      navigate(`/${user.role}`, { replace: true });
      toast.dismiss();
    }
  }, [isAuthenticated, user, navigate]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const doLogin = async (email, password) => {
    if (!email || !password) {
      toast.error('Please fill in all fields');
      return;
    }
    setSubmitting(true);
    try {
      const data = await login(email, password);
      toast.success(`Welcome back, ${data.name || 'User'}!`);
      const role = (data.role || 'student').toLowerCase();
      navigate(`/${role}`, { replace: true });
    } catch (err) {
      const message = err.response?.data?.message || err.message === 'Network Error' 
        ? 'Cannot connect to backend server. Make sure Spring Boot is running on port 8080.' 
        : 'Login failed. Please check your credentials.';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    doLogin(formData.email, formData.password);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-100 via-purple-50 to-indigo-100 px-4 relative overflow-hidden">
      <div className="absolute -top-32 -left-32 w-96 h-96 bg-blue-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-pulse" />
      <div className="absolute -bottom-32 -right-32 w-96 h-96 bg-purple-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-pulse" style={{ animationDelay: '1s' }} />
      <div className="relative w-full max-w-md">
        <div className="bg-white/90 backdrop-blur-xl rounded-2xl shadow-2xl p-8">
          <div className="text-center mb-6">
            <div className="w-16 h-16 bg-[#1a237e] rounded-2xl flex items-center justify-center mx-auto mb-4">
              <FaBuilding className="text-white text-2xl" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900">Welcome Back</h2>
            <p className="text-gray-500 text-sm mt-1">Sign in to your Smart Hostel account</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
              <div className="relative">
                <FaEnvelope className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  placeholder="Enter your email"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
              <div className="relative">
                <FaLock className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  className="w-full pl-10 pr-10 py-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  placeholder="Enter your password"
                  required
                />
                <button
                  type="button"
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <FaEyeSlash /> : <FaEye />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Login As</label>
              <div className="flex gap-2">
                {['student', 'warden', 'admin'].map((role) => (
                  <button
                    key={role}
                    type="button"
                    className={`flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-medium border-2 transition-all ${
                      formData.role === role
                        ? 'border-[#1a237e] bg-[#1a237e]/5 text-[#1a237e]'
                        : 'border-gray-200 text-gray-500 hover:border-gray-300'
                    }`}
                    onClick={() => setFormData({ ...formData, role })}
                  >
                    {roleIcons[role]}
                    <span>{role.charAt(0).toUpperCase() + role.slice(1)}</span>
                  </button>
                ))}
              </div>
            </div>

            <button
              type="submit"
              className="w-full bg-[#1a237e] text-white py-3 rounded-lg text-sm font-semibold hover:bg-[#0d47a1] transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 cursor-pointer"
              disabled={submitting}
            >
              {submitting ? (
                <>
                  <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Signing In...
                </>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          <div className="text-center mt-6 text-sm text-gray-500">
            <span>Don't have an account? </span>
            <Link to="/register" className="text-[#1a237e] font-semibold hover:underline">Create Account</Link>
          </div>

          <div className="mt-6 pt-5 border-t border-gray-100">
            <p className="text-xs font-semibold text-gray-400 text-center uppercase tracking-wider mb-3">Quick Demo Logins (Instant Login)</p>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                className="py-2 px-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-bold rounded-xl border border-indigo-200/60 transition-all cursor-pointer hover:scale-105 active:scale-95 shadow-sm"
                onClick={() => {
                  setFormData({ email: 'admin@hostel.com', password: 'password123', role: 'admin' });
                  doLogin('admin@hostel.com', 'password123');
                }}
              >
                ⚡ Admin
              </button>
              <button
                type="button"
                className="py-2 px-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-bold rounded-xl border border-emerald-200/60 transition-all cursor-pointer hover:scale-105 active:scale-95 shadow-sm"
                onClick={() => {
                  setFormData({ email: 'warden@hostel.com', password: 'password123', role: 'warden' });
                  doLogin('warden@hostel.com', 'password123');
                }}
              >
                ⚡ Warden
              </button>
              <button
                type="button"
                className="py-2 px-2 bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs font-bold rounded-xl border border-blue-200/60 transition-all cursor-pointer hover:scale-105 active:scale-95 shadow-sm"
                onClick={() => {
                  setFormData({ email: 'student@hostel.com', password: 'password123', role: 'student' });
                  doLogin('student@hostel.com', 'password123');
                }}
              >
                ⚡ Student
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
