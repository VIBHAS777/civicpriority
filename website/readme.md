# CivicPriority

A community issue prioritization and resource allocation platform built with React. Issues are scored using a transparent weighted formula, ranked deterministically, and resources are allocated using a greedy algorithm — replacing FIFO-based resolution with impact-first scheduling.



---

## Table of Contents

- [Getting Started](#getting-started)
- [Demo Accounts](#demo-accounts)
- [How the Scoring Works](#how-the-scoring-works)
- [How the Optimizer Works](#how-the-optimizer-works)
- [Project Structure](#project-structure)
- [Data Layer — How localStorage is Used](#data-layer--how-localstorage-is-used)
- [Seed Data — Customising the Starting State](#seed-data--customising-the-starting-state)
- [Extending the Project](#extending-the-project)
- [Replacing localStorage with a Real Database](#replacing-localstorage-with-a-real-database)
- [Stack](#stack)

---

## Getting Started

**Prerequisites:** Node.js ≥ 18

```bash
npm install
npm run dev
```

App opens at `http://localhost:5173`

```bash
npm run build        # production build → dist/ folder
```

**Deploy to Netlify (free, 2 minutes):**
```bash
npm run build
# drag and drop the dist/ folder at netlify.com
```

---

## Demo Accounts

The app ships with 6 pre-loaded accounts across all three roles:

| Role | Email | Password |
|---|---|---|
| System Admin | sysadmin@civic.gov | sysadmin123 |
| Admin Authority | admin@civic.gov | admin123 |
| Community Member | priya@email.com | priya123 |
| Community Member | rahul@email.com | rahul123 |
| Community Member | fatima@email.com | fatima123 |
| Community Member | david@email.com | david123 |

New registrations always default to the `community_member` role.

**Reset all data to the original seed state:**
```js
// Open browser DevTools (F12) → Console → run:
localStorage.clear()
// Then refresh the page
```

---

## How the Scoring Works

Every issue receives a **FinalScore between 0 and 100**. The system ranks and resolves issues in descending score order.

### Formula

```
FinalScore = BaseScore + VoteScore   (hard cap: 100)

BaseScore  = (w1 × Severity_norm
            + w2 × PeopleAffected_norm
            + w3 × TimePending_norm
            + w4 × LocationSensitivity) × 100
            + CategoryBonus          (hard cap: 92)

VoteScore  = min(votes.length × 0.5, 8)
```

### Weights (defined in `src/data/seed.js`)

```js
export const WEIGHTS = { w1: 0.35, w2: 0.25, w3: 0.20, w4: 0.20 };
// w1 + w2 + w3 + w4 must always equal 1.0
```

### Parameter Normalization

Each parameter is mapped to [0, 1] before weighting:

| Parameter | Normalization | Notes |
|---|---|---|
| Severity (1–5) | `(severity - 1) / 4` | 1 → 0.0, 5 → 1.0 |
| People Affected | `min(people / 5000, 1.0)` | Capped at 5000 |
| Time Pending | `min(√days / √365, 1.0)` | Progressive — grows fast then slows |
| Location Sensitivity | Lookup table in seed.js | Hospital=1.00, Park=0.20 |

**Why `√days` for time pending?** Linear growth would make very old issues dominate everything else. Square root gives diminishing returns — an issue pending 90 days is more urgent than one pending 7 days, but not infinitely so.

### VoteScore Cap

Each community vote adds 0.5 points, capped at 8 points total. This means:
- 16 votes = maximum vote influence (8 pts)
- A low-severity issue with 100 votes **cannot** outrank a critical issue with 0 votes
- The cap prevents coordinated voting from gaming the ranking

### Tie-Breaking

When two issues share the same FinalScore, the system breaks ties in this exact order:
1. Severity DESC (higher severity wins)
2. PeopleAffected DESC (more affected wins)
3. createdAt ASC (older issue wins)

This is implemented in `tieBreakSort()` in `src/utils/scoring.js` and is fully deterministic — the same input always produces the same ranking.

### Priority Levels

| Score | Level |
|---|---|
| 80 – 100 | Critical |
| 60 – 79 | High |
| 40 – 59 | Medium |
| 0 – 39 | Low |

---

## How the Optimizer Works

The Resource Optimizer (`runOptimizer()` in `src/utils/scoring.js`) allocates limited resources to issues using a **priority-based greedy algorithm**.

### Algorithm

```
1. Filter    Remove resolved issues. Only open + in-progress are considered.
2. Score     Run calcPriority() on every remaining issue.
3. Sort      Order by FinalScore DESC using tieBreakSort().
4. Loop      For each issue (highest priority first):
               Check: workers needed ≤ remaining workers?
               Check: vehicles needed ≤ remaining vehicles?
               Check: hours needed ≤ remaining hours?
               All pass → SCHEDULE (subtract from remaining, push to scheduled[])
               Any fail → DEFER   (record specific failure reason, push to deferred[])
5. Return    { scheduled[], deferred[], remaining }
```

### Resource Requirements per Category

Each issue category has a predefined resource need (defined in `src/data/seed.js`):

```js
export const RESOURCE_NEEDS = {
  'Utilities':      { workers: 4, vehicles: 2, hours: 8  },
  'Public Safety':  { workers: 2, vehicles: 1, hours: 4  },
  'Infrastructure': { workers: 5, vehicles: 2, hours: 12 },
  'Environment':    { workers: 6, vehicles: 3, hours: 16 },
  'Sanitation':     { workers: 4, vehicles: 2, hours: 8  },
  'Transport':      { workers: 3, vehicles: 2, hours: 6  },
  'Healthcare':     { workers: 2, vehicles: 1, hours: 4  },
  'Education':      { workers: 2, vehicles: 1, hours: 4  },
};
// Unknown categories default to { workers: 2, vehicles: 1, hours: 4 }
```

### Available Resource Caps

```js
export const RESOURCE_CONFIG = [
  { id: 'workers',  label: 'Field Workers',    total: 20  },
  { id: 'vehicles', label: 'Service Vehicles', total: 8   },
  { id: 'hours',    label: 'Working Hours',    total: 120 },
];
```

### Why Greedy?

The greedy approach was chosen over integer programming because:
- Every allocation decision is **explainable** — you can trace exactly why each issue was scheduled or deferred
- No external solver dependency
- Runs in O(n log n) — instant even for large datasets
- Fully deterministic and auditable

---

## Project Structure

```
civic-priority/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.jsx                  # React root
    ├── App.jsx                   # BrowserRouter + all Providers + all Routes
    ├── index.css                 # CSS variables + global reset
    │
    ├── data/
    │   └── seed.js               # ← START HERE for customisation
    │                             #   WEIGHTS, CATEGORIES, LOCATION_TYPES,
    │                             #   LOCATION_SENSITIVITY, CATEGORY_BONUS,
    │                             #   RESOURCE_NEEDS, RESOURCE_CONFIG,
    │                             #   SEED_USERS, SEED_ISSUES
    │
    ├── utils/
    │   └── scoring.js            # ← ALL scoring + optimizer logic lives here
    │                             #   calcBaseScore, calcVoteScore, calcPriority,
    │                             #   getScoreBreakdown, tieBreakSort,
    │                             #   runOptimizer, sortIssues, filterIssues,
    │                             #   getLevel, LEVEL_META, formatDate, genId
    │                             #   re-exports: WEIGHTS (from seed.js)
    │
    ├── context/
    │   ├── AuthContext.jsx        # user session, login, register, logout,
    │   │                         # Boolean role helpers (canVote, canOverride…)
    │   │                         # Reads/writes: localStorage civic_users
    │   │                         #              sessionStorage civic_user
    │   │
    │   ├── IssuesContext.jsx      # issues[], auditLog[], all mutations:
    │   │                         # addIssue, toggleVote, updateStatus,
    │   │                         # overrideStatus (atomic + audit), addComment
    │   │                         # Reads/writes: localStorage civic_issues
    │   │                         #              localStorage civic_audit
    │   │
    │   └── ToastContext.jsx       # global show() notification (no storage)
    │
    ├── components/
    │   ├── IssueCard.jsx          # main reusable card component
    │   │                         # exports: ScoreRing, PriorityBadge,
    │   │                         #          StatusBadge, IssueCard (default)
    │   ├── IssueCard.module.css
    │   └── layout/
    │       ├── Navbar.jsx         # role-based dynamic navigation
    │       ├── Navbar.module.css
    │       ├── Toast.jsx          # animated notification display
    │       └── Toast.module.css
    │
    └── pages/
        ├── Auth.jsx               # login + register
        ├── Dashboard.jsx          # analytics: stat cards, charts, top issues
        ├── Issues.jsx             # filterable + sortable issue list
        ├── Report.jsx             # issue submission with live score preview
        ├── Ranking.jsx            # 12-column priority ranking table
        ├── Optimizer.jsx          # resource allocation (Admin+)
        ├── Audit.jsx              # override history log (Admin+)
        ├── Users.jsx              # user management (SysAdmin only)
        └── Profile.jsx            # personal stats + history
        (each page has a matching .module.css file)
```

---

## Data Layer — How localStorage is Used

Since there is no backend, all data lives in the browser's localStorage. Here is the complete picture:

### Storage Keys

| Storage Type | Key | What It Stores |
|---|---|---|
| localStorage | `civic_issues` | JSON array of all issues including votes[], comments[], status, overriddenBy |
| localStorage | `civic_audit` | JSON array of all admin override audit entries |
| localStorage | `civic_users` | JSON array of all registered users |
| sessionStorage | `civic_user` | JSON object of the currently logged-in user (clears on tab close) |

### Initialisation Flow

When the app loads for the first time (empty localStorage):

```
App loads
  ↓
IssuesContext initialises
  ↓
localStorage.getItem('civic_issues') → null
  ↓
Falls back to SEED_ISSUES from src/data/seed.js
Saves them to localStorage immediately
  ↓
Same flow for users via AuthContext → SEED_USERS
```

On every subsequent load, localStorage data takes priority over seed data.

### How Issues Are Structured

```js
{
  id:            'id_1741234_abc12',  // generated by genId()
  title:         'Burst Water Main — Main Street',
  category:      'Utilities',
  description:   'Water main has burst...',
  severity:      5,                   // 1–5
  affectedPeople:450,                 // 0–5000
  locationType:  'Residential',
  createdAt:     '2025-01-15',        // YYYY-MM-DD string
  status:        'open',              // 'open' | 'inprogress' | 'resolved'
  votes:         ['u2', 'u3'],        // array of userId strings
  reporterId:    'u2',
  reporter:      'Priya Sharma',
  comments:      [],                  // array of comment objects
  overriddenBy:  null,                // userId string or null
  overriddenAt:  null,                // ISO timestamp string or null
}
```

### How Audit Entries Are Structured

```js
{
  id:         'id_1741234_def34',
  type:       'STATUS_OVERRIDE',
  issueId:    'id_1741234_abc12',
  issueTitle: 'Burst Water Main — Main Street',
  oldStatus:  'open',
  newStatus:  'inprogress',
  adminId:    'u_admin1',
  adminName:  'Admin Authority',
  timestamp:  '2026-03-20T10:24:33.000Z',  // ISO 8601
  note:       'Crew dispatched to the area',
}
```

### How State Updates Work

All mutations go through a **useReducer** inside IssuesContext. localStorage is updated synchronously after every dispatch:

```
User action (e.g. vote)
  ↓
Component calls toggleVote(issueId, userId)
  ↓
dispatch({ type: 'TOGGLE_VOTE', issueId, userId })
  ↓
Reducer: adds or removes userId from issue.votes[]
         returns new state (immutable — spread operator)
  ↓
useEffect on state → localStorage.setItem('civic_issues', JSON.stringify(issues))
  ↓
All components using useIssues() re-render with new data
```

The **OVERRIDE_STATUS** action is deliberately atomic — both the issue status update and the audit entry creation happen in the same reducer case. This prevents any scenario where the issue is updated but the audit entry is missing.

---

## Seed Data — Customising the Starting State

All customisation starts in **`src/data/seed.js`**. After editing, run `localStorage.clear()` in the browser console and refresh.

### Change Issue Categories

```js
export const CATEGORIES = [
  'Public Safety', 'Infrastructure', 'Utilities',
  'Environment', 'Sanitation', 'Transport', 'Healthcare', 'Education',
];
```

### Change Category Urgency Bonus

Higher number = more bonus points on top of the weighted score:

```js
export const CATEGORY_BONUS = {
  'Public Safety':  15,
  'Healthcare':     12,
  'Infrastructure': 10,
  'Utilities':       8,
  'Sanitation':      6,
  'Environment':     6,
  'Transport':       5,
  'Education':       4,
};
```

### Change Location Types and Sensitivity

Higher number = more sensitive location = more weight toward urgency:

```js
export const LOCATION_TYPES = [
  'Hospital Zone', 'School Zone', 'Highway', 'Residential',
  'Commercial', 'Industrial', 'Park/Recreation',
];

export const LOCATION_SENSITIVITY = {
  'Hospital Zone':   1.00,
  'School Zone':     0.85,
  'Highway':         0.70,
  'Residential':     0.50,
  'Commercial':      0.40,
  'Industrial':      0.30,
  'Park/Recreation': 0.20,
};
```

### Change Scoring Weights

```js
export const WEIGHTS = { w1: 0.35, w2: 0.25, w3: 0.20, w4: 0.20 };
// MUST sum to 1.0 — verify before saving
```

### Change Resource Requirements Per Category

```js
export const RESOURCE_NEEDS = {
  'Utilities': { workers: 4, vehicles: 2, hours: 8 },
  // add your categories here
};
```

### Add or Edit Seed Users

```js
export const SEED_USERS = [
  {
    id:          'u_sysadmin',
    name:        'System Administrator',
    email:       'sysadmin@civic.gov',
    password:    'sysadmin123',
    role:        'system_admin',      // 'community_member' | 'admin_authority' | 'system_admin'
    avatar:      'SA',                // up to 2 characters, shown as initials
    joined:      '2023-01-01',
    description: 'System administrator',
  },
  // add more users here
];
```

### Add or Edit Seed Issues

```js
export const SEED_ISSUES = [
  {
    id:            1,                   // unique number or string
    title:         'Issue Title',
    category:      'Infrastructure',    // must match a value in CATEGORIES
    description:   'Detailed description...',
    severity:      4,                   // 1–5
    affectedPeople:300,
    locationType:  'Residential',       // must match a value in LOCATION_TYPES
    status:        'open',              // 'open' | 'inprogress' | 'resolved'
    votes:         [],                  // start empty or pre-populate with userIds
    reporterId:    'u2',
    reporter:      'Priya Sharma',
    createdAt:     '2025-12-01',        // YYYY-MM-DD, older = more time pending score
    overriddenBy:  null,
    overriddenAt:  null,
    comments:      [],
  },
];
```

---

## Extending the Project

### Add a New Page

1. Create `src/pages/MyPage.jsx` and `src/pages/MyPage.module.css`
2. Add the route in `src/App.jsx`:
```jsx
<Route path="/mypage" element={<MyPage />} />
```
3. Add the nav link in `src/components/layout/Navbar.jsx` inside `ROLE_LINKS` for the appropriate role.

### Add a New Scoring Parameter

1. Add the raw field to the issue schema in `SEED_ISSUES` (seed.js)
2. Normalize it to [0, 1] inside `calcBaseScore()` in `scoring.js`
3. Add a weight to `WEIGHTS` in seed.js — adjust others so they still sum to 1.0
4. Add a row for it in the score breakdown display inside `IssueCard.jsx`

### Add a New Resource Constraint

1. Add the resource to `RESOURCE_CONFIG` in seed.js:
```js
{ id: 'equipment', label: 'Heavy Equipment', total: 5 }
```
2. Add the per-category need to `RESOURCE_NEEDS`:
```js
'Infrastructure': { workers: 5, vehicles: 2, hours: 12, equipment: 2 }
```
3. The `runOptimizer()` function in scoring.js reads constraints dynamically from `RESOURCE_CONFIG` — no changes needed there.
4. Add a slider for the new resource in `src/pages/Optimizer.jsx`.

### Add a New User Role

1. Add the role string to the role check helpers in `AuthContext.jsx`
2. Add the corresponding Boolean helper (e.g. `canDoX = user?.role === 'new_role'`)
3. Add the navigation links for that role in `Navbar.jsx` inside `ROLE_LINKS`
4. Add route protection on any pages restricted to that role

---

## Replacing localStorage with a Real Database

Currently all data operations live in two files:

| File | What to replace |
|---|---|
| `src/context/IssuesContext.jsx` | localStorage reads/writes for issues and audit log |
| `src/context/AuthContext.jsx` | localStorage reads/writes for users and session |

**Pages and components do not touch storage directly** — they only call functions from context (`addIssue`, `toggleVote`, `overrideStatus`, `login`, etc.). This means you only need to change the two context files.

### Option 1 — Firebase Firestore (recommended for quick setup)

```bash
npm install firebase
```

Replace in `IssuesContext.jsx`:
```js
// BEFORE (localStorage)
const saved = JSON.parse(localStorage.getItem('civic_issues') || '[]');
localStorage.setItem('civic_issues', JSON.stringify(issues));

// AFTER (Firestore)
import { collection, onSnapshot, addDoc } from 'firebase/firestore';

// Real-time listener — updates all users instantly when any issue changes
onSnapshot(collection(db, 'issues'), snapshot => {
  const issues = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
  dispatch({ type: 'SET_ISSUES', issues });
});

// Add new issue
await addDoc(collection(db, 'issues'), newIssue);
```

Replace in `AuthContext.jsx`:
```js
// Use Firebase Authentication instead of localStorage user matching
import { signInWithEmailAndPassword, createUserWithEmailAndPassword } from 'firebase/auth';
```

### Option 2 — Supabase (PostgreSQL)

```bash
npm install @supabase/supabase-js
```

```js
import { createClient } from '@supabase/supabase-js';
const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// Fetch issues
const { data: issues } = await supabase.from('issues').select('*');

// Insert issue
await supabase.from('issues').insert(newIssue);

// Real-time updates
supabase.channel('issues').on('postgres_changes',
  { event: '*', schema: 'public', table: 'issues' },
  payload => { /* update state */ }
).subscribe();
```

### Option 3 — Custom REST API (Node.js + Express + MongoDB)

```js
// Replace localStorage reads with fetch calls

// Get issues
const res = await fetch('/api/issues');
const issues = await res.json();

// Add issue
await fetch('/api/issues', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(newIssue),
});
```

The scoring logic in `src/utils/scoring.js` is framework-agnostic pure JavaScript — it can be moved to a Node.js backend unchanged if you want server-side score computation.

---

## Stack

| | |
|---|---|
| UI | React 18 |
| Routing | React Router DOM 6 |
| Build | Vite 5 |
| Styling | CSS Modules |
| State | React Context + useReducer |
| Storage | localStorage + sessionStorage |
| Fonts | Google Fonts (Space Mono + Epilogue) |
| Backend | None |
