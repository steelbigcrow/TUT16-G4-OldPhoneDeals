import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { fetchAdminPhones } from '../../api/adminPhones';
import Notification from '../../components/Notification';
import { AdminPhoneListItem } from '../../types/phone';

const AdminListingPage = () => {
  const navigate = useNavigate();
  const [phones, setPhones] = useState<AdminPhoneListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | undefined>();
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const limit = 10;

  const totalPages = Math.max(1, Math.ceil(total / limit));

  const loadPhones = async () => {
    setLoading(true);
    setError(undefined);
    try {
      const response = await fetchAdminPhones(page, limit);
      setPhones(response.phones || []);
      setTotal(response.total || 0);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load listings';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPhones();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  return (
    <div className="page">
      <div className="toolbar">
        <h1>Admin Listing Management</h1>
        <div className="toolbar-actions">
          <button onClick={loadPhones} disabled={loading}>
            Refresh
          </button>
        </div>
      </div>

      <div className="card">
        {error && <Notification message={error} type="error" />}
        {loading ? (
          <div className="loader">Loading listings...</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Brand</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Seller</th>
                <th>Status</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {phones.length === 0 && (
                <tr>
                  <td colSpan={8} className="muted">
                    No listings found.
                  </td>
                </tr>
              )}
              {phones.map((phone) => (
                <tr key={phone._id}>
                  <td>
                    <Link to={`/admin/listings/${phone._id}`}>{phone.title}</Link>
                  </td>
                  <td>{phone.brand}</td>
                  <td>${phone.price.toFixed(2)}</td>
                  <td>{phone.stock}</td>
                  <td>
                    {phone.seller
                      ? `${phone.seller.firstName} ${phone.seller.lastName}`
                      : 'Unknown'}
                    <div className="muted">{phone.seller?.email}</div>
                  </td>
                  <td>
                    <span
                      className={`status-pill ${
                        phone.isDisabled ? 'status-disabled' : 'status-enabled'
                      }`}
                    >
                      {phone.isDisabled ? 'Disabled' : 'Enabled'}
                    </span>
                  </td>
                  <td>{new Date(phone.createdAt).toLocaleString()}</td>
                  <td>
                    <div className="actions">
                      <button
                        className="secondary"
                        onClick={() => navigate(`/admin/listings/${phone._id}`)}
                      >
                        View
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="toolbar" style={{ marginTop: '12px' }}>
        <div className="muted">
          Page {page} of {totalPages}
        </div>
        <div className="toolbar-actions">
          <button disabled={page === 1 || loading} onClick={() => setPage((p) => Math.max(1, p - 1))}>
            Previous
          </button>
          <button
            disabled={page >= totalPages || loading}
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
};

export default AdminListingPage;
