import { initializeApp, getApp, getApps } from 'firebase/app';
import {
  getAuth,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  User as FirebaseUser,
  onAuthStateChanged,
} from 'firebase/auth';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

const isConfigured = Boolean(
  firebaseConfig.apiKey &&
  firebaseConfig.authDomain &&
  firebaseConfig.projectId &&
  firebaseConfig.appId &&
  !firebaseConfig.apiKey.includes('...')
);

let auth: ReturnType<typeof getAuth> | null = null;
let initializationError: string | null = null;

if (isConfigured) {
  try {
    const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();
    auth = getAuth(app);
  } catch (error) {
    initializationError = error instanceof Error ? error.message : 'Firebase initialization failed.';
  }
}

export const isMockMode = false;

function requireAuth() {
  if (!auth) {
    throw new Error(initializationError || 'Firebase configuration is missing. Configure the VITE_FIREBASE_* variables.');
  }
  return auth;
}

export async function loginWithGoogle(): Promise<FirebaseUser> {
  const provider = new GoogleAuthProvider();
  const result = await signInWithPopup(requireAuth(), provider);
  return result.user;
}

export async function logoutUser(): Promise<void> {
  await signOut(requireAuth());
}

export function subscribeToAuthChanges(callback: (user: FirebaseUser | null) => void) {
  if (!auth) {
    callback(null);
    return () => undefined;
  }
  return onAuthStateChanged(auth, callback);
}
