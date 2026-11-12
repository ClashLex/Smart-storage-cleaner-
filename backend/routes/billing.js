const express = require('express');
const { google } = require('googleapis');
const { OAuth2Client } = require('google-auth-library');
const User = require('../models/User');
const Subscription = require('../models/Subscription');
const { authenticateJWT } = require('../middleware/auth');

const router = express.Router();
const oauthClient = new OAuth2Client();
const mockBillingEnabled = process.env.NODE_ENV === 'test' ||
  (process.env.NODE_ENV !== 'production' && process.env.BILLING_MOCK_MODE === 'true');

let androidPublisher = null;
try {
  if (process.env.GOOGLE_PLAY_CLIENT_EMAIL && process.env.GOOGLE_PLAY_PRIVATE_KEY) {
    const auth = new google.auth.JWT(
      process.env.GOOGLE_PLAY_CLIENT_EMAIL,
      null,
      process.env.GOOGLE_PLAY_PRIVATE_KEY.replace(/\\n/g, '\n'),
      ['https://www.googleapis.com/auth/androidpublisher']
    );
    androidPublisher = google.androidpublisher({ version: 'v3', auth });
  }
} catch (error) {
  console.error('Failed to initialize Google Play API client:', error.message);
}

async function verifyPubSubIdentity(req) {
  if (process.env.NODE_ENV === 'test') return true;
  const audience = process.env.PUBSUB_OIDC_AUDIENCE;
  const expectedEmail = process.env.PUBSUB_OIDC_EMAIL;
  const authorization = req.headers.authorization || '';
  const match = /^Bearer\s+(\S+)$/i.exec(authorization);
  if (!audience || !expectedEmail || !match) return false;

  try {
    const ticket = await oauthClient.verifyIdToken({ idToken: match[1], audience });
    const payload = ticket.getPayload();
    return Boolean(payload && payload.email === expectedEmail && payload.email_verified);
  } catch (error) {
    console.warn('Pub/Sub OIDC verification failed:', error.message);
    return false;
  }
}

router.post('/verify', authenticateJWT, async (req, res) => {
  const { productId, purchaseToken } = req.body || {};
  if (typeof productId !== 'string' || !productId.trim() || productId.length > 128 ||
      typeof purchaseToken !== 'string' || !purchaseToken.trim() || purchaseToken.length > 4096) {
    return res.status(400).json({ error: 'A valid productId and purchaseToken are required.' });
  }

  if (!androidPublisher && !mockBillingEnabled) {
    return res.status(503).json({ error: 'Google Play purchase verification is not configured.' });
  }

  try {
    const existingSub = await Subscription.findOne({ purchaseToken });
    if (existingSub) {
      if (existingSub.userId.toString() === req.user._id.toString()) {
        return res.json({
          success: true,
          message: 'Subscription already verified and linked to your account.',
          entitlement: {
            premiumEntitled: req.user.premiumEntitled,
            expiryTime: existingSub.expiryTime,
          },
        });
      }
      return res.status(409).json({ error: 'This purchase token has already been validated on another account.' });
    }

    let status = 'active';
    let expiryTime;
    let orderId;
    let rawVerificationData;

    if (androidPublisher) {
      const packageName = process.env.GOOGLE_PLAY_PACKAGE_NAME;
      if (!packageName) return res.status(503).json({ error: 'Google Play package name is not configured.' });

      const response = await androidPublisher.purchases.subscriptions.get({
        packageName,
        subscriptionId: productId,
        token: purchaseToken,
      });
      rawVerificationData = response.data;
      orderId = response.data.orderId;
      const expiryMillis = Number(response.data.expiryTimeMillis);
      if (!Number.isFinite(expiryMillis) || expiryMillis <= 0) {
        return res.status(400).json({ error: 'Google Play returned an invalid subscription expiry.' });
      }
      expiryTime = new Date(expiryMillis);
      if (expiryMillis <= Date.now()) status = 'expired';
      else if (response.data.paymentState === 0) status = 'pending';
      else if (response.data.cancelReason !== undefined && response.data.cancelReason !== null) status = 'cancelled';
    } else {
      // Explicit test-only simulation; never grants premium in production.
      expiryTime = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
      orderId = `GPA.test-${Date.now()}`;
      rawVerificationData = { test: true };
      if (purchaseToken.includes('expired')) {
        status = 'expired';
        expiryTime = new Date(Date.now() - 10000);
      }
    }

    const subscription = new Subscription({
      userId: req.user._id,
      orderId,
      purchaseToken,
      productId,
      status,
      expiryTime,
      rawVerificationData,
    });
    await subscription.save();

    if (status === 'active' && expiryTime > new Date()) {
      req.user.premiumEntitled = true;
      await req.user.save();
    }

    return res.json({
      success: true,
      subscription: { orderId, productId, status, expiryTime },
      entitlement: { premiumEntitled: req.user.premiumEntitled },
    });
  } catch (error) {
    if (error.code === 11000) return res.status(409).json({ error: 'This purchase token has already been claimed.' });
    console.error('Error during billing verification:', error.message);
    return res.status(400).json({ error: 'Google Play purchase verification failed.' });
  }
});

router.get('/entitlement', authenticateJWT, async (req, res) => {
  try {
    const latestActiveSub = await Subscription.findOne({
      userId: req.user._id,
      status: 'active',
      expiryTime: { $gt: new Date() },
    }).sort({ expiryTime: -1 });

    const isEntitled = Boolean(latestActiveSub) || req.user.premiumEntitled === true;
    if (req.user.premiumEntitled !== isEntitled) {
      req.user.premiumEntitled = isEntitled;
      await req.user.save();
    }

    return res.json({
      premiumEntitled: isEntitled,
      subscription: latestActiveSub ? {
        productId: latestActiveSub.productId,
        orderId: latestActiveSub.orderId,
        expiryTime: latestActiveSub.expiryTime,
      } : null,
    });
  } catch (error) {
    console.error('Error getting subscription entitlement:', error.message);
    return res.status(500).json({ error: 'Failed to load entitlement status.' });
  }
});

router.post('/webhook', async (req, res) => {
  if (!(await verifyPubSubIdentity(req))) {
    return res.status(401).json({ error: 'Unauthorized Pub/Sub notification.' });
  }

  const { message } = req.body || {};
  if (!message || typeof message.data !== 'string' || message.data.length > 100000) {
    return res.status(400).json({ error: 'Missing or invalid Pub/Sub message envelope.' });
  }

  try {
    const rawData = Buffer.from(message.data, 'base64').toString('utf8');
    const notification = JSON.parse(rawData);
    const event = notification.subscriptionNotification;
    if (!event) return res.status(200).json({ message: 'Notification category acknowledged.' });

    const { purchaseToken, subscriptionId, notificationType } = event;
    if (typeof purchaseToken !== 'string' || !purchaseToken) {
      return res.status(400).json({ error: 'Notification is missing purchaseToken.' });
    }

    const sub = await Subscription.findOne({ purchaseToken });
    if (!sub) return res.status(200).json({ message: 'Unknown purchase token acknowledged.' });

    let status = sub.status;
    let expiryTime = sub.expiryTime;
    if ([1, 2, 4, 7].includes(notificationType)) {
      if (androidPublisher) {
        const response = await androidPublisher.purchases.subscriptions.get({
          packageName: process.env.GOOGLE_PLAY_PACKAGE_NAME,
          subscriptionId,
          token: purchaseToken,
        });
        expiryTime = new Date(Number(response.data.expiryTimeMillis));
        status = expiryTime > new Date() ? 'active' : 'expired';
      } else if (mockBillingEnabled && notificationType === 2) {
        expiryTime = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
        status = 'active';
      }
    } else if (notificationType === 3) {
      status = 'cancelled';
    } else if ([5, 13].includes(notificationType)) {
      status = notificationType === 13 ? 'refunded' : 'expired';
    }

    sub.status = status;
    sub.expiryTime = expiryTime;
    await sub.save();

    const user = await User.findById(sub.userId);
    if (user) {
      const activeCount = await Subscription.countDocuments({
        userId: user._id,
        status: 'active',
        expiryTime: { $gt: new Date() },
      });
      user.premiumEntitled = activeCount > 0;
      await user.save();
    }
    return res.status(200).json({ success: true, message: 'Notification processed.' });
  } catch (error) {
    console.error('Error processing RTDN webhook:', error.message);
    return res.status(500).json({ error: 'Failed to process notification.' });
  }
});

module.exports = router;
