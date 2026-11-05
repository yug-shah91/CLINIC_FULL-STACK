import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';

import Login from './pages/Login';
import Register from './pages/Register';
import NotFound from './pages/NotFound';

import Profile from './pages/patient/Profile';
import MyAppointments from './pages/patient/MyAppointments';
import BookAppointment from './pages/patient/BookAppointment';

import Dashboard from './pages/admin/Dashboard';
import AllAppointments from './pages/admin/AllAppointments';
import Patients from './pages/admin/Patients';
import PatientDetail from './pages/admin/PatientDetail';
import Prescription from './pages/admin/Prescription';
import Reports from './pages/admin/Reports';

// "/" sends each role to its own home page.
function HomeRedirect() {
  const { isDoctor } = useAuth();
  return <Navigate to={isDoctor ? '/admin' : '/appointments'} replace />;
}

export default function App() {
  return (
    <Routes>
      {/* Public pages */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* Everything below needs a logged-in user */}
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route index element={<HomeRedirect />} />

          {/* Patient only (backend role: PATIENT) */}
          <Route element={<ProtectedRoute role="PATIENT" />}>
            <Route path="profile" element={<Profile />} />
            <Route path="appointments" element={<MyAppointments />} />
            <Route path="appointments/new" element={<BookAppointment />} />
          </Route>

          {/* Doctor only (backend role: ADMIN) */}
          <Route path="admin" element={<ProtectedRoute role="ADMIN" />}>
            <Route index element={<Dashboard />} />
            <Route path="appointments" element={<AllAppointments />} />
            <Route path="patients" element={<Patients />} />
            <Route path="patients/:id" element={<PatientDetail />} />
            <Route path="reports" element={<Reports />} />
            <Route path="prescriptions/:appointmentId" element={<Prescription />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
