# GitForge Pusher

<p align="center">
  <img src="app/src/main/res/drawable/ic_app_logo.png" width="120" height="120" alt="GitForge Pusher Logo" />
</p>

<p align="center">
  <strong>Native Android application that pushes local projects, ZIP archives, folders, and files directly to GitHub repositories using the Git Database API.</strong>
</p>

---

## 🚀 Key Highlights & Purpose

GitForge Pusher eliminates the need for a terminal or desktop machine to publish code to GitHub. Whether you have a ZIP archive downloaded on your phone, an exported IDE project folder, or a set of assets, GitForge Pusher allows you to:
- Authenticate via **Personal Access Token (PAT)** or **GitHub OAuth Device Flow**.
- Browse and search all your GitHub repositories with live public/private badges and stars.
- Select target branches or create new branches on the fly.
- Extract ZIP archives safely with **Zip Slip path traversal protection**.
- Preview files, check uncompressed sizes, and exclude unwanted entries.
- Push clean multi-file commits using GitHub's **Git Database API** (Blobs → Trees → Commits → Refs).
- Track full commit history in an encrypted local **Room database**.
- Leverage three integrated AI tools: **AI README Generator**, **AI Code Reviewer**, and **AI Error Explainer**.

---

## 🛠️ Technology Stack

- **Language:** Java 17
- **UI:** Native XML Layouts + Material Design 3 Components
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Networking:** Retrofit 2 + OkHttp + Gson
- **Local Persistence:** Android Jetpack Room Database
- **Security:** Android Keystore + `EncryptedSharedPreferences` (AES-256 GCM)
- **File System:** Android Storage Access Framework (SAF) + Java NIO
- **Minimum SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 36

---

## 📱 The 16 Core Screens & User Journeys

1. **Splash Screen:** Animated brand launch showcasing the custom adaptive logo and tagline.
2. **Onboarding:** Interactive walkthrough of One-Tap Push, Git Trees Engine, and AI Tools.
3. **GitHub Login:** Personal Access Token validation and OAuth Device Flow initiation.
4. **Dashboard:** Developer stats (total repositories, total pushes, success rate), quick push button, and recent uploads.
5. **Repository List:** Real-time GitHub API repository listing with search, fork indicator, and stars count.
6. **Branch Selection:** Branch inspector displaying commit SHAs, protected status, and new branch creator.
7. **Project File Selection:** Multi-source picker supporting ZIP archives, SAF folders, and individual files.
8. **Upload Preview:** Interactive file tree showing relative paths, file sizes, text previews, and toggle checkboxes.
9. **Commit Details:** Commit message editor with quick-suggestion chips and author information.
10. **Upload Progress:** Live step-by-step indicator tracking blob uploads, tree generation, and ref updates.
11. **Upload Result:** Success card displaying commit SHA, total files pushed, and direct "View on GitHub" action.
12. **Upload History:** Room database log of previous push operations with error traces and commit links.
13. **AI README Generator:** Creates comprehensive Markdown READMEs with architecture, install steps, and tech stack.
14. **AI Code Reviewer:** Inspects code snippets for security vulnerabilities, edge cases, and performance bottlenecks.
15. **AI Error Explainer:** Analyzes cryptic compiler or Git errors and provides terminal fixes.
16. **Settings:** Theme selection (Dark / Light / System), token management, AI provider & model configuration.

---

## 🔒 Security Architecture

1. **Android Keystore Encryption:**
   All sensitive tokens and API keys are stored using `EncryptedSharedPreferences` with MasterKey AES-256 GCM encryption.
2. **Zip Slip / Path Traversal Protection:**
   ZIP extraction sanitizes relative paths by verifying that every entry's canonical destination strictly resides inside the sandbox root directory and rejects `../` traversal sequences.
3. **No Secrets in Logs:**
   OkHttp interceptors redact sensitive authorization headers and API tokens from log outputs.
4. **HTTPS Enforced:**
   Cleartext HTTP traffic is disabled. All API communications use TLS 1.3/1.2.
5. **Client-Side vs Proxy Security:**
   Users can enter a Gemini API key locally or configure the included Node.js backend proxy (`backend/server.js`) for centralized credential isolation.

---

## 🗄️ Room Database Schema

### Table: `upload_history`
| Column | Type | Description |
|---|---|---|
| `id` | INTEGER (Primary Key, Auto-generate) | Unique record ID |
| `repo_name` | TEXT | Destination repository (`owner/repo`) |
| `branch` | TEXT | Target branch name |
| `commit_message` | TEXT | User commit message |
| `timestamp` | INTEGER | Timestamp of upload (epoch millis) |
| `status` | TEXT | `SUCCESS` or `FAILED` |
| `commit_sha` | TEXT | Resulting Git commit SHA |
| `file_count` | INTEGER | Number of committed files |
| `error_message` | TEXT | Error trace if failed |

---

## 📦 Build & Installation Instructions

### Local Gradle Build
```bash
# Clone the repository
git clone https://github.com/asifqureshi/gitforge.git
cd gitforge

# Run unit tests
gradle testDebugUnitTest

# Build Debug APK
gradle assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Unit Testing

Unit tests are located in `app/src/test/java/com/asifqureshi/gitforge/GitForgeLogicTest.java` and verify:
- Zip Slip path traversal rejection.
- File ignore filters (`.git`, `node_modules`, `build`, `.DS_Store`).
- FileItem size formatting logic.
- GitHub Git Database models (`CreateTreeRequest`, `CreateBlobRequest`, `CreateCommitRequest`).
- Room entity data structures.
- Gemini AI request serialization.
