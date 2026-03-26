# Socially — Firebase-Powered Social Media Android App

> A feature-rich, Instagram-inspired Android social media platform built with **Kotlin**, **Firebase**, and **Agora RTC**.

---

## 📋 Summary

**Socially** is a full-stack Android social media application that replicates and extends core Instagram-like social functionality. Users can sign up, follow others, post images, watch stories, message each other in real time, make audio/video calls, and receive push notifications — all backed by Firebase's real-time infrastructure and a companion Node.js notification server.

| Property | Value |
|---|---|
| **Platform** | Android (minSdk 24 / API 36 target) |
| **Language** | Kotlin |
| **Architecture** | Activity-based MVC |
| **Backend** | Firebase (Auth + Realtime DB + FCM) |
| **RTC Engine** | Agora SDK 4.6.0 |
| **Notification Server** | Node.js + Express + Firebase Admin SDK |

---

## 🏗️ System Architecture

<img src="assets/Socially.png" alt="Socially" width="400"/>

The system is composed of **three tiers**:

1. **Android App (Kotlin)** — The client application handling all UI, user interaction, media processing, and direct Firebase communication.
2. **Firebase (Google Cloud)** — Provides Authentication, real-time NoSQL database, and Cloud Messaging (FCM) token registration.
3. **Node.js Notification Server** — A standalone Express server using the Firebase Admin SDK that polls for pending follow requests and listens for new chat messages, then dispatches FCM push notifications to devices.

---

## ✨ Key Features


### 🔐 Authentication & Account Management
- Email/password registration and login via **Firebase Authentication**
- **Username-based login** — usernames are resolved to emails before authentication
- Username uniqueness enforced at registration via a pre-check query
- Profile picture selected from gallery, compressed to ≤400×400px, and stored as **Base64** in the Realtime Database
- **FCM token** automatically saved to the user profile on every login for push delivery
- Age validation (minimum 13 years) enforced.
- Multi-account switching screen.
- Online/offline presence tracking.

### 📰 Home Feed
- Displays posts only from users the current user **follows** (plus their own)
- Instagram-style **horizontal Stories bar** above the post feed
- Stories are sorted by the most-recently posted story per user
- Posts sorted in reverse-chronological order (newest first)
- Real-time listener via `addValueEventListener` — feed updates instantly without manual refresh
- Tapping a post author's name navigates to their full profile

### 🔍 Explore / For You Page
- Displays a **3-column grid of all posts** across the entire platform
- Tapping the search bar oprns search interface for username lookup

### 📸 Post Upload
- Multi-image selection from the gallery.
- Caption text supported
- Post count in user stats atomically incremented using Firebase **Transaction** API (race-condition safe)
- Tabbed UI to switch between "Post" and "Story" upload modes

### 📖 Stories
- Single-image stories uploaded from gallery
- Grouped by user
- Stories bar in home feed shows a ring-bordered circular avatar per user
- Tapping opens a full-screen story viewer
- Own story management

### 👤 User Profiles
- Own profile: 3-column post grid, follower/following/post counts, edit profile, highlights
- **Other users' profiles**: same layout with Follow / Requested / Following toggle button
- Followers/Following lists are tappable and navigate to dedicated list pages

### 💬 Real-Time Messaging (1:1 Chat)
- Message data stored under `/Chats/{senderId}/{receiverId}/{messageId}` and mirrored to the receiver's node
- Supports **text messages** and **image messages** (gallery pick → Base64)
- **Edit message** within 5 minutes via a long-press dialog (updates both sender and receiver nodes)
- **Delete for everyone** within 5 minutes (removes from both nodes)
- **Screenshot detection**: `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` detects new screenshots and writes a `screenshot` notification to the other user's notifications node
- Incoming call banner shown inside the chat if the other user initiates an audio or video call
- Chat list preview and user search for new chats

### 📞 Audio & Video Calls
- Powered by **Agora RTC SDK 4.6.0** (`io.agora.rtc:full-sdk`)
- Call signaling via Firebase Realtime Database
- Audio call: mute, speakerphone toggle, end call
- Video call: camera toggle, mute, end call
- Incoming call detectede — shows an in-chat banner

### 🔔 Push Notifications (FCM)
- **MyFirebaseMessagingService** (a `FirebaseMessagingService` subclass) handles incoming FCM messages on-device
- The companion **Node.js server**:
  - Listens in real-time via `child_added` on `/Chats` for new messages
  - Dispatches FCM push notifications via **Firebase Admin SDK** using the recipient's stored `fcmToken`
  - Skips messages sent before server startup to avoid duplicate notifications on restart
- Notification types: **Follow Request**, **New Messages**

### 📷 Camera & Content
- In-app camera capture via the device camera
- Story draft preview before posting 
- Post image view page 
- Highlight albums accessible from the profile page

---

## 🛠️ Technologies Used

| Technology | Version | Role |
|---|---|---|
| **Kotlin** | JVM Target 11 | Primary language for all Android activities, adapters, and data classes |
| **Android SDK** | compileSdk 36 / minSdk 24 | Platform target (Android 9.0+) |
| **Firebase Authentication** | BOM 33.6.0 | Email/password user auth, session management |
| **Firebase Realtime Database** | BOM 33.6.0 | Primary NoSQL real-time data store for all app data |
| **Firebase Cloud Messaging** | 23.4.1 | Push notification delivery to devices |
| **Agora RTC Full SDK** | 4.6.0 | Audio and video calling over WebRTC |
| **Glide** | 4.16.0 | Image loading and caching library |
| **CircleImageView** | 3.1.0 | Circular profile picture views |
| **OkHttp3** | 4.12.0 | HTTP client (used alongside Volley for API requests) |
| **Volley** | — | Network request queue (used in celebrity profile page) |
| **Material Components** | — | MaterialButton, themed UI components |
| **AndroidX ConstraintLayout** | — | Flexible constraint-based layouts |
| **Node.js + Express** | — | Lightweight HTTP server for the FCM notification dispatcher |
| **Firebase Admin SDK (Node)** | — | Server-side Firebase access for notification polling and dispatch |
| **Gradle (Kotlin DSL)** | — | Build system with `.kts` build scripts |

---



## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- A Firebase project with Authentication, Realtime Database, and FCM enabled
- Node.js 18+ (for the notification server)

### Android App Setup
1. Clone the repository.
2. Open the project in Android Studio.
3. Place your `google-services.json` in `app/`.
4. Sync Gradle and build the project.
5. Run on a device or emulator with API 24+.

### Notification Server Setup
```bash
cd Server
npm install firebase-admin express
# Add your serviceAccountKey.json
node server.js
```
The server listens on port **3000** by default (configurable via `PORT` environment variable).

---

## 📐 Design Patterns & Technical Highlights

| Pattern / Technique | Where Used |
|---|---|
| **Real-time listeners** (`addValueEventListener`) | Home feed, chat, stories, notifications |
| **Single-event listeners** (`addListenerForSingleValueEvent`) | Profile load, stat fetches, call check |
| **Firebase Transactions** | `postCount` increment on upload (atomic, race-free) |
| **Firebase `ServerValue.increment()`** | Follower / following count on accept/unfollow |
| **Base64 image storage** | Profile pictures, post images, story images, chat images |
| **RecyclerView with multiple adapters** | Each screen has a purpose-built adapter |
| **Agora IRtcEngineEventHandler** | Handles call join, user join/leave, error events |
| **ContentObserver** | Screenshot detection in chat |
| **FCM polling (Node.js)** | `setInterval` every 5s for follow-request notifications |
| **FCM real-time (Node.js)** | `child_added` listener on `/Chats` for message notifications |
| **Username → email resolution** | `usernameLookup` node enables username-based login |

---


*Built with ❤️ using Kotlin, Firebase, and Agora RTC*
