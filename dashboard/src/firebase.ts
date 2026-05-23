import { initializeApp, getApp, getApps } from "firebase/app";
import { 
  getAuth, 
  signInWithPopup, 
  GoogleAuthProvider, 
  signOut,
  User as FirebaseUser,
  onAuthStateChanged
} from "firebase/auth";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
};

let auth: any = null;
let isMockMode = false;

// Determine if we should fail over to Mock Authentication Mode
if (!firebaseConfig.apiKey || firebaseConfig.apiKey.includes('...')) {
  console.warn("Using simulated Admin Sandbox mode as Firebase parameters are empty.");
  isMockMode = true;
} else {
  try {
    const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();
    auth = getAuth(app);
  } catch (error) {
    console.warn("Firebase Auth failed to initial, fallback to Mock Mode activated.", error);
    isMockMode = true;
  }
}

// Custom structure for session mock updates
class MockAuth {
  private listeners: Array<(user: any) => void> = [];
  private currentUser: any = null;

  constructor() {
    // Check if there is an active mock session cached locally
    const cachedUser = localStorage.getItem("SSC_MOCK_ADMIN");
    if (cachedUser) {
      try {
        this.currentUser = JSON.parse(cachedUser);
      } catch (e) {
        this.currentUser = null;
      }
    }
  }

  onAuthStateChanged(callback: (user: any) => void) {
    this.listeners.push(callback);
    // Execute immediately
    callback(this.currentUser);
    return () => {
      this.listeners = this.listeners.filter(l => l !== callback);
    };
  }

  async signInWithGoogle() {
    const mockUser = {
      uid: "mock_admin_uid_999",
      email: "admin@smartcleaner.ai",
      displayName: "Ansil Admin Designer",
      photoURL: "https://api.dicebear.com/7.x/bottts/svg?seed=ansil",
      getIdToken: async () => "mock_token_admin_uid_999",
    };
    this.currentUser = mockUser;
    localStorage.setItem("SSC_MOCK_ADMIN", JSON.stringify(mockUser));
    this.notify();
    return mockUser;
  }

  async signOut() {
    this.currentUser = null;
    localStorage.removeItem("SSC_MOCK_ADMIN");
    this.notify();
  }

  private notify() {
    this.listeners.forEach(cb => cb(this.currentUser));
  }
}

const mockAuthInstance = new MockAuth();

export { isMockMode };

export async function loginWithGoogle() {
  if (isMockMode) {
    return await mockAuthInstance.signInWithGoogle();
  }
  const provider = new GoogleAuthProvider();
  const result = await signInWithPopup(auth, provider);
  return result.user;
}

export async function logoutUser() {
  if (isMockMode) {
    return await mockAuthInstance.signOut();
  }
  return await signOut(auth);
}

export function subscribeToAuthChanges(callback: (user: FirebaseUser | null) => void) {
  if (isMockMode) {
    return mockAuthInstance.onAuthStateChanged(callback);
  }
  return onAuthStateChanged(auth, callback);
}
