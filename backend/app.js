const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const { apiRateLimiter } = require('./middleware/rateLimiter');

// Import routes
const authRoutes = require('./routes/auth');
const scanRoutes = require('./routes/scan');
const cleanupRoutes = require('./routes/cleanup');
const billingRoutes = require('./routes/billing');
const adminRoutes = require('./routes/admin');

const app = express();

// Apply Global Security & Parsing Middleware
app.use(helmet());
app.use(cors({ origin: '*' })); // Allow requests from mobile and web apps
app.use(express.json());

// Apply rate limiting to all standard API endpoints
app.use('/api/', apiRateLimiter);

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'OK', environment: process.env.NODE_ENV || 'development' });
});

// Mount Routes
app.use('/api/auth', authRoutes);
app.use('/api/scan', scanRoutes);
app.use('/api/cleanup', cleanupRoutes);
app.use('/api/billing', billingRoutes);
app.use('/api/admin', adminRoutes);

// Global 404 handler for unmatched routes
app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found.' });
});

// Global internal error handler
app.use((err, req, res, next) => {
  console.error('Unhandled Server Error:', err.stack);
  res.status(err.status || 500).json({
    error: 'Internal server error occurred',
    message: process.env.NODE_ENV === 'development' ? err.message : 'An unexpected error occurred.'
  });
});

module.exports = app;
