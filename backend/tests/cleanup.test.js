const request = require('supertest');
const app = require('../app');
const User = require('../models/User');
const CleanupLog = require('../models/CleanupLog');
const jwt = require('jsonwebtoken');

jest.mock('../models/User');
jest.mock('../models/CleanupLog');

describe('Cleanup Logging Routes', () => {
  let token;
  const mockUserId = '507f1f77bcf86cd799439011';

  beforeEach(() => {
    jest.clearAllMocks();
    token = jwt.sign({ userId: mockUserId }, process.env.JWT_SECRET || 'fallback_secret');
  });

  it('should successfully log a valid cleanup action', async () => {
    const mockUser = {
      _id: mockUserId,
      email: 'user@example.com',
    };
    User.findById.mockResolvedValue(mockUser);

    const mockSavedLog = {
      _id: '707f1f77bcf86cd799439033',
      userId: mockUserId,
      category: 'Cache',
      bytesFreed: 104857600, // 100MB
      createdAt: new Date(),
    };
    CleanupLog.prototype.save = jest.fn().mockResolvedValue(mockSavedLog);

    const res = await request(app)
      .post('/api/cleanup/log')
      .set('Authorization', `Bearer ${token}`)
      .send({
        category: 'Cache',
        bytesFreed: 104857600
      });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.log.category).toBe('Cache');
    expect(res.body.log.bytesFreed).toBe(104857600);
  });

  it('should return 400 for negative byte parameters or missing category', async () => {
    const res = await request(app)
      .post('/api/cleanup/log')
      .set('Authorization', `Bearer ${token}`)
      .send({
        category: 'Cache',
        bytesFreed: -500 // Invalid negative number
      });

    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });
});
