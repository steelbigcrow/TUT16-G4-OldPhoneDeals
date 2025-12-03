import { useEffect, useState } from 'react';
import Notification from '../../components/Notification';
import { fetchUserOrders } from '../../api/orders';
import type { Order } from '../../types/order';

const DEFAULT_PAGE_SIZE = 10;

const UserOrdersPage = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | undefined>();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);

  const loadOrders = async () => {
    setLoading(true);
    setError(undefined);
    try {
      const result = await fetchUserOrders(page, pageSize);
      setOrders(result.items);
      setTotalPages(result.pagination.totalPages || 1);
      setTotalItems(result.pagination.totalItems || result.items.length);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load orders';
      setError(message);
      setOrders([]);
      setTotalPages(1);
      setTotalItems(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, pageSize]);

  const handlePageChange = (nextPage: number) => {
    setPage((current) => {
      const clamped = Math.min(Math.max(1, nextPage), totalPages || 1);
      return clamped === current ? current : clamped;
    });
  };

  const handlePageSizeChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    const value = Number(event.target.value) || DEFAULT_PAGE_SIZE;
    setPageSize(value);
    setPage(1);
  };

  const hasOrders = orders.length > 0;

  return (
    <div className="page">
      <div className="toolbar">
        <h1>User Order History</h1>
        <div className="toolbar-actions">
          <button onClick={loadOrders} disabled={loading}>
            Refresh
          </button>
        </div>
      </div>

      <div className="card">
        {error && <Notification message={error} type="error" />}
        {loading ? (
          <div className="loader">Loading your orders...</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Order</th>
                <th>Placed</th>
                <th>Items</th>
                <th>Total</th>
                <th>Shipping</th>
              </tr>
            </thead>
            <tbody>
              {!hasOrders && (
                <tr>
                  <td colSpan={5} className="muted">
                    You have no orders yet.
                  </td>
                </tr>
              )}
              {orders.map((order) => {
                const orderId = order.id || order._id || 'unknown';
                const itemSummary = order.items
                  .map((item) => `${item.title} (x${item.quantity})`)
                  .join(', ');
                const address = order.address;
                const addressSummary = address
                  ? `${address.city}, ${address.state}, ${address.country}`
                  : 'N/A';

                return (
                  <tr key={orderId}>
                    <td>{orderId}</td>
                    <td>{new Date(order.createdAt).toLocaleString()}</td>
                    <td>{itemSummary || 'No items'}</td>
                    <td>${order.totalAmount.toFixed(2)}</td>
                    <td>{addressSummary}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      <div className="toolbar" style={{ marginTop: '12px' }}>
        <div className="muted">
          Page {totalPages === 0 ? 0 : page} of {Math.max(totalPages, 1)} ({totalItems} orders)
        </div>
        <div className="toolbar-actions" style={{ gap: '8px', alignItems: 'center' }}>
          <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            Page size
            <select value={pageSize} onChange={handlePageSizeChange} disabled={loading}>
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
            </select>
          </label>
          <button disabled={page <= 1 || loading} onClick={() => handlePageChange(page - 1)}>
            Previous
          </button>
          <button
            disabled={page >= totalPages || loading || totalPages === 0}
            onClick={() => handlePageChange(page + 1)}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
};

export default UserOrdersPage;
