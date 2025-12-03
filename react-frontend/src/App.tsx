import { Navigate, Route, Routes } from 'react-router-dom';
import AdminRoute from './components/AdminRoute';
import AdminListingPage from './pages/admin/AdminListingPage';
import AdminPhoneDetailPage from './pages/admin/AdminPhoneDetailPage';

function App() {
  return (
    <div className="app-shell">
      <Routes>
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
