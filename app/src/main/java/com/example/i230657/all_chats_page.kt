package com.example.i230657

import User
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.yourapp.adapters.ChatAdapter
import com.yourapp.models.ChatPreview
import java.text.SimpleDateFormat
import java.util.*

class all_chats_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    private lateinit var recyclerViewStatus: RecyclerView

    private lateinit var chatAdapter: ChatAdapter
    private var chatList = mutableListOf<ChatPreview>()
    private lateinit var userChatsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var chatsRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var statusAdapter: StatusAdapter
    private var statusList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.all_chats_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_chat_page)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        setupUI()
        setupRecyclerView()
        setupRecyclerViewStatus()

        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        userChatsRef = FirebaseDatabase.getInstance().getReference("user_chats").child(currentUserId)
        usersRef = FirebaseDatabase.getInstance().getReference("users")
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats")

        fetchChatsFromFirebase(currentUserId)
        fetchUsernamefromDatabase(currentUserId)
    }

    private fun setupUI() {
        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            startActivity(Intent(this, home_feed::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.new_chat_button).setOnClickListener {
            startActivity(Intent(this, add_chats_page::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.search_bar).setOnClickListener {
            startActivity(Intent(this, search_page::class.java))
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.chat_list_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(chatList) { selectedChat ->
            val intent = Intent(this, chat_page::class.java)
            intent.putExtra("receiverId", selectedChat.userId)
            intent.putExtra("receiverUsername", selectedChat.username)
            intent.putExtra("receiverProfileBase64", selectedChat.profileImageBase64)
            startActivity(intent)
        }
        recyclerView.adapter = chatAdapter
    }

    private fun setupRecyclerViewStatus() {
        recyclerViewStatus = findViewById(R.id.recyclerViewStatus)
        recyclerViewStatus.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        statusAdapter = StatusAdapter(statusList) { clickedUser ->
            // Handle status click
            Toast.makeText(this, "Clicked: ${clickedUser.username}", Toast.LENGTH_SHORT).show()
        }
        recyclerViewStatus.adapter = statusAdapter

        fetchStatusesFromFirebase() // load the online/offline users
    }

    private fun fetchStatusesFromFirebase() {
        val currentUserId = auth.currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance().reference

        db.child("users").child(currentUserId).child("following")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    statusList.clear()

                    val followingIds = snapshot.children.mapNotNull { it.key }
                    if (followingIds.isEmpty()) {
                        statusAdapter.notifyDataSetChanged()
                        return
                    }

                    for (uid in followingIds) {
                        db.child("users").child(uid).child("profile")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnap: DataSnapshot) {
                                    val user = userSnap.getValue(User::class.java)
                                    if (user != null) {
                                        statusList.add(user)
                                        statusAdapter.notifyDataSetChanged()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchChatsFromFirebase(currentUserId: String) {
        chatList.clear()

        userChatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()

                if (!snapshot.exists()) {
                    chatAdapter.notifyDataSetChanged()
                    return
                }

                for (chatSnapshot in snapshot.children) {
                    val partnerUserId = chatSnapshot.key ?: continue

                    usersRef.child(partnerUserId).child("profile")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(profileSnap: DataSnapshot) {
                                val username = profileSnap.child("username").getValue(String::class.java) ?: "Unknown"
                                val profileImageBase64 = profileSnap.child("profilePictureUrl").getValue(String::class.java) ?: ""
                                val displayName = profileSnap.child("displayName").getValue(String::class.java) ?: ""

                                chatsRef.child(currentUserId).child(partnerUserId)
                                    .orderByChild("timestamp")
                                    .limitToLast(1)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(chatSnap: DataSnapshot) {
                                            var lastMessage = "Tap to start chatting"
                                            var lastMessageTime = ""

                                            for (msgSnap in chatSnap.children) {
                                                val type = msgSnap.child("type").getValue(String::class.java) ?: "text"
                                                val msgText = msgSnap.child("messageText").getValue(String::class.java) ?: ""
                                                val timestamp = msgSnap.child("timestamp").getValue(Long::class.java)

                                                lastMessage = if (type == "image") {
                                                    "Photo"
                                                } else if (msgText.isNotEmpty()) {
                                                    msgText
                                                } else {
                                                    lastMessage
                                                }

                                                lastMessageTime = if (timestamp != null) getTimeAgo(timestamp) else ""
                                            }

                                            val chatPreview = ChatPreview(
                                                userId = partnerUserId,
                                                username = username,
                                                displayName = displayName,
                                                profileImageBase64 = profileImageBase64,
                                                lastMessage = lastMessage,
                                                lastMessageTime = lastMessageTime
                                            )

                                            chatList.removeAll { it.userId == partnerUserId }
                                            chatList.add(chatPreview)
                                            chatAdapter.notifyDataSetChanged()
                                        }

                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@all_chats_page, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            else -> "${days}d"
        }
    }


    private fun fetchUsernamefromDatabase(userId: String){
        val db = FirebaseDatabase.getInstance().reference
        val currentUser = FirebaseAuth.getInstance().currentUser

        if(currentUser == null){
            return
        } else {

            val userRef = FirebaseDatabase.getInstance()
                .reference
                .child("users")
                .child(currentUser.uid)
                .child("profile")
                .child("username")

            userRef.get().addOnSuccessListener { snapshot ->
                val username = snapshot.value?.toString()
                if (!username.isNullOrEmpty()) {
                    val usernamePreview = findViewById<TextView>(R.id.username_text)
                    usernamePreview.text = username
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error fetching username", Toast.LENGTH_SHORT).show()
            }
        }





    }
}
