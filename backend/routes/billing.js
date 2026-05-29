const express = require('express');
const { google } = require('googleapis');
const User = require('../models/User');
const Subscription = require('../models/Subscription');
const { authenticateJWT } = require('../middleware/auth');

const router = express.Router();

// Initialize Android Publisher client if credentials are configured
let androidPublisher = null;
try {
  if (process.env.GOOGLE_PLAY_CLIENT_EMAIL && process.env.GOOGLE_PLAY_PRIVATE_KEY) {
    const auth = new google.auth.JWT(
      process.env.GOOGLE_PLAY_CLIENT_EMAIL,
      null,
      process.env.GOOGLE_PLAY_PRIVATE_KEY.replace(/\\n/g, '\n'),
      ['https://www.googleapis.com/auth/androidpublisher']
    );
    androidPublisher = google.androidpublisher({
      version: 'v3',
      auth,
    });
    console.log('Google Play Developer API client initialized successfully.');
  } else {
    console.warn('Google Play API credentials missing. Play Store purchases will verify in Mock/Sandbox mode.');
  }
} catch (error) {
  console.error('Failed to initialize Google Play API client:', error.message);
}

/**
 * POST /api/billing/verify
 * Verifies purchaseToken using Google Play Console Developer API v3.
 * Prevents replay attacks because purchaseToken is guarded by a unique index schema.
 */
router.post('/verify', authenticateJWT, async (req, res) => {
  const { productId, purchaseToken } = req.body;

  if (!productId || !purchaseToken) {
    return res.status(400).json({ error: 'productId and purchaseToken are required parameters.' });
  }

  try {
    // 1. Check if the purchaseToken was already processed to secure replay prevention
    const existingSub = await Subscription.findOne({ purchaseToken });
    if (existingSub) {
      // Re-link if it belongs to current user, else reject
      if (existingSub.userId.toString() === req.user._id.toString()) {
        return res.json({
          success: true,
          message: 'Subscription already verified and linked to your account.',
          entitlement: {
            premiumEntitled: req.user.premiumEntitled,
            expiryTime: existingSub.expiryTime,
          }
        });
      }
      return res.status(409).json({ error: 'This purchase token has already been validated on another account.' });
    }

    let status = 'active';
    let expiryTime = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // Default 30 days
    let orderId = `GPA.mock-${Math.floor(Math.random() * 1000000000)}`;
    let rawVerificationData = { sandbox: true };

    // 2. Query Play Store if API is active
    if (androidPublisher) {
      try {
        const packageName = process.env.GOOGLE_PLAY_PACKAGE_NAME || 'com.aistudio.smartcleaner';
        const response = await androidPublisher.purchases.subscriptions.get({
          packageName,
          subscriptionId: productId,
          token: purchaseToken,
        });

        rawVerificationData = response.data;
        orderId = response.data.orderId || orderId;
        expiryTime = new Date(parseInt(response.data.expiryTimeMillis, 10));

        // Evaluate state
        // PaymentState: 0 (Pending), 1 (Received), 2 (Free trial)
        const paymentState = response.data.paymentState;
        const expiryDateMillis = parseInt(response.data.expiryTimeMillis, 10);

        if (expiryDateMillis < Date.now()) {
          status = 'expired';
        } else if (paymentState === 0) {
          status = 'pending';
        }
      } catch (playError) {
        console.error('Play Store verification network error:', playError.message);
        return res.status(400).json({ error: 'Google Play Store failed to verify purchase token.', details: playError.message });
      }
    } else {
      // Direct mock fallback for local sandboxes
      // If client provides mock_expired_token, mock expiration
      if (purchaseToken.includes('expired')) {
        status = 'expired';
        expiryTime = new Date(Date.now() - 10000);
      }
    }

    // 3. Save subscription details in MongoDB
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

    // 4. Update user entitlement if the subscription is active
    if (status === 'active' && expiryTime > new Date()) {
      req.user.premiumEntitled = true;
      await req.user.save();
    }

    res.json({
      success: true,
      subscription: {
        orderId,
        productId,
        status,
        expiryTime,
      },
      entitlement: {
        premiumEntitled: req.user.premiumEntitled,
      }
    });

  } catch (error) {
    if (error.code === 11000) {
      return res.status(409).json({ error: 'This purchase token has already been claimed.' });
    }
    console.error('Error during billing verification:', error);
    res.status(500).json({ error: 'Failed to complete billing verification.' });
  }
});

/**
 * GET /api/billing/entitlement
 * Returns current premium subscription state for the user
 */
router.get('/entitlement', authenticateJWT, async (req, res) => {
  try {
    // Audit current active subscription
    const latestActiveSub = await Subscription.findOne({
      userId: req.user._id,
      status: 'active',
      expiryTime: { $gt: new Date() }
    }).sort({ expiryTime: -1 });

    // Entitled if they have an active Play Store subscription, or if marked premiumEntitled (e.g. comp/sandbox) directly in profile
    const isEntitled = !!latestActiveSub || req.user.premiumEntitled === true;

    // Synchronize DB user entitlement state just in case
    if (req.user.premiumEntitled !== isEntitled) {
      req.user.premiumEntitled = isEntitled;
      await req.user.save();
    }

    res.json({
      premiumEntitled: isEntitled,
      subscription: latestActiveSub ? {
        productId: latestActiveSub.productId,
        orderId: latestActiveSub.orderId,
        expiryTime: latestActiveSub.expiryTime,
      } : null
    });
  } catch (error) {
    console.error('Error getting subscription entitlement:', error);
    res.status(500).json({ error: 'Failed to load entitlement status' });
  }
});

/**
 * POST /api/billing/webhook
 * Receives Real-Time Developer Notifications (RTDN) from Google Play Console (via Pub/Sub)
 */
router.post('/webhook', async (req, res) => {
  // Pull push notifications from PubSub payload
  const { message } = req.body;
  if (!message || !message.data) {
    return res.status(400).json({ error: 'Missing standard PubSub messenger message envelope.' });
  }

  try {
    // Parse base64 payload from Google Pub/Sub
    const rawData = Buffer.from(message.data, 'base64').toString('utf-8');
    const notification = JSON.parse(rawData);

    console.log('Received Play Store RTDN Event:', notification);

    const subscriptionNotification = notification.subscriptionNotification;
    if (!subscriptionNotification) {
      return res.status(200).json({ message: 'Irrelevant notification category, event skipped.' });
    }

    const { purchaseToken, subscriptionId, notificationType } = subscriptionNotification;

    // Query DB for known subscription record
    const sub = await Subscription.findOne({ purchaseToken });
    if (!sub) {
      // A purchase from another endpoint or unknown token, log and acknowledge
      console.log('Play Store notification for unknown purchase token:', purchaseToken);
      return res.status(200).json({ message: 'Event acknowledged; token not found in our database.' });
    }

    // Interpret Google Notification Types
    // 1: Recovered, 2: Renewed, 3: Cancelled, 13: Refunded, etc.
    let updatedStatus = sub.status;
    let newExpiryTime = sub.expiryTime;

    if (notificationType === 2) {
      updatedStatus = 'active';
      // If we have publisher client, query Google API for new expiry time
      if (androidPublisher) {
        try {
          const packageName = process.env.GOOGLE_PLAY_PACKAGE_NAME || 'com.aistudio.smartcleaner';
          const r = await androidPublisher.purchases.subscriptions.get({
            packageName,
            subscriptionId,
            token: purchaseToken,
          });
          newExpiryTime = new Date(parseInt(r.data.expiryTimeMillis, 10));
        } catch (e) {
          console.error('Error querying updated Google subscription stats inside webhook:', e);
          newExpiryTime = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // extend by 30 days fallback
        }
      } else {
        newExpiryTime = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
      }
    } else if (notificationType === 3) {
      updatedStatus = 'cancelled';
    } else if (notificationType === 13) {
      updatedStatus = 'refunded';
    } else if (notificationType === 5) {
      updatedStatus = 'expired';
    }

    // Update Subscription Database
    sub.status = updatedStatus;
    sub.expiryTime = newExpiryTime;
    await sub.save();

    // Sync corresponding user entitlement state
    const user = await User.findById(sub.userId);
    if (user) {
      const activeCount = await Subscription.countDocuments({
        userId: user._id,
        status: 'active',
        expiryTime: { $gt: new Date() }
      });
      user.premiumEntitled = activeCount > 0;
      await user.save();
    }

    res.status(200).json({ success: true, message: 'Google RTDN Webhook event processed successfully.' });

  } catch (error) {
    console.error('Error processing RTDN webhook:', error);
    // Return 500 to let Google Pub/Sub retry
    res.status(500).json({ error: 'Failed to fully execute RTDN processing.' });
  }
});

module.exports = router;
