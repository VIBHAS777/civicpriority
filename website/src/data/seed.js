// ─── Role Definitions ────────────────────────────────────────────────────────
// community_member : submit issues, vote (1 vote/issue), track status
// admin_authority  : input resources, run optimizer, override status, view audit log
// system_admin     : view all audit logs, manage users, monitor system

export const CATEGORIES = [
  'Lift Maintenance','Water Supply','Electricity',
  'Parking','Security','Housekeeping','Generator','Common Area',
];
export const LOCATION_TYPES = [
  'Block A','Block B','Block C',
  'Basement','Terrace','Clubhouse','Main Gate','Common Area',
];

export const SEVERITY_LABELS = { 1:'Minor', 2:'Moderate', 3:'Serious', 4:'Severe', 5:'Critical' };

// ─── Scoring weights (normalized: w1+w2+w3+w4 = 1.0) ────────────────────────
// w1=Severity, w2=PeopleAffected, w3=TimePending, w4=LocationSensitivity
// Each is multiplied by 100 for a 0–100 scale, then VoteScore added (capped)
export const WEIGHTS = { w1: 0.35, w2: 0.25, w3: 0.20, w4: 0.20 };

// Location sensitivity (0–1 scale, used with w4)
export const LOCATION_SENSITIVITY = {
  'Hospital Zone':  1.00,
  'School Zone':    0.85,
  'Highway':        0.70,
  'Residential':    0.50,
  'Commercial':     0.40,
  'Industrial':     0.30,
  'Park/Recreation':0.20,
};

// Category urgency bonus (flat bonus added after weighted score)
export const CATEGORY_BONUS = {
  'Water Supply':     15,
  'Security':         14,
  'Electricity':      13,
  'Lift Maintenance': 12,
  'Generator':        10,
  'Common Area':       6,
  'Parking':           5,
  'Housekeeping':      4,
};

// Resource needs per category for optimizer
export const RESOURCE_NEEDS = {
  'Utilities':      { workers:4, vehicles:2, hours:8  },
  'Public Safety':  { workers:2, vehicles:1, hours:4  },
  'Infrastructure': { workers:5, vehicles:2, hours:12 },
  'Environment':    { workers:6, vehicles:3, hours:16 },
  'Sanitation':     { workers:4, vehicles:2, hours:8  },
  'Transport':      { workers:3, vehicles:2, hours:6  },
  'Healthcare':     { workers:2, vehicles:1, hours:4  },
  'Education':      { workers:2, vehicles:1, hours:4  },
};

// Resource sliders for optimizer UI (workers, vehicles, hours — matching SRS)
export const RESOURCE_CONFIG = [
  { id:'workers',  name:'Field Workers',    icon:'👷', total:20,  unit:'workers' },
  { id:'vehicles', name:'Service Vehicles', icon:'🚛', total:8,   unit:'vehicles'},
  { id:'hours',    name:'Working Hours',    icon:'⏱️', total:120, unit:'hrs'     },
];

// ─── Seed Users (3 roles) ────────────────────────────────────────────────────
export const SEED_USERS = [
  {
    id:'u_sysadmin', name:'System Administrator', email:'sysadmin@civic.gov',
    password:'sysadmin123', role:'system_admin',
    avatar:'SA', joined:'2023-01-01',
    description:'Monitors system integrity, manages users, reviews all audit logs.',
  },
  {
    id:'u_admin1', name:'Admin Authority', email:'admin@civic.gov',
    password:'admin123', role:'admin_authority',
    avatar:'AA', joined:'2024-01-01',
    description:'Inputs resources, runs optimizer, overrides issue status.',
  },
  {
    id:'u2', name:'Priya Sharma',   email:'priya@email.com',
    password:'priya123',  role:'community_member', avatar:'PS', joined:'2024-06-15',
    description:'Community member — can report issues and vote.',
  },
  {
    id:'u3', name:'Rahul Mehta',    email:'rahul@email.com',
    password:'rahul123',  role:'community_member', avatar:'RM', joined:'2024-08-20',
    description:'Community member — can report issues and vote.',
  },
  {
    id:'u4', name:'Fatima Okonkwo', email:'fatima@email.com',
    password:'fatima123', role:'community_member', avatar:'FO', joined:'2024-09-10',
    description:'Community member — can report issues and vote.',
  },
  {
    id:'u5', name:'David Chen',     email:'david@email.com',
    password:'david123',  role:'community_member', avatar:'DC', joined:'2025-01-02',
    description:'Community member — can report issues and vote.',
  },
];

// ─── Seed Issues ─────────────────────────────────────────────────────────────
export const SEED_ISSUES = [
  {
    id: 1,
    title: 'Lift Not Working — Block A',
    category: 'Lift Maintenance',
    description: 'The lift in Block A has been completely non-functional since Monday morning. Elderly residents, differently-abled people, and families with small children are severely impacted. No technician has visited despite multiple complaints to the management office.',
    severity: 5,
    affectedPeople: 120,
    locationType: 'Block A',
    status: 'open',
    votes: [],
    reporterId: 'u2',
    reporter: 'Priya Sharma',
    createdAt: '2025-03-10',
    overriddenBy: null,
    comments: [{ id: 101, authorId: 'u_admin1', author: 'Admin Authority', text: 'Technician visit scheduled for tomorrow morning.', date: '2025-03-11' }],
  },
  {
    id: 2,
    title: 'Water Supply Disruption — Block B & C',
    category: 'Water Supply',
    description: 'No water supply from 6am to 12pm daily for the past 5 days in Block B and Block C. Residents are struggling with basic daily needs. The overhead tank appears to have a leakage issue that has not been addressed.',
    severity: 5,
    affectedPeople: 200,
    locationType: 'Block B',
    status: 'inprogress',
    votes: ['u3', 'u4'],
    reporterId: 'u2',
    reporter: 'Priya Sharma',
    createdAt: '2025-03-12',
    overriddenBy: null,
    comments: [{ id: 201, authorId: 'u_admin1', author: 'Admin Authority', text: 'Plumber has been contacted. Work in progress.', date: '2025-03-13' }],
  },
  {
    id: 3,
    title: 'Broken CCTV Camera — Main Gate',
    category: 'Security',
    description: 'The main gate CCTV camera has been non-functional for over 2 weeks. This is a major security risk for all residents. Unidentified vehicles have been entering the premises without proper verification.',
    severity: 4,
    affectedPeople: 500,
    locationType: 'Main Gate',
    status: 'open',
    votes: ['u2', 'u5'],
    reporterId: 'u3',
    reporter: 'Rahul Mehta',
    createdAt: '2025-03-13',
    overriddenBy: null,
    comments: [],
  },
  {
    id: 4,
    title: 'Generator Failure During Power Cuts',
    category: 'Generator',
    description: 'The backup generator is not starting automatically during power outages. Residents are left without electricity for hours especially at night. The generator was last serviced over 6 months ago and needs urgent inspection.',
    severity: 4,
    affectedPeople: 350,
    locationType: 'Common Area',
    status: 'open',
    votes: ['u2', 'u3', 'u4'],
    reporterId: 'u4',
    reporter: 'Fatima Okonkwo',
    createdAt: '2025-03-14',
    overriddenBy: null,
    comments: [],
  },
  {
    id: 5,
    title: 'Illegal Parking Blocking Emergency Access',
    category: 'Parking',
    description: 'Multiple vehicles are being regularly parked in the emergency access lane near Block C. This is blocking the pathway for ambulances and fire trucks. Several residents have raised this concern verbally but no action has been taken.',
    severity: 3,
    affectedPeople: 180,
    locationType: 'Block C',
    status: 'open',
    votes: ['u5'],
    reporterId: 'u5',
    reporter: 'David Chen',
    createdAt: '2025-03-15',
    overriddenBy: null,
    comments: [],
  },
  {
    id: 6,
    title: 'Clubhouse Washroom Unhygienic Condition',
    category: 'Housekeeping',
    description: 'The clubhouse washrooms have not been cleaned properly for over a week. There is a foul smell and water logging on the floor. This is a health hazard especially for children who use the clubhouse daily for activities.',
    severity: 3,
    affectedPeople: 150,
    locationType: 'Clubhouse',
    status: 'inprogress',
    votes: ['u2', 'u3'],
    reporterId: 'u3',
    reporter: 'Rahul Mehta',
    createdAt: '2025-03-16',
    overriddenBy: null,
    comments: [{ id: 601, authorId: 'u_admin1', author: 'Admin Authority', text: 'Housekeeping staff assigned. Will be cleaned by evening.', date: '2025-03-16' }],
  },
  {
    id: 7,
    title: 'Street Lights Not Working — Basement Parking',
    category: 'Electricity',
    description: 'More than half the lights in the basement parking area have stopped working. Residents are finding it very difficult and unsafe to park their vehicles at night. Two minor vehicle scraping incidents have already been reported.',
    severity: 4,
    affectedPeople: 280,
    locationType: 'Basement',
    status: 'open',
    votes: ['u3', 'u4', 'u5'],
    reporterId: 'u4',
    reporter: 'Fatima Okonkwo',
    createdAt: '2025-03-17',
    overriddenBy: null,
    comments: [],
  },
  {
    id: 8,
    title: 'Terrace Garden Water Leakage — Top Floor',
    category: 'Common Area',
    description: 'The terrace garden irrigation system has a major leakage that is seeping into the top floor apartments causing water stains and dampness on the ceiling. Three top floor residents have complained of paint damage and damp walls.',
    severity: 3,
    affectedPeople: 60,
    locationType: 'Terrace',
    status: 'resolved',
    votes: ['u2', 'u3', 'u4', 'u5'],
    reporterId: 'u2',
    reporter: 'Priya Sharma',
    createdAt: '2025-03-08',
    overriddenBy: null,
    comments: [{ id: 801, authorId: 'u_admin1', author: 'Admin Authority', text: 'Irrigation pipe repaired and terrace waterproofing done. Issue resolved.', date: '2025-03-10' }],
  },
];