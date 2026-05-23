const express = require('express');
const CleanupLog = require('../models/CleanupLog');
const { authenticateJWT } = require('../middleware/auth');

const router = express.Router();

/**
 * POST /api/cleanup/log
 * Log bytes freed from a successful storage cleanup.
 */
router.post('/log', authenticateJWT, async (req, res) => {
  const { category, bytesFreed } = req.body;

  if (!category || typeof bytesFreed !== 'number' || bytesFreed < 0) {
    return res.status(400).json({ error: 'Invalid parameters. Requires a valid category and a non-negative bytesFreed number.' });
  }

  try {
    const log = new CleanupLog({
      userId: req.user._id,
      category,
      bytesFreed,
    });

    await log.save();

    res.status(201).json({
      success: true,
      log: {
        id: log._id,
        category: log.category,
        bytesFreed: log.bytesFreed,
        timestamp: log.createdAt,
      },
    });
  } catch (error) {
    console.error('Error logging cleanup data:', error);
    res.status(500).json({ error: 'Failed to record cleanup log metrics.' });
  }
});

module.exports = router;
