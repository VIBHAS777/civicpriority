import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useIssues } from '../context/IssuesContext.jsx';
import { calcPriority } from '../utils/scoring.js';
import IssueCard from '../components/IssueCard.jsx';
import s from './Profile.module.css';

const ROLE_DISPLAY = {
  community_member: { label:'Community Member', color:s.rBlue, perms:['Report issues','Cast one vote per issue','Track issue status','View priority scores'] },
  admin_authority:  { label:'Admin Authority',  color:s.rGold, perms:['All member permissions','Input resources & run optimizer','Override issue status (with audit)','View audit log'] },
  system_admin:     { label:'System Admin',     color:s.rRed,  perms:['All admin permissions','View and manage all users','Monitor system integrity','Full audit log access'] },
};

export default function Profile() {
  const { user, logout } = useAuth();
  const { issues, auditLog } = useIssues();
  const navigate = useNavigate();

  if (!user) { navigate('/auth'); return null; }

  const myIssues = useMemo(() => issues.filter(i => i.reporterId === user.id), [issues, user]);
  const myVotes  = useMemo(() => issues.filter(i => (i.votes ?? []).includes(user.id)), [issues, user]);
  const myOverrides = useMemo(() => auditLog.filter(e => e.adminId === user.id), [auditLog, user]);
  const rd = ROLE_DISPLAY[user.role] ?? ROLE_DISPLAY.community_member;

  return (
    <div className={`${s.page} fade-in`}>
      {/* Profile card */}
      <div className={s.profileCard}>
        <div className={`${s.avatarLg} ${rd.color}`}>{user.avatar}</div>
        <div className={s.userInfo}>
          <div className={s.userName}>{user.name}</div>
          <div className={s.userEmail}>{user.email}</div>
          <div className={s.userMeta}>
            <span className={`${s.rolePill} ${rd.color}`}>{rd.label}</span>
            <span className={s.joinedTxt}>Member since {user.joined}</span>
          </div>
          <div className={s.permsList}>
            {rd.perms.map(p => <span key={p} className={s.perm}>✓ {p}</span>)}
          </div>
        </div>
        <button className={s.logoutBtn} onClick={() => { logout(); navigate('/auth'); }}>Sign Out</button>
      </div>

      {/* Stats */}
      <div className={s.statsRow}>
        <div className={s.stat}>
          <div className={s.statV} style={{ color:'var(--accent)' }}>{myIssues.length}</div>
          <div className={s.statL}>Issues Reported</div>
        </div>
        <div className={s.stat}>
          <div className={s.statV} style={{ color:'var(--blue)' }}>{myVotes.length}</div>
          <div className={s.statL}>Votes Cast</div>
        </div>
        <div className={s.stat}>
          <div className={s.statV} style={{ color:'var(--green)' }}>{myIssues.filter(i=>i.status==='resolved').length}</div>
          <div className={s.statL}>My Issues Resolved</div>
        </div>
        {(user.role === 'admin_authority' || user.role === 'system_admin') && (
          <div className={s.stat}>
            <div className={s.statV} style={{ color:'var(--warn)' }}>{myOverrides.length}</div>
            <div className={s.statL}>Overrides Performed</div>
          </div>
        )}
      </div>

      <div className={s.grid2}>
        <div>
          <div className={s.secHead}>Issues I've Reported ({myIssues.length})</div>
          {myIssues.length === 0
            ? <div className={s.empty}>You haven't reported any issues yet.</div>
            : <div className={s.cardList}>{myIssues.map(i => <IssueCard key={i.id} issue={i} compact />)}</div>
          }
        </div>
        <div>
          <div className={s.secHead}>Issues I've Voted On ({myVotes.length})</div>
          {myVotes.length === 0
            ? <div className={s.empty}>You haven't voted on any issues yet. Browse the Issue Registry to cast votes!</div>
            : <div className={s.cardList}>{myVotes.map(i => <IssueCard key={i.id} issue={i} compact />)}</div>
          }
        </div>
      </div>

      {/* Admin override history */}
      {myOverrides.length > 0 && (
        <div style={{ marginTop:'1.5rem' }}>
          <div className={s.secHead}>My Override History ({myOverrides.length})</div>
          <div className={s.overrideList}>
            {myOverrides.map(e => (
              <div key={e.id} className={s.overrideRow}>
                <span className={s.overrideIcon}>⚡</span>
                <div className={s.overrideInfo}>
                  <div className={s.overrideTitle}>{e.issueTitle}</div>
                  <div className={s.overrideMeta}>{e.oldStatus} → {e.newStatus} · {e.note || 'No note'} · {new Date(e.timestamp).toLocaleDateString('en-IN')}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
