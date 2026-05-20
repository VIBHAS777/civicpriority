import { useMemo } from 'react';
import { useIssues } from '../context/IssuesContext.jsx';
import { calcPriority, getScoreBreakdown, formatDate, tieBreakSort } from '../utils/scoring.js';
import { ScoreRing, PriorityBadge, StatusBadge } from '../components/IssueCard.jsx';
import { SEVERITY_LABELS } from '../data/seed.js';
import s from './Ranking.module.css';

export default function Ranking() {
  const { issues } = useIssues();

  const ranked = useMemo(() =>
    issues
      .map(i => ({ ...i, _score: calcPriority(i), _bd: getScoreBreakdown(i) }))
      .sort(tieBreakSort),
    [issues]
  );

  return (
    <div className={`${s.page} fade-in`}>
      <div className={s.hdr}>
        <h1 className={s.title}>Priority Ranking</h1>
        <p className={s.sub}>
          All {issues.length} issues ranked by FinalScore with deterministic tie-breaking:
          Severity → PeopleAffected → TimePending (oldest first)
        </p>
      </div>

      <div className={s.tableWrap}>
        <table className={s.table}>
          <thead>
            <tr>
              <th>Rank</th>
              <th>Score</th>
              <th>Issue</th>
              <th>Level</th>
              <th>Base</th>
              <th>Vote</th>
              <th>Severity</th>
              <th>Affected</th>
              <th>Days Pending</th>
              <th>Category</th>
              <th>Status</th>
              <th>Overridden</th>
            </tr>
          </thead>
          <tbody>
            {ranked.map((issue, idx) => (
              <tr key={issue.id} className={`${s.row} ${issue.overriddenBy ? s.overridden : ''}`}>
                <td>
                  <span className={s.rank} style={{ color: idx < 3 ? 'var(--accent)' : 'var(--muted)' }}>
                    #{idx + 1}
                  </span>
                </td>
                <td><ScoreRing score={issue._score} size={44} /></td>
                <td className={s.titleCell}>
                  <div className={s.issueTitle}>{issue.title}</div>
                  <div className={s.reporter}>by {issue.reporter}</div>
                </td>
                <td><PriorityBadge score={issue._score} /></td>
                <td><span className={s.mono}>{issue._bd.baseScore}</span></td>
                <td><span className={s.mono} style={{ color:'var(--blue)' }}>+{issue._bd.voteScore}</span></td>
                <td>
                  <span className={s.mono}>{issue.severity}/5</span>
                  <div className={s.sevLabel}>{SEVERITY_LABELS[issue.severity]}</div>
                </td>
                <td><span className={s.mono}>{issue.affectedPeople?.toLocaleString()}</span></td>
                <td><span className={s.mono}>{issue._bd.days}d</span></td>
                <td><span className={s.cat}>{issue.category}</span></td>
                <td><StatusBadge status={issue.status} /></td>
                <td>
                  {issue.overriddenBy
                    ? <span className={s.overridePill}>⚡ Yes</span>
                    : <span className={s.noPill}>—</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
