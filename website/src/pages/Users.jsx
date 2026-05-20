import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import s from './Users.module.css';

const ROLE_LABELS = {
  community_member: 'Community Member',
  admin_authority:  'Admin Authority',
  system_admin:     'System Admin',
};
const ROLE_COLORS = {
  community_member: s.rBlue,
  admin_authority:  s.rGold,
  system_admin:     s.rRed,
};

export default function Users() {
  const { user, users, canManageUsers } = useAuth();
  const navigate = useNavigate();

  if (!user || !canManageUsers) {
    return (
      <div className={s.accessWrap}>
        <div className={s.accessBox}>
          <div className={s.accessIcon}>🔐</div>
          <h2 className={s.accessTitle}>System Admin Access Required</h2>
          <p className={s.accessSub}>Only System Admin accounts can access user management.</p>
          <button className={s.accessBtn} onClick={() => navigate('/auth')}>Sign In as System Admin</button>
        </div>
      </div>
    );
  }

  const byRole = role => users.filter(u => u.role === role);

  return (
    <div className={`${s.page} fade-in`}>
      <div className={s.hdr}>
        <h1 className={s.title}>User Management</h1>
        <p className={s.sub}>System Admin view — all registered users across all roles. Total: <strong>{users.length}</strong></p>
      </div>

      <div className={s.roleSummary}>
        {['system_admin','admin_authority','community_member'].map(role => (
          <div key={role} className={s.roleCard}>
            <div className={`${s.roleDot} ${ROLE_COLORS[role]}`} />
            <div className={s.roleName}>{ROLE_LABELS[role]}</div>
            <div className={s.roleCount}>{byRole(role).length}</div>
          </div>
        ))}
      </div>

      <div className={s.tableWrap}>
        <table className={s.table}>
          <thead>
            <tr>
              <th>Avatar</th>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Joined</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id} className={`${s.row} ${u.id === user.id ? s.self : ''}`}>
                <td>
                  <div className={`${s.avatar} ${ROLE_COLORS[u.role]}`}>{u.avatar}</div>
                </td>
                <td>
                  <div className={s.uname}>{u.name} {u.id === user.id && <span className={s.youTag}>you</span>}</div>
                  <div className={s.uid}>{u.id}</div>
                </td>
                <td><span className={s.email}>{u.email}</span></td>
                <td>
                  <span className={`${s.roleBadge} ${ROLE_COLORS[u.role]}`}>
                    {ROLE_LABELS[u.role]}
                  </span>
                </td>
                <td><span className={s.date}>{u.joined}</span></td>
                <td><span className={s.desc}>{u.description}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className={s.noteBox}>
        <strong>Note:</strong> Role assignment and user creation in production would be managed through a secure backend API. This view is read-only for demonstration purposes.
      </div>
    </div>
  );
}
