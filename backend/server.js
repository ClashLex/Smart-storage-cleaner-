const app = require('./app');
const mongoose = require('mongoose');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

const PORT = process.env.PORT || 3000;
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/smart_cleaner';

// Initialize MongoDB Connection
mongoose
  .connect(MONGODB_URI)
  .then(() => {
    console.log('Successfully connected to MongoDB database.');
    
    // Listen for incoming requests
    app.listen(PORT, () => {
      console.log(`Smart Storage Cleaner server running in ${process.env.NODE_ENV || 'development'} on port ${PORT}`);
    });
  })
  .catch(error => {
    console.error('CRITICAL: MongoDB connection failed:', error.message);
    process.exit(1);
  });

// Handle graceful terminations
process.on('SIGTERM', () => {
  console.log('SIGTERM signal received: closing server gracefully.');
  mongoose.connection.close(() => {
    console.log('MongoDB connection closed.');
    process.exit(0);
  });
});
