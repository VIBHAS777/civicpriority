import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useIssues } from '../context/IssuesContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { runOptimizer, calcPriority, WEIGHTS } from '../utils/scoring.js';
import { RESOURCE_CONFIG } from '../data/seed.js';
import { ScoreRing, PriorityBadge, StatusBadge } from '../components/IssueCard.jsx';
import s from './Optimizer.module.css';

const INIT_RES = { workers: 20, vehicles: 8, hours: 120 };

export default function Optimizer() {
  const { issues } = useIssues();
  const { user, canRunOptimizer } = useAuth();
  const navigate = useNavigate();
  const [res, setRes]       = useState(INIT_RES);
  const [result, setResult] = useState(null);

  if (!user || !canRunOptimizer) {
    return (
      <div className={s.accessWrap}>
        <div className={s.accessBox}>
          <div className={s.accessIcon}>🔐</div>
          <h2 className={s.accessTitle}>Admin Access Required</h2>
          <p className={s.accessSub}>Only Admin Authority and System Admin accounts can access the Resource Optimizer.</p>
          <button className={s.accessBtn} onClick={() => navigate('/auth')}>Sign In with Admin Account</button>
        </div>
      </div>
    );
  }

  const set = (k, v) => setRes(r => ({ ...r, [k]: Math.max(0, parseInt(v) || 0) }));
  const run = () => setResult(runOptimizer(issues, res));

  const openCount = issues.filter(i => i.status !== 'resolved').length;

  return (
    <div className={`${s.page} fade-in`}>
      <div className={s.hdr}>
        <h1 className={s.title}>Resource Optimizer</h1>
        <p className={s.sub}>
          Configure available resources → run greedy algorithm → get priority-ordered allocation plan.
          Constraints: workers · vehicles · working hours (SRS §5.2).
        </p>
      </div>

      <div className={s.layout}>
        {/* Left: config */}
        <div>
          <div className={s.card}>
            <div className={s.cardHd}>Available Resources ({openCount} active issues)</div>
            {RESOURCE_CONFIG.map(r => (
              <div key={r.id} className={s.resItem}>
                <div className={s.resTop}>
                  <span className={s.resIcon}>{r.icon}</span>
                  <span className={s.resName}>{r.name}</span>
                  <span className={s.resVal}>{res[r.id]} / {r.total} {r.unit}</span>
                </div>
                <input type="range" min="0" max={r.total} value={res[r.id]}
                  onChange={e => set(r.id, e.target.value)}
                  className={s.range} style={{ accentColor:'var(--blue)' }} />
                <div className={s.resBar}>
                  <div className={s.resBarFill} style={{ width:`${(res[r.id]/r.total)*100}%` }} />
                </div>
              </div>
            ))}
            <button className={s.runBtn} onClick={run}>⚡ Run Optimizer</button>
          </div>

          <div className={s.card} style={{ marginTop:'1rem' }}>
            <div className={s.cardHd}>Algorithm Details</div>
            <div className={s.algo}>
              <div className={s.algoTitle}>Priority-Based Greedy Algorithm (SRS §5.2)</div>
              <ol className={s.algoSteps}>
                <li>Filter open + in-progress issues only</li>
                <li>Score each issue using FinalScore formula</li>
                <li>Sort by FinalScore DESC with tie-breaking:<br/>
                  <span className={s.tieBreak}>Severity → PeopleAffected → TimePending (oldest first)</span>
                </li>
                <li>For each issue (highest priority first):
                  <ul>
                    <li>Check workers, vehicles, hours constraints</li>
                    <li>If all fit → allocate, subtract from pool</li>
                    <li>If any fail → defer with specific reason</li>
                  </ul>
                </li>
                <li>Output: ordered scheduled plan + deferred list</li>
              </ol>
              <div className={s.algoNote}>
                Replaces FIFO — a critical issue submitted today beats a minor issue from last month.
              </div>
            </div>
          </div>
        </div>

        {/* Right: results */}
        <div>
          {!result ? (
            <div className={s.empty}>
              <div className={s.emptyIcon}>⚡</div>
              <div className={s.emptyText}>Set your resource levels and click <strong>Run Optimizer</strong> to generate the allocation plan.</div>
            </div>
          ) : (
            <>
              <div className={s.summGrid}>
                <div className={s.summCard}><div className={s.summVal} style={{color:'var(--green)'}}>{result.scheduled.length}</div><div className={s.summLabel}>Scheduled</div></div>
                <div className={s.summCard}><div className={s.summVal} style={{color:'var(--accent)'}}>{result.deferred.length}</div><div className={s.summLabel}>Deferred</div></div>
                <div className={s.summCard}><div className={s.summVal}>{result.remaining.workers}</div><div className={s.summLabel}>Workers Left</div></div>
                <div className={s.summCard}><div className={s.summVal}>{result.remaining.hours}h</div><div className={s.summLabel}>Hours Left</div></div>
              </div>

              {result.scheduled.length > 0 && (
                <div className={s.sectionLbl}>✓ Scheduled — {result.scheduled.length} issues</div>
              )}
              {result.scheduled.map(({ issue, need, rank }) => (
                <div key={issue.id} className={`${s.allocCard} ${s.scheduled} fade-in`}>
                  <div className={s.allocTop}>
                    <span className={s.allocRank}>#{rank}</span>
                    <ScoreRing score={issue._score ?? calcPriority(issue)} size={42} />
                    <div className={s.allocInfo}>
                      <div className={s.allocTitle}>{issue.title}</div>
                      <div className={s.allocBadges}>
                        <PriorityBadge score={issue._score ?? calcPriority(issue)} />
                        <StatusBadge status={issue.status} />
                      </div>
                    </div>
                    <span className={s.scheduledTag}>✓ Scheduled</span>
                  </div>
                  <div className={s.allocRes}>
                    <span className={s.allocPill}>👷 {need.workers} workers</span>
                    <span className={s.allocPill}>🚛 {need.vehicles} vehicles</span>
                    <span className={s.allocPill}>⏱️ {need.hours}h</span>
                  </div>
                </div>
              ))}

              {result.deferred.length > 0 && (
                <div className={s.sectionLbl} style={{ marginTop:'1rem' }}>✗ Deferred — {result.deferred.length} issues</div>
              )}
              {result.deferred.map(({ issue, reason }) => (
                <div key={issue.id} className={`${s.allocCard} ${s.deferred} fade-in`}>
                  <div className={s.allocTop}>
                    <ScoreRing score={issue._score ?? calcPriority(issue)} size={42} />
                    <div className={s.allocInfo}>
                      <div className={s.allocTitle}>{issue.title}</div>
                      <div className={s.deferReason}>⚠ {reason}</div>
                    </div>
                    <span className={s.deferredTag}>✗ Deferred</span>
                  </div>
                </div>
              ))}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
