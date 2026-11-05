import { Link } from 'react-router-dom';
import { CalendarDays, CalendarOff, CheckCircle2, Clock, Users } from 'lucide-react';
import { appointmentApi, reportApi } from '../../api/services';
import useFetch from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import StatCard from '../../components/StatCard';
import EmptyState from '../../components/EmptyState';
import ErrorMessage from '../../components/ErrorMessage';
import ManagedAppointmentTable from '../../components/ManagedAppointmentTable';
import { CardsSkeleton, TableSkeleton } from '../../components/Skeleton';
import { formatDate } from '../../utils/format';

export default function Dashboard() {
  const overview = useFetch(() => reportApi.overview(), []);
  const today = useFetch(() => appointmentApi.today(), []);

  // After confirming / completing / cancelling, both the cards and the table must refresh.
  const reloadAll = () => { overview.reload(); today.reload(); };

  // ISO date-time strings sort correctly as plain text
  const schedule = today.data
    ? [...today.data].sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime))
    : [];
  const o = overview.data;

  return (
    <>
      <PageHeader title="Dashboard" subtitle={formatDate(new Date())} />

      {overview.error ? (
        <ErrorMessage error={overview.error} onRetry={overview.reload} />
      ) : !o ? (
        <CardsSkeleton />
      ) : (
        <div className="stats-grid">
          <StatCard icon={Users} tone="brand" label="Total patients" value={o.totalPatients} />
          <StatCard icon={CalendarDays} tone="blue" label="Today's appointments" value={o.todayAppointments} />
          <StatCard icon={Clock} tone="amber" label="Pending confirmation" value={o.pendingAppointments} hint="Booked, waiting for you" />
          <StatCard icon={CheckCircle2} tone="green" label="Completed this month" value={o.completedThisMonth} />
        </div>
      )}

      <div className="section-head">
        <h2>Today's schedule</h2>
        <Link to="/admin/appointments">All appointments</Link>
      </div>

      {today.error ? (
        <ErrorMessage error={today.error} onRetry={today.reload} />
      ) : !today.data ? (
        <TableSkeleton />
      ) : schedule.length === 0 ? (
        <EmptyState icon={CalendarOff} title="No appointments today">
          <p className="muted">New bookings for today will appear here.</p>
        </EmptyState>
      ) : (
        <ManagedAppointmentTable appointments={schedule} reload={reloadAll} showDate={false} />
      )}
    </>
  );
}
