const mongoose = require('mongoose');

const SubscriptionSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    orderId: {
      type: String,
      trim: true,
    },
    purchaseToken: {
      type: String,
      required: true,
      unique: true, // Unique index to prevent replay attacks
      index: true,
    },
    productId: {
      type: String,
      required: true,
    },
    status: {
      type: String,
      enum: ['active', 'expired', 'cancelled', 'refunded'],
      default: 'active',
      index: true,
    },
    expiryTime: {
      type: Date,
      required: true,
      index: true,
    },
    rawVerificationData: {
      type: mongoose.Schema.Types.Mixed,
    },
  },
  {
    timestamps: true,
  }
);

// Indexes for fast querying of dynamic status & expiry conditions
SubscriptionSchema.index({ status: 1, expiryTime: 1 });

module.exports = mongoose.model('Subscription', SubscriptionSchema);
