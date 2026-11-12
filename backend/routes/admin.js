const express = require('express');
const mongoose = require('mongoose');
const User = require('../models/User');
const ScanSession = require('../models/ScanSession');
const CleanupLog = require('../models/CleanupLog');
const Subscription = require('../models/Subscription');
const { authenticateJWT } = require('../middleware/auth');
const { isAdmin } = require('../middleware/admin');

const router = express.Router();
router.use(authenticateJWT, isAdmin);

function validUserId(id) {
  return mongoose.isValidObjectId(id);
}

router.get('/overview', async (req, res) => {
  try {
    const [totalUsers, premiumUsers, totalScansCount, bytesFreedStats] = await Promise.all([
      User.countDocuments(),
      User.countDocuments({ premiumEntitled: true }),
      ScanSession.countDocuments(),
      CleanupLog.aggregate([
        { $group: { _id: '$category', totalBytes: { $sum: '$bytesFreed' }, count: { $sum: 1 } } },
      ]),
    ]);
    const breakdown = bytesFreedStats.map((item) => ({
      category: item._id,
      bytesFreed: item.totalBytes,
      actionsLogged: item.count,
    }));
    const cumulativeBytesFreed = breakdown.reduce((sum, item) => sum + item.bytesFreed, 0);
    res.json({
      overview: {
        users: { total: totalUsers, premium: premiumUsers, free: totalUsers - premiumUsers },
        scans: { totalSessions: totalScansCount },
        cleanups: {
          totalActions: breakdown.reduce((sum, item) => sum + item.actionsLogged, 0),
          cumulativeBytesFreed,
          breakdown,
        },
      },
    });
  } catch (error) {
    console.error('Admin overview failed:', error.message);
    res.status(500).json({ error: 'Failed to calculate platform metrics.' });
  }
});

router.get('/users', async (req, res) => {
  try {
    const page = Math.max(1, Number.parseInt(req.query.page, 10) || 1);
    const limit = Math.min(100, Math.max(1, Number.parseInt(req.query.limit, 10) || 25));
    const [users, total] = await Promise.all([
      User.find().sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit).lean(),
      User.countDocuments(),
    ]);
    const ids = users.map((user) => user._id);
    const logs = await CleanupLog.aggregate([
      { $match: { userId: { $in: ids } } },
      { $group: { _id: '$userId', totalBytesFreed: { $sum: '$bytesFreed' } } },
    ]);
    const freedByUser = new Map(logs.map((item) => [item._id.toString(), item.totalBytesFreed]));
    const records = users.map((user) => ({
      id: user._id.toString(),
      name: user.displayName || user.email?.split('@')[0] || 'Unknown user',
      email: user.email || '',
      premiumEntitled: Boolean(user.premiumEntitled),
      gbFreed: Number(((freedByUser.get(user._id.toString()) || 0) / (1024 ** 3)).toFixed(2)),
      lastSeen: user.updatedAt ? user.updatedAt.toISOString().slice(0, 16).replace('T', ' ') : 'N/A',
      createdDate: user.createdAt ? user.createdAt.toISOString().slice(0, 10) : 'N/A',
      hasAdminPrivilege: user.role === 'admin',
    }));
    res.json({ users: records, pagination: { page, limit, total, pages: Math.ceil(total / limit) } });
  } catch (error) {
    console.error('Admin users query failed:', error.message);
    res.status(500).json({ error: 'Failed to retrieve user records.' });
  }
});

router.get('/users/:id', async (req, res) => {
  if (!validUserId(req.params.id)) return res.status(400).json({ error: 'Invalid user ID.' });
  try {
    const user = await User.findById(req.params.id).lean();
    if (!user) return res.status(404).json({ error: 'User not found.' });
    const [subscriptions, logs] = await Promise.all([
      Subscription.find({ userId: user._id }).sort({ createdAt: -1 }).lean(),
      CleanupLog.aggregate([
        { $match: { userId: user._id } },
        { $group: { _id: null, totalBytesFreed: { $sum: '$bytesFreed' } } },
      ]),
    ]);
    const totalBytesFreed = logs[0]?.totalBytesFreed || 0;
    res.json({
      user: {
        _id: user._id,
        email: user.email || '',
        displayName: user.displayName || '',
        premiumEntitled: Boolean(user.premiumEntitled),
        role: user.role,
        gbFreed: Number((totalBytesFreed / (1024 ** 3)).toFixed(2)),
        createdAt: user.createdAt,
        updatedAt: user.updatedAt,
      },
      subscriptions: subscriptions.map((sub) => ({
        id: sub._id.toString(),
        productId: sub.productId,
        orderId: sub.orderId || '',
        status: sub.status,
        purchaseDate: sub.createdAt?.toISOString().slice(0, 10) || '',
        expiryDate: sub.expiryTime?.toISOString().slice(0, 10) || '',
      })),
    });
  } catch (error) {
    console.error('Admin user detail failed:', error.message);
    res.status(500).json({ error: 'Failed to retrieve user detail.' });
  }
});

router.post('/users/:id/comp-premium', async (req, res) => {
  if (!validUserId(req.params.id)) return res.status(400).json({ error: 'Invalid user ID.' });
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ error: 'User not found.' });
    user.premiumEntitled = !user.premiumEntitled;
    await user.save();
    res.json({ success: true, premiumEntitled: user.premiumEntitled });
  } catch (error) {
    console.error('Premium entitlement update failed:', error.message);
    res.status(500).json({ error: 'Failed to update premium authorization.' });
  }
});

router.post('/users/:id/grant-admin', async (req, res) => {
  if (!validUserId(req.params.id)) return res.status(400).json({ error: 'Invalid user ID.' });
  if (req.params.id === req.user._id.toString()) {
    return res.status(400).json({ error: 'Administrators cannot change their own role.' });
  }
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ error: 'User not found.' });
    user.role = user.role === 'admin' ? 'user' : 'admin';
    await user.save();
    res.json({ success: true, role: user.role });
  } catch (error) {
    console.error('Admin role update failed:', error.message);
    res.status(500).json({ error: 'Failed to update administrator authorization.' });
  }
});

router.delete('/users/:id', async (req, res) => {
  if (!validUserId(req.params.id)) return res.status(400).json({ error: 'Invalid user ID.' });
  if (req.params.id === req.user._id.toString()) {
    return res.status(400).json({ error: 'Administrators cannot delete their own account from this endpoint.' });
  }
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ error: 'User not found.' });
    await Promise.all([
      Subscription.deleteMany({ userId: user._id }),
      ScanSession.deleteMany({ userId: user._id }),
      CleanupLog.deleteMany({ userId: user._id }),
      User.deleteOne({ _id: user._id }),
    ]);
    res.json({ success: true });
  } catch (error) {
    console.error('Admin user deletion failed:', error.message);
    res.status(500).json({ error: 'Failed to purge user data.' });
  }
});

router.get('/subscriptions', async (req, res) => {
  try {
    const page = Math.max(1, Number.parseInt(req.query.page, 10) || 1);
    const limit = Math.min(100, Math.max(1, Number.parseInt(req.query.limit, 10) || 25));
    const [subs, total] = await Promise.all([
      Subscription.find().populate('userId', 'email').sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit),
      Subscription.countDocuments(),
    ]);
    res.json({
      subscriptions: subs.map((sub) => ({
        _id: sub._id.toString(),
        userId: sub.userId?._id?.toString() || '',
        email: sub.userId?.email || '',
        productId: sub.productId,
        orderId: sub.orderId || '',
        status: sub.status,
        expiryTime: sub.expiryTime?.toISOString().slice(0, 16).replace('T', ' ') || 'N/A',
        createdAt: sub.createdAt?.toISOString().slice(0, 10) || 'N/A',
      })),
      pagination: { page, limit, total, pages: Math.ceil(total / limit) },
    });
  } catch (error) {
    console.error('Admin subscriptions query failed:', error.message);
    res.status(500).json({ error: 'Failed to retrieve subscription ledger.' });
  }
});

module.exports = router;
