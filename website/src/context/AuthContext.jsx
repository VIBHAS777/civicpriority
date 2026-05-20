import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { db } from '../utils/firebase.js';
import { collection, onSnapshot, doc, setDoc, getDocs, updateDoc } from 'firebase/firestore';
import { SEED_USERS } from '../data/seed.js';
import { genId } from '../utils/scoring.js';

const AuthContext = createContext(null);

/* SHA-256 hash using the browser Web Crypto API */
async function sha256(str) {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(str));
  return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('');
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(sessionStorage.getItem('civic_user')); } catch { return null; }
  });
  const [users, setUsers] = useState([]);
  const [error, setError] = useState(null);

  /* ── Load from Firestore (unchanged logic) ── */
  useEffect(() => {
    if (!db) {
      console.warn('No DB connection for AuthProvider. Falling back to SEED_USERS.');
      setUsers(SEED_USERS);
      return;
    }

    const seedData = async () => {
      try {
        const usersSnapshot = await getDocs(collection(db, 'users'));
        if (usersSnapshot.empty) {
          console.log('Seeding Firestore with initial users...');
          for (const u of SEED_USERS) {
            await setDoc(doc(db, 'users', String(u.id)), u);
          }
        }
      } catch (e) {
        console.warn('Failed to fetch from Firebase (likely missing keys). Falling back to local data.', e);
        setUsers(SEED_USERS);
      }
    };
    seedData();

    let unsubUsers = () => {};
    try {
      unsubUsers = onSnapshot(collection(db, 'users'), (snapshot) => {
        const usersData = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
        setUsers(usersData);
      }, (err) => {
        console.warn('onSnapshot failed. Using local data instead.', err);
      });
    } catch (e) {
      console.warn('onSnapshot setup failed.', e);
    }

    return () => unsubUsers();
  }, []);

  /* ── Login — async, with SHA-256 + backward-compat migration ── */
  const login = useCallback(async (email, password) => {
    const hashed = await sha256(password);

    let currentUsers = users;
    if (currentUsers.length === 0) {
      if (db) {
        try {
          const snap = await getDocs(collection(db, 'users'));
          if (snap.empty) {
            currentUsers = SEED_USERS;
          } else {
            currentUsers = snap.docs.map(d => ({ id: d.id, ...d.data() }));
          }
        } catch (e) {
          console.warn('Firebase login fetch failed, using local data.', e);
          currentUsers = SEED_USERS;
        }
      } else {
        currentUsers = SEED_USERS;
      }
    }

    // Try hashed match first
    let found = currentUsers.find(
      u => u.email.toLowerCase() === email.toLowerCase() && u.password === hashed
    );

    // Backward-compat: seeded users still have plain-text passwords → migrate on login
    if (!found) {
      const plain = currentUsers.find(
        u => u.email.toLowerCase() === email.toLowerCase() && u.password === password
      );
      if (plain) {
        found = plain;
        if (db) {
          try { await updateDoc(doc(db, 'users', String(plain.id)), { password: hashed }); } catch {}
        }
      }
    }

    if (found) {
      try { sessionStorage.setItem('civic_user', JSON.stringify(found)); } catch {}
      setUser(found);
      setError(null);
      return { ok: true };
    }
    setError('Invalid email or password.');
    return { ok: false };
  }, [users]);

  /* ── Register — stores SHA-256 hash (unchanged logic otherwise) ── */
  const register = useCallback(async (name, email, password) => {
    if (!name.trim() || !email.trim() || !password.trim()) {
      setError('All fields are required.');
      return { ok: false };
    }
    if (users.find(u => u.email.toLowerCase() === email.toLowerCase())) {
      setError('An account with this email already exists.');
      return { ok: false };
    }
    if (!db) { setError('Database connection error.'); return { ok: false }; }

    const hashed = await sha256(password);

    const newUser = {
      id:          genId(),
      name:        name.trim(),
      email:       email.trim().toLowerCase(),
      password:    hashed,
      role:        'community_member',
      avatar:      name.trim().split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase(),
      joined:      new Date().toISOString().split('T')[0],
      description: 'Community member — can report issues and vote.',
    };

    await setDoc(doc(db, 'users', String(newUser.id)), newUser);
    try { sessionStorage.setItem('civic_user', JSON.stringify(newUser)); } catch {}
    setUser(newUser);
    setError(null);
    return { ok: true };
  }, [users]);

  const logout = useCallback(() => {
    try { sessionStorage.removeItem('civic_user'); } catch {}
    setUser(null);
    setError(null);
  }, []);

  const clearError = useCallback(() => setError(null), []);

  /* ── Role helpers (unchanged) ── */
  const isMember       = user?.role === 'community_member';
  const isAdmin        = user?.role === 'admin_authority';
  const isSysAdmin     = user?.role === 'system_admin';
  const canVote        = !!user;
  const canReport      = !!user;
  const canOverride    = isAdmin || isSysAdmin;
  const canRunOptimizer   = isAdmin || isSysAdmin;
  const canViewAuditLog   = isAdmin || isSysAdmin;
  const canManageUsers    = isSysAdmin;

  return (
    <AuthContext.Provider value={{
      user, users, error,
      login, register, logout, clearError,
      isMember, isAdmin, isSysAdmin,
      canVote, canReport, canOverride, canRunOptimizer, canViewAuditLog, canManageUsers,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth outside AuthProvider');
  return ctx;
}
