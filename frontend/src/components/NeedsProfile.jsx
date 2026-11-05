import { Link } from 'react-router-dom';
import { UserRound } from 'lucide-react';
import EmptyState from './EmptyState';

// The backend returns 404 "Patient profile not found" until a patient fills in their profile.
export default function NeedsProfile() {
  return (
    <EmptyState icon={UserRound} title="Set up your profile first">
      <p className="muted">The clinic needs a few details before you can book visits.</p>
      <Link className="btn btn-primary" to="/profile">Create my profile</Link>
    </EmptyState>
  );
}
