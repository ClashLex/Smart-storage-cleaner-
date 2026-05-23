const request = require('supertest');
const app = require('../app');
const User = require('../models/User');
const Subscription = require('../models/Subscription');
const ScanSession = require('../models/ScanSession');
const CleanupLog = require('../models/CleanupLog');
const jwt = require('jsonwebtoken');

// Mock all Mongoose models
jest.mock('../models/User');
jest.mock('../models/Subscription');
jest.mock('../models/ScanSession');
jest.mock('../models/CleanupLog');

describe('Auth Routes Integration Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('POST /api/auth/sync', () => {
    it('should successfully sync and return JWT for a new user', async () => {
      // Mock User.findOne to return null (identifying new user)
      User.findOne.mockResolvedValue(null);

      // Mock User.prototype.save
      const mockUserInstance = {
        _id: '507f1f77bcf86cd799439011',
        firebaseUid: 'test_uid_123',
        email: 'test@example.com',
        displayName: 'Test User',
        premiumEntitled: false,
        role: 'user',
        save: jest.fn().mockResolvedValue(this),
      };
      User.mockImplementation(() => mockUserInstance);

      const res = await request(app)
        .post('/api/auth/sync')
        .set('Authorization', 'Bearer mock_token_test_uid_123'); // Triggers mock Firebase auth mechanism in middleware

      expect(res.status).toBe(200);
      expect(res.body).toHaveProperty('token');
      expect(res.body.user).toEqual({
        id: '507f1f77bcf86cd799439011',
        firebaseUid: 'test_uid_123',
        email: 'test@example.com',
        displayName: 'Test User',
        premiumEntitled: false,
        role: 'user',
      });
    });

    it('should sync existing user details without duplicating', async () => {
      // Mock existing User
      const mockUser = {
        _id: '507f1f77bcf86cd799439011',
        firebaseUid: 'test_uid_123',
        email: 'test_old@example.com',
        displayName: 'Old Name',
        premiumEntitled: true,
        role: 'user',
        save: jest.fn().mockResolvedValue(this),
      };
      User.findOne.mockResolvedValue(mockUser);

      const res = await request(app)
        .post('/api/auth/sync')
        .set('Authorization', 'Bearer mock_token_test_uid_123');

      expect(res.status).toBe(200);
      expect(res.body.user.email).toBe('test_uid_123@example.com'); // Updated by sync logic
      expect(res.body.user.premiumEntitled).toBe(true);
    });

    it('should return 401 if authorization header is missing', async () => {
      const res = await request(app).post('/api/auth/sync');
      expect(res.status).toBe(401);
      expect(res.body).toHaveProperty('error');
    });
  });

  describe('GET /api/auth/me', () => {
    it('should return the authenticated user profile', async () => {
      const mockUser = {
        _id: '507f1f77bcf86cd799439011',
        firebaseUid: 'test_uid_123',
        email: 'test@example.com',
        displayName: 'Test User',
        premiumEntitled: false,
        role: 'user',
        createdAt: new Date(),
      };
      User.findById.mockResolvedValue(mockUser);

      const token = jwt.sign({ userId: '507f1f77bcf86cd799439011' }, process.env.JWT_SECRET || 'fallback_secret');

      const res = await request(app)
        .get('/api/auth/me')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.email).toBe('test@example.com');
      expect(res.body.id).toBe('507f1f77bcf86cd799439011');
    });

    it('should return 401 for an invalid access token', async () => {
      const res = await request(app)
        .get('/api/auth/me')
        .set('Authorization', 'Bearer invalid_jwt_signature');

      expect(res.status).toBe(401);
    });
  });

  describe('DELETE /api/auth/me', () => {
    it('should purge user data and return success for compliance', async () => {
      const mockUser = {
        _id: '507f1f77bcf86cd799439011',
        firebaseUid: 'test_uid_123',
        role: 'user',
      };
      User.findById.mockResolvedValue(mockUser);

      Subscription.deleteMany.mockResolvedValue({ DeletedCount: 1 });
      ScanSession.deleteMany.mockResolvedValue({ DeletedCount: 5 });
      CleanupLog.deleteMany.mockResolvedValue({ DeletedCount: 10 });
      User.findByIdAndDelete.mockResolvedValue(mockUser);

      const token = jwt.sign({ userId: '507f1f77bcf86cd799439011' }, process.env.JWT_SECRET || 'fallback_secret');

      const res = await request(app)
        .delete('/api/auth/me')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body).toEqual({
        success: true,
        message: 'Account and associated storage metrics purged successfully'
      });
      expect(User.findByIdAndDelete).toHaveBeenCalledWith('507f1f77bcf86cd799439011');
    });
  });
});
