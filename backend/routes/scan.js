const express = require('express');
const { GoogleGenerativeAI } = require('@google/generative-ai');
const ScanSession = require('../models/ScanSession');
const { authenticateJWT } = require('../middleware/auth');

const router = express.Router();
const MAX_CATEGORIES = 50;
const MAX_CATEGORY_NAME = 64;

function formatBytes(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 Bytes';
  const units = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${Number((bytes / (1024 ** index)).toFixed(2))} ${units[index]}`;
}

router.post('/', authenticateJWT, async (req, res) => {
  const { categories } = req.body || {};
  if (!Array.isArray(categories) || categories.length > MAX_CATEGORIES) {
    return res.status(400).json({ error: `categories must be an array with at most ${MAX_CATEGORIES} entries.` });
  }

  const normalizedCategories = [];
  for (const item of categories) {
    if (!item || typeof item.category !== 'string' ||
        !item.category.trim() || item.category.trim().length > MAX_CATEGORY_NAME ||
        !Number.isSafeInteger(item.sizeBytes) || item.sizeBytes < 0) {
      return res.status(400).json({ error: 'Each category requires a name and a non-negative safe-integer sizeBytes value.' });
    }
    normalizedCategories.push({ category: item.category.trim(), sizeBytes: item.sizeBytes });
  }

  try {
    const totalSizeBytes = normalizedCategories.reduce((sum, item) => sum + item.sizeBytes, 0);
    if (!Number.isSafeInteger(totalSizeBytes)) return res.status(400).json({ error: 'The combined scan size is too large.' });

    const formattedCategoriesString = normalizedCategories
      .map((item) => `- ${item.category}: ${formatBytes(item.sizeBytes)}`)
      .join('\n');
    const totalSizeStr = formatBytes(totalSizeBytes);
    const systemPrompt = 'You are a friendly, highly intelligent Smart Cleaner storage assistant for Android devices.';
    const userPrompt = `The user completed a system storage scan. Here are the size details per folder category:
${formattedCategoriesString}
Total potential space to reclaim: ${totalSizeStr}

Recommend a priority hierarchy. Advise reviewing cache and temporary files first, duplicates next, and personal media carefully. Give at most 4 short bullet points.`;

    let recommendations = '';
    const apiKey = process.env.GEMINI_API_KEY;
    if (apiKey && apiKey !== 'your_gemini_api_key_here') {
      try {
        const genAI = new GoogleGenerativeAI(apiKey);
        const model = genAI.getGenerativeModel({
          model: process.env.GEMINI_MODEL || 'gemini-1.5-flash',
          systemInstruction: systemPrompt,
        });
        const result = await model.generateContent({
          contents: [{ parts: [{ text: userPrompt }] }],
          generationConfig: { temperature: 0.5, maxOutputTokens: 350 },
        });
        recommendations = result.response.text().trim();
      } catch (error) {
        console.warn('Gemini recommendation failed; using local guidance:', error.message);
      }
    }

    if (!recommendations) {
      const cacheBytes = normalizedCategories.find((item) => item.category.toLowerCase() === 'cache')?.sizeBytes || 0;
      const duplicateBytes = normalizedCategories.find((item) => item.category.toLowerCase() === 'duplicates')?.sizeBytes || 0;
      recommendations = [
        `Step 1: Review temporary cache files first (${formatBytes(cacheBytes)} detected).`,
        `Step 2: Review duplicate photos before deleting (${formatBytes(duplicateBytes)} detected).`,
        'Step 3: Check downloaded installers and APKs individually before removal.',
        'Step 4: Review personal photos and videos carefully; never delete them automatically.',
      ].join('\n');
    }

    const newSession = new ScanSession({
      userId: req.user._id,
      categories: normalizedCategories,
      totalSizeBytes,
      recommendations,
    });
    const savedSession = await newSession.save();

    return res.json({
      sessionId: savedSession?._id || newSession._id,
      totalSizeBytes,
      totalSizeFormatted: totalSizeStr,
      recommendations,
      createdAt: savedSession?.createdAt || newSession.createdAt,
    });
  } catch (error) {
    console.error('Failed to process scan stats:', error.message);
    return res.status(500).json({ error: 'Failed to run storage analysis and produce advice.' });
  }
});

module.exports = router;
