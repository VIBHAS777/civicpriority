import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext.jsx';
import { IssuesProvider } from './context/IssuesContext.jsx';
import { ToastProvider } from './context/ToastContext.jsx';
import Navbar    from './components/layout/Navbar.jsx';
import Toast     from './components/layout/Toast.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Issues    from './pages/Issues.jsx';
import Report    from './pages/Report.jsx';
import Ranking   from './pages/Ranking.jsx';
import Optimizer from './pages/Optimizer.jsx';
import Audit     from './pages/Audit.jsx';
import Users     from './pages/Users.jsx';
import Profile   from './pages/Profile.jsx';
import AuthPage  from './pages/Auth.jsx';
import './index.css';

function Shell() {
  const { user } = useAuth();
  return (
    <div style={{ display:'flex', flexDirection:'column', minHeight:'100vh', background:'var(--paper)' }}>
      <Navbar />
      <main style={{ flex:1, maxWidth:'1300px', width:'100%', margin:'0 auto', padding:'2rem 1.5rem' }}>
        <Routes>
          {/* Public pages — everyone can view */}
          <Route path="/"        element={<Dashboard />} />
          <Route path="/issues"  element={<Issues />} />
          <Route path="/ranking" element={<Ranking />} />

          {/* Members only — community_member, admin_authority, system_admin */}
          <Route path="/report"  element={<Report />} />
          <Route path="/profile" element={user ? <Profile /> : <Navigate to="/auth" replace />} />

          {/* Admin only — admin_authority, system_admin */}
          <Route path="/optimizer" element={<Optimizer />} />
          <Route path="/audit"     element={<Audit />} />

          {/* System admin only */}
          <Route path="/users" element={<Users />} />

          {/* Auth — redirect to home if already logged in */}
          <Route path="/auth" element={user ? <Navigate to="/" replace /> : <AuthPage />} />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <Toast />
    </div>
  );
}

export default function App() {
  return (
    <HashRouter>
      <ToastProvider>
        <AuthProvider>
          <IssuesProvider>
            <Shell />
          </IssuesProvider>
        </AuthProvider>
      </ToastProvider>
    </HashRouter>
  );
}
