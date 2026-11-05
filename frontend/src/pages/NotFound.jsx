import { Link } from 'react-router-dom';
import EmptyState from '../components/EmptyState';

export default function NotFound() {
  return (
    <div className="content">
      <EmptyState title="Page not found">
        The page you are looking for does not exist.
        <br />
        <Link className="btn btn-primary" to="/">Go to home</Link>
      </EmptyState>
    </div>
  );
}
