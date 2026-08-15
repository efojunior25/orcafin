import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import './Layout.css';

const navItems = [
  { to: '/', label: 'Dashboard', icon: '📊', end: true },
  { to: '/transactions', label: 'Transações', icon: '💸', end: false },
  { to: '/budgets', label: 'Orçamento', icon: '🎯', end: false },
  { to: '/accounts', label: 'Contas', icon: '🏦', end: false },
  { to: '/credit-cards', label: 'Cartões', icon: '💳', end: false },
  { to: '/prepaid-cards', label: 'Cartões Pré-pagos', icon: '🎫', end: false },
  { to: '/vaults', label: 'Caixinhas', icon: '🐷', end: false },
  { to: '/investments', label: 'Investimentos', icon: '📈', end: false },
  { to: '/categories', label: 'Categorias', icon: '🏷️', end: false },
  { to: '/login-history', label: 'Acessos', icon: '🔒', end: false },
  { to: '/profile', label: 'Perfil', icon: '👤', end: false },
];

export default function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">
          Orça<span>Fin</span>
        </div>
        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => (isActive ? 'active' : '')}
            >
              <span>{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="sidebar-user">
            <strong>{user?.name}</strong>
            {user?.email}
          </div>
          <div className="sidebar-actions">
            <button className="theme-toggle" onClick={toggleTheme} title="Alternar tema">
              {theme === 'dark' ? '☀️' : '🌙'}
            </button>
            <button className="btn btn-ghost logout-btn" onClick={logout}>
              Sair
            </button>
          </div>
        </div>
      </aside>
      <main className="content">{children}</main>
    </div>
  );
}
