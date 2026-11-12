const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const { apiRateLimiter } = require('./middleware/rateLimiter');

const authRoutes = require('./routes/auth');
const scanRoutes = require('./routes/scan');
const cleanupRoutes = require('./routes/cleanup');
const billingRoutes = require('./routes/billing');
const adminRoutes = require('./routes/admin');

const app = express();
const allowedOrigins = (process.env.CORS_ORIGINS || '')
  .split(',')
  .map((origin) => origin.trim())
  .filter(Boolean);

app.disable('x-powered-by');
app.use(helmet());
app.use(cors({
  origin(origin, callback) {
    // Native mobile clients and server-to-server callers do not send an Origin header.
    if (!origin || process.env.NODE_ENV !== 'production') return callback(null, true);
    return callback(null, allowedOrigins.includes(origin));
  },
  optionsSuccessStatus: 204,
}));
app.use(express.json({ limit: '1mb' }));
app.use('/api/', apiRateLimiter);

app.get('/health', (req, res) => {
  res.json({ status: 'OK', environment: process.env.NODE_ENV || 'development' });
});

app.use('/api/auth', authRoutes);
app.use('/api/scan', scanRoutes);
app.use('/api/cleanup', cleanupRoutes);
app.use('/api/billing', billingRoutes);
app.use('/api/admin', adminRoutes);

app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found.' });
});

app.use((err, req, res, next) => {
  console.error('Unhandled server error:', err.message);
  if (res.headersSent) return next(err);
  res.status(err.status || 500).json({
    error: 'Internal server error occurred.',
    message: process.env.NODE_ENV === 'development' ? err.message : 'An unexpected error occurred.',
  });
});

module.exports = app;
