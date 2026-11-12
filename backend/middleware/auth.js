const admin = require('firebase-admin');
const jwt = require('jsonwebtoken');
const User = require('../models/User');

let firebaseAdminReady = false;

try {
  if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
    if (!admin.apps.length) {
      admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
    }
    firebaseAdminReady = true;
    console.log('Firebase Admin SDK initialized successfully.');
  } else if (process.env.NODE_ENV !== 'test') {
    console.warn('FIREBASE_SERVICE_ACCOUNT_JSON is not configured; Firebase authentication is unavailable.');
  }
} catch (error) {
  console.error('Failed to initialize Firebase Admin SDK:', error.message);
}

function getJwtSecret() {
  if (process.env.JWT_SECRET) return process.env.JWT_SECRET;
  return process.env.NODE_ENV === 'test' ? 'fallback_secret' : null;
}

async function verifyFirebaseToken(req, res, next) {
  const match = /^Bearer\s+(\S+)$/i.exec(req.headers.authorization || '');
  if (!match) {
    return res.status(401).json({ error: 'Authorization header is missing or malformed. Use Bearer <Firebase_ID_Token>.' });
  }

  const token = match[1];

  if (!firebaseAdminReady) {
    if (process.env.NODE_ENV === 'test' && token.startsWith('mock_token_')) {
      const uid = token.slice('mock_token_'.length);
      if (!uid) return res.status(401).json({ error: 'Invalid test token.' });
      req.firebaseUser = {
        uid,
        email: `${uid}@example.com`,
        name: `Mock User ${uid}`,
      };
      return next();
    }
    return res.status(503).json({ error: 'Firebase authentication is not configured.' });
  }

  try {
    const decodedToken = await admin.auth().verifyIdToken(token);
    req.firebaseUser = {
      uid: decodedToken.uid,
      email: decodedToken.email,
      name: decodedToken.name || decodedToken.email || 'User',
    };
    return next();
  } catch (error) {
    console.warn('Firebase token verification failed:', error.message);
    return res.status(401).json({ error: 'Invalid Firebase ID token.' });
  }
}

async function authenticateJWT(req, res, next) {
  const match = /^Bearer\s+(\S+)$/i.exec(req.headers.authorization || '');
  if (!match) {
    return res.status(401).json({ error: 'Access token missing or malformed.' });
  }

  const secret = getJwtSecret();
  if (!secret) {
    return res.status(503).json({ error: 'Token authentication is not configured.' });
  }

  try {
    const decoded = jwt.verify(match[1], secret, { algorithms: ['HS256'] });
    if (!decoded || typeof decoded.userId !== 'string' || !decoded.userId) {
      return res.status(401).json({ error: 'Invalid access token.' });
    }

    const user = await User.findById(decoded.userId);
    if (!user) return res.status(401).json({ error: 'User session not found.' });

    req.user = user;
    return next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Access token expired.' });
    }
    console.warn('JWT verification failed:', error.message);
    return res.status(401).json({ error: 'Invalid access token.' });
  }
}

module.exports = {
  verifyFirebaseToken,
  authenticateJWT,
  getJwtSecret,
};
