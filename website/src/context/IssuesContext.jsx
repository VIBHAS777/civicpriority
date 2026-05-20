import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { db } from '../utils/firebase.js';
import { collection, onSnapshot, doc, setDoc, updateDoc, addDoc, getDocs } from 'firebase/firestore';
import { SEED_ISSUES } from '../data/seed.js';
import { genId } from '../utils/scoring.js';

const IssuesContext = createContext(null);

export function IssuesProvider({ children }) {
  const [issues, setIssues] = useState([]);
  const [auditLog, setAuditLog] = useState([]);

  // Load from Firestore
  useEffect(() => {
    if (!db) {
      console.warn("No DB connection for IssuesProvider");
      return;
    }
    
    // Seed initial data if Firebase is completely empty (Dev helper)
    const seedData = async () => {
      const issuesSnapshot = await getDocs(collection(db, 'issues'));
      if (issuesSnapshot.empty) {
        console.log("Seeding Firestore with initial issues...");
        for (const issue of SEED_ISSUES) {
          // Provide fixed string ID for UI compatibility and easy reading
          await setDoc(doc(db, 'issues', String(issue.id)), issue);
        }
      }
    };
    seedData();

    // Listen to Issues
    const unsubIssues = onSnapshot(collection(db, 'issues'), (snapshot) => {
      const issuesData = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      // Sort issues by newest first by default if needed, or keeping original structure
      setIssues(issuesData);
    });

    // Listen to Audit Log
    const unsubAudit = onSnapshot(collection(db, 'auditLog'), (snapshot) => {
      const auditData = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      // Sort by timestamp descending
      setAuditLog(auditData.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp)));
    });

    return () => {
      unsubIssues();
      unsubAudit();
    };
  }, []);

  const addIssue = useCallback(async (data, user) => {
    if (!db) return;
    const issueId = genId();
    const issue = {
      ...data,
      votes: [],
      status: 'open',
      createdAt: new Date().toISOString().split('T')[0],
      reporter: user.name,
      reporterId: user.id,
      comments: [],
      overriddenBy: null,
    };
    await setDoc(doc(db, 'issues', String(issueId)), issue);
  }, []);

  const toggleVote = useCallback(async (issueId, userId) => {
    if (!db) return;
    const issueRef = doc(db, 'issues', String(issueId));
    const issueObj = issues.find(i => String(i.id) === String(issueId));
    if (!issueObj) return;

    const votes = issueObj.votes ?? [];
    const already = votes.includes(userId);
    const newVotes = already ? votes.filter(v => v !== userId) : [...votes, userId];
    
    await updateDoc(issueRef, { votes: newVotes });
  }, [issues]);

  const updateStatus = useCallback(async (id, status) => {
    if (!db) return;
    const issueRef = doc(db, 'issues', String(id));
    await updateDoc(issueRef, { status });
  }, []);

  const overrideStatus = useCallback(async (issue, newStatus, admin, note = '') => {
    if (!db) return;
    const timestamp = new Date().toISOString();
    
    // Update the issue
    const issueRef = doc(db, 'issues', String(issue.id));
    await updateDoc(issueRef, {
      status: newStatus,
      overriddenBy: admin.id,
      overriddenAt: timestamp
    });

    // Write to audit log
    await addDoc(collection(db, 'auditLog'), {
      type: 'STATUS_OVERRIDE',
      issueId: issue.id,
      issueTitle: issue.title,
      oldStatus: issue.status,
      newStatus: newStatus,
      adminId: admin.id,
      adminName: admin.name,
      timestamp,
      note,
    });
  }, []);

  const addComment = useCallback(async (issueId, user, text) => {
    if (!db) return;
    const issueRef = doc(db, 'issues', String(issueId));
    const issueObj = issues.find(i => String(i.id) === String(issueId));
    if (!issueObj) return;

    const comment = {
      id: genId(),
      authorId: user.id,
      author: user.name,
      text,
      date: new Date().toISOString().split('T')[0],
    };
    
    const newComments = [...(issueObj.comments ?? []), comment];
    await updateDoc(issueRef, { comments: newComments });
  }, [issues]);

  return (
    <IssuesContext.Provider value={{
      issues,
      auditLog,
      addIssue, toggleVote, updateStatus, overrideStatus, addComment,
    }}>
      {children}
    </IssuesContext.Provider>
  );
}

export function useIssues() {
  const ctx = useContext(IssuesContext);
  if (!ctx) throw new Error('useIssues outside IssuesProvider');
  return ctx;
}
