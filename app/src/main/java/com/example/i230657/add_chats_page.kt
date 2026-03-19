package com.example.i230657

import User
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class add_chats_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userList: MutableList<User>
    private lateinit var userAdapter: UserAdapter
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var userChatsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_chats_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        userList = mutableListOf()
        userAdapter = UserAdapter(userList) { selectedUser ->
            addUserToChatList(selectedUser)
        }

        recyclerView.adapter = userAdapter

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")
        userChatsRef = FirebaseDatabase.getInstance().getReference("user_chats")

        fetchAllUsers()
    }

    private fun fetchAllUsers() {
        val currentUserId = auth.currentUser?.uid
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()

                for (userSnapshot in snapshot.children) {
                    val profileSnapshot = userSnapshot.child("profile")
                    val user = profileSnapshot.getValue(User::class.java)

                    if (user != null && user.userId != currentUserId) {
                        userList.add(user)
                    }
                }

                if (userList.isEmpty()) {
                    Toast.makeText(this@add_chats_page, "No users found", Toast.LENGTH_SHORT).show()
                }

                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@add_chats_page, "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addUserToChatList(selectedUser: User) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Check if chat already exists
        userChatsRef.child(currentUserId).child(selectedUser.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(
                            this@add_chats_page,
                            "Chat already exists with ${selectedUser.username}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Add new chat entry
                        userChatsRef.child(currentUserId).child(selectedUser.userId).setValue(true)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@add_chats_page,
                                    "${selectedUser.username} added to chats",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Optionally also add reverse link (for two-way chats)
                                userChatsRef.child(selectedUser.userId).child(currentUserId).setValue(true)

                                // Return to all_chats_page
                                val intent = Intent(this@add_chats_page, all_chats_page::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this@add_chats_page,
                                    "Failed to add chat: ${it.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@add_chats_page, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
