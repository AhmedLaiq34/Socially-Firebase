# Socially — Firebase-Powered Social Media Android App

> A feature-rich, Instagram-inspired Android social media platform built with **Kotlin**, **Firebase**, and **Agora RTC**, created as a university assignment response to a detailed requirements specification.

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

![Socially System Architecture](C:/Users/ahmed/.gemini/antigravity/brain/d58b94db-a8a5-4509-850b-a37b42a3bc52/socially_architecture_1774548992727.png)

The system is composed of **three tiers**:

1. **Android App (Kotlin)** — The client application handling all UI, user interaction, media processing, and direct Firebase communication.
2. **Firebase (Google Cloud)** — Provides Authentication, real-time NoSQL database, and Cloud Messaging (FCM) token registration.
3. **Node.js Notification Server** — A standalone Express server using the Firebase Admin SDK that polls for pending follow requests and listens for new chat messages, then dispatches FCM push notifications to devices.

---

## ✨ Key Features

![Feature Overview](C:/Users/ahmed/.gemini/antigravity/brain/d58b94db-a8a5-4509-850b-a37b42a3bc52/socially_features_1774549025823.png)

### 🔐 Authentication & Account Management
- Email/password registration and login via **Firebase Authentication**
- **Username-based login** — usernames are resolved to emails via the `usernameLookup` node before authentication
- Username uniqueness enforced at registration via a pre-check query
- Profile picture selected from gallery, compressed to ≤400×400px, and stored as **Base64** in the Realtime Database
- **FCM token** automatically saved to the user profile on every login for push delivery
- Age validation (minimum 13 years) enforced with a `DatePickerDialog`
- Multi-account switching screen (`switch_accounts_page`)
- Online/offline presence tracking with `lastSeen` timestamp

### 📰 Home Feed
- Displays posts only from users the current user **follows** (plus their own)
- Instagram-style **horizontal Stories bar** above the post feed
- Stories are sorted by the most-recently posted story per user
- Posts sorted in reverse-chronological order (newest first)
- Real-time listener via `addValueEventListener` — feed updates instantly without manual refresh
- Tapping a post author's name navigates to their full profile

### 🔍 Explore / For You Page
- Displays a **3-column grid of all posts** across the entire platform
- Tapping the search bar navigates to `search_page` for username lookup
- Uses `ForYouPageAdapter` with a `GridLayoutManager`

### 📸 Post Upload
- Multi-image selection from the gallery (`Intent.EXTRA_ALLOW_MULTIPLE`)
- All selected images converted to Base64 and stored in the `imagesBase64` list field of the `Post` model
- Caption text supported
- Post count in user stats atomically incremented using Firebase **Transaction** API (race-condition safe)
- Tabbed UI to switch between "Post" and "Story" upload modes

### 📖 Stories
- Single-image stories uploaded from gallery
- Grouped by user in `UserStories` objects in the `stories` node
- Stories bar in home feed shows a ring-bordered circular avatar per user
- Tapping opens a full-screen story viewer (`story_page`)
- Own story management via `own_story_page`

### 👤 User Profiles
- Own profile: 3-column post grid, follower/following/post counts, edit profile, highlights
- **Other users' profiles** (`celebrity_follow_page`): same layout with Follow / Requested / Following toggle button
- Follow flow: creates a `notification` record with `type: "follow_request"` and `status: "pending"`
- The target user sees it in `notifications_page` and can Accept (creates bidirectional followers/following entries with atomic stat increments) or Reject
- Unfollow removes both the bidirectional relationship nodes and decrements stats
- Followers/Following lists are tappable and navigate to dedicated list pages

### 💬 Real-Time Messaging (1:1 Chat)
- Message data stored under `/Chats/{senderId}/{receiverId}/{messageId}` and mirrored to the receiver's node
- Supports **text messages** and **image messages** (gallery pick → Base64)
- **Edit message** within 5 minutes via a long-press dialog (updates both sender and receiver nodes)
- **Delete for everyone** within 5 minutes (removes from both nodes)
- **Screenshot detection**: `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` detects new screenshots and writes a `screenshot` notification to the other user's notifications node
- Incoming call banner shown inside the chat if the other user initiates an audio or video call
- Chat list preview (`all_chats_page`) and user search for new chats (`add_chats_page`)

### 📞 Audio & Video Calls
- Powered by **Agora RTC SDK 4.6.0** (`io.agora.rtc:full-sdk`)
- Call signaling via Firebase Realtime Database (`/calls/{callerId}_{receiverId}` node)
- Audio call (`call_page`): mute, speakerphone toggle, end call
- Video call (`video_call_page`): camera toggle, mute, end call
- Incoming call detected via `ChildEventListener` on the calls node — shows an in-chat banner
- Permissions requested at runtime: `RECORD_AUDIO`, `CAMERA`, `BLUETOOTH_CONNECT`

### 🔔 Push Notifications (FCM)
- **MyFirebaseMessagingService** (a `FirebaseMessagingService` subclass) handles incoming FCM messages on-device
- The companion **Node.js server** (`Server/server.js`):
  - Polls `/notifications` every **5 seconds** for `type: "follow_request"` with `status: "pending"` and `notified: false`
  - Listens in real-time via `child_added` on `/Chats` for new messages
  - Dispatches FCM push notifications via **Firebase Admin SDK** using the recipient's stored `fcmToken`
  - Marks follow-request notifications as `notified: true` after dispatching
  - Skips messages sent before server startup to avoid duplicate notifications on restart
- Notification types: **Follow Request**, **New Message**

### 📷 Camera & Content
- In-app camera capture via the device camera (`camera_page`)
- Story draft preview before posting (`story_draft`)
- Post image view page (`post_view_page`)
- Highlight albums accessible from the profile page (`highlight_page`)

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

## 📁 File Structure

```
Socially-master/
├── app/
│   ├── build.gradle.kts              # App-level dependencies & build config
│   ├── google-services.json          # Firebase project configuration
│   └── src/main/
│       ├── AndroidManifest.xml       # All activities, permissions, FCM service declaration
│       ├── java/com/example/i230657/
│       │   │
│       │   ├── ── ACTIVITIES (Screens) ──
│       │   ├── splash_screen.kt          # App entry point
│       │   ├── launch_page.kt            # Pre-auth landing
│       │   ├── login_page.kt             # Email + username login
│       │   ├── signup_page.kt            # Registration with profile pic, DOB, validation
│       │   ├── switch_accounts_page.kt   # Multi-account switcher
│       │   │
│       │   ├── home_feed.kt              # Main feed: posts + stories from following
│       │   ├── for_you_page.kt           # Explore: global post grid
│       │   ├── search_page.kt            # Search users by username
│       │   ├── upload_page.kt            # Multi-image post upload
│       │   ├── upload_page_story.kt      # Story upload
│       │   ├── camera_page.kt            # In-app camera capture
│       │   ├── select_photo_page.kt      # Photo selection helper
│       │   │
│       │   ├── profile_page.kt           # Own profile (grid, stats, edit)
│       │   ├── edit_profile_page.kt      # Edit profile fields
│       │   ├── celebrity_page.kt         # View-only profile
│       │   ├── celebrity_follow_page.kt  # Other user profile with Follow/Unfollow
│       │   ├── followers_page.kt         # Follower list
│       │   ├── following_page.kt         # Following list
│       │   ├── highlight_page.kt         # Story highlights
│       │   ├── own_story_page.kt         # Own story management
│       │   ├── story_page.kt             # Full-screen story viewer
│       │   ├── story_draft.kt            # Story preview before posting
│       │   │
│       │   ├── post_view_page.kt         # Full post detail view
│       │   ├── comments_page.kt          # Post comments
│       │   ├── own_posts_feed.kt         # Own posts full-screen feed
│       │   │
│       │   ├── notifications_page.kt     # Follow request accept/reject
│       │   ├── notis_page.kt             # Activity/notifications list
│       │   ├── notis_page_you.kt         # Alternate notifications view
│       │   │
│       │   ├── all_chats_page.kt         # Chat list / DM inbox
│       │   ├── add_chats_page.kt         # Start a new chat
│       │   ├── chat_page.kt              # 1:1 chat with text, images, call triggers
│       │   ├── call_page.kt              # Audio call (Agora RTC)
│       │   ├── video_call_page.kt        # Video call (Agora RTC)
│       │   │
│       │   ├── logout_page.kt            # Logout confirmation
│       │   │
│       │   ├── ── ADAPTERS ──
│       │   ├── PostAdapter.kt            # Home feed posts RecyclerView
│       │   ├── ProfilePostAdapter.kt     # Profile/celebrity post grid
│       │   ├── CelebProfilePostAdapter.kt
│       │   ├── OwnFeedPostAdapter.kt     # Own posts feed
│       │   ├── ForYouPageAdapter.kt      # Explore grid
│       │   ├── MessageAdapter.kt         # Chat messages (sent/received)
│       │   ├── ChatAdapter.kt            # Chat list previews
│       │   ├── ShareChatAdapter.kt       # Share post to chat
│       │   ├── CommentAdapter.kt         # Comments
│       │   ├── Story_Adaptor.kt          # Stories bar
│       │   ├── StatusAdapter.kt          # Status items
│       │   ├── FollowRequestAdapter.kt   # Follow request notifications
│       │   ├── FollowersAdapter.kt       # Followers list
│       │   ├── FollowingAdapter.kt       # Following list
│       │   ├── SearchAdapter.kt          # User search results
│       │   ├── UserAdapter.kt            # Generic user list
│       │   ├── ImagePagerAdapter.kt      # Multi-image post pager
│       │   │
│       │   ├── ── DATA MODELS ──
│       │   ├── User.kt                   # User profile + account data class
│       │   ├── Post.kt                   # Post model (id, userId, imagesBase64, likes)
│       │   ├── Story.kt / UserStories.kt # Story and grouped-story models
│       │   ├── Message.kt                # Chat message model
│       │   ├── Comment.kt                # Comment model
│       │   ├── Notifcation.kt            # Notification/follow-request model
│       │   ├── ChatPreview.kt            # Chat list item model
│       │   ├── AudioCall.kt              # Call signaling model (type, status)
│       │   │
│       │   ├── ── SERVICES ──
│       │   ├── MyFirebaseMessagingService.kt  # FCM message handler
│       │   │
│       │   └── ── DIALOG FRAGMENTS ──
│       │       └── SharePostDialogFragment.kt
│       │
│       └── res/
│           ├── layout/     # 40+ XML layouts (one per screen/item)
│           ├── drawable/   # 80+ drawables: icons, backgrounds, gradients, borders
│           ├── font/       # Instagram Sans typeface family (5 weights)
│           ├── values/     # colors.xml, strings.xml, themes.xml
│           ├── xml/        # file_paths, provider_paths, backup_rules
│           └── mipmap-*/   # App launcher icons (all densities)
│
├── Server/
│   ├── server.js             # Node.js FCM dispatcher (Express + Firebase Admin)
│   └── serviceAccountKey.json # Firebase service account credentials
│
├── build.gradle.kts          # Project-level Gradle config
├── settings.gradle.kts       # Module includes
├── gradle.properties         # Gradle JVM / build properties
├── gradlew / gradlew.bat     # Gradle wrapper scripts
└── Assignment.pdf            # Original requirements specification
```

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

## 🎓 Assignment Context

This project was built as a graded university assignment. The `Assignment.pdf` in the repository root contains the full requirements specification that drove the feature set, including the social graph model, messaging requirements, call functionality, notification subsystem, and profile management details.

---

*Built with ❤️ using Kotlin, Firebase, and Agora RTC*
