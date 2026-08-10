import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';
import {
  FaUser, FaEnvelope, FaLock, FaPhone, FaMapMarkerAlt,
  FaCalendarAlt, FaVenusMars, FaIdCard, FaUserFriends,
  FaUserGraduate, FaBuilding, FaUserShield, FaGraduationCap
} from 'react-icons/fa';

const roleIcons = {
  student: <FaUserGraduate />,
  warden: <FaBuilding />,
  admin: <FaUserShield />,
};

const Register = () => {
  const [formData, setFormData] = useState({
    name: '', email: '', password: '', confirmPassword: '',
    phone: '', enrollmentNo: '', parentContact: '', address: '',
    dateOfBirth: '', gender: '', role: 'student', qualification: '', department: '',
  });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (errors[e.target.name]) {
      setErrors({ ...errors, [e.target.name]: '' });
    }
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.name.trim()) newErrors.name = 'Name is required';
    if (!formData.email.trim()) newErrors.email = 'Email is required';
    else if (!/\S+@\S+\.\S+/.test(formData.email)) newErrors.email = 'Invalid email format';
    if (!formData.password) newErrors.password = 'Password is required';
    else if (formData.password.length < 6) newErrors.password = 'Password must be at least 6 characters';
    if (formData.password !== formData.confirmPassword) newErrors.confirmPassword = 'Passwords do not match';
    if (!formData.phone) newErrors.phone = 'Phone is required';
    if (formData.role === 'student') {
      if (!formData.enrollmentNo) newErrors.enrollmentNo = 'Enrollment number is required';
      if (!formData.gender) newErrors.gender = 'Gender is required';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    try {
      const { confirmPassword, ...data } = formData;
      const res = await register(data);
      toast.success('Registration successful! Welcome aboard.');
      navigate(`/${res.user.role}`, { replace: true });
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const renderField = (name, label, icon, type = 'text', options = null) => (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <div className="relative">
        {icon && <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">{icon}</span>}
        {options ? (
          <select
            name={name}
            value={formData[name]}
            onChange={handleChange}
            className={`w-full ${icon ? 'pl-10' : 'pl-3'} pr-4 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors[name] ? 'border-red-400' : 'border-gray-300'}`}
          >
            <option value="">Select {label}</option>
            {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        ) : (
          <input
            type={type}
            name={name}
            value={formData[name]}
            onChange={handleChange}
            className={`w-full ${icon ? 'pl-10' : 'pl-3'} pr-4 py-2.5 border rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none ${errors[name] ? 'border-red-400' : 'border-gray-300'}`}
            placeholder={`Enter ${label.toLowerCase()}`}
            required
          />
        )}
      </div>
      {errors[name] && <p className="text-red-500 text-xs mt-1">{errors[name]}</p>}
    </div>
  );

  const isStudent = formData.role === 'student';
  const isWarden = formData.role === 'warden';
  const isAdmin = formData.role === 'admin';

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-100 via-purple-50 to-indigo-100 px-4 py-8 relative overflow-hidden">
      <div className="absolute -top-32 -left-32 w-96 h-96 bg-blue-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-pulse" />
      <div className="absolute -bottom-32 -right-32 w-96 h-96 bg-purple-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-pulse" style={{ animationDelay: '1s' }} />
      <div className="relative w-full max-w-2xl">
        <div className="bg-white/90 backdrop-blur-xl rounded-2xl shadow-2xl p-8">
          <div className="text-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900">Create Account</h2>
            <p className="text-gray-500 text-sm mt-1">Join the Smart Hostel community</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid md:grid-cols-2 gap-4">
              <div className="space-y-4">
                {renderField('name', 'Full Name', <FaUser />)}
                {renderField('email', 'Email Address', <FaEnvelope />, 'email')}
                {renderField('password', 'Password', <FaLock />, 'password')}
                {renderField('confirmPassword', 'Confirm Password', <FaLock />, 'password')}
                {renderField('phone', 'Phone Number', <FaPhone />)}
              </div>
              <div className="space-y-4">
                {isStudent && (
                  <>
                    {renderField('enrollmentNo', 'Enrollment No.', <FaIdCard />)}
                    {renderField('parentContact', 'Parent Contact', <FaUserFriends />)}
                    {renderField('address', 'Address', <FaMapMarkerAlt />)}
                    {renderField('dateOfBirth', 'Date of Birth', <FaCalendarAlt />, 'date')}
                    {renderField('gender', 'Gender', <FaVenusMars />, 'select', [
                      { value: 'male', label: 'Male' },
                      { value: 'female', label: 'Female' },
                      { value: 'other', label: 'Other' },
                    ])}
                  </>
                )}
                {isWarden && (
                  <>
                    {renderField('qualification', 'Qualification', <FaGraduationCap />)}
                  </>
                )}
                {isAdmin && (
                  <>
                    {renderField('department', 'Department', <FaBuilding />)}
                  </>
                )}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Register As</label>
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
              className="w-full bg-[#1a237e] text-white py-3 rounded-lg text-sm font-semibold hover:bg-[#0d47a1] transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              disabled={submitting}
            >
              {submitting ? (
                <>
                  <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Creating Account...
                </>
              ) : (
                'Create Account'
              )}
            </button>
          </form>

          <div className="text-center mt-6 text-sm text-gray-500">
            <span>Already have an account? </span>
            <Link to="/login" className="text-[#1a237e] font-semibold hover:underline">Sign In</Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
