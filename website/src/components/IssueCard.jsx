import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useIssues } from '../context/IssuesContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { calcPriority, getLevel, LEVEL_META, formatDate, getScoreBreakdown } from '../utils/scoring.js';
import { SEVERITY_LABELS } from '../data/seed.js';
import s from './IssueCard.module.css';

// ── Shared badge components ───────────────────────────────────────────────────
export function ScoreRing({ score, size = 54 }) {
  const m = LEVEL_META[getLevel(score)];
  return (
    <div className={s.ring} style={{ width:size, height:size, borderColor:m.border, color:m.color, background:m.bg, fontSize: size < 46 ? '0.72rem' : '1rem' }}>
      {score}
    </div>
  );
}

export function PriorityBadge({ score }) {
  const m = LEVEL_META[getLevel(score)];
  return <span className={s.badge} style={{ color:m.color, background:m.bg, borderColor:m.border }}>{m.label}</span>;
}

export function StatusBadge({ status }) {
  const MAP = {
    open:       [s.sOpen,       'Open'],
    inprogress: [s.sInprogress, 'In Progress'],
    resolved:   [s.sResolved,   'Resolved'],
  };
  const [cls, label] = MAP[status] ?? [s.sOpen, status];
  return <span className={`${s.statusBadge} ${cls}`}>{label}</span>;
}

// ── Main IssueCard ────────────────────────────────────────────────────────────
export default function IssueCard({ issue, compact = false }) {
  const { user, canOverride } = useAuth();
  const { toggleVote, overrideStatus } = useIssues();
  const { show } = useToast();
  const navigate  = useNavigate();
  const [expanded,   setExpanded]   = useState(false);
  const [showBD,     setShowBD]     = useState(false);
  const [overriding, setOverriding] = useState(false);
  const [noteInput,  setNoteInput]  = useState('');
  const [selStatus,  setSelStatus]  = useState(issue.status);

  const score    = calcPriority(issue);
  const hasVoted = user ? (issue.votes ?? []).includes(user.id) : false;
  const bd       = getScoreBreakdown(issue);

  const handleVote = () => {
    if (!user) { show('Please sign in to vote.', 'error'); navigate('/auth'); return; }
    toggleVote(issue.id, user.id);
    show(hasVoted ? 'Vote removed.' : '▲ Vote counted! Priority score updated.');
  };

  const handleOverride = () => {
    if (!noteInput.trim()) { show('Please enter an override note.', 'error'); return; }
    overrideStatus(issue, selStatus, user, noteInput.trim());
    show(`Status overridden to "${selStatus}". Audit log updated.`);
    setOverriding(false);
    setNoteInput('');
  };

  const desc = issue.description ?? '';
  const shortDesc = desc.length > 110 && !expanded ? desc.slice(0,110) + '…' : desc;

  return (
    <div className={`${s.card} ${compact ? s.compact : ''} fade-in`}>
      <div className={s.top}>
        <ScoreRing score={score} size={compact ? 42 : 54} />
        <div className={s.meta}>
          <div className={s.title}>{issue.title}</div>
          {issue.overriddenBy && (
            <div className={s.overrideBanner}>⚡ Manually overridden by admin on {formatDate(issue.overriddenAt?.split('T')[0])}</div>
          )}
          {!compact && (
            <div className={s.desc}>
              {shortDesc}
              {desc.length > 110 && (
                <button className={s.expandBtn} onClick={() => setExpanded(e=>!e)}>
                  {expanded ? ' less' : ' more'}
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      <div className={s.pills}>
        {[
          `📁 ${issue.category}`,
          `📍 ${issue.locationType}`,
          `👥 ${issue.affectedPeople?.toLocaleString()} affected`,
          `⚠️ Severity ${issue.severity}/5 — ${SEVERITY_LABELS[issue.severity]}`,
          ...(!compact ? [`📅 ${formatDate(issue.createdAt)}`, `👤 ${issue.reporter}`, `🕐 ${bd.days}d pending`] : []),
          ...(issue.comments?.length ? [`💬 ${issue.comments.length}`] : []),
        ].map(t => <span key={t} className={s.pill}>{t}</span>)}
      </div>

      {/* Score breakdown toggle */}
      {!compact && (
        <button className={s.bdToggle} onClick={() => setShowBD(b=>!b)}>
          {showBD ? '▲ Hide' : '▼ Show'} score breakdown
        </button>
      )}

      {showBD && !compact && (
        <div className={s.bdBox}>
          <div className={s.bdTitle}>Score Breakdown (w1+w2+w3+w4 = 1.0)</div>
          {[
            ['w1 × Severity',        `${bd.weights.w1} × ${((issue.severity-1)/4).toFixed(2)}×100`, bd.severityPoints ],
            ['w2 × People Affected', `${bd.weights.w2} × ${Math.min(issue.affectedPeople/5000,1).toFixed(2)}×100`, bd.peoplePoints ],
            ['w3 × Time Pending',    `${bd.weights.w3} × [${bd.days}d] ×100`,  bd.timePoints    ],
            ['w4 × Location',        `${bd.weights.w4} × loc×100`,              bd.locationPoints],
            ['Category Bonus',       issue.category,                            bd.categoryPoints],
            ['Base Score',           '(sum above, cap 92)',                      bd.baseScore, true],
            ['Vote Score',           `min(${bd.voteCount}×0.5, 8)`,            bd.voteScore     ],
            ['Final Score',          'BaseScore + VoteScore, cap 100',           bd.finalScore, true],
          ].map(([lbl, formula, val, bold]) => (
            <div key={lbl} className={`${s.bdRow} ${bold ? s.bdBold : ''}`}>
              <span className={s.bdLabel}>{lbl}</span>
              <span className={s.bdFormula}>{formula}</span>
              <span className={s.bdVal}>{bold ? val : `+${val}`}</span>
            </div>
          ))}
        </div>
      )}

      <div className={s.footer}>
        <div className={s.badges}>
          <PriorityBadge score={score} />
          <StatusBadge status={issue.status} />
        </div>
        <div className={s.actions}>
          {/* Vote button — any logged-in user */}
          <button
            className={`${s.voteBtn} ${hasVoted ? s.voted : ''}`}
            onClick={handleVote}
            title={!user ? 'Sign in to vote' : hasVoted ? 'Remove vote' : 'Vote to prioritize'}
          >
            <span>▲</span>
            <span className={s.voteCount}>{issue.votes?.length ?? 0}</span>
            <span className={s.voteLabel}>{!user ? 'sign in' : hasVoted ? 'voted' : 'vote'}</span>
          </button>

          {/* Admin override button */}
          {canOverride && (
            <button className={s.overrideBtn} onClick={() => setOverriding(o=>!o)}>
              {overriding ? '✕ Cancel' : '⚡ Override'}
            </button>
          )}
        </div>
      </div>

      {/* Override panel */}
      {overriding && canOverride && (
        <div className={s.overridePanel}>
          <div className={s.overrideTitle}>Admin Override — Audit log will be updated</div>
          <div className={s.overrideRow}>
            <select className={s.overrideSel} value={selStatus} onChange={e=>setSelStatus(e.target.value)}>
              <option value="open">Open</option>
              <option value="inprogress">In Progress</option>
              <option value="resolved">Resolved</option>
            </select>
            <input
              className={s.overrideNote}
              placeholder="Override reason / note (required)…"
              value={noteInput}
              onChange={e=>setNoteInput(e.target.value)}
            />
            <button className={s.overrideConfirm} onClick={handleOverride}>Confirm</button>
          </div>
        </div>
      )}
    </div>
  );
}
