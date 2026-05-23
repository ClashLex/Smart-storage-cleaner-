const mongoose = require('mongoose');

const CategorySizeSchema = new mongoose.Schema({
  category: {
    type: String,
    required: true,
  },
  sizeBytes: {
    type: Number,
    required: true,
    min: 0,
  },
}, { _id: false });

const ScanSessionSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
      index: true,
    },
    categories: [CategorySizeSchema],
    totalSizeBytes: {
      type: Number,
      required: true,
      default: 0,
    },
    recommendations: {
      type: String,
      required: true,
    },
  },
  {
    timestamps: true,
  }
);

ScanSessionSchema.index({ createdAt: -1 });

module.exports = mongoose.model('ScanSession', ScanSessionSchema);
