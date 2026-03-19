package com.example.i230657

import User
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.util.Base64

class home_feed : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private val postsList = ArrayList<Pair<Post, User>>()

    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var postsRef: DatabaseReference
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storiesAdapter: StoryAdapter
    private val userStoriesList = ArrayList<UserStories>()

    private val usersMap = HashMap<String, User>()
    private val followingList = HashSet<String>() // Store users current user is following

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home_feed)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_home_feed)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Firebase references
        database = FirebaseDatabase.getInstance()
        usersRef = database.getReference("users")
        postsRef = database.getReference("posts")

        // --- Posts RecyclerView ---
        recyclerView = findViewById(R.id.recyclerViewPosts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PostAdapter(postsList) { username ->
            val intent = Intent(this, celebrity_follow_page::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        recyclerView.adapter = adapter

        // --- Stories RecyclerView ---
        storiesRecyclerView = findViewById(R.id.recyclerViewStories)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        storiesAdapter = StoryAdapter(userStoriesList) { userStories ->
            val intent = Intent(this, story_page::class.java)
            intent.putExtra("userId", userStories.userId)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        storiesRecyclerView.adapter = storiesAdapter

        // First fetch following list, then users and posts
        fetchFollowingList()
        fetchGroupedStories()

        setupBottomNav()
        setupCameraMessage()
    }

    // ✅ NEW: Fetch the list of users the current user is following
    private fun fetchFollowingList() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Add current user to following list to show their own posts
        followingList.add(currentUserId)

        val followingRef = database.getReference("users")
            .child(currentUserId)
            .child("following")

        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followingList.clear()
                followingList.add(currentUserId) // Always include own posts

                for (followSnap in snapshot.children) {
                    val followedUserId = followSnap.key
                    if (followedUserId != null) {
                        followingList.add(followedUserId)
                    }
                }

                // After fetching following list, fetch users and posts
                fetchUsersAndPosts()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@home_feed, "Failed to load following list", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUsersAndPosts() {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersMap.clear()
                for (userSnap in snapshot.children) {
                    val userId = userSnap.key ?: continue

                    // ✅ Only load users that current user is following
                    if (followingList.contains(userId)) {
                        val profileSnap = userSnap.child("profile")
                        val user = profileSnap.getValue(User::class.java)
                        if (user != null) {
                            usersMap[userId] = user
                        }
                    }
                }
                fetchPosts()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@home_feed, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPosts() {
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                for (postSnap in snapshot.children) {
                    val post = postSnap.getValue(Post::class.java)
                    if (post != null) {
                        // ✅ Only show posts from users in following list
                        if (followingList.contains(post.userId)) {
                            val user = usersMap[post.userId]
                            if (user != null) {
                                postsList.add(Pair(post, user))
                            }
                        }
                    }
                }
                postsList.sortByDescending { it.first.createdAt }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@home_feed, "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ✅ Real-time listener for grouped stories (updates instantly like Instagram)
    private fun fetchGroupedStories() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storiesRef = database.getReference("stories")

        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userStoriesList.clear()

                for (userSnap in snapshot.children) {
                    val userStories = userSnap.getValue(UserStories::class.java)

                    // ✅ Only show stories from users in following list (including own stories)
                    if (userStories != null &&
                        userStories.stories.isNotEmpty() &&
                        (followingList.contains(userStories.userId) || userStories.userId == currentUserId)) {

                        // Sort this user's stories by createdAt (newest last)
                        userStories.stories.sortBy { it.createdAt }
                        userStoriesList.add(userStories)
                    }
                }

                // Sort all users by their latest story (Instagram-style)
                userStoriesList.sortByDescending { it.stories.last().createdAt }

                storiesAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@home_feed, "Failed to load stories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBottomNav() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        val bottomNavSearch = findViewById<ImageView>(R.id.bottomNavSearch)
        val bottomNavCreate = findViewById<ImageView>(R.id.bottomNavCreate)
        val bottomNavLikes = findViewById<ImageView>(R.id.bottomNavLikes)
        val bottomNavProfile = findViewById<ImageView>(R.id.bottomNavProfile)

        bottomNavSearch.setOnClickListener { startActivity(Intent(this, for_you_page::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)}
        bottomNavCreate.setOnClickListener { startActivity(Intent(this, upload_page::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)}
        bottomNavLikes.setOnClickListener { startActivity(Intent(this, notifications_page::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)}
        bottomNavProfile.setOnClickListener { startActivity(Intent(this, profile_page::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)}

        // 🔑 Load current user's profile picture directly
        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("profile")
                .child("profilePictureUrl")

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val base64String = snapshot.getValue(String::class.java)
                    if (!base64String.isNullOrEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            bottomNavProfile.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@home_feed, "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupCameraMessage() {
        val ivCamera = findViewById<ImageView>(R.id.iv_camera)
        val ivMessage = findViewById<ImageView>(R.id.iv_message)

        ivCamera.setOnClickListener {
            startActivity(Intent(this, upload_page::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        ivMessage.setOnClickListener {
            startActivity(Intent(this, all_chats_page::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}