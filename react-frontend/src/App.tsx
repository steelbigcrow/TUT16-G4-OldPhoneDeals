import { Navigate, Route, Routes } from 'react-router-dom';
import AdminRoute from './components/AdminRoute';
import ProtectedRoute from './components/ProtectedRoute';
import AdminListingPage from './pages/admin/AdminListingPage';
import AdminPhoneDetailPage from './pages/admin/AdminPhoneDetailPage';
import UserOrdersPage from './pages/user/UserOrdersPage';
import ProfilePage from './pages/profile/ProfilePage';
import EditProfilePage from './pages/profile/EditProfilePage';
import ChangePasswordPage from './pages/profile/ChangePasswordPage';

function App() {
  return (
    <div className="app-shell">
      <Routes>
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile/edit"
          element={
            <ProtectedRoute>
              <EditProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile/change-password"
          element={
            <ProtectedRoute>
              <ChangePasswordPage />
            </ProtectedRoute>
          }
        />
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
