const express = require('express');
const CleanupLog = require('../models/CleanupLog');
const { authenticateJWT } = require('../middleware/auth');

const router = express.Router();

router.post('/log', authenticateJWT, async (req, res) => {
  const { category, bytesFreed } = req.body || {};
  if (typeof category !== 'string' || !category.trim() || category.trim().length > 64 ||
      !Number.isSafeInteger(bytesFreed) || bytesFreed < 0) {
    return res.status(400).json({ error: 'Requires a category (1-64 characters) and a non-negative safe-integer bytesFreed value.' });
  }

  try {
    const log = new CleanupLog({
      userId: req.user._id,
      category: category.trim(),
      bytesFreed,
    });
    const savedLog = await log.save();
    return res.status(201).json({
      success: true,
      log: {
        id: savedLog?._id || log._id,
        category: savedLog?.category || log.category,
        bytesFreed: savedLog?.bytesFreed ?? log.bytesFreed,
        timestamp: savedLog?.createdAt || log.createdAt,
      },
    });
  } catch (error) {
    console.error('Error logging cleanup data:', error.message);
    return res.status(500).json({ error: 'Failed to record cleanup log metrics.' });
  }
});

module.exports = router;
