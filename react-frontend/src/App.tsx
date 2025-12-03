import { Navigate, Route, Routes } from 'react-router-dom';
import AdminRoute from './components/AdminRoute';
import ProtectedRoute from './components/ProtectedRoute';
import AdminListingPage from './pages/admin/AdminListingPage';
import AdminPhoneDetailPage from './pages/admin/AdminPhoneDetailPage';
import UserOrdersPage from './pages/user/UserOrdersPage';

function App() {
  return (
    <div className="app-shell">
      <Routes>
        <Route
          path="/orders"
          element={
            <ProtectedRoute>
              <UserOrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/listings"
          element={
            <AdminRoute>
              <AdminListingPage />
            </AdminRoute>
          }
        />
        <Route
          path="/admin/listings/:phoneId"
          element={
            <AdminRoute>
              <AdminPhoneDetailPage />
            </AdminRoute>
          }
        />
        <Route path="*" element={<Navigate to="/admin/listings" replace />} />
      </Routes>
    </div>
  );
}

export default App;
