package com.example.i230657

import User
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class own_posts_feed : AppCompatActivity() {

    private lateinit var rvProfilePosts: RecyclerView
    private lateinit var postAdapter: OwnFeedPostAdapter
    private val postsList = ArrayList<Pair<Post, User>>()

    private val database = FirebaseDatabase.getInstance()
    private val postsRef = database.getReference("posts")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.own_posts_feed)

        rvProfilePosts = findViewById(R.id.rvProfilePosts)
        rvProfilePosts.layoutManager = LinearLayoutManager(this)

        // Back button
        findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }

        // Get userId from intent
        val userId = intent.getStringExtra("USER_ID")
        if (userId != null) {
            loadUserAndPosts(userId)
        } else {
            finish() // no userId passed, close activity
        }
    }

    private fun loadUserAndPosts(userId: String) {
        val usersRef = database.getReference("users").child(userId).child("profile")

        // Step 1: Load that user's profile
        usersRef.get().addOnSuccessListener { profileSnap ->

            val user = User(
                userId = profileSnap.child("userId").getValue(String::class.java) ?: "",
                username = profileSnap.child("username").getValue(String::class.java) ?: "",
                displayName = profileSnap.child("displayName").getValue(String::class.java) ?: "",
                firstName = profileSnap.child("firstName").getValue(String::class.java) ?: "",
                lastName = profileSnap.child("lastName").getValue(String::class.java) ?: "",
                dateOfBirth = profileSnap.child("dateOfBirth").getValue(String::class.java) ?: "",
                phoneNumber = profileSnap.child("phoneNumber").getValue(String::class.java) ?: "",
                bio = profileSnap.child("bio").getValue(String::class.java) ?: "",
                profilePictureUrl = profileSnap.child("profilePictureUrl").getValue(String::class.java) ?: "",
                gender = profileSnap.child("gender").getValue(String::class.java) ?: "",
                website = profileSnap.child("website").getValue(String::class.java) ?: "",
                accountPrivate = profileSnap.child("accountPrivate").getValue(Boolean::class.java) ?: false,
                createdAt = profileSnap.child("createdAt").getValue(Long::class.java) ?: 0L,
                isOnline = profileSnap.child("online").getValue(Boolean::class.java) ?: true,
                lastSeen = profileSnap.child("lastSeen").getValue(Long::class.java) ?: 0L,
                fcmToken = profileSnap.child("fcmToken").getValue(String::class.java) ?: ""
            )

            // Step 2: Load posts for this user
            postsRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        postsList.clear()
                        for (postSnapshot in snapshot.children) {
                            val post = postSnapshot.getValue(Post::class.java) ?: continue
                            postsList.add(Pair(post, user))
                        }
                        postsList.sortByDescending { it.first.createdAt }

                        // Step 3: Initialize or refresh adapter
                        if (!::postAdapter.isInitialized) {
                            postAdapter = OwnFeedPostAdapter(postsList) { username ->
                                // Handle username click if needed
                            }
                            rvProfilePosts.adapter = postAdapter
                        } else {
                            postAdapter.notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }
}

