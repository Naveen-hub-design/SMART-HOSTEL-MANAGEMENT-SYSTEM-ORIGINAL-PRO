import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import accountService from '../../services/accountService';
import {
  FaUser, FaEnvelope, FaPhone, FaLock, FaEdit, FaSave, FaTimes,
  FaIdBadge, FaBuilding,
} from 'react-icons/fa';

const editableKeys = ['name', 'phone'];

const WardenProfile = () => {
  const [profile, setProfile] = useState(null);
  const [editData, setEditData] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [pwData, setPwData] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [showPw, setShowPw] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [imgFile, setImgFile] = useState(null);
  const [imgPreview, setImgPreview] = useState('');
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    accountService.getProfile()
      .then(data => { setProfile(data); setEditData(data); })
      .catch(() => toast.error('Failed to load profile'))
      .finally(() => setLoading(false));
  }, []);

  const handleUpdate = async () => {
    if (!editData?.name?.trim()) return toast.error('Name is required');
    setSubmitting(true);
    try {
      const res = await accountService.updateProfile({
        name: editData.name,
        phone: editData.phone || '',
      });
      const updated = res.user || res;
      setProfile(updated);
      setEditData(updated);
      setIsEditing(false);
      toast.success('Profile updated successfully');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Update failed');
    } finally {
      setSubmitting(false);
    }
  };

  const handleChangePassword = async () => {
    if (!pwData.currentPassword || !pwData.newPassword) return toast.error('Please fill all password fields');
    if (pwData.newPassword !== pwData.confirmPassword) return toast.error('Passwords do not match');
    if (pwData.newPassword.length < 6) return toast.error('Password must be at least 6 characters');
    setSubmitting(true);
    try {
      await accountService.changePassword({
        currentPassword: pwData.currentPassword,
        newPassword: pwData.newPassword,
      });
      toast.success('Password changed successfully');
      setPwData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setShowPw(false);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Password change failed');
    } finally {
      setSubmitting(false);
    }
  };

  const handleImageSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!accountService.isAllowedImage(file)) {
      toast.error('Only JPG, PNG or GIF images are allowed');
      e.target.value = '';
      return;
    }
    if (imgPreview) URL.revokeObjectURL(imgPreview);
    setImgFile(file);
    setImgPreview(URL.createObjectURL(file));
  };

  const handleImageUpload = async () => {
    if (!imgFile) return toast.error('Please choose an image first');
    setUploading(true);
    try {
      await accountService.uploadProfilePicture(imgFile);
      const updated = await accountService.getProfile();
      setProfile(updated);
      setEditData(updated);
      if (imgPreview) URL.revokeObjectURL(imgPreview);
      setImgFile(null);
      setImgPreview('');
      toast.success('Profile picture updated');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Image upload failed');
    } finally {
      setUploading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="w-10 h-10 border-4 border-gray-200 border-t-[#1a237e] rounded-full animate-spin" />
      </div>
    );
  }
  if (!profile) {
    return <div className="p-6"><div className="flex flex-col items-center justify-center py-12 text-gray-400"><FaUser size={32} /><p>Failed to load profile</p></div></div>;
  }

  const fields = [
    { label: 'Name', key: 'name', icon: <FaUser /> },
    { label: 'Email', key: 'email', icon: <FaEnvelope /> },
    { label: 'Phone', key: 'phone', icon: <FaPhone /> },
    { label: 'Role', key: 'role', icon: <FaIdBadge /> },
    { label: 'Qualification', key: 'qualification', icon: <FaIdBadge /> },
    { label: 'Assigned Block', key: 'blockName', icon: <FaBuilding /> },
  ];

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Profile</h1>
          <p className="text-sm text-gray-500">Manage your personal information</p>
        </div>
        <button
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            isEditing ? 'bg-green-600 text-white hover:bg-green-700' : 'bg-[#1a237e] text-white hover:bg-[#0d47a1]'
          }`}
          onClick={() => isEditing ? handleUpdate() : setIsEditing(true)}
          disabled={submitting}
        >
          {isEditing ? <><FaSave /> Save Changes</> : <><FaEdit /> Edit Profile</>}
        </button>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
        <div className="flex flex-col items-center mb-6">
          {imgPreview || profile.profileImageUrl ? (
            <img
              src={imgPreview || accountService.resolveImageUrl(profile.profileImageUrl)}
              alt="Profile"
              className="w-20 h-20 object-cover rounded-full mb-2"
            />
          ) : (
            <div className="w-20 h-20 bg-[#1a237e] text-white rounded-full flex items-center justify-center text-3xl font-bold mb-2">
              {profile.name?.charAt(0).toUpperCase() || <FaUser />}
            </div>
          )}
          <label className="text-xs text-[#1a237e] hover:underline cursor-pointer mb-2">
            {profile.profileImageUrl || imgPreview ? 'Change Image' : 'Choose Image'}
            <input type="file" accept="image/*" className="hidden" onChange={handleImageSelect} />
          </label>
          {imgFile && (
            <button
              className="px-4 py-1.5 bg-[#1a237e] text-white rounded-lg text-xs font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              onClick={handleImageUpload} disabled={uploading}>
              {uploading ? 'Uploading...' : 'Upload'}
            </button>
          )}
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {fields.map((f, i) => (
            <div key={i}>
              <span className="flex items-center gap-2 text-xs text-gray-500 uppercase tracking-wide mb-1">
                {f.icon} {f.label}
              </span>
              {isEditing && editableKeys.includes(f.key) ? (
                <input
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  value={editData?.[f.key] || ''}
                  onChange={(e) => setEditData({ ...editData, [f.key]: e.target.value })}
                />
              ) : (
                <p className="text-sm font-medium text-gray-900">{profile[f.key] || '\u2014'}</p>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2"><FaLock /> Change Password</h3>
          <button
            className="flex items-center gap-1 px-3 py-1.5 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:border-[#1a237e] hover:text-[#1a237e] transition-colors"
            onClick={() => setShowPw(!showPw)}
          >
            {showPw ? <><FaTimes /> Cancel</> : <><FaLock /> Change</>}
          </button>
        </div>
        {showPw && (
          <div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Current Password</label>
                <input type="password"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  value={pwData.currentPassword}
                  onChange={(e) => setPwData({ ...pwData, currentPassword: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                <input type="password"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  value={pwData.newPassword}
                  onChange={(e) => setPwData({ ...pwData, newPassword: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Confirm Password</label>
                <input type="password"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-[#1a237e] focus:border-transparent outline-none"
                  value={pwData.confirmPassword}
                  onChange={(e) => setPwData({ ...pwData, confirmPassword: e.target.value })} />
              </div>
            </div>
            <button
              className="bg-[#1a237e] text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-[#0d47a1] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              onClick={handleChangePassword} disabled={submitting}>
              {submitting ? 'Updating...' : 'Update Password'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default WardenProfile;
