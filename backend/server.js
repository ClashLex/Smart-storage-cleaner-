const dotenv = require('dotenv');
dotenv.config();

const app = require('./app');
const mongoose = require('mongoose');

function validateProductionEnvironment() {
  if (process.env.NODE_ENV !== 'production') return;

  const required = ['MONGODB_URI', 'JWT_SECRET', 'FIREBASE_SERVICE_ACCOUNT_JSON'];
  const missing = required.filter((key) => !process.env[key] || !process.env[key].trim());
  if (missing.length) {
    throw new Error(`Missing required production environment variables: ${missing.join(', ')}`);
  }
  if (process.env.JWT_SECRET.length < 32) {
    throw new Error('JWT_SECRET must contain at least 32 characters in production.');
  }
  try {
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
    if (!serviceAccount.project_id || !serviceAccount.client_email || !serviceAccount.private_key) {
      throw new Error('service account fields are incomplete');
    }
  } catch (error) {
    throw new Error(`FIREBASE_SERVICE_ACCOUNT_JSON is invalid: ${error.message}`);
  }
}

const PORT = Number(process.env.PORT) || 3000;
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/smart_cleaner';

let server;

async function start() {
  validateProductionEnvironment();
  await mongoose.connect(MONGODB_URI);
  console.log('Successfully connected to MongoDB database.');
  server = app.listen(PORT, () => {
    console.log(`Smart Storage Cleaner server running in ${process.env.NODE_ENV || 'development'} on port ${PORT}`);
  });
}

async function shutdown(signal) {
  console.log(`${signal} signal received; shutting down gracefully.`);
  try {
    if (server) await new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
    await mongoose.disconnect();
    process.exit(0);
  } catch (error) {
    console.error('Error during graceful shutdown:', error.message);
    process.exit(1);
  }
}

process.once('SIGTERM', () => shutdown('SIGTERM'));
process.once('SIGINT', () => shutdown('SIGINT'));

start().catch((error) => {
  console.error('Backend startup failed:', error.message);
  process.exit(1);
});
