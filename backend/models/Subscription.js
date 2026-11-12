const mongoose = require('mongoose');

const SubscriptionSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    orderId: { type: String, trim: true },
    purchaseToken: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    productId: { type: String, required: true, trim: true },
    status: {
      type: String,
      enum: ['active', 'pending', 'expired', 'cancelled', 'refunded'],
      default: 'active',
      index: true,
    },
    expiryTime: { type: Date, required: true, index: true },
    rawVerificationData: { type: mongoose.Schema.Types.Mixed, select: false },
  },
  { timestamps: true }
);

SubscriptionSchema.index({ status: 1, expiryTime: 1 });
module.exports = mongoose.model('Subscription', SubscriptionSchema);
