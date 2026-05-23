const request = require('supertest');
const app = require('../app');
const User = require('../models/User');
const Subscription = require('../models/Subscription');
const jwt = require('jsonwebtoken');

jest.mock('../models/User');
jest.mock('../models/Subscription');

describe('Billing Control Routes', () => {
  let token;
  const mockUserId = '507f1f77bcf86cd799439011';

  beforeEach(() => {
    jest.clearAllMocks();
    token = jwt.sign({ userId: mockUserId }, process.env.JWT_SECRET || 'fallback_secret');
  });

  describe('POST /api/billing/verify', () => {
    it('should grant premium entitlements for active purchaseToken', async () => {
      const mockUser = {
        _id: mockUserId,
        email: 'user@example.com',
        premiumEntitled: false,
        save: jest.fn().mockResolvedValue(this),
      };
      User.findById.mockResolvedValue(mockUser);
      Subscription.findOne.mockResolvedValue(null); // No existing matches for replay checks

      const mockSavedSub = {
        _id: '807f1f77bcf86cd799439044',
        userId: mockUserId,
        orderId: 'GPA.1234-5678',
        productId: 'premium_monthly',
        purchaseToken: 'token_abc_123',
        status: 'active',
        expiryTime: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
      };
      Subscription.prototype.save = jest.fn().mockResolvedValue(mockSavedSub);

      const res = await request(app)
        .post('/api/billing/verify')
        .set('Authorization', `Bearer ${token}`)
        .send({
          productId: 'premium_monthly',
          purchaseToken: 'token_abc_123'
        });

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.subscription.status).toBe('active');
      expect(mockUser.premiumEntitled).toBe(true);
      expect(mockUser.save).toHaveBeenCalled();
    });

    it('should reject replay validations of identical purchase tokens', async () => {
      const mockUser = { _id: mockUserId };
      User.findById.mockResolvedValue(mockUser);

      const existingSubInDb = {
        userId: 'some_other_user_id',
        purchaseToken: 'token_abc_123',
      };
      Subscription.findOne.mockResolvedValue(existingSubInDb);

      const res = await request(app)
        .post('/api/billing/verify')
        .set('Authorization', `Bearer ${token}`)
        .send({
          productId: 'premium_monthly',
          purchaseToken: 'token_abc_123'
        });

      expect(res.status).toBe(409); // Conflict / Replay detected
    });
  });

  describe('GET /api/billing/entitlement', () => {
    it('should output entitlement:true when active premium purchase matches', async () => {
      const mockUser = {
        _id: mockUserId,
        premiumEntitled: true,
        save: jest.fn(),
      };
      User.findById.mockResolvedValue(mockUser);

      Subscription.findOne.mockReturnValue({
        sort: jest.fn().mockResolvedValue({
          productId: 'premium_monthly',
          orderId: 'GPA.123',
          expiryTime: new Date(Date.now() + 50000000),
        })
      });

      const res = await request(app)
        .get('/api/billing/entitlement')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.premiumEntitled).toBe(true);
    });
  });

  describe('POST /api/billing/webhook', () => {
    it('should process cancel notifications from Play Console callback', async () => {
      const mockSub = {
        userId: mockUserId,
        purchaseToken: 'token_rtdn_123',
        status: 'active',
        expiryTime: new Date(),
        save: jest.fn(),
      };
      Subscription.findOne.mockResolvedValue(mockSub);
      Subscription.countDocuments.mockResolvedValue(0);

      const mockUser = {
        _id: mockUserId,
        premiumEntitled: true,
        save: jest.fn(),
      };
      User.findById.mockResolvedValue(mockUser);

      // Base64-encoded PubSub payload for cancellation event (notificationType 3)
      const testEventPayload = {
        version: '1.0',
        packageName: 'com.aistudio.smartcleaner',
        eventTimeMillis: Date.now().toString(),
        subscriptionNotification: {
          version: '1.0',
          notificationType: 3, // CANCELLED
          purchaseToken: 'token_rtdn_123',
          subscriptionId: 'premium_monthly'
        }
      };
      const base64Data = Buffer.from(JSON.stringify(testEventPayload)).toString('base64');

      const res = await request(app)
        .post('/api/billing/webhook')
        .send({
          message: {
            data: base64Data
          }
        });

      expect(res.status).toBe(200);
      expect(mockSub.status).toBe('cancelled');
      expect(mockUser.premiumEntitled).toBe(false); // No active subscriptions remain
    });
  });
});
