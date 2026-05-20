import emailjs from '@emailjs/browser';

const SERVICE_ID  = import.meta.env.VITE_EMAILJS_SERVICE_ID;
const TEMPLATE_ID = import.meta.env.VITE_EMAILJS_TEMPLATE_ID;
const PUBLIC_KEY  = import.meta.env.VITE_EMAILJS_PUBLIC_KEY;

/** Generate a cryptographically random 6-digit OTP */
export function generateOtp() {
  const arr = new Uint32Array(1);
  crypto.getRandomValues(arr);
  return String(100000 + (arr[0] % 900000));
}

/**
 * Send OTP to the given email via EmailJS.
 * Falls back gracefully (logs to console) if env vars are missing.
 * @returns {Promise<{ok:boolean, error?:string}>}
 */
export async function sendOtpEmail(email, name, otp) {
  if (!SERVICE_ID || !TEMPLATE_ID || !PUBLIC_KEY) {
    // Dev fallback — log OTP so testing is still possible
    console.warn('[EmailJS] Env vars not set. OTP for dev:', otp);
    return { ok: true, devMode: true };
  }
  try {
    const sendPromise = emailjs.send(
      SERVICE_ID,
      TEMPLATE_ID,
      { to_email: email, to_name: name, otp_code: otp, app_name: 'CivicPriority' },
      PUBLIC_KEY
    );
    
    // 10 second timeout to prevent infinite hanging
    const timeoutPromise = new Promise((_, reject) => 
      setTimeout(() => reject(new Error('EmailJS request timed out after 10s')), 10000)
    );

    await Promise.race([sendPromise, timeoutPromise]);
    return { ok: true };
  } catch (err) {
    console.error('[EmailJS] Send failed or timed out:', err);
    return { ok: false, error: 'Failed to send OTP. Please check your network or try again.' };
  }
}

/**
 * Returns true if the email should bypass OTP
 * (privileged government / admin domain accounts)
 */
export function isAdminEmail(email) {
  const lower = email.toLowerCase().trim();
  return lower.endsWith('@civic.gov');
}
