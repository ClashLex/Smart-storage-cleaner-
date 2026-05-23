const admin = require('firebase-admin');
const jwt = require('jsonwebtoken');
const User = require('../models/User');

// Initialize Firebase Admin SDK if service account is provided in environment
let firebaseAdminReady = false;
try {
  if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    firebaseAdminReady = true;
    console.log('Firebase Admin SDK initialized successfully.');
  } else {
    console.warn('FIREBASE_SERVICE_ACCOUNT_JSON is missing. Firebase auth-sync token validation will run in mock mode.');
  }
} catch (error) {
  console.error('Failed to initialize Firebase Admin SDK:', error.message);
}

/**
 * Middleware to verify a Google/Firebase ID Token (Bearer)
 */
async function verifyFirebaseToken(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authorization header is missing or malformed. Use Bearer <Firebase_ID_Token>' });
  }

  const token = authHeader.split(' ')[1];

  try {
    if (firebaseAdminReady) {
      const decodedToken = await admin.auth().verifyIdToken(token);
      req.firebaseUser = {
        uid: decodedToken.uid,
        email: decodedToken.email,
        name: decodedToken.name || decodedToken.email,
      };
      return next();
    } else {
      // Stub/Mock mode for local / testing when Firebase service account is absent
      if (process.env.NODE_ENV === 'test' || !process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
        // Simple JWT or mock token decoding for tests
        // Let's check if the token starts with "mock_token_"
        if (token.startsWith('mock_token_')) {
          const uid = token.replace('mock_token_', '');
          req.firebaseUser = {
            uid: uid,
            email: `${uid}@example.com`,
            name: `Mock User ${uid}`
          };
          return next();
        }
      }
      return res.status(503).json({ error: 'Firebase Auth service is unavailable' });
    }
  } catch (error) {
    console.error('Firebase Token Verification Failed:', error);
    return res.status(401).json({ error: 'Invalid Firebase ID token', details: error.message });
  }
}

/**
 * Middleware to authenticate app-specific JWT
 */
async function authenticateJWT(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Access token missing or malformed' });
  }

  const token = authHeader.split(' ')[1];

  try {
    const secret = process.env.JWT_SECRET || 'fallback_secret';
    const decoded = jwt.verify(token, secret);
    
    // Find absolute user from database
    const user = await User.findById(decoded.userId);
    if (!user) {
      return res.status(401).json({ error: 'User session not found' });
    }

    req.user = user;
    return next();
  } catch (error) {
    console.error('JWT Verification Failed:', error.message);
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Access token expired' });
    }
    return res.status(401).json({ error: 'Invalid access token' });
  }
}

module.exports = {
  verifyFirebaseToken,
  authenticateJWT,
};
