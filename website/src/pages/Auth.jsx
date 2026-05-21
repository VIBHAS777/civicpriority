import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { generateOtp, sendOtpEmail, isAdminEmail } from '../utils/emailOtp.js';
import s from './Auth.module.css';

const DEMOS = [
  { role: 'System Admin',  email: 'sysadmin@civic.gov', pass: 'sysadmin123', desc: 'Full access · users · audit' },
  { role: 'Admin Auth.',   email: 'admin@civic.gov',    pass: 'admin123',    desc: 'Override · optimizer · audit' },
  { role: 'Member',        email: 'priya@email.com',    pass: 'priya123',    desc: 'Report issues · vote' },
  { role: 'Member',        email: 'rahul@email.com',    pass: 'rahul123',    desc: 'Report issues · vote' },
];

function getStrength(pw) {
  let score = 0;
  if (pw.length >= 8)                      score++;
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++;
  if (/[0-9]/.test(pw))                   score++;
  if (/[^A-Za-z0-9]/.test(pw))            score++;
  return score; // 0–4
}
const STRENGTH_LABEL = ['', 'Weak', 'Fair', 'Good', 'Strong'];
const STRENGTH_COLOR = ['', '#ef4444', '#f97316', '#f5a623', '#22c97e'];

export default function AuthPage() {
  const [mode,     setMode]     = useState('login');
  const [step,     setStep]     = useState(1);      // 1=form, 2=otp
  const [email,    setEmail]    = useState('');
  const [password, setPassword] = useState('');
  const [confirm,  setConfirm]  = useState('');
  const [name,     setName]     = useState('');
  const [showPw,   setShowPw]   = useState(false);
  const [loading,  setLoading]  = useState(false);
  const [otpSent,  setOtpSent]  = useState(false);
  const [otpDigits, setOtpDigits] = useState(['','','','','','']);
  const [pendingOtp, setPendingOtp] = useState('');
  const [otpTimer,  setOtpTimer]   = useState(0);
  const timerRef = useRef(null);
  const otpRefs  = useRef([]);

  const { login, register, error, clearError } = useAuth();
  const { show } = useToast();
  const navigate  = useNavigate();

  const strength = getStrength(password);

  // Countdown timer for OTP
  useEffect(() => {
    if (otpTimer > 0) {
      timerRef.current = setTimeout(() => setOtpTimer(t => t - 1), 1000);
    }
    return () => clearTimeout(timerRef.current);
  }, [otpTimer]);

  const switchMode = m => {
    setMode(m); setStep(1); clearError();
    setEmail(''); setPassword(''); setConfirm(''); setName('');
    setOtpDigits(['','','','','','']); setPendingOtp(''); setOtpTimer(0);
  };

  /* ── Login submit ── */
  const handleLogin = async () => {
    setLoading(true);
    try {
      const res = await login(email, password);
      if (res.ok) { show('Welcome back!'); navigate('/'); }
      else { show(error || 'Invalid email or password.', 'error'); }
    } catch (err) {
      console.error('Login Error:', err);
      show('An unexpected error occurred during login.', 'error');
    } finally {
      setLoading(false);
    }
  };

  /* ── Register step 1: validate then send OTP (or bypass for admins) ── */
  const handleSendOtp = async () => {
    clearError();
    if (!name.trim() || !email.trim() || !password.trim()) return;
    if (password !== confirm) { show('Passwords do not match.', 'error'); return; }
    if (strength < 2) { show('Password is too weak. Add uppercase, numbers, or symbols.', 'error'); return; }

    setLoading(true);
    try {
      // Admin-domain emails bypass OTP
      if (isAdminEmail(email)) {
        const res = await register(name, email, password);
        if (res.ok) { show('Account created! Welcome to CivicPriority.'); navigate('/'); }
        return;
      }
      // Generate + send OTP
      const otp = generateOtp();
      setPendingOtp(otp);
      const sent = await sendOtpEmail(email, name, otp);
      if (!sent.ok) { show(sent.error || 'Failed to send OTP.', 'error'); return; }
      if (sent.devMode) show(`[Dev] OTP logged to console — check browser DevTools`, 'error');
      else              show(`OTP sent to ${email}!`);
      setStep(2);
      setOtpTimer(300); // 5 min
      setTimeout(() => otpRefs.current[0]?.focus(), 100);
    } catch (err) {
      console.error('Send OTP Error:', err);
      show('An unexpected error occurred. Please try again.', 'error');
    } finally {
      setLoading(false);
    }
  };

  /* ── OTP digit input handling ── */
  const handleOtpKey = (i, e) => {
    if (e.key === 'Backspace') {
      const next = [...otpDigits]; next[i] = '';
      setOtpDigits(next);
      if (i > 0) otpRefs.current[i - 1]?.focus();
      return;
    }
    if (!/^\d$/.test(e.key)) return;
    const next = [...otpDigits]; next[i] = e.key;
    setOtpDigits(next);
    if (i < 5) otpRefs.current[i + 1]?.focus();
  };

  /* ── Register step 2: verify OTP then create account ── */
  const handleVerifyOtp = async () => {
    const entered = otpDigits.join('');
    if (entered.length < 6) { show('Please enter all 6 digits.', 'error'); return; }
    if (otpTimer === 0)     { show('OTP expired. Please resend.', 'error'); return; }
    if (entered !== pendingOtp) { show('Incorrect OTP. Try again.', 'error'); return; }
    setLoading(true);
    const res = await register(name, email, password);
    if (res.ok) { show('Account verified & created! Welcome to CivicPriority.'); navigate('/'); }
    setLoading(false);
  };

  const handleResendOtp = async () => {
    const otp = generateOtp();
    setPendingOtp(otp);
    setOtpDigits(['','','','','','']);
    setOtpTimer(300);
    await sendOtpEmail(email, name, otp);
    show('New OTP sent!');
    otpRefs.current[0]?.focus();
  };

  const handleSubmit = e => { e.preventDefault(); if (mode === 'login') handleLogin(); };

  return (
    <div className={s.page}>
      {/* ── Left Panel ── */}
      <div className={s.left}>
        <div className={s.leftBg} aria-hidden="true">
          <div className={s.orb1} /><div className={s.orb2} /><div className={s.orb3} />
          <div className={s.grid3d} />
        </div>
        <div className={s.leftContent}>
          <div className={s.brand}>Civic<span>Priority</span></div>
          <h1 className={s.headline}>Your voice shapes<br/>your community</h1>
          <p className={s.sub}>Transparent, rule-based issue prioritization with community voting and constraint-aware resource allocation.</p>

          <div className={s.rolesBox}>
            <div className={s.rolesTitle}>Three User Roles</div>
            {[
              { r: 'Community Member', d: 'Submit issues, cast one vote per issue, track status', c: s.rBlue },
              { r: 'Admin Authority',  d: 'Input resources, run optimizer, override status with audit', c: s.rGold },
              { r: 'System Admin',     d: 'Monitor system, manage users, view all audit logs', c: s.rRed  },
            ].map(({ r, d, c }) => (
              <div key={r} className={s.roleItem}>
                <span className={`${s.roleDot} ${c}`} />
                <div><div className={s.roleName}>{r}</div><div className={s.roleDesc}>{d}</div></div>
              </div>
            ))}
          </div>

          <div className={s.demoBox}>
            <div className={s.demoTitle}>Demo Accounts — click to fill</div>
            <div className={s.demoGrid}>
              {DEMOS.map(d => (
                <button key={d.email} className={s.demoBtn}
                  onClick={() => { setMode('login'); setEmail(d.email); setPassword(d.pass); }}>
                  <span className={s.demoRole}>{d.role}</span>
                  <span className={s.demoEmail}>{d.email}</span>
                  <span className={s.demoDesc}>{d.desc}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* ── Right Panel ── */}
      <div className={s.right}>
        {/* Holographic scanner effect */}
        <div className={s.scannerLine} />
        
        {/* Massive HUD Frame around the card */}
        <div className={s.hudWrapper}>
          {/* Cyberpunk corner brackets */}
          <div className={s.hudBracketTopLeft} />
          <div className={s.hudBracketTopRight} />
          <div className={s.hudBracketBottomLeft} />
          <div className={s.hudBracketBottomRight} />

          {/* Floating tech readouts */}
          <div className={s.techReadout1}>SYS.AUTH // 89.2%</div>
          <div className={s.techReadout2}>NET.SECURE // OK</div>

          <div className={s.card}>
            <div className={s.tabs}>
              <button className={`${s.tab} ${mode==='login' ? s.tabActive : ''}`} onClick={() => switchMode('login')}>Sign In</button>
              <button className={`${s.tab} ${mode==='register' ? s.tabActive : ''}`} onClick={() => switchMode('register')}>Register</button>
            </div>

          {/* ─ LOGIN ─ */}
          {mode === 'login' && (
            <form onSubmit={handleSubmit} className={s.form}>
              <div className={s.field}>
                <label className={s.label}>Email Address</label>
                <input className={s.input} type="email" placeholder="you@email.com" value={email}
                  onChange={e => setEmail(e.target.value)} required autoComplete="email"/>
              </div>
              <div className={s.field}>
                <label className={s.label}>Password</label>
                <div className={s.pwWrap}>
                  <input className={s.input} type={showPw ? 'text' : 'password'} placeholder="••••••••"
                    value={password} onChange={e => setPassword(e.target.value)} required autoComplete="current-password"/>
                  <button type="button" className={s.eyeBtn} onClick={() => setShowPw(v => !v)}>
                    {showPw ? '🙈' : '👁️'}
                  </button>
                </div>
              </div>
              {error && <div className={s.errorBox}>⚠ {error}</div>}
              <button type="submit" className={s.submitBtn} disabled={loading}>
                {loading ? <span className={s.spinner}/> : 'Sign In →'}
              </button>
              <p className={s.hint}>New here? <button type="button" className={s.link} onClick={() => switchMode('register')}>Create account</button></p>
            </form>
          )}

          {/* ─ REGISTER — Step 1: form ─ */}
          {mode === 'register' && step === 1 && (
            <div className={s.form}>
              <div className={s.field}>
                <label className={s.label}>Full Name</label>
                <input className={s.input} type="text" placeholder="Your full name" value={name}
                  onChange={e => setName(e.target.value)} required />
              </div>
              <div className={s.field}>
                <label className={s.label}>Email Address</label>
                <input className={s.input} type="email" placeholder="you@email.com" value={email}
                  onChange={e => setEmail(e.target.value)} required autoComplete="email"/>
              </div>
              <div className={s.field}>
                <label className={s.label}>Password</label>
                <div className={s.pwWrap}>
                  <input className={s.input} type={showPw ? 'text' : 'password'} placeholder="Min 8 chars, mixed case + number"
                    value={password} onChange={e => setPassword(e.target.value)} required autoComplete="new-password"/>
                  <button type="button" className={s.eyeBtn} onClick={() => setShowPw(v => !v)}>
                    {showPw ? '🙈' : '👁️'}
                  </button>
                </div>
                {password && (
                  <div className={s.strengthWrap}>
                    <div className={s.strengthBar}>
                      {[1,2,3,4].map(n => (
                        <div key={n} className={s.strengthSeg}
                          style={{ background: n <= strength ? STRENGTH_COLOR[strength] : 'rgba(255,255,255,0.08)' }} />
                      ))}
                    </div>
                    <span className={s.strengthLabel} style={{ color: STRENGTH_COLOR[strength] }}>
                      {STRENGTH_LABEL[strength]}
                    </span>
                  </div>
                )}
              </div>
              <div className={s.field}>
                <label className={s.label}>Confirm Password</label>
                <input className={s.input} type={showPw ? 'text' : 'password'} placeholder="Repeat password"
                  value={confirm} onChange={e => setConfirm(e.target.value)} required autoComplete="new-password"/>
                {confirm && password !== confirm && <span className={s.mismatch}>Passwords do not match</span>}
              </div>
              {error && <div className={s.errorBox}>⚠ {error}</div>}
              <button type="button" className={s.submitBtn}
                disabled={loading || !name.trim() || !email.trim() || !password || !confirm}
                onClick={handleSendOtp}>
                {loading ? <span className={s.spinner}/> : isAdminEmail(email) ? 'Create Account →' : 'Send OTP →'}
              </button>
              <p className={s.hint}>Have an account? <button type="button" className={s.link} onClick={() => switchMode('login')}>Sign in</button></p>
              <div className={s.noteBox}>
                <div className={s.noteTitle}>Note</div>
                New registrations are assigned the <strong>Community Member</strong> role. An OTP will be sent to your email for verification. Contact your system administrator to request elevated access.
              </div>
            </div>
          )}

          {/* ─ REGISTER — Step 2: OTP verify ─ */}
          {mode === 'register' && step === 2 && (
            <div className={s.form}>
              <div className={s.otpHeader}>
                <div className={s.otpIcon}>📧</div>
                <div className={s.otpTitle}>Check your email</div>
                <p className={s.otpSub}>We sent a 6-digit OTP to <strong>{email}</strong></p>
              </div>

              <div className={s.otpBoxes}>
                {otpDigits.map((d, i) => (
                  <input key={i} ref={el => otpRefs.current[i] = el}
                    className={s.otpDigit} type="text" inputMode="numeric"
                    maxLength={1} value={d}
                    onChange={() => {}}
                    onKeyDown={e => handleOtpKey(i, e)} />
                ))}
              </div>

              <div className={s.otpTimer}>
                {otpTimer > 0
                  ? `OTP expires in ${Math.floor(otpTimer/60)}:${String(otpTimer%60).padStart(2,'0')}`
                  : <button type="button" className={s.link} onClick={handleResendOtp}>Resend OTP</button>}
              </div>

              {error && <div className={s.errorBox}>⚠ {error}</div>}
              <button type="button" className={s.submitBtn}
                disabled={loading || otpDigits.join('').length < 6}
                onClick={handleVerifyOtp}>
                {loading ? <span className={s.spinner}/> : 'Verify & Create Account →'}
              </button>
              <button type="button" className={s.backBtn} onClick={() => setStep(1)}>← Back</button>
            </div>
          )}
        </div>
        </div> {/* Close hudWrapper */}
      </div>
    </div>
  );
}
