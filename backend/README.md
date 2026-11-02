# GitForge Pusher Backend Proxy

This backend service acts as a secure intermediary for:
1. GitHub OAuth Device Flow token exchange (protecting `GITHUB_CLIENT_SECRET`).
2. Google Gemini AI API requests (protecting `GEMINI_API_KEY`).

## Requirements
- Node.js 18+ or 20+
- npm or yarn

## Setup Instructions

1. Install dependencies:
   ```bash
   npm install
   ```

2. Configure environment:
   ```bash
   cp .env.example .env
   # Edit .env with your GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, and GEMINI_API_KEY
   ```

3. Run server:
   ```bash
   npm start
   ```

## API Endpoints

- `GET /health`: Health status check.
- `POST /api/oauth/device/code`: Initiates GitHub Device Code authorization.
- `POST /api/oauth/device/token`: Exchanges device code for access token using client secret.
- `POST /api/ai/generate`: Proxies AI README/Code Review/Error Explainer requests to Gemini API.

## Deployment
Can be deployed to Google Cloud Run, AWS App Runner, Heroku, or Render.
Ensure HTTPS is enforced in production.
