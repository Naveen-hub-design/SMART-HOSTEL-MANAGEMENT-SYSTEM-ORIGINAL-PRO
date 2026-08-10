import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  FaBed, FaCalendarAlt, FaExclamationTriangle, FaBullhorn,
  FaSearch, FaStore, FaArrowRight, FaStar, FaShieldAlt,
  FaBuilding, FaCheckCircle, FaChartLine, FaSmile
} from 'react-icons/fa';

const features = [
  { icon: <FaBed />, title: 'Room Management', desc: 'Efficient room allocation, transfers, and real-time occupancy tracking across all hostel blocks.' },
  { icon: <FaCalendarAlt />, title: 'Leave Tracking', desc: 'Digital leave applications with automated warden approval workflow and status tracking.' },
  { icon: <FaExclamationTriangle />, title: 'Complaint System', desc: 'File and track complaints with image uploads, status updates, and resolution tracking.' },
  { icon: <FaStore />, title: 'Marketplace', desc: 'Student marketplace for buying, selling, and trading items within the hostel community.' },
  { icon: <FaSearch />, title: 'Lost & Found', desc: 'Report lost items or browse found items with category filtering and status tracking.' },
  { icon: <FaBullhorn />, title: 'Mess Feedback', desc: 'Rate hostel mess food quality, taste, and cleanliness with detailed feedback forms.' },
];

const stats = [
  { icon: <FaChartLine />, value: '500+', label: 'Students' },
  { icon: <FaBuilding />, value: '10+', label: 'Hostel Blocks' },
  { icon: <FaCheckCircle />, value: '200+', label: 'Complaints Resolved' },
  { icon: <FaSmile />, value: '98%', label: 'Happy Students' },
];

const Home = () => {
  const { isAuthenticated, user } = useAuth();
  const dashboardPath = user?.role ? `/${user.role.toLowerCase()}/dashboard` : '/login';

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-blue-50">
      <nav className="flex items-center justify-between px-6 md:px-16 py-4 bg-white/80 backdrop-blur-md shadow-sm sticky top-0 z-50">
        <Link to="/" className="flex items-center gap-2 text-xl font-bold text-[#1a237e] no-underline">
          <FaBuilding className="text-[#1a237e]" />
          Smart Hostel
        </Link>
        <div className="flex items-center gap-4">
          <a href="#features" className="text-gray-600 hover:text-[#1a237e] text-sm font-medium no-underline hidden sm:block">Features</a>
          {isAuthenticated ? (
            <Link to={dashboardPath} className="bg-[#1a237e] text-white px-5 py-2 rounded-lg text-sm font-semibold no-underline hover:bg-[#0d47a1] transition-colors shadow-md">
              Go to Dashboard
            </Link>
          ) : (
            <>
              <Link to="/login" className="text-gray-600 hover:text-[#1a237e] text-sm font-medium no-underline">Sign In</Link>
              <Link to="/register" className="bg-[#1a237e] text-white px-5 py-2 rounded-lg text-sm font-medium no-underline hover:bg-[#0d47a1] transition-colors">
                Get Started
              </Link>
            </>
          )}
        </div>
      </nav>

      <section className="relative overflow-hidden px-6 md:px-16 py-20 md:py-32 text-center">
        <div className="absolute -top-20 -left-20 w-72 h-72 bg-blue-200 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-pulse" />
        <div className="absolute -bottom-20 -right-20 w-72 h-72 bg-purple-200 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-pulse" style={{ animationDelay: '1s' }} />
        <div className="relative z-10 max-w-4xl mx-auto">
          <div className="inline-flex items-center gap-2 bg-blue-100 text-[#1a237e] px-4 py-2 rounded-full text-sm font-medium mb-6">
            <FaStar className="text-yellow-500" /> Smart Campus Solution
          </div>
          <h1 className="text-4xl md:text-6xl font-bold text-gray-900 leading-tight mb-6">
            Smart Hostel Management<br />
            <span className="text-[#1a237e]">& Student Community Platform</span>
          </h1>
          <p className="text-lg md:text-xl text-gray-600 max-w-2xl mx-auto mb-8">
            A comprehensive platform for managing hostel operations, fostering student community engagement,
            and streamlining campus life with modern digital tools.
          </p>
          <div className="flex flex-wrap justify-center gap-4 mb-12">
            {isAuthenticated ? (
              <Link to={dashboardPath} className="inline-flex items-center gap-2 bg-[#1a237e] text-white px-8 py-3 rounded-xl text-lg font-medium no-underline hover:bg-[#0d47a1] transition-all shadow-lg hover:shadow-xl">
                Go to Dashboard <FaArrowRight />
              </Link>
            ) : (
              <Link to="/register" className="inline-flex items-center gap-2 bg-[#1a237e] text-white px-8 py-3 rounded-xl text-lg font-medium no-underline hover:bg-[#0d47a1] transition-all shadow-lg hover:shadow-xl">
                Get Started Free <FaArrowRight />
              </Link>
            )}
            <a href="#features" className="inline-flex items-center gap-2 border-2 border-gray-300 text-gray-700 px-8 py-3 rounded-xl text-lg font-medium no-underline hover:border-[#1a237e] hover:text-[#1a237e] transition-all">
              Learn More
            </a>
          </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 max-w-3xl mx-auto">
          {stats.map((s, i) => (
            <div key={i} className="bg-white/80 backdrop-blur rounded-xl p-4 shadow-md">
              <div className="text-[#1a237e] text-2xl mb-2">{s.icon}</div>
              <div className="text-2xl md:text-3xl font-bold text-gray-900">{s.value}</div>
              <div className="text-sm text-gray-500">{s.label}</div>
            </div>
          ))}
        </div>
      </div>
    </section>

    <section id="features" className="px-6 md:px-16 py-20 bg-white">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-12">
          <div className="inline-flex items-center gap-2 bg-blue-100 text-[#1a237e] px-4 py-2 rounded-full text-sm font-medium mb-4">
            <FaStar className="text-yellow-500" /> Features
          </div>
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-3">Everything You Need</h2>
          <p className="text-gray-500 text-lg">Powerful tools to manage every aspect of hostel life</p>
        </div>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((f, i) => (
            <div key={i} className="bg-white border border-gray-100 rounded-2xl p-6 shadow-sm hover:shadow-lg hover:-translate-y-1 transition-all">
              <div className="w-12 h-12 bg-blue-100 text-[#1a237e] rounded-xl flex items-center justify-center text-xl mb-4">
                {f.icon}
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">{f.title}</h3>
              <p className="text-gray-500 text-sm leading-relaxed">{f.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>

    <section className="px-6 md:px-16 py-20 bg-gradient-to-br from-[#1a237e] to-[#0d47a1] text-white">
      <div className="max-w-6xl mx-auto">
        <div className="grid md:grid-cols-2 gap-12 items-center">
          <div>
            <div className="inline-flex items-center gap-2 bg-white/20 px-4 py-2 rounded-full text-sm font-medium mb-4">
              <FaShieldAlt /> About Us
            </div>
            <h2 className="text-3xl md:text-4xl font-bold mb-4">Modern Hostel Management</h2>
            <p className="text-blue-200 mb-6">
              Smart Hostel is a next-generation hostel management platform designed to digitize and
              streamline all aspects of hostel administration. From room allocation to leave management,
              complaint resolution to marketplace, we provide a seamless experience for students, wardens, and administrators.
            </p>
            <div className="space-y-3">
              {['Real-time notifications & updates', 'Role-based access control', 'Secure & scalable architecture', 'Mobile-responsive design'].map((item, i) => (
                <div key={i} className="flex items-center gap-3">
                  <FaCheckCircle className="text-green-300 shrink-0" />
                  <span>{item}</span>
                </div>
              ))}
            </div>
          </div>
          <div className="bg-white/10 backdrop-blur rounded-2xl p-12 text-center hidden md:block">
            <FaBuilding size={80} className="mx-auto mb-4 opacity-80" />
            <h3 className="text-2xl font-bold mb-2">Smart Hostel</h3>
            <p className="text-blue-200">Where Community Meets Convenience</p>
          </div>
        </div>
      </div>
    </section>

    <footer className="bg-gray-900 text-gray-400 px-6 md:px-16 py-12">
      <div className="max-w-6xl mx-auto">
        <div className="grid md:grid-cols-3 gap-8 mb-8">
          <div>
            <div className="flex items-center gap-2 text-white text-lg font-bold mb-3">
              <FaBuilding /> Smart Hostel
            </div>
            <p className="text-sm">Modern hostel management for modern campuses.</p>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Quick Links</h4>
            <div className="flex flex-col gap-2 text-sm">
              <Link to="/login" className="hover:text-white no-underline">Sign In</Link>
              <Link to="/register" className="hover:text-white no-underline">Register</Link>
              <a href="#features" className="hover:text-white no-underline">Features</a>
            </div>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-3">Contact</h4>
            <div className="flex flex-col gap-2 text-sm">
              <span>support@smarthostel.com</span>
              <span>+1 (555) 123-4567</span>
              <span>123 University Ave, Campus</span>
            </div>
          </div>
        </div>
        <div className="border-t border-gray-800 pt-8 text-center text-sm">
          &copy; {new Date().getFullYear()} Smart Hostel Management System. All rights reserved.
        </div>
      </div>
    </footer>
  </div>
  );
};

export default Home;
