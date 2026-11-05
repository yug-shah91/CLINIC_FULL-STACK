import Modal from './Modal';

export default function ConfirmModal({ title, children, confirmLabel = 'Confirm', cancelLabel = 'Go back', danger, busy, onConfirm, onClose }) {
  return (
    <Modal
      title={title}
      onClose={onClose}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose}>{cancelLabel}</button>
          <button className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} disabled={busy} onClick={onConfirm}>
            {busy ? 'Please wait...' : confirmLabel}
          </button>
        </>
      }
    >
      {children}
    </Modal>
  );
}
