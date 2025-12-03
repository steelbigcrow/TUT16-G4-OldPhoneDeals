import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { deleteAdminPhone, fetchAdminPhoneDetail, updateAdminPhoneStatus } from '../../api/adminPhones';
import Notification from '../../components/Notification';
import { AdminPhoneDetail } from '../../types/phone';

const AdminPhoneDetailPage = () => {
  const { phoneId } = useParams<{ phoneId: string }>();
  const navigate = useNavigate();

  const [phone, setPhone] = useState<AdminPhoneDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | undefined>();
  const [message, setMessage] = useState<string | undefined>();

  const resolvedImage = useMemo(() => {
    if (!phone?.image) return '';
    if (phone.image.startsWith('http')) {
      return phone.image;
    }
    return `http://localhost:3000${phone.image.startsWith('/') ? '' : '/'}${phone.image}`;
  }, [phone]);

  const loadDetail = async () => {
    setLoading(true);
    setError(undefined);
    setMessage(undefined);

    if (!phoneId) {
      setError('Missing phone id.');
      setLoading(false);
      return;
    }

    try {
      const data = await fetchAdminPhoneDetail(phoneId);
      setPhone(data);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to load phone detail';
      setError(msg);
      setPhone(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phoneId]);

  const handleToggleStatus = async () => {
    if (!phone) return;
    const nextStatus = !phone.isDisabled;
    const confirmMessage = `Are you sure you want to ${nextStatus ? 'disable' : 'enable'} "${phone.title}"?`;
    if (!window.confirm(confirmMessage)) return;

    setActionLoading(true);
    setMessage(undefined);
    setError(undefined);
    try {
      await updateAdminPhoneStatus(phone._id, nextStatus);
      setMessage(`Listing ${nextStatus ? 'disabled' : 'enabled'} successfully`);
      await loadDetail();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to update status';
      setError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!phone) return;
    if (!window.confirm(`Delete listing "${phone.title}"? This cannot be undone.`)) {
      return;
    }
    setActionLoading(true);
    setError(undefined);
    setMessage(undefined);
    try {
      await deleteAdminPhone(phone._id);
      setMessage('Listing deleted successfully');
      navigate('/admin/listings');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to delete listing';
      setError(msg);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="page">
      <div className="toolbar">
        <button className="secondary" onClick={() => navigate('/admin/listings')}>
          Back to listings
        </button>
        <h1 style={{ margin: 0 }}>Listing Detail</h1>
        <div className="toolbar-actions" />
      </div>

      <div className="card">
        {error && <Notification message={error} type="error" />}
        {message && <Notification message={message} type="success" />}

        {loading ? (
          <div className="loader">Loading phone detail...</div>
        ) : phone ? (
          <>
            <div className="grid" style={{ marginBottom: '12px' }}>
              <div>
                <div className="field-label">Title</div>
                <div className="field-value">{phone.title}</div>
              </div>
              <div>
                <div className="field-label">Brand</div>
                <div className="field-value">{phone.brand}</div>
              </div>
              <div>
                <div className="field-label">Price</div>
                <div className="field-value">${phone.price.toFixed(2)}</div>
              </div>
              <div>
                <div className="field-label">Stock</div>
                <div className="field-value">{phone.stock}</div>
              </div>
              <div>
                <div className="field-label">Status</div>
                <span
                  className={`status-pill ${phone.isDisabled ? 'status-disabled' : 'status-enabled'}`}
                >
                  {phone.isDisabled ? 'Disabled' : 'Enabled'}
                </span>
              </div>
              <div>
                <div className="field-label">Seller</div>
                <div className="field-value">
                  {phone.seller ? `${phone.seller.firstName} ${phone.seller.lastName}` : 'Unknown'}
                </div>
                <div className="muted">{phone.seller?.email}</div>
              </div>
              <div>
                <div className="field-label">Created</div>
                <div className="field-value">{new Date(phone.createdAt).toLocaleString()}</div>
              </div>
              <div>
                <div className="field-label">Updated</div>
                <div className="field-value">{new Date(phone.updatedAt).toLocaleString()}</div>
              </div>
            </div>

            {resolvedImage && (
              <div style={{ marginBottom: '16px' }}>
                <div className="field-label">Image</div>
                <img src={resolvedImage} alt={phone.title} className="image-preview" />
              </div>
            )}

            <div className="toolbar-actions" style={{ gap: '10px' }}>
              <button onClick={handleToggleStatus} disabled={actionLoading}>
                {phone.isDisabled ? 'Enable' : 'Disable'}
              </button>
              <button className="danger" onClick={handleDelete} disabled={actionLoading}>
                Delete
              </button>
            </div>
          </>
        ) : (
          <div className="muted">
            Could not find phone detail. Please return to listings and try again.
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminPhoneDetailPage;
