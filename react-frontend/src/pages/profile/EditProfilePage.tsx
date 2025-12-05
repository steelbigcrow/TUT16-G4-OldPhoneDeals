import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProfile, updateProfile } from '../../api/profile';
import Notification from '../../components/Notification';
import type { UserProfile } from '../../types/user';

const EditProfilePage = () => {
  const navigate = useNavigate();

  const [initialProfile, setInitialProfile] = useState<UserProfile | null>(
    null
  );
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | undefined>();
  const [success, setSuccess] = useState<string | undefined>();

  const emailChanged =
    initialProfile && email.trim() !== '' && email.trim() !== initialProfile.email;

  const loadProfile = async () => {
    setLoading(true);
    setError(undefined);
    setSuccess(undefined);
    try {
      const profile = await getProfile();
      setInitialProfile(profile);
      setFirstName(profile.firstName);
      setLastName(profile.lastName);
      setEmail(profile.email);
      setCurrentPassword('');
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Failed to load profile';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!firstName.trim() || !lastName.trim() || !email.trim()) {
      setError('First name, last name, and email are required.');
      setSuccess(undefined);
      return;
    }

    if (emailChanged && !currentPassword) {
      setError('Current password is required to change email.');
      setSuccess(undefined);
      return;
    }

    setSaving(true);
    setError(undefined);
    setSuccess(undefined);

    try {
      const updated = await updateProfile({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        currentPassword: emailChanged ? currentPassword : undefined
      });

      setInitialProfile(updated);
      setFirstName(updated.firstName);
      setLastName(updated.lastName);
      setEmail(updated.email);
      setCurrentPassword('');
      setSuccess('Profile updated successfully.');
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Failed to update profile';
      setError(message);
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    if (initialProfile) {
      setFirstName(initialProfile.firstName);
      setLastName(initialProfile.lastName);
      setEmail(initialProfile.email);
      setCurrentPassword('');
      setError(undefined);
      setSuccess(undefined);
    } else {
      navigate('/profile');
    }
  };

  return (
    <div className="page">
      <div className="toolbar">
        <button className="secondary" onClick={() => navigate('/profile')}>
          Back to profile
        </button>
        <h1 style={{ margin: 0 }}>Edit Profile</h1>
        <div className="toolbar-actions" />
      </div>

      <div className="card">
        {error && <Notification message={error} type="error" />}
        {success && <Notification message={success} type="success" />}

        {loading ? (
          <div className="loader">Loading profile...</div>
        ) : (
          <form onSubmit={handleSubmit} className="form">
            <div className="field">
              <label className="field-label" htmlFor="firstName">
                First name
              </label>
              <input
                id="firstName"
                type="text"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
              />
            </div>
            <div className="field">
              <label className="field-label" htmlFor="lastName">
                Last name
              </label>
              <input
                id="lastName"
                type="text"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
              />
            </div>
            <div className="field">
              <label className="field-label" htmlFor="email">
                Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <div className="muted">
                Changing your email requires your current password.
              </div>
            </div>
            <div className="field">
              <label className="field-label" htmlFor="currentPassword">
                Current password {emailChanged ? '(required for email change)' : '(optional)'}
              </label>
              <input
                id="currentPassword"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder={emailChanged ? '' : 'Only needed when changing email'}
              />
            </div>

            <div className="toolbar-actions" style={{ marginTop: '16px', gap: '8px' }}>
              <button type="submit" disabled={saving}>
                {saving ? 'Saving...' : 'Save changes'}
              </button>
              <button
                type="button"
                className="secondary"
                onClick={handleCancel}
                disabled={saving}
              >
                Cancel
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default EditProfilePage;

