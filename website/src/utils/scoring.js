import { WEIGHTS, LOCATION_SENSITIVITY, CATEGORY_BONUS, RESOURCE_NEEDS } from '../data/seed.js';

// ─── Time Pending Score ───────────────────────────────────────────────────────
// Progressive increment with upper cap (as per SRS Sprint 1, Scrum 01.04)
// Days pending → score contribution (capped at 1.0 to prevent dominance)
export function getTimePendingScore(createdAt) {
  if (!createdAt) return 0;
  const days = Math.floor((Date.now() - new Date(createdAt).getTime()) / 86400000);
  // Progressive: sqrt(days)/sqrt(365) capped at 1.0
  return Math.min(Math.sqrt(days) / Math.sqrt(365), 1.0);
}

// ─── Base Score (normalized weights, SRS §5.1) ────────────────────────────────
// BaseScore = w1×Severity_norm + w2×People_norm + w3×TimePending_norm + w4×LocationSensitivity
// Each component is normalized to [0,1] before weighting → result in [0,1]
// Multiply by 100 → [0,100] scale. Cap at 92 to leave room for VoteScore.
export function calcBaseScore(issue) {
  const sev      = (issue.severity - 1) / 4;                           // 1–5 → 0–1
  const people   = Math.min(issue.affectedPeople / 5000, 1.0);         // 0–5000 → 0–1
  const time     = getTimePendingScore(issue.createdAt);               // 0–1 progressive
  const loc      = LOCATION_SENSITIVITY[issue.locationType] ?? 0.3;   // 0–1

  const base = (WEIGHTS.w1 * sev + WEIGHTS.w2 * people + WEIGHTS.w3 * time + WEIGHTS.w4 * loc) * 100;
  const catBonus = CATEGORY_BONUS[issue.category] ?? 2;
  return Math.min(Math.round(base + catBonus), 92); // cap base at 92
}

// ─── Vote Score (capped to prevent dominance, SRS §5.1) ──────────────────────
// VoteScore = min(votes × 0.5, 8) → max 8 points, needs 16 votes to max
export function calcVoteScore(issue) {
  return Math.min((issue.votes?.length ?? 0) * 0.5, 8);
}

// ─── Final Score ─────────────────────────────────────────────────────────────
// FinalScore = BaseScore + VoteScore  (capped at 100)
export function calcPriority(issue) {
  return Math.min(Math.round(calcBaseScore(issue) + calcVoteScore(issue)), 100);
}

// ─── Full breakdown for transparency display ──────────────────────────────────
export function getScoreBreakdown(issue) {
  const sev    = (issue.severity - 1) / 4;
  const people = Math.min(issue.affectedPeople / 5000, 1.0);
  const time   = getTimePendingScore(issue.createdAt);
  const loc    = LOCATION_SENSITIVITY[issue.locationType] ?? 0.3;
  const days   = issue.createdAt ? Math.floor((Date.now() - new Date(issue.createdAt).getTime()) / 86400000) : 0;

  const severityPoints  = Math.round(WEIGHTS.w1 * sev * 100);
  const peoplePoints    = Math.round(WEIGHTS.w2 * people * 100);
  const timePoints      = Math.round(WEIGHTS.w3 * time * 100);
  const locationPoints  = Math.round(WEIGHTS.w4 * loc * 100);
  const categoryPoints  = CATEGORY_BONUS[issue.category] ?? 2;
  const baseScore       = calcBaseScore(issue);
  const voteScore       = Math.round(calcVoteScore(issue));
  const finalScore      = calcPriority(issue);

  return {
    severityPoints, peoplePoints, timePoints, locationPoints,
    categoryPoints, baseScore, voteScore, finalScore,
    days, voteCount: issue.votes?.length ?? 0,
    weights: WEIGHTS,
  };
}

// ─── Priority Level ───────────────────────────────────────────────────────────
export function getLevel(score) {
  if (score >= 80) return 'critical';
  if (score >= 60) return 'high';
  if (score >= 40) return 'medium';
  return 'low';
}

export const LEVEL_META = {
  critical: { label:'Critical', color:'#d4402a', bg:'#fde8e5', border:'#d4402a' },
  high:     { label:'High',     color:'#e07b20', bg:'#fef3e5', border:'#e07b20' },
  medium:   { label:'Medium',   color:'#c8972a', bg:'#fff9e5', border:'#c8972a' },
  low:      { label:'Low',      color:'#2a8c4a', bg:'#e8f5ec', border:'#2a8c4a' },
};

// ─── Tie-breaking sort (SRS Sprint 3, Scrum 03.05) ────────────────────────────
// Primary: FinalScore DESC
// Secondary: Severity DESC
// Tertiary: PeopleAffected DESC
// Quaternary: TimePending (createdAt ASC — older issues first)
export function tieBreakSort(a, b) {
  const scoreDiff = (b._score ?? calcPriority(b)) - (a._score ?? calcPriority(a));
  if (scoreDiff !== 0) return scoreDiff;
  if (b.severity !== a.severity) return b.severity - a.severity;
  if (b.affectedPeople !== a.affectedPeople) return b.affectedPeople - a.affectedPeople;
  return new Date(a.createdAt) - new Date(b.createdAt); // older = more urgent
}

// ─── Greedy Resource Optimizer (SRS §5.2) ────────────────────────────────────
// Constraints: workers ≤ available, vehicles ≤ available, hours ≤ available
// Sorts by FinalScore DESC with tie-breaking, then allocates greedily
export function runOptimizer(issues, availableResources) {
  const pool = issues
    .filter(i => i.status !== 'resolved')
    .map(i => ({ ...i, _score: calcPriority(i) }))
    .sort(tieBreakSort);

  const rem = { workers: availableResources.workers, vehicles: availableResources.vehicles, hours: availableResources.hours };
  const scheduled = [];
  const deferred  = [];

  for (const issue of pool) {
    const need = RESOURCE_NEEDS[issue.category] ?? { workers:2, vehicles:1, hours:4 };
    const fits =
      need.workers  <= rem.workers  &&
      need.vehicles <= rem.vehicles &&
      need.hours    <= rem.hours;

    if (fits) {
      rem.workers  -= need.workers;
      rem.vehicles -= need.vehicles;
      rem.hours    -= need.hours;
      scheduled.push({ issue, need, rank: scheduled.length + 1 });
    } else {
      const reasons = [];
      if (need.workers  > rem.workers)  reasons.push(`${need.workers} workers needed, ${rem.workers} left`);
      if (need.vehicles > rem.vehicles) reasons.push(`${need.vehicles} vehicles needed, ${rem.vehicles} left`);
      if (need.hours    > rem.hours)    reasons.push(`${need.hours}h needed, ${rem.hours}h left`);
      deferred.push({ issue, need, reason: reasons.join(' · ') });
    }
  }

  return { scheduled, deferred, remaining: rem };
}

// ─── Sort & Filter helpers ────────────────────────────────────────────────────
export function sortIssues(list, by) {
  const s = list.map(i => ({ ...i, _score: calcPriority(i) }));
  if (by === 'score')    return s.sort(tieBreakSort);
  if (by === 'votes')    return s.sort((a,b) => (b.votes?.length??0) - (a.votes?.length??0));
  if (by === 'newest')   return s.sort((a,b) => new Date(b.createdAt) - new Date(a.createdAt));
  if (by === 'oldest')   return s.sort((a,b) => new Date(a.createdAt) - new Date(b.createdAt));
  if (by === 'affected') return s.sort((a,b) => b.affectedPeople - a.affectedPeople);
  return s;
}

export function filterIssues(list, { status, level, category, search }) {
  return list.filter(i => {
    if (status   && status   !== 'all' && i.status   !== status)   return false;
    if (category && category !== 'all' && i.category !== category) return false;
    if (level    && level    !== 'all' && getLevel(calcPriority(i)) !== level) return false;
    if (search) {
      const q = search.toLowerCase();
      if (!i.title.toLowerCase().includes(q) &&
          !i.description.toLowerCase().includes(q) &&
          !i.category.toLowerCase().includes(q) &&
          !i.reporter.toLowerCase().includes(q)) return false;
    }
    return true;
  });
}

export function formatDate(d) {
  if (!d) return '';
  return new Date(d).toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' });
}

export function genId() {
  return `id_${Date.now()}_${Math.random().toString(36).slice(2,7)}`;
}

// Re-export WEIGHTS so pages can import it from one place
export { WEIGHTS } from '../data/seed.js';
