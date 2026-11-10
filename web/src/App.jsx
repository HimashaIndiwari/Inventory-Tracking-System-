import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { firebaseConfigured } from './firebase';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';
import { LoadingState } from './components/ui';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Products from './pages/Products';
import ProductHistory from './pages/ProductHistory';
import ProductForm from './pages/ProductForm';
import Categories from './pages/Categories';
import Suppliers from './pages/Suppliers';
import StockManagement from './pages/StockManagement';
import TransactionHistory from './pages/TransactionHistory';
import NotFound from './pages/NotFound';

/** Login and Register are for signed-out users only. */
function PublicOnly({ children }) {
  const { user, profile, loading } = useAuth();
  if (loading) return <div className="page-center"><LoadingState /></div>;
  if (user && profile) return <Navigate to="/" replace />;
  return children;
}

function SetupRequired() {
  return (
    <div className="page-center">
      <div className="card auth-card">
        <h1>Firebase is not configured</h1>
        <p>Copy <code>web/.env.example</code> to <code>web/.env.local</code>, fill in the Firebase web app values of the
          non-production project, and restart <code>npm run dev</code>.</p>
      </div>
    </div>
  );
}

export default function App() {
  if (!firebaseConfigured) return <SetupRequired />;
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
            <Route path="/register" element={<PublicOnly><Register /></PublicOnly>} />
            <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
              <Route index element={<Dashboard />} />
              <Route path="products" element={<Products />} />
              <Route path="products/new" element={<ProtectedRoute adminOnly><ProductForm /></ProtectedRoute>} />
              <Route path="products/:id/edit" element={<ProtectedRoute adminOnly><ProductForm /></ProtectedRoute>} />
              <Route path="products/:id/history" element={<ProductHistory />} />
              <Route path="categories" element={<Categories />} />
              <Route path="suppliers" element={<Suppliers />} />
              <Route path="stock" element={<StockManagement />} />
              <Route path="transactions" element={<TransactionHistory />} />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
