import fs from 'fs';
import { initializeApp } from 'firebase/app';
import { getFirestore, collection, getDocs } from 'firebase/firestore';

const env = fs.readFileSync('.env', 'utf-8').split('\n').reduce((acc, line) => {
  const [key, val] = line.split('=');
  if (key) acc[key] = val;
  return acc;
}, {});

const firebaseConfig = {
  apiKey: env.VITE_FIREBASE_API_KEY,
  authDomain: env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: env.VITE_FIREBASE_APP_ID
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

async function run() {
  const qs = await getDocs(collection(db, 'issues'));
  let foundImage = false;
  qs.forEach(doc => {
    const data = doc.data();
    if (data.imageUrl) {
       console.log(`Issue: ${data.title} has imageUrl! Length: ${data.imageUrl.length}`);
       foundImage = true;
    }
  });
  if (!foundImage) console.log("NO issues have imageUrl in the database.");
  process.exit(0);
}
run();
