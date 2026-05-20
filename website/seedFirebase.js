import { initializeApp } from "firebase/app";
import { getFirestore, collection, getDocs, setDoc, doc } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "AIzaSyAIzevAbGzZzdODlxtXGhmLr61ee4m04Gc",
  authDomain: "web-app-e4d56.firebaseapp.com",
  projectId: "web-app-e4d56",
  storageBucket: "web-app-e4d56.firebasestorage.app",
  messagingSenderId: "369086481525",
  appId: "1:369086481525:web:f13473fa3b129ec118643c",
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

const SEED_ISSUES = [
  {
    id: 1,
    title: 'Pothole on Main St',
    category: 'Infrastructure',
    description: 'Large pothole causing traffic slowdowns and potential damage near the intersection.',
    status: 'open',
    reporter: 'Alice',
    reporterId: 'u1',
    createdAt: '2024-03-01',
    votes: ['u2', 'u3', 'u4'],
    comments: [],
    location: 'Main St & 4th Ave',
  },
  {
    id: 2,
    title: 'Broken Streetlight',
    category: 'Safety',
    description: 'Streetlight is out, making the crosswalk dangerous at night.',
    status: 'resolved',
    reporter: 'Bob',
    reporterId: 'u2',
    createdAt: '2024-03-05',
    votes: ['u1'],
    comments: [
      { id: 101, author: 'City Admin', authorId: 'admin1', text: 'Fixed on Mar 10.', date: '2024-03-10' }
    ],
    location: 'Oak St Park Entrance',
  },
  {
    id: 3,
    title: 'Graffiti on City Hall',
    category: 'Vandalism',
    description: 'Spray paint on the east wall.',
    status: 'in-progress',
    reporter: 'Charlie',
    reporterId: 'u3',
    createdAt: '2024-03-12',
    votes: ['u1', 'u2', 'u5'],
    comments: [],
    location: 'City Hall East Wall',
  }
];

async function seed() {
  try {
    const issuesSnapshot = await getDocs(collection(db, 'issues'));
    if (issuesSnapshot.empty) {
      console.log("Seeding Firestore with initial issues...");
      for (const issue of SEED_ISSUES) {
        await setDoc(doc(db, 'issues', String(issue.id)), issue);
      }
      console.log("Done seeding.");
    } else {
        console.log("Firestore already has issues.");
    }
  } catch (e) {
    console.error("Error connecting to Firestore:", e.message);
  }
}

seed();
