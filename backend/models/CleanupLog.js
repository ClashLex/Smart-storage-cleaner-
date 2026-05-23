const mongoose = require('mongoose');

const CleanupLogSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    category: {
      type: String,
      required: true,
      index: true,
    },
    bytesFreed: {
      type: Number,
      required: true,
      min: 0,
    },
  },
  {
    timestamps: true,
  }
);

// Advanced indexes to allow quick cumulative queries (e.g. sum by category)
CleanupLogSchema.index({ category: 1, bytesFreed: 1 });
CleanupLogSchema.index({ createdAt: -1 });

module.exports = mongoose.model('CleanupLog', CleanupLogSchema);
