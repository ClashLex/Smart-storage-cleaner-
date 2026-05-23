const express = require('express');
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const Subscription = require('../models/Subscription');
const ScanSession = require('../models/ScanSession');
const CleanupLog = require('../models/CleanupLog');
const { verifyFirebaseToken, authenticateJWT } = require('../middleware/auth');

const router = express.Router();

/**
 * POST /api/auth/sync
 * Verifies Firebase ID Token, upserts user in MongoDB, and signs / returns an app-specific JWT.
 */
router.post('/sync', verifyFirebaseToken, async (req, res) => {
  const { uid, email, name } = req.firebaseUser;

  try {
    let user = await User.findOne({ firebaseUid: uid });

    if (!user) {
      user = new User({
        firebaseUid: uid,
        email: email,
        displayName: name,
        premiumEntitled: false,
      });
      await user.save();
    } else {
      // Keep DB synchronized with updated auth information
      user.email = email || user.email;
      user.displayName = name || user.displayName;
      await user.save();
    }

    // Sign Application JWT
    const secret = process.env.JWT_SECRET || 'fallback_secret';
    const token = jwt.sign(
      { userId: user._id, firebaseUid: user.firebaseUid, role: user.role },
      secret,
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );

    res.json({
      token,
      user: {
        id: user._id,
        firebaseUid: user.firebaseUid,
        email: user.email,
        displayName: user.displayName,
        premiumEntitled: user.premiumEntitled,
        role: user.role,
      },
    });
  } catch (error) {
    console.error('Error synchronizing user:', error);
    res.status(500).json({ error: 'Failed to synchronize account status' });
  }
});

/**
 * GET /api/auth/me
 * Retrieves current user profile details.
 */
router.get('/me', authenticateJWT, async (req, res) => {
  res.json({
    id: req.user._id,
    firebaseUid: req.user.firebaseUid,
    email: req.user.email,
    displayName: req.user.displayName,
    premiumEntitled: req.user.premiumEntitled,
    role: req.user.role,
    createdAt: req.user.createdAt,
  });
});

/**
 * DELETE /api/auth/me
 * Deletes user account and cascade-deletes associated subscriptions, scans, and cleanup logs for Play Store compliance.
 */
router.delete('/me', authenticateJWT, async (req, res) => {
  const userId = req.user._id;

  try {
    // Cascade delete across all related collections
    await Promise.all([
      Subscription.deleteMany({ userId }),
      ScanSession.deleteMany({ userId }),
      CleanupLog.deleteMany({ userId }),
      User.findByIdAndDelete(userId),
    ]);

    res.json({ success: true, message: 'Account and associated storage metrics purged successfully' });
  } catch (error) {
    console.error('Error deleting user account:', error);
    res.status(500).json({ error: 'Failed to delete account completely' });
  }
});

module.exports = router;
