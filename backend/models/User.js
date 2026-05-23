const mongoose = require('mongoose');

const UserSchema = new mongoose.Schema(
  {
    firebaseUid: {
      type: String,
      required: true,
      unique: true,
      index: true,
    },
    email: {
      type: String,
      lowercase: true,
      trim: true,
    },
    displayName: {
      type: String,
      trim: true,
    },
    premiumEntitled: {
      type: Boolean,
      default: false,
      index: true,
    },
    role: {
      type: String,
      enum: ['user', 'admin'],
      default: 'user',
    },
  },
  {
    timestamps: true,
  }
);

// Optimize query performance with single and compound indexes if needed
UserSchema.index({ createdAt: -1 });

module.exports = mongoose.model('User', UserSchema);
