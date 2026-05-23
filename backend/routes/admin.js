const express = require('express');
const User = require('../models/User');
const ScanSession = require('../models/ScanSession');
const CleanupLog = require('../models/CleanupLog');
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

module.exports = router;
