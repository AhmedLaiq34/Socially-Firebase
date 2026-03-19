package com.example.i230657

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import android.net.Uri
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import java.io.ByteArrayOutputStream
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.yourapp.adapters.MessageAdapter
import com.yourapp.models.Message
import de.hdodenhof.circleimageview.CircleImageView

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper


import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build

class chat_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    private lateinit var dbRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var btnSend: ImageView
    private lateinit var btnAttach: ImageView
    private lateinit var inputMessage: EditText
    private lateinit var profileImage: CircleImageView
    private lateinit var txtUsername: TextView

    private var receiverId: String? = null
    private val PICK_IMAGE_REQUEST = 101

    private lateinit var callBanner: LinearLayout
    private lateinit var callRef: DatabaseReference

    private lateinit var videoCallBanner: LinearLayout

    // Add these properties to your chat_page class
    private var screenshotObserver: ContentObserver? = null
    private val screenshotHandler = Handler(Looper.getMainLooper())
    private var lastScreenshotTime = 0L
    private var lastScreenshotPath = ""



    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.chat_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_root_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("Chats")
        usersRef = FirebaseDatabase.getInstance().getReference("users")

        receiverId = intent.getStringExtra("receiverId")

        if (receiverId == null) {
            Toast.makeText(this, "Error: Receiver ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recycler_view_messages)
        btnSend = findViewById(R.id.btn_send)
        inputMessage = findViewById(R.id.input_message)
        profileImage = findViewById(R.id.img_profile_picture)
        txtUsername = findViewById(R.id.txt_username)
        btnAttach = findViewById(R.id.btn_attach)

        loadReceiverProfile(receiverId!!)
        // --- Setup RecyclerView ---
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager

        messageAdapter = MessageAdapter(messageList) { message ->
            showMessageOptionsDialog(message)
        }
        recyclerView.adapter = messageAdapter

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            startActivity(Intent(this, all_chats_page::class.java))
            finish()
        }

        btnSend.setOnClickListener { sendMessage() }
        btnAttach.setOnClickListener { openGallery() }
        fetchMessages()

        callBanner = findViewById(R.id.callBanner)
        callRef = FirebaseDatabase.getInstance().getReference("calls")



        findViewById<ImageView>(R.id.call)?.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener
            val receiver = receiverId ?: return@setOnClickListener

            // 1. Create call entry in Firebase
            val callKey = "${currentUserId}_${receiver}"
            val callData = AudioCall(type = "voice", status = "calling")
            callRef.child(callKey).setValue(callData)

            // 2. Start the call page
            val intent = Intent(this, call_page::class.java)
            intent.putExtra("userId", receiver)
            startActivity(intent)
        }

        callBanner.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener
            val receiver = receiverId ?: return@setOnClickListener

            // 1. Start the call page FIRST
            val intent = Intent(this, call_page::class.java)
            intent.putExtra("userId", receiver)
            startActivity(intent)

            // 2. Then remove the incoming call entry
            FirebaseDatabase.getInstance().getReference("calls")
                .child("${receiver}_$currentUserId")
                .removeValue()
        }

        videoCallBanner = findViewById(R.id.videoCallBanner)

        videoCallBanner.setOnClickListener {
            val currentUserIdVideo = auth.currentUser?.uid ?: return@setOnClickListener
            val receiver = receiverId ?: return@setOnClickListener

            // 1. Start video call page
            val intent = Intent(this, video_call_page::class.java)
            intent.putExtra("userId", receiver)
            startActivity(intent)

            // 2. Remove the incoming video call entry
            FirebaseDatabase.getInstance().getReference("calls")
                .child("${receiver}_$currentUserIdVideo")
                .removeValue()
        }

        findViewById<ImageView>(R.id.video_call)?.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener
            val receiver = receiverId ?: return@setOnClickListener

            // 1. Create call entry in Firebase (mark it as video)
            val callKey = "${currentUserId}_${receiver}_video"
            val callData = AudioCall(type = "video", status = "calling")
            callRef.child(callKey).setValue(callData)

            // 2. Start the video call page
            val intent = Intent(this, video_call_page::class.java)
            intent.putExtra("userId", receiver)
            startActivity(intent)
        }

        listenForIncomingCalls()
        checkAndRequestPermissions()
    }



    private fun listenForIncomingCalls() {
        val currentUserId = auth.currentUser?.uid ?: return
        val incomingCallRef = FirebaseDatabase.getInstance().getReference("calls")

        incomingCallRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleCallSnapshot(snapshot, currentUserId)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleCallSnapshot(snapshot, currentUserId)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                callBanner.visibility = View.GONE
                videoCallBanner.visibility = View.GONE
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun handleCallSnapshot(snapshot: DataSnapshot, currentUserId: String) {
        val call = snapshot.getValue(AudioCall::class.java) ?: return

        val key = snapshot.key ?: return
        val keyParts = key.split("_")
        if (keyParts.size < 2) return

        val senderId = keyParts[0]
        val receiver = keyParts[1]
        val isVideo = keyParts.size == 3 && keyParts[2] == "video"

        if (receiver != currentUserId) return // only show calls meant for me

        runOnUiThread {
            if (call.status == "calling") {
                if (isVideo) {
                    videoCallBanner.visibility = View.VISIBLE
                    callBanner.visibility = View.GONE
                } else {
                    callBanner.visibility = View.VISIBLE
                    videoCallBanner.visibility = View.GONE
                }
            } else {
                callBanner.visibility = View.GONE
                videoCallBanner.visibility = View.GONE
            }
        }
    }

    //  UPDATED: Message options now include “Edit”
    //  UPDATED: Message options now include “Edit” with 5-minute timer
    // UPDATED: Edit & Delete allowed only within 5 minutes
    private fun showMessageOptionsDialog(message: Message) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Message options")
        builder.setItems(arrayOf("Edit", "Delete", "Cancel")) { dialog, which ->
            val currentTime = System.currentTimeMillis()
            val messageTime = message.timestamp
            val diff = currentTime - messageTime

            when (which) {
                0 -> { // Edit
                    if (diff <= 5 * 60 * 1000) { // 5 minutes
                        showEditMessageDialog(message)
                    } else {
                        Toast.makeText(this, "You can only edit messages within 5 minutes", Toast.LENGTH_SHORT).show()
                    }
                }

                1 -> { // Delete
                    if (diff <= 5 * 60 * 1000) { // 5 minutes
                        deleteMessageForBoth(message)
                    } else {
                        Toast.makeText(this, "You can only delete messages within 5 minutes", Toast.LENGTH_SHORT).show()
                    }
                }

                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }


    // ✅ Step 1: Popup dialog for editing message
    private fun showEditMessageDialog(message: Message) {
        val editText = EditText(this)
        editText.setText(message.messageText)

        AlertDialog.Builder(this)
            .setTitle("Edit message")
            .setView(editText)
            .setPositiveButton("Save") { dialog, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    updateMessageForBoth(message, newText)
                } else {
                    Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // ✅ Step 1 helper: Update message in both sender & receiver nodes
    private fun updateMessageForBoth(message: Message, newText: String) {
        val senderId = auth.currentUser?.uid ?: return
        val receiver = receiverId ?: return
        val messageId = message.messageId ?: return

        val updatedMessage = message.copy(messageText = newText)

        val senderRef = dbRef.child(senderId).child(receiver).child(messageId)
        val receiverRef = dbRef.child(receiver).child(senderId).child(messageId)

        senderRef.setValue(updatedMessage).addOnSuccessListener {
            receiverRef.setValue(updatedMessage)
            Toast.makeText(this, "Message updated", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReceiverProfile(userId: String) {
        usersRef.child(userId).child("profile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    val displayName = snapshot.child("displayName").getValue(String::class.java) ?: username
                    val profileBase64 = snapshot.child("profilePictureUrl").getValue(String::class.java) ?: ""
                    txtUsername.text = displayName.ifEmpty { username }
                    decodeProfileImage(profileBase64)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@chat_page, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun decodeProfileImage(base64String: String?) {
        if (!base64String.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            profileImage.setImageResource(R.drawable.placeholder_pfp)
        }
    }

    private fun sendMessage() {
        val messageText = inputMessage.text.toString().trim()
        val senderId = auth.currentUser?.uid ?: return
        val receiver = receiverId ?: return

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = System.currentTimeMillis()
        val messageId = dbRef.child(senderId).child(receiver).push().key ?: return

        val message = Message(
            senderId = senderId,
            receiverId = receiver,
            messageText = messageText,
            imageUrl = "",
            timestamp = timestamp,
            type = "text",
            messageId = messageId
        )

        val senderRef = dbRef.child(senderId).child(receiver).child(messageId)
        val receiverRef = dbRef.child(receiver).child(senderId).child(messageId)

        senderRef.setValue(message).addOnSuccessListener {
            receiverRef.setValue(message)
            inputMessage.text.clear()
        }
    }

    private fun sendImageMessage(imageBase64: String) {
        val senderId = auth.currentUser?.uid ?: return
        val receiver = receiverId ?: return
        val timestamp = System.currentTimeMillis()
        val messageId = dbRef.child(senderId).child(receiver).push().key ?: return

        val message = Message(
            senderId = senderId,
            receiverId = receiver,
            messageText = "",
            imageUrl = imageBase64,
            timestamp = timestamp,
            type = "image",
            messageId = messageId
        )

        val senderRef = dbRef.child(senderId).child(receiver).child(messageId)
        val receiverRef = dbRef.child(receiver).child(senderId).child(messageId)

        senderRef.setValue(message).addOnSuccessListener {
            receiverRef.setValue(message)
        }

        messageList.add(message)
        messageAdapter.notifyItemInserted(messageList.size - 1)
        recyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun sendPostMessage() {
        val senderId = auth.currentUser?.uid ?: return
        val receiver = receiverId ?: return
        val timestamp = System.currentTimeMillis()
        val messageId = dbRef.child(senderId).child(receiver).push().key ?: return

        val message = Message(
            senderId = senderId,
            receiverId = receiver,
            messageText = "",
            imageUrl = "",
            timestamp = timestamp,
            type = "post",
            messageId = messageId
        )

        val senderRef = dbRef.child(senderId).child(receiver).child(messageId)
        val receiverRef = dbRef.child(receiver).child(senderId).child(messageId)

        senderRef.setValue(message).addOnSuccessListener {
            receiverRef.setValue(message)
        }

        messageList.add(message)
        messageAdapter.notifyItemInserted(messageList.size - 1)
        recyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun fetchMessages() {
        val senderId = auth.currentUser?.uid ?: return
        val receiver = receiverId ?: return
        val chatRef = dbRef.child(senderId).child(receiver)

        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (msgSnapshot in snapshot.children) {
                    val message = msgSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        message.messageId = msgSnapshot.key
                        messageList.add(message)
                    }
                }
                messageAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messageList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chat_page, "Error loading messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri? = data.data
            imageUri?.let {
                val imageBase64 = convertImageToBase64(it)
                sendImageMessage(imageBase64)
            }
        }
    }

    private fun convertImageToBase64(imageUri: Uri): String {
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun deleteMessageForBoth(message: Message) {
        val senderId = auth.currentUser?.uid ?: return
        val receiver = receiverId ?: return
        val messageId = message.messageId ?: return

        val senderRef = dbRef.child(senderId).child(receiver).child(messageId)
        val receiverRef = dbRef.child(receiver).child(senderId).child(messageId)

        senderRef.removeValue().addOnSuccessListener {
            receiverRef.removeValue()
            messageList.remove(message)
            messageAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Message deleted for everyone", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show()
        }
    }




    private fun setupScreenshotDetection() {
        screenshotObserver = object : ContentObserver(screenshotHandler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                detectScreenshot()
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver!!
        )
    }

    private fun detectScreenshot() {
        val projection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                val imagePath = cursor.getString(dataColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val currentTime = System.currentTimeMillis() / 1000

                // Check if image was added within last 3 seconds and path contains "screenshot"
                if (currentTime - dateAdded <= 3 &&
                    imagePath.lowercase().contains("screenshot")) {
                    onScreenshotTaken()
                }
            }
        }
    }

    private fun onScreenshotTaken() {
        val senderId = auth.currentUser?.uid ?: return
        val receiver = receiverId ?: return

        // Send notification to Firebase
        sendScreenshotNotification(senderId, receiver)

        // Optional: Show toast to current user
        Toast.makeText(this, "Screenshot detected", Toast.LENGTH_SHORT).show()
    }

    private fun sendScreenshotNotification(senderId: String, receiverId: String) {
        val notificationRef = FirebaseDatabase.getInstance()
            .getReference("notifications")
            .child(receiverId)
            .push()

        val notificationData = mapOf(
            "type" to "screenshot",
            "fromUserId" to senderId,
            "timestamp" to System.currentTimeMillis(),
            "status" to "pending",
            "notified" to false,
            "message" to "took a screenshot of your chat"
        )

        notificationRef.setValue(notificationData)
            .addOnSuccessListener {
                println("Screenshot notification sent to $receiverId")
            }
            .addOnFailureListener { e ->
                println("Failed to send screenshot notification: ${e.message}")
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        screenshotObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    private val PERMISSION_REQUEST_CODE = 100

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                setupScreenshotDetection()
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                setupScreenshotDetection()
            }
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupScreenshotDetection()
                Toast.makeText(this, "Screenshot detection enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Permission denied. Screenshot detection disabled.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
