import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useIssues } from '../context/IssuesContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { filterIssues, sortIssues } from '../utils/scoring.js';
import { CATEGORIES } from '../data/seed.js';
import IssueCard from '../components/IssueCard.jsx';
import s from './Issues.module.css';

export default function Issues() {
  const { issues } = useIssues();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [search,   setSearch]   = useState('');
  const [status,   setStatus]   = useState('all');
  const [level,    setLevel]    = useState('all');
  const [category, setCategory] = useState('all');
  const [sortBy,   setSortBy]   = useState('score');

  const filtered = useMemo(() =>
    sortIssues(filterIssues(issues, { status, level, category, search }), sortBy),
    [issues, search, status, level, category, sortBy]
  );

  const clear = () => { setSearch(''); setStatus('all'); setLevel('all'); setCategory('all'); setSortBy('score'); };
  const hasF  = search || status !== 'all' || level !== 'all' || category !== 'all';

  return (
    <div className={`${s.page} fade-in`}>
      <div className={s.hdr}>
        <div>
          <h1 className={s.title}>Issue Registry</h1>
          <p className={s.sub}>{filtered.length} of {issues.length} issues · sorted by priority score</p>
        </div>
        {!user && (
          <button className={s.nudge} onClick={() => navigate('/auth')}>
            🔒 Sign in to vote & report
          </button>
        )}
      </div>

      <div className={s.filterBar}>
        <input className={s.search} placeholder="Search title, description, reporter…"
          value={search} onChange={e => setSearch(e.target.value)} />
        <select className={s.sel} value={status}   onChange={e => setStatus(e.target.value)}>
          <option value="all">All Status</option>
          <option value="open">Open</option>
          <option value="inprogress">In Progress</option>
          <option value="resolved">Resolved</option>
        </select>
        <select className={s.sel} value={level}    onChange={e => setLevel(e.target.value)}>
          <option value="all">All Levels</option>
          <option value="critical">Critical</option>
          <option value="high">High</option>
          <option value="medium">Medium</option>
          <option value="low">Low</option>
        </select>
        <select className={s.sel} value={category} onChange={e => setCategory(e.target.value)}>
          <option value="all">All Categories</option>
          {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
        <select className={s.sel} value={sortBy}   onChange={e => setSortBy(e.target.value)}>
          <option value="score">↓ Priority Score</option>
          <option value="votes">↓ Most Voted</option>
          <option value="newest">↓ Newest</option>
          <option value="oldest">↓ Oldest</option>
          <option value="affected">↓ Most Affected</option>
        </select>
        {hasF && <button className={s.clearBtn} onClick={clear}>✕ Clear</button>}
      </div>

      {filtered.length === 0 ? (
        <div className={s.empty}>
          <div className={s.emptyIcon}>🔍</div>
          <div className={s.emptyText}>No issues match your current filters.</div>
        </div>
      ) : (
        <div className={s.list}>
          {filtered.map(i => <IssueCard key={i.id} issue={i} />)}
        </div>
      )}
    </div>
  );
}
