const express = require('express');
const { GoogleGenerativeAI } = require('@google/generative-ai');
const ScanSession = require('../models/ScanSession');
const { authenticateJWT } = require('../middleware/auth');

const router = express.Router();

// Helper to format bytes to human readable form
function formatBytes(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * POST /api/scan
 * Receives category sizes, saves the scan session, and queries the Gemini Model for tailored storage cleanup advice.
 */
router.post('/', authenticateJWT, async (req, res) => {
  const { categories } = req.body;

  if (!categories || !Array.isArray(categories)) {
    return res.status(400).json({ error: 'Request body must contain an array of categories.' });
  }

  try {
    // 1. Calculate cumulative size metrics
    let totalSizeBytes = 0;
    const formattedCategoriesString = categories.map(cat => {
      const sizeStr = formatBytes(cat.sizeBytes);
      totalSizeBytes += cat.sizeBytes;
      return `- ${cat.category}: ${sizeStr}`;
    }).join('\n');

    const totalSizeStr = formatBytes(totalSizeBytes);

    // 2. Formulate dynamic prompt for Gemini
    const systemPrompt = "You are a friendly, highly intelligent Smart Cleaner storage assistant for Android devices.";
    const userPrompt = `The user completed a system storage scan. Here is the size details per folder category:
${formattedCategoriesString}
Total potential space to reclaim: ${totalSizeStr}

Recommend a specific priority hierarchy of what the user should clean first. Keep it light, visual, and action-oriented.
Guidelines:
1. Advise clearing "Cache" and "Temp Files" first since they won't harm user data.
2. Recommend running the Duplicate Finder for duplicate files next to restore space effortlessly.
3. Suggest caution before deleting large custom images/downloaded APKs unless older than 30 days.
Write the exact layout steps in a maximum of 4 short bullet points. Do not include introductory text, just jump straight to the action items.`;

    let recommendations = "";

    // 3. Request AI recommendations via Google Gemini SDK
    const apiKey = process.env.GEMINI_API_KEY;
    if (apiKey && apiKey !== 'your_gemini_api_key_here' && apiKey.trim() !== '') {
      try {
        const genAI = new GoogleGenerativeAI(apiKey);
        const model = genAI.getGenerativeModel({
          model: 'gemini-3.5-flash',
          systemInstruction: systemPrompt
        });

        const result = await model.generateContent({
          contents: [{ parts: [{ text: userPrompt }] }],
          generationConfig: {
            temperature: 0.5,
            maxOutputTokens: 350,
          }
        });

        const responseText = result.response.text();
        if (responseText && responseText.trim() !== "") {
          recommendations = responseText.trim();
        }
      } catch (geminiError) {
        console.error('Gemini API request failed, using intelligent rule engine fallback:', geminiError.message);
      }
    }

    // 4. Custom rule-based backup Generator if Gemini is unavailable
    if (!recommendations) {
      recommendations = [
        `🧹 **Step 1:** Clear temporary app caches first (saves ${formatBytes(categories.find(c => c.category === 'Cache')?.sizeBytes || 0)}). Safe and fast.`,
        `👥 **Step 2:** De-duplicate your space! Review redundant photos to win back ${formatBytes(categories.find(c => c.category === 'Duplicates')?.sizeBytes || 0)} instantly.`,
        `📦 **Step 3:** Purge downloaded setup files (.apk) older than 30 days to free heavy overhead.`,
        `⚠️ **Note:** Review any large media files manually to ensure you do not lose important memories.`
      ].join('\n');
    }

    // 5. Store session to maintain chronological records
    const newSession = new ScanSession({
      userId: req.user._id,
      categories,
      totalSizeBytes,
      recommendations,
    });
    await newSession.save();

    res.json({
      sessionId: newSession._id,
      totalSizeBytes,
      totalSizeFormatted: totalSizeStr,
      recommendations,
      createdAt: newSession.createdAt,
    });

  } catch (error) {
    console.error('Failed to process scan stats:', error);
    res.status(500).json({ error: 'Failed to run storage analysis and produce advice.' });
  }
});

module.exports = router;
