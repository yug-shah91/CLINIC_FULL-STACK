import { useState } from 'react';
import { reportApi } from '../../api/services';
import useFetch from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import Tabs from '../../components/Tabs';
import StatCard from '../../components/StatCard';
import Chart, { Legend } from '../../components/Chart';
import ErrorMessage from '../../components/ErrorMessage';
import { CardsSkeleton, Skeleton } from '../../components/Skeleton';
import { formatShortDate } from '../../utils/format';
import { CalendarDays, CheckCircle2, Percent, XCircle } from 'lucide-react';

const PERIODS = {
  DAILY: { label: 'Daily', current: 'Today', previous: 'Yesterday', chartTitle: 'Last 14 days' },
  WEEKLY: { label: 'Weekly', current: 'This week', previous: 'Last week', chartTitle: 'Last 8 weeks (weeks start on Monday)' },
  MONTHLY: { label: 'Monthly', current: 'This month', previous: 'Last month', chartTitle: 'Last 6 months' },
};

const monthName = (iso, long) =>
  new Date(`${iso}T00:00:00`).toLocaleDateString('en-GB', long ? { month: 'long', year: 'numeric' } : { month: 'short' });

const bucketLabel = (period, b, long = false) => {
  if (period === 'MONTHLY') return monthName(b.start, long);
  if (period === 'WEEKLY' && long) return `${formatShortDate(b.start)} to ${formatShortDate(b.end)}`;
  return formatShortDate(b.start);
};

const percent = (part, whole) => (whole === 0 ? 0 : Math.round((part / whole) * 100));

const APPOINTMENT_SERIES = [
  { key: 'completed', label: 'Completed', color: '#2f8f5b' },
  { key: 'active', label: 'Booked or confirmed', color: '#4f86b8' },
  { key: 'cancelled', label: 'Cancelled', color: '#c4574d' },
];
const RATE_SERIES = [{ key: 'rate', label: 'Cancellation rate', color: '#c4574d' }];

export default function Reports() {
  const [period, setPeriod] = useState('DAILY');
  const { data, error, reload } = useFetch(() => reportApi.appointments(period), [period]);
  const meta = PERIODS[period];

  const body = () => {
    if (error) return <ErrorMessage error={error} onRetry={reload} />;
    if (!data || data.period !== period) {
      return (
        <>
          <CardsSkeleton />
          <Skeleton height={260} style={{ borderRadius: 12, marginTop: 24 }} />
        </>
      );
    }

    const buckets = data.buckets;
    const now = buckets[buckets.length - 1];
    const before = buckets[buckets.length - 2];
    const chartData = buckets.map((b) => ({
      label: bucketLabel(period, b),
      values: { completed: b.completed, active: b.booked + b.confirmed, cancelled: b.cancelled },
    }));
    const rateData = buckets.map((b) => ({ label: bucketLabel(period, b), values: { rate: percent(b.cancelled, b.total) } }));

    return (
      <>
        <div className="stats-grid">
          <StatCard icon={CalendarDays} tone="blue" label={`Appointments ${meta.current.toLowerCase()}`} value={now.total} hint={`${meta.previous}: ${before.total}`} />
          <StatCard icon={CheckCircle2} tone="green" label="Completed" value={now.completed} hint={`${meta.previous}: ${before.completed}`} />
          <StatCard icon={XCircle} tone="red" label="Cancelled" value={now.cancelled} hint={`${meta.previous}: ${before.cancelled}`} />
          <StatCard icon={Percent} tone="amber" label="Cancellation rate" value={`${percent(now.cancelled, now.total)}%`} hint={`${meta.previous}: ${percent(before.cancelled, before.total)}%`} />
        </div>

        <section className="panel chart-panel">
          <div className="section-head tight"><h2>Appointments</h2><span className="muted">{meta.chartTitle}</span></div>
          <Legend series={APPOINTMENT_SERIES} />
          <Chart buckets={chartData} series={APPOINTMENT_SERIES}
            description={`Stacked bar chart of appointments. ${meta.chartTitle}. A table with the same numbers follows.`} />
        </section>

        <section className="panel chart-panel">
          <div className="section-head tight"><h2>Cancellations</h2><span className="muted">Share of appointments that were cancelled</span></div>
          <Chart buckets={rateData} series={RATE_SERIES} yFormat={(v) => `${v}%`} maxValue={100}
            description="Bar chart of the cancellation rate per period. A table with the same numbers follows." />
        </section>

        <h2 className="section-title">Breakdown</h2>
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr><th>{meta.label === 'Daily' ? 'Day' : meta.label === 'Weekly' ? 'Week' : 'Month'}</th><th>Total</th><th>Completed</th><th>Booked or confirmed</th><th>Cancelled</th><th>Cancel rate</th></tr>
            </thead>
            <tbody>
              {[...buckets].reverse().map((b) => (
                <tr key={b.start}>
                  <td>{bucketLabel(period, b, true)}</td>
                  <td><strong>{b.total}</strong></td>
                  <td>{b.completed}</td>
                  <td>{b.booked + b.confirmed}</td>
                  <td>{b.cancelled}</td>
                  <td>{percent(b.cancelled, b.total)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </>
    );
  };

  return (
    <>
      <PageHeader title="Reports" subtitle="How busy the clinic is, and how many visits are cancelled." />
      <Tabs
        label="Report period"
        active={period}
        onChange={setPeriod}
        tabs={Object.entries(PERIODS).map(([id, p]) => ({ id, label: p.label }))}
      />
      {body()}
    </>
  );
}
