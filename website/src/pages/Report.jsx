import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useIssues } from '../context/IssuesContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { calcPriority, getScoreBreakdown, LEVEL_META, getLevel } from '../utils/scoring.js';
import { CATEGORIES, LOCATION_TYPES, SEVERITY_LABELS } from '../data/seed.js';
import { ScoreRing } from '../components/IssueCard.jsx';
import s from './Report.module.css';

const INIT = { title:'', category:'Infrastructure', description:'', severity:3, affectedPeople:100, locationType:'Residential' };

export default function Report() {
  const { user, canReport } = useAuth();
  const { addIssue } = useIssues();
  const { show } = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState(INIT);
  const [done, setDone] = useState(false);

  if (!user || !canReport) {
    return (
      <div className={s.guestWrap}>
        <div className={s.guestBox}>
          <div className={s.guestIcon}>🔒</div>
          <h2 className={s.guestTitle}>Sign in to Report an Issue</h2>
          <p className={s.guestSub}>Only registered community members can submit issue reports. Please sign in or create a free account.</p>
          <button className={s.signInBtn} onClick={() => navigate('/auth')}>Go to Sign In →</button>
        </div>
      </div>
    );
  }

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  // Live preview — use today's date as createdAt for time-pending calculation
  const previewIssue = { ...form, votes: [], createdAt: new Date().toISOString().split('T')[0] };
  const preview = calcPriority(previewIssue);
  const bd      = getScoreBreakdown(previewIssue);
  const meta    = LEVEL_META[getLevel(preview)];

  const handleSubmit = e => {
    e.preventDefault();
    if (!form.title.trim()) return;
    addIssue(form, user);
    setDone(true);
    show('Issue submitted successfully!');
  };

  if (done) {
    return (
      <div className={s.guestWrap}>
        <div className={s.guestBox}>
          <div className={s.successIcon}>✓</div>
          <h2 className={s.guestTitle}>Issue Submitted!</h2>
          <p className={s.guestSub}>Your report has been added to the registry with an initial priority score of <strong>{preview}</strong>. The community can now vote to boost its score.</p>
          <div className={s.doneBtns}>
            <button className={s.signInBtn} onClick={() => navigate('/issues')}>View All Issues</button>
            <button className={s.secondBtn} onClick={() => { setForm(INIT); setDone(false); }}>Report Another</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`${s.page} fade-in`}>
      <div className={s.hdr}>
        <h1 className={s.title}>Report a Community Issue</h1>
        <p className={s.sub}>Fill in structured details — each field feeds directly into the transparent weighted priority formula.</p>
      </div>

      <div className={s.layout}>
        <form onSubmit={handleSubmit} className={s.form}>
          <div className={s.card}>
            <div className={s.cardHd}>Issue Information</div>

            <div className={s.field}>
              <label className={s.label}>Issue Title *</label>
              <input className={s.input} placeholder="Concise, descriptive title…" value={form.title}
                onChange={e => set('title', e.target.value)} required />
            </div>

            <div className={s.row2}>
              <div className={s.field}>
                <label className={s.label}>Category</label>
                <select className={s.select} value={form.category} onChange={e => set('category', e.target.value)}>
                  {CATEGORIES.map(c => <option key={c}>{c}</option>)}
                </select>
              </div>
              <div className={s.field}>
                <label className={s.label}>Location Type</label>
                <select className={s.select} value={form.locationType} onChange={e => set('locationType', e.target.value)}>
                  {LOCATION_TYPES.map(l => <option key={l}>{l}</option>)}
                </select>
              </div>
            </div>

            <div className={s.field}>
              <label className={s.label}>Description</label>
              <textarea className={s.textarea} rows={4}
                placeholder="Describe the issue clearly — what, where, how long, and who is affected…"
                value={form.description} onChange={e => set('description', e.target.value)} />
            </div>

            <div className={s.field}>
              <label className={s.label}>
                Severity Level — <strong>{SEVERITY_LABELS[form.severity]}</strong> ({form.severity}/5)
              </label>
              <input type="range" min="1" max="5" value={form.severity}
                onChange={e => set('severity', parseInt(e.target.value))}
                className={s.range} style={{ accentColor: meta.color }} />
              <div className={s.rangeLabels}>
                <span>Minor</span><span>Moderate</span><span>Serious</span><span>Severe</span><span>Critical</span>
              </div>
            </div>

            <div className={s.field}>
              <label className={s.label}>
                People Affected — <strong>{form.affectedPeople.toLocaleString()}</strong>
              </label>
              <input type="range" min="1" max="5000" value={form.affectedPeople}
                onChange={e => set('affectedPeople', parseInt(e.target.value))}
                className={s.range} style={{ accentColor: 'var(--blue)' }} />
              <div className={s.rangeLabels}><span>1</span><span style={{ marginLeft:'auto' }}>5,000+</span></div>
            </div>

            <div className={s.reporterRow}>
              <span className={s.reporterLabel}>Submitting as:</span>
              <span className={s.reporterName}>{user.name}</span>
              <span className={s.reporterRole}>{user.role.replace('_',' ')}</span>
            </div>

            <button type="submit" className={s.submitBtn} disabled={!form.title.trim()}>
              Submit Issue Report →
            </button>
          </div>
        </form>

        <div className={s.sidebar}>
          <div className={s.card}>
            <div className={s.cardHd}>Live Priority Preview</div>
            <div className={s.previewTop}>
              <ScoreRing score={preview} size={66} />
              <div>
                <div className={s.previewScore}>{preview} / 100</div>
                <span className={s.previewBadge}
                  style={{ color:meta.color, background:meta.bg, borderColor:meta.border }}>
                  {meta.label} Priority
                </span>
              </div>
            </div>

            <div className={s.bdBox}>
              <div className={s.bdHeader}>Score Breakdown (w1+w2+w3+w4 = 1.0)</div>
              {[
                ['w1 × Severity',         `${bd.weights.w1} × norm × 100`,  bd.severityPoints ],
                ['w2 × People Affected',  `${bd.weights.w2} × norm × 100`,  bd.peoplePoints   ],
                ['w3 × Time Pending',     `${bd.weights.w3} × 0d × 100`,    bd.timePoints     ],
                ['w4 × Location',         `${bd.weights.w4} × sens × 100`,  bd.locationPoints ],
                ['Category Bonus',        form.category,                     bd.categoryPoints ],
                ['Base Score',            'sum above (cap 92)',               bd.baseScore,  true],
                ['Vote Score',            'min(0×0.5, 8)',                    0                 ],
                ['Final Score',           'Base + Votes (cap 100)',           preview,       true],
              ].map(([lbl, formula, val, bold]) => (
                <div key={lbl} className={`${s.bdRow} ${bold ? s.bdBold : ''}`}>
                  <span className={s.bdLabel}>{lbl}</span>
                  <span className={s.bdFormula}>{formula}</span>
                  <span className={s.bdVal}>{bold ? val : `+${val}`}</span>
                </div>
              ))}
            </div>
          </div>

          <div className={s.card} style={{ marginTop:'1rem' }}>
            <div className={s.cardHd}>Priority Level Guide</div>
            {[['critical','80–100','Immediate action'],['high','60–79','Urgent within 48h'],['medium','40–59','Scheduled resolution'],['low','0–39','Queued for review']].map(([l,r,d]) => (
              <div key={l} className={s.guideRow}>
                <span className={s.guideBadge} style={{ color:LEVEL_META[l].color, background:LEVEL_META[l].bg, borderColor:LEVEL_META[l].border }}>{LEVEL_META[l].label}</span>
                <span className={s.guideRange}>{r}</span>
                <span className={s.guideDesc}>{d}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
