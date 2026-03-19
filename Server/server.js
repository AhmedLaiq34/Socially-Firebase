// server.js
// FCM server: sends push notifications for pending follow requests & new messages

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');
const express = require('express');

const SERVICE_KEY_PATH = path.join(__dirname, 'serviceAccountKey.json');
if (!fs.existsSync(SERVICE_KEY_PATH)) {
  console.error('serviceAccountKey.json not found in project root.');
  process.exit(1);
}

const serviceAccount = JSON.parse(fs.readFileSync(SERVICE_KEY_PATH, 'utf8'));
const DB_URL = 'https://i230657-i230007-assignment02-default-rtdb.firebaseio.com/';
const PORT = process.env.PORT || 3000;

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: DB_URL,
});

const db = admin.database();
const usersRef = db.ref('users');
const notificationsRef = db.ref('notifications');
const chatsRef = db.ref('Chats'); // <-- Added for message notifications

const app = express();

// Poll interval in milliseconds
const POLL_INTERVAL = 5000;

// ============================
// Follow-request notifications
// ============================
async function sendFollowNotification(targetUserId, notifSnap) {
  const notif = notifSnap.val();
  if (!notif || notif.type !== 'follow_request' || notif.status !== 'pending') return;

  try {
    const tokenSnap = await usersRef.child(targetUserId).child('profile').child('fcmToken').once('value');
    const token = tokenSnap.val();
    if (!token) return console.log(`⚠️ No FCM token for user ${targetUserId}`);

    const senderId = notif.fromUserId;
    let senderName = 'Someone';
    if (senderId) {
      const sSnap = await usersRef.child(senderId).child('profile').child('username').once('value');
      senderName = sSnap.val() || senderName;
    }

    const message = {
      token,
      notification: {
        title: 'New follow request',
        body: `${senderName} wants to follow you on Socially!`,
      },
      data: {
        type: 'follow_request',
        senderId,
        receiverId: targetUserId,
      },
      android: { priority: 'high' },
    };

    const resp = await admin.messaging().send(message);
    console.log(`✅ Sent follow-request push to ${targetUserId} (fromUser=${senderId}) resp=${resp}`);
    await notifSnap.ref.update({ notified: true, notifiedAt: Date.now() });
  } catch (err) {
    console.error('❌ Error sending follow notification:', err);
  }
}

// ============================
// Message notifications
// ============================
const sentMessages = new Set();
const serverStartTime = Date.now(); // Timestamp when server started

async function sendMessageNotification(msgSnap) {
  const msg = msgSnap.val();
  if (!msg) return;

  const { senderId, receiverId, timestamp } = msg;

  // Skip if sender = receiver
  if (senderId === receiverId) return;

  // Skip old messages sent before server started
  if (timestamp < serverStartTime) return;

  // Skip duplicates
  if (sentMessages.has(msgSnap.key)) return;
  sentMessages.add(msgSnap.key);

  try {
    const tokenSnap = await usersRef.child(receiverId).child('profile').child('fcmToken').once('value');
    const token = tokenSnap.val();
    if (!token) return console.log(`⚠️ No FCM token for receiver ${receiverId}`);

    const senderSnap = await usersRef.child(senderId).child('profile').child('username').once('value');
    const senderName = senderSnap.val() || 'Someone';

    const messagePayload = {
      token,
      notification: {
        title: senderName,
        body: msg.type === 'text' ? msg.messageText : 'Sent you an image',
      },
      data: {
        type: 'message',
        chatId: `${receiverId}_${senderId}`,
        senderId,
        receiverId,
        messageId: msgSnap.key
      },
      android: { priority: 'high' }
    };

    await admin.messaging().send(messagePayload);
    console.log(`✅ Message notification sent to ${receiverId} from ${senderName}`);
  } catch (err) {
    console.error('❌ Error sending message notification:', err);
  }
}

// ============================
// Poll follow requests
// ============================
async function pollNotifications() {
  try {
    const snapshot = await notificationsRef.once('value');
    if (!snapshot.exists()) return;

    snapshot.forEach((targetSnap) => {
      const targetUserId = targetSnap.key;
      if (!targetUserId) return;

      targetSnap.forEach((notifSnap) => {
        const notif = notifSnap.val();
        if (notif && notif.type === 'follow_request' && notif.status === 'pending' && !notif.notified) {
          sendFollowNotification(targetUserId, notifSnap);
        }
      });
    });
  } catch (err) {
    console.error('❌ Error polling notifications:', err);
  }
}

// ============================
// Listen for new chat messages
// ============================
function listenForNewMessages() {
  chatsRef.on('child_added', (senderSnap) => {
    senderSnap.ref.on('child_added', (receiverSnap) => {
      receiverSnap.ref.on('child_added', (msgSnap) => {
        sendMessageNotification(msgSnap);
      });
    });
  });
}

// ============================
// Start server
// ============================
setInterval(pollNotifications, POLL_INTERVAL);
listenForNewMessages();

app.get('/health', (req, res) => res.send('FCM server ok'));

app.listen(PORT, () => {
  console.log(`🚀 FCM server running on port ${PORT}`);
});
