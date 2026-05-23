const request = require('supertest');
const app = require('../app');
const User = require('../models/User');
const ScanSession = require('../models/ScanSession');
const jwt = require('jsonwebtoken');

jest.mock('../models/User');
jest.mock('../models/ScanSession');

describe('Scan Recommendation Route Tests', () => {
  let token;
  const mockUserId = '507f1f77bcf86cd799439011';

  beforeEach(() => {
    jest.clearAllMocks();
    token = jwt.sign({ userId: mockUserId }, process.env.JWT_SECRET || 'fallback_secret');
  });

  it('should successfully calculate storage stats and output recommendations', async () => {
    const mockUser = {
      _id: mockUserId,
      email: 'test@example.com',
    };
    User.findById.mockResolvedValue(mockUser);

    const mockSavedSession = {
      _id: '607f1f77bcf86cd799439022',
      userId: mockUserId,
      createdAt: new Date(),
    };
    ScanSession.prototype.save = jest.fn().mockResolvedValue(mockSavedSession);

    const scanPayload = {
      categories: [
        { category: 'Cache', sizeBytes: 50000000 },     // ~50MB
        { category: 'Duplicates', sizeBytes: 250000000 }, // ~250MB
        { category: 'APKs', sizeBytes: 120000000 }       // ~120MB
      ]
    };

    const res = await request(app)
      .post('/api/scan')
      .set('Authorization', `Bearer ${token}`)
      .send(scanPayload);

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('sessionId');
    expect(res.body.totalSizeBytes).toBe(420000000); // 50MB + 250MB + 120MB
    expect(res.body.totalSizeFormatted).toBe('400.54 MB');
    expect(res.body).toHaveProperty('recommendations');
    expect(res.body.recommendations).toContain('Step 1'); // Fallback heuristic check
  });

  it('should return 400 space allocation body is malformed', async () => {
    const res = await request(app)
      .post('/api/scan')
      .set('Authorization', `Bearer ${token}`)
      .send({}); // Missing categories

    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });
});
