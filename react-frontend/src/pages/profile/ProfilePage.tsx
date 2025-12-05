import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getProfile } from '../../api/profile';
import Notification from '../../components/Notification';
import type { UserProfile } from '../../types/user';

const ProfilePage = () => {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | undefined>();

  const loadProfile = async () => {
    setLoading(true);
    setError(undefined);
    try {
      const data = await getProfile();
      setProfile(data);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Failed to load profile';
      setError(message);
      setProfile(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="page">
      <div className="toolbar">
        <h1>My Profile</h1>
        <div className="toolbar-actions" style={{ gap: '8px' }}>
          <button onClick={loadProfile} disabled={loading}>
            Refresh
          </button>
          <Link to="/profile/edit">Edit profile</Link>
          <Link to="/profile/change-password">Change password</Link>
        </div>
      </div>

      <div className="card">
        {error && <Notification message={error} type="error" />}
        {loading ? (
          <div className="loader">Loading your profile...</div>
        ) : profile ? (
          <div className="grid">
            <div>
              <div className="field-label">First name</div>
              <div className="field-value">{profile.firstName}</div>
            </div>
            <div>
              <div className="field-label">Last name</div>
              <div className="field-value">{profile.lastName}</div>
            </div>
            <div>
              <div className="field-label">Email</div>
              <div className="field-value">{profile.email}</div>
              {typeof profile.emailVerified === 'boolean' && (
                <div className="muted">
                  {profile.emailVerified ? 'Verified' : 'Not verified'}
                </div>
              )}
            </div>
            <div>
              <div className="field-label">Created</div>
              <div className="field-value">
                {profile.createdAt
                  ? new Date(profile.createdAt).toLocaleString()
                  : 'Unknown'}
              </div>
            </div>
            <div>
              <div className="field-label">Last updated</div>
              <div className="field-value">
                {profile.updatedAt
                  ? new Date(profile.updatedAt).toLocaleString()
                  : 'Unknown'}
              </div>
            </div>
          </div>
        ) : (
          <div className="muted">
            Could not load your profile. Please try again later.
          </div>
        )}
      </div>
    </div>
  );
};

export default ProfilePage;

