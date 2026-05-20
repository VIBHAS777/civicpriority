import { useNavigate } from 'react-router-dom';
import { useIssues } from '../context/IssuesContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import s from './Audit.module.css';

function formatTs(ts) {
  if (!ts) return '—';
  const d = new Date(ts);
  return d.toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' }) +
    ' · ' + d.toLocaleTimeString('en-IN', { hour:'2-digit', minute:'2-digit' });
}

export default function Audit() {
  const { auditLog } = useIssues();
  const { user, canViewAuditLog } = useAuth();
  const navigate = useNavigate();

  if (!user || !canViewAuditLog) {
    return (
      <div className={s.accessWrap}>
        <div className={s.accessBox}>
          <div className={s.accessIcon}>🔐</div>
          <h2 className={s.accessTitle}>Admin Access Required</h2>
          <p className={s.accessSub}>Only Admin Authority and System Admin accounts can view the audit log.</p>
          <button className={s.accessBtn} onClick={() => navigate('/auth')}>Sign In with Admin Account</button>
        </div>
      </div>
    );
  }

  return (
    <div className={`${s.page} fade-in`}>
      <div className={s.hdr}>
        <h1 className={s.title}>Audit Log</h1>
        <p className={s.sub}>
          All admin override actions are recorded here with timestamp, admin identity, and reason.
          Total entries: <strong>{auditLog.length}</strong>
        </p>
      </div>

      {auditLog.length === 0 ? (
        <div className={s.empty}>
          <div className={s.emptyIcon}>📋</div>
          <div className={s.emptyText}>No override actions recorded yet. When an admin overrides an issue status, it will appear here.</div>
        </div>
      ) : (
        <div className={s.tableWrap}>
          <table className={s.table}>
            <thead>
              <tr>
                <th>#</th>
                <th>Timestamp</th>
                <th>Action</th>
                <th>Issue</th>
                <th>Old Status</th>
                <th>New Status</th>
                <th>Admin</th>
                <th>Note / Reason</th>
              </tr>
            </thead>
            <tbody>
              {auditLog.map((entry, idx) => (
                <tr key={entry.id} className={s.row}>
                  <td><span className={s.mono} style={{ color:'var(--muted)' }}>#{auditLog.length - idx}</span></td>
                  <td><span className={s.timestamp}>{formatTs(entry.timestamp)}</span></td>
                  <td><span className={s.actionBadge}>{entry.type.replace('_',' ')}</span></td>
                  <td className={s.issueTd}>
                    <div className={s.issueTitle}>{entry.issueTitle}</div>
                    <div className={s.issueId}>{String(entry.issueId).slice(0,16)}</div>
                  </td>
                  <td><span className={`${s.statusChip} ${s[entry.oldStatus]}`}>{entry.oldStatus}</span></td>
                  <td><span className={`${s.statusChip} ${s[entry.newStatus]}`}>{entry.newStatus}</span></td>
                  <td>
                    <div className={s.adminName}>{entry.adminName}</div>
                    <div className={s.adminId}>{String(entry.adminId).slice(0,12)}</div>
                  </td>
                  <td><span className={s.noteText}>{entry.note || '—'}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
