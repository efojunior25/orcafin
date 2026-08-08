import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import PrivateRoute from './components/PrivateRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Transactions from './pages/Transactions';
import Accounts from './pages/Accounts';
import Categories from './pages/Categories';
import CreditCards from './pages/CreditCards';
import Budgets from './pages/Budgets';
import LoginHistory from './pages/LoginHistory';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';

function ProtectedLayout({ children }: { children: React.ReactNode }) {
  return (
    <PrivateRoute>
      <Layout>{children}</Layout>
    </PrivateRoute>
  );
}

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
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
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
