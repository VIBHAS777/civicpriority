import { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { useIssues } from '../../context/IssuesContext.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import s from './Navbar.module.css';

const ROLE_LINKS = {
  community_member: [
    { to:'/',         label:'Dashboard' },
    { to:'/issues',   label:'Issues'    },
    { to:'/report',   label:'Report'    },
    { to:'/ranking',  label:'Ranking'   },
  ],
  admin_authority: [
    { to:'/',          label:'Dashboard' },
    { to:'/issues',    label:'Issues'    },
    { to:'/ranking',   label:'Ranking'   },
    { to:'/optimizer', label:'Optimizer' },
    { to:'/audit',     label:'Audit Log' },
  ],
  system_admin: [
    { to:'/',          label:'Dashboard' },
    { to:'/issues',    label:'Issues'    },
    { to:'/ranking',   label:'Ranking'   },
    { to:'/optimizer', label:'Optimizer' },
    { to:'/audit',     label:'Audit Log' },
    { to:'/users',     label:'Users'     },
  ],
};

const GUEST_LINKS = [
  { to:'/',        label:'Dashboard' },
  { to:'/issues',  label:'Issues'    },
  { to:'/ranking', label:'Ranking'   },
];

const ROLE_LABEL = {
  community_member: 'Member',
  admin_authority:  'Admin',
  system_admin:     'SysAdmin',
};
const ROLE_COLOR = {
  community_member: s.roleBlue,
  admin_authority:  s.roleGold,
  system_admin:     s.roleRed,
};

export default function Navbar() {
  const { user, logout } = useAuth();
  const { issues } = useIssues();
  const { show } = useToast();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const openCount = issues.filter(i => i.status === 'open').length;
  const links = user ? (ROLE_LINKS[user.role] ?? GUEST_LINKS) : GUEST_LINKS;

  const handleLogout = () => { logout(); show('Signed out.'); navigate('/auth'); setOpen(false); };

  return (
    <nav className={s.nav}>
      <div className={s.brand}>Civic<span>Priority</span></div>

      <div className={s.links}>
        {links.map(({ to, label }) => (
          <NavLink key={to} to={to} end={to==='/'} className={({ isActive }) => `${s.link} ${isActive ? s.active : ''}`}>
            {label}
          </NavLink>
        ))}
      </div>

      <div className={s.right}>
        <div className={s.openCount}><span className={s.dot}/>{openCount} open</div>

        {user ? (
          <div className={s.userWrap}>
            <button className={s.avatarBtn} onClick={() => setOpen(o=>!o)}>
              <span className={s.avatar}>{user.avatar}</span>
              <span className={s.uname}>{user.name.split(' ')[0]}</span>
              <span className={`${s.roleTag} ${ROLE_COLOR[user.role]}`}>{ROLE_LABEL[user.role]}</span>
              <span className={s.chevron}>{open?'▲':'▼'}</span>
            </button>
            {open && (
              <div className={s.dropdown}>
                <div className={s.dropUser}>
                  <div className={s.dropName}>{user.name}</div>
                  <div className={s.dropEmail}>{user.email}</div>
                  <div className={s.dropDesc}>{user.description}</div>
                </div>
                <div className={s.dropDivider}/>
                <button className={s.dropItem} onClick={() => { navigate('/profile'); setOpen(false); }}>👤 My Profile</button>
                <button className={s.dropItem} onClick={handleLogout}>🚪 Sign Out</button>
              </div>
            )}
          </div>
        ) : (
          <button className={s.loginBtn} onClick={() => navigate('/auth')}>Sign In</button>
        )}
      </div>
    </nav>
  );
}
