const express = require('express');
const User = require('../models/User');
const ScanSession = require('../models/ScanSession');
const CleanupLog = require('../models/CleanupLog');
const Subscription = require('../models/Subscription');
const { authenticateJWT } = require('../middleware/auth');
const { isAdmin } = require('../middleware/admin');

const router = express.Router();

/**
 * GET /api/admin/overview
 * Analytical calculations to display key system stats. Protected for Admin role.
 */
router.get('/overview', authenticateJWT, isAdmin, async (req, res) => {
  try {
    // 1. Basic Counts
    const [totalUsers, premiumUsers, totalScansCount] = await Promise.all([
      User.countDocuments(),
      User.countDocuments({ premiumEntitled: true }),
      ScanSession.countDocuments(),
    ]);

    // 2. Mongoose Aggregation for total and category-wise bytes freed
    const bytesFreedStats = await CleanupLog.aggregate([
      {
        $group: {
          _id: '$category',
          totalBytes: { $sum: '$bytesFreed' },
          count: { $sum: 1 },
        },
      },
    ]);

    // Format aggregation output
    let totalBytesFreedSum = 0;
    const items = bytesFreedStats.map(stat => {
      totalBytesFreedSum += stat.totalBytes;
      return {
        category: stat._id,
        bytesFreed: stat.totalBytes,
        actionsLogged: stat.count,
      };
    });

    res.json({
      overview: {
        users: {
          total: totalUsers,
          premium: premiumUsers,
          free: totalUsers - premiumUsers,
        },
        scans: {
          totalSessions: totalScansCount,
        },
        cleanups: {
          totalActions: items.reduce((acc, current) => acc + current.actionsLogged, 0),
          cumulativeBytesFreed: totalBytesFreedSum,
          breakdown: items,
        },
      },
    });
  } catch (error) {
    console.error('Core analytics calculation failed:', error);
    res.status(500).json({ error: 'Failed to calculate platform metrics metrics analytics.' });
  }
});

/**
 * GET /api/admin/users
 * Lists all registered users with consolidated metrics (bytes freed, tenure dates).
 */
router.get('/users', authenticateJWT, isAdmin, async (req, res) => {
  try {
    const users = await User.find().sort({ createdAt: -1 });
    
    const userRecords = await Promise.all(users.map(async (u) => {
      const logs = await CleanupLog.find({ userId: u._id });
      const totalBytesFreed = logs.reduce((sum, log) => sum + log.bytesFreed, 0);
      const gbFreed = totalBytesFreed / (1024 * 1024 * 1024); // GB
      
      return {
        id: u._id.toString(),
        name: u.displayName || u.email.split('@')[0],
        email: u.email,
        premiumEntitled: u.premiumEntitled,
        gbFreed: parseFloat(gbFreed.toFixed(2)),
        lastSeen: u.updatedAt ? new Date(u.updatedAt).toISOString().slice(0, 16).replace('T', ' ') : 'N/A',
        createdDate: u.createdAt ? new Date(u.createdAt).toISOString().slice(0, 10) : 'N/A',
        hasAdminPrivilege: u.role === 'admin'
      };
    }));

    res.json({ users: userRecords });
  } catch (error) {
    console.error('Error fetching admin users:', error);
    res.status(500).json({ error: 'Failed to retrieve active user records.' });
  }
});

/**
 * GET /api/admin/users/:id
 * Detailed review of user properties and complete ledger subscription actions.
 */
router.get('/users/:id', authenticateJWT, isAdmin, async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    const subs = await Subscription.find({ userId: user._id }).sort({ createdAt: -1 });
    const formattedSubs = subs.map(s => ({
      id: s._id.toString(),
      productId: s.productId,
      orderId: s.orderId || 'GPA.sandbox-bypass',
      status: s.status,
      purchaseDate: new Date(s.createdAt).toISOString().slice(0, 10),
      expiryDate: new Date(s.expiryTime).toISOString().slice(0, 10)
    }));

    res.json({
      user: {
        _id: user._id,
        email: user.email,
        displayName: user.displayName,
        premiumEntitled: user.premiumEntitled,
        role: user.role,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt
      },
      subscriptions: formattedSubs
    });
  } catch (error) {
    console.error('Error fetching admin user detail:', error);
    res.status(500).json({ error: 'Failed to retrieve user detail.' });
  }
});

/**
 * POST /api/admin/users/:id/comp-premium
 * Entitles or revokes complimentary premium credentials.
 */
router.post('/users/:id/comp-premium', authenticateJWT, isAdmin, async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    user.premiumEntitled = !user.premiumEntitled;
    await user.save();
    
    res.json({ success: true, premiumEntitled: user.premiumEntitled });
  } catch (error) {
    console.error('Error toggling premium status:', error);
    res.status(500).json({ error: 'Failed to update premium authorization.' });
  }
});

/**
 * POST /api/admin/users/:id/grant-admin
 * Elevates or demotes user administrative security roles.
 */
router.post('/users/:id/grant-admin', authenticateJWT, isAdmin, async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    user.role = user.role === 'admin' ? 'user' : 'admin';
    await user.save();
    
    res.json({ success: true, role: user.role });
  } catch (error) {
    console.error('Error toggling admin privilege:', error);
    res.status(500).json({ error: 'Failed to update user security authorization.' });
  }
});

/**
 * DELETE /api/admin/users/:id
 * Wipes out account subscription maps, logs, indexing stats, and user records (cascade-delete).
 */
router.delete('/users/:id', authenticateJWT, isAdmin, async (req, res) => {
  try {
    const userId = req.params.id;
    
    await Promise.all([
      Subscription.deleteMany({ userId }),
      ScanSession.deleteMany({ userId }),
      CleanupLog.deleteMany({ userId }),
      User.findByIdAndDelete(userId),
    ]);
    
    res.json({ success: true });
  } catch (error) {
    console.error('Error deleting user:', error);
    res.status(500).json({ error: 'Failed to purge user completely.' });
  }
});

/**
 * GET /api/admin/subscriptions
 * Retrieves all registered subscriptions in the system.
 */
router.get('/subscriptions', authenticateJWT, isAdmin, async (req, res) => {
  try {
    const subs = await Subscription.find().populate('userId').sort({ createdAt: -1 });
    
    const formatted = subs.map(s => {
      return {
        _id: s._id.toString(),
        userId: s.userId ? s.userId._id.toString() : 'sandbox_bypass',
        email: s.userId ? s.userId.email : 'sandbox@example.com',
        productId: s.productId,
        orderId: s.orderId || 'GPA.sandbox-bypass',
        status: s.status,
        expiryTime: s.expiryTime ? new Date(s.expiryTime).toISOString().slice(0, 16).replace('T', ' ') : 'N/A',
        createdAt: s.createdAt ? new Date(s.createdAt).toISOString().slice(0, 10) : 'N/A'
      };
    });

    res.json({ subscriptions: formatted });
  } catch (error) {
    console.error('Error fetching admin subscriptions:', error);
    res.status(500).json({ error: 'Failed to retrieve subscription ledger.' });
  }
});

module.exports = router;
