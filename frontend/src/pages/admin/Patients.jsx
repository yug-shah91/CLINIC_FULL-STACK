import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Search, Users } from 'lucide-react';
import { patientApi } from '../../api/services';
import useFetch from '../../hooks/useFetch';
import useDebounce from '../../hooks/useDebounce';
import PageHeader from '../../components/PageHeader';
import ErrorMessage from '../../components/ErrorMessage';
import EmptyState from '../../components/EmptyState';
import Pagination from '../../components/Pagination';
import { TableSkeleton } from '../../components/Skeleton';
import { bloodGroupLabel, formatDate, initials } from '../../utils/format';

const SORTS = [
  ['registeredAt,desc', 'Newest first'],
  ['registeredAt,asc', 'Oldest first'],
  ['name,asc', 'Name A to Z'],
  ['name,desc', 'Name Z to A'],
];

export default function Patients() {
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState('registeredAt,desc');
  const [page, setPage] = useState(0);
  const debouncedQuery = useDebounce(query.trim(), 400); // wait until typing pauses

  const { data, loading, error, reload } = useFetch(
    () => (debouncedQuery
      ? patientApi.search({ query: debouncedQuery, page, sort })
      : patientApi.list({ page, sort })),
    [debouncedQuery, page, sort]
  );

  return (
    <>
      <PageHeader title="Patients" subtitle={data ? `${data.totalElements} found` : undefined} />

      <div className="filter-bar">
        <div className="field grow">
          <label htmlFor="search">Search</label>
          <div className="input-icon">
            <Search size={16} aria-hidden="true" />
            <input
              id="search" type="search" placeholder="Name or phone number" value={query}
              onChange={(e) => { setQuery(e.target.value); setPage(0); }}
            />
          </div>
        </div>
        <div className="field">
          <label htmlFor="sort">Sort by</label>
          <select id="sort" value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }}>
            {SORTS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </div>
      </div>

      {error ? (
        <ErrorMessage error={error} onRetry={reload} />
      ) : loading && !data ? (
        <TableSkeleton rows={8} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Users} title="No patients found">
          <p className="muted">
            {debouncedQuery ? `Nobody matches "${debouncedQuery}".` : 'Patients appear here after they create a profile.'}
          </p>
        </EmptyState>
      ) : (
        <>
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr><th>Patient</th><th>Phone</th><th>Blood group</th><th>Registered</th></tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <Link to={`/admin/patients/${p.id}`} className="person">
                        <span className="avatar" aria-hidden="true">{initials(p.name)}</span>
                        <span><strong>{p.name}</strong><br /><span className="muted">{p.email}</span></span>
                      </Link>
                    </td>
                    <td>{p.phone || <span className="muted">Not given</span>}</td>
                    <td>{bloodGroupLabel(p.bloodGroup)}</td>
                    <td>{formatDate(p.registeredAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </>
  );
}
