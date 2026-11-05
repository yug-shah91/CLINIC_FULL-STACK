import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  BarChart3, CalendarDays, CalendarPlus, HeartPulse, LayoutDashboard, LogOut, Moon, Sun, UserRound, Users,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import useTheme from '../hooks/useTheme';
import { initials } from '../utils/format';

const PATIENT_LINKS = [
  { to: '/appointments', label: 'Appointments', icon: CalendarDays, end: true },
  { to: '/appointments/new', label: 'Book visit', icon: CalendarPlus },
  { to: '/profile', label: 'My profile', icon: UserRound },
];

const DOCTOR_LINKS = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/admin/appointments', label: 'Appointments', icon: CalendarDays },
  { to: '/admin/patients', label: 'Patients', icon: Users },
  { to: '/admin/reports', label: 'Reports', icon: BarChart3 },
];

function Brand() {
  return (
    <div className="brand">
      <span className="brand-mark"><HeartPulse size={18} aria-hidden="true" /></span>
      Clinic
    </div>
  );
}

export default function Layout() {
  const { user, isDoctor, logout } = useAuth();
  const navigate = useNavigate();
  const { theme, toggle } = useTheme();
  const themeLabel = theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
  const ThemeIcon = theme === 'dark' ? Sun : Moon;
  const links = isDoctor ? DOCTOR_LINKS : PATIENT_LINKS;

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="shell">
      {/* Phones: a slim top bar. The sidebar below turns into a bottom tab bar. */}
      <header className="topbar">
        <Brand />
        <div className="topbar-actions">
          <button className="icon-btn" onClick={toggle} aria-label={themeLabel}>
            <ThemeIcon size={18} aria-hidden="true" />
          </button>
          <button className="icon-btn" onClick={handleLogout} aria-label="Log out">
            <LogOut size={18} aria-hidden="true" />
          </button>
        </div>
      </header>

      <aside className="sidebar">
        <Brand />
        <nav className="nav" aria-label="Main">
          {links.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end}>
              <Icon size={18} aria-hidden="true" />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <span className="avatar" aria-hidden="true">{initials(user.name)}</span>
          <div className="who">
            <strong>{user.name}</strong>
            <span className="muted">{isDoctor ? 'Doctor' : 'Patient'}</span>
          </div>
          <button className="icon-btn theme-toggle" onClick={toggle} aria-label={themeLabel} title={themeLabel}>
            <ThemeIcon size={18} aria-hidden="true" />
          </button>
          <button className="icon-btn" onClick={handleLogout} aria-label="Log out" title="Log out">
            <LogOut size={18} aria-hidden="true" />
          </button>
        </div>
      </aside>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
