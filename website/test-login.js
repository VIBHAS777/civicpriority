import { SEED_USERS } from './src/data/seed.js';
import crypto from 'crypto';

async function sha256(str) {
  return crypto.createHash('sha256').update(str).digest('hex');
}

async function testLogin(email, password) {
  const hashed = await sha256(password);
  
  let currentUsers = SEED_USERS;
  
  let found = currentUsers.find(
    u => u.email.toLowerCase() === email.toLowerCase() && u.password === hashed
  );

  if (!found) {
    const plain = currentUsers.find(
      u => u.email.toLowerCase() === email.toLowerCase() && u.password === password
    );
    if (plain) {
      found = plain;
      console.log('Plain text matched (will be migrated):', plain.email);
    }
  }

  if (found) {
    console.log('Login Success for:', found.email);
    return true;
  }
  
  console.log('Login Failed');
  return false;
}

testLogin('admin@civic.gov', 'admin123');
testLogin('sysadmin@civic.gov', 'sysadmin123');
