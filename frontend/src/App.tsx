import { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import PrivateRoute from './components/PrivateRoute';
import Layout from './components/Layout';

const Login = lazy(() => import('./pages/Login'));
const Register = lazy(() => import('./pages/Register'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Transactions = lazy(() => import('./pages/Transactions'));
const Accounts = lazy(() => import('./pages/Accounts'));
const Categories = lazy(() => import('./pages/Categories'));
const CreditCards = lazy(() => import('./pages/CreditCards'));
const Budgets = lazy(() => import('./pages/Budgets'));
const LoginHistory = lazy(() => import('./pages/LoginHistory'));
const Profile = lazy(() => import('./pages/Profile'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'));
const ResetPassword = lazy(() => import('./pages/ResetPassword'));

function ProtectedLayout({ children }: { children: React.ReactNode }) {
  return (
    <PrivateRoute>
      <Layout>{children}</Layout>
    </PrivateRoute>
  );
}

function PageFallback() {
  return <div style={{ padding: 24 }}>Carregando...</div>;
}

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <Suspense fallback={<PageFallback />}>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route
              path="/"
              element={
                <ProtectedLayout>
                  <Dashboard />
                </ProtectedLayout>
              }
            />
            <Route
              path="/transactions"
              element={
                <ProtectedLayout>
                  <Transactions />
                </ProtectedLayout>
              }
            />
            <Route
              path="/accounts"
              element={
                <ProtectedLayout>
                  <Accounts />
                </ProtectedLayout>
              }
            />
            <Route
              path="/categories"
              element={
                <ProtectedLayout>
                  <Categories />
                </ProtectedLayout>
              }
            />
            <Route
              path="/credit-cards"
              element={
                <ProtectedLayout>
                  <CreditCards />
                </ProtectedLayout>
              }
            />
            <Route
              path="/budgets"
              element={
                <ProtectedLayout>
                  <Budgets />
                </ProtectedLayout>
              }
            />
            <Route
              path="/login-history"
              element={
                <ProtectedLayout>
                  <LoginHistory />
                </ProtectedLayout>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedLayout>
                  <Profile />
                </ProtectedLayout>
              }
            />
          </Routes>
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
