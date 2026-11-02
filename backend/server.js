/**
 * GitForge Pusher - Secure Backend Proxy Server
 * 
 * Purpose:
 * Provides secure server-side OAuth exchange and AI request proxying.
 * Prevents client-side exposure of GitHub Client Secrets and AI Master Keys.
 */

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const axios = require('axios');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'ok', service: 'GitForge Pusher Backend', timestamp: new Date().toISOString() });
});

/**
 * Endpoint: POST /api/oauth/device/code
 * Requests a new device code from GitHub OAuth.
 */
app.post('/api/oauth/device/code', async (req, res) => {
    try {
        const clientId = process.env.GITHUB_CLIENT_ID;
        const response = await axios.post('https://github.com/login/device/code', {
            client_id: clientId,
            scope: 'repo,read:user'
        }, {
            headers: { 'Accept': 'application/json' }
        });
        res.json(response.data);
    } catch (err) {
        console.error('OAuth device code error:', err.message);
        res.status(500).json({ error: 'Failed to initiate device code', details: err.message });
    }
});

/**
 * Endpoint: POST /api/oauth/device/token
 * Polls for OAuth access token using client secret safely kept on the server.
 */
app.post('/api/oauth/device/token', async (req, res) => {
    const { device_code } = req.body;
    if (!device_code) {
        return res.status(400).json({ error: 'device_code is required' });
    }

    try {
        const response = await axios.post('https://github.com/login/oauth/access_token', {
            client_id: process.env.GITHUB_CLIENT_ID,
            client_secret: process.env.GITHUB_CLIENT_SECRET,
            device_code: device_code,
            grant_type: 'urn:ietf:params:oauth:grant-type:device_code'
        }, {
            headers: { 'Accept': 'application/json' }
        });
        res.json(response.data);
    } catch (err) {
        console.error('OAuth token exchange error:', err.message);
        res.status(500).json({ error: 'Failed to poll token', details: err.message });
    }
});

/**
 * Endpoint: POST /api/ai/generate
 * Proxies Gemini AI requests using server-side GEMINI_API_KEY.
 */
app.post('/api/ai/generate', async (req, res) => {
    const { prompt, model } = req.body;
    if (!prompt) {
        return res.status(400).json({ error: 'prompt is required' });
    }

    const aiModel = model || 'gemini-1.5-flash';
    const apiKey = process.env.GEMINI_API_KEY;

    if (!apiKey) {
        return res.status(500).json({ error: 'Server AI key not configured in .env' });
    }

    try {
        const response = await axios.post(
            `https://generativelanguage.googleapis.com/v1beta/models/${aiModel}:generateContent?key=${apiKey}`,
            {
                contents: [{ parts: [{ text: prompt }] }]
            },
            {
                headers: { 'Content-Type': 'application/json' }
            }
        );
        res.json(response.data);
    } catch (err) {
        console.error('AI Proxy error:', err.response?.data || err.message);
        res.status(err.response?.status || 500).json({
            error: 'AI request failed',
            details: err.response?.data || err.message
        });
    }
});

app.listen(PORT, () => {
    console.log(`GitForge Pusher backend proxy listening on port ${PORT}`);
});
