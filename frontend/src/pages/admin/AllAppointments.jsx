import { useState } from 'react';
import { CalendarSearch } from 'lucide-react';
import { appointmentApi } from '../../api/services';
import useFetch from '../../hooks/useFetch';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import Pagination from '../../components/Pagination';
import ManagedAppointmentTable from '../../components/ManagedAppointmentTable';
import { TableSkeleton } from '../../components/Skeleton';
import { STATUS_LABELS } from '../../utils/format';

const NO_FILTERS = { status: '', from: '', to: '', sort: 'appointmentTime,desc' };
const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0 };

export default function AllAppointments() {
  const [filters, setFilters] = useState(NO_FILTERS);
  const [page, setPage] = useState(0);

  const rangeInvalid = filters.from && filters.to && filters.from > filters.to;
  const hasFilters = filters.status || filters.from || filters.to;

  // Re-fetch whenever the page or any filter changes (the dependency list below).
  const { data, loading, error, reload } = useFetch(
    () => (rangeInvalid ? Promise.resolve(EMPTY_PAGE) : appointmentApi.listAll({ page, ...filters })),
    [page, filters.status, filters.from, filters.to, filters.sort]
  );

  const setFilter = (name) => (e) => {
    setFilters((f) => ({ ...f, [name]: e.target.value }));
    setPage(0); // a new filter always starts from the first page
  };

  return (
    <>
      <PageHeader
        title="Appointments"
        subtitle={data ? `${data.totalElements} ${hasFilters ? 'matching' : 'in total'}` : undefined}
      />

      <div className="filter-bar">
        <div className="field">
          <label htmlFor="f-status">Status</label>
          <select id="f-status" value={filters.status} onChange={setFilter('status')}>
            <option value="">All statuses</option>
            {Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </div>
        <div className="field">
          <label htmlFor="f-from">From</label>
          <input id="f-from" type="date" value={filters.from} onChange={setFilter('from')} />
        </div>
        <div className="field">
          <label htmlFor="f-to">To</label>
          <input id="f-to" type="date" value={filters.to} min={filters.from || undefined} onChange={setFilter('to')} />
        </div>
        <div className="field">
          <label htmlFor="f-sort">Order</label>
          <select id="f-sort" value={filters.sort} onChange={setFilter('sort')}>
            <option value="appointmentTime,desc">Newest first</option>
            <option value="appointmentTime,asc">Oldest first</option>
          </select>
        </div>
        {hasFilters && (
          <button className="btn btn-ghost" onClick={() => { setFilters(NO_FILTERS); setPage(0); }}>Clear filters</button>
        )}
      </div>
      {rangeInvalid && <p className="field-error" role="alert">The "From" date must not be after the "To" date.</p>}

      {error ? (
        <ErrorMessage error={error} onRetry={reload} />
      ) : loading && !data ? (
        <TableSkeleton rows={8} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={CalendarSearch} title={hasFilters ? 'No appointments match these filters' : 'No appointments yet'}>
          {hasFilters && <p className="muted">Try a wider date range or a different status.</p>}
        </EmptyState>
      ) : (
        <>
          <ManagedAppointmentTable appointments={data.content} reload={reload} />
          <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </>
  );
}
