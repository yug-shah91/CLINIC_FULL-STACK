export default function ErrorMessage({ error, onRetry }) {
  return (
    <div className="alert alert-error" role="alert">
      <span>{error.message}</span>
      {onRetry && <button className="btn btn-ghost btn-sm" onClick={onRetry}>Try again</button>}
    </div>
  );
}
