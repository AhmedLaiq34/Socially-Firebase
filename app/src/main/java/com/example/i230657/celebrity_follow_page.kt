package com.example.i230657

import User
import UserStats
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class celebrity_follow_page : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvLink: TextView
    private lateinit var ivPfp: CircleImageView
    private lateinit var ivPfpBottom: CircleImageView
    private lateinit var recyclerViewPosts: RecyclerView
    private lateinit var postAdapter: ProfilePostAdapter
    private val postsList = mutableListOf<Post>()

    private lateinit var tvFollowerCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var tvPostCount: TextView

    private lateinit var btnFollow: MaterialButton

    private var currentUserId: String = ""
    private var targetUserId: String = ""
    private var isFollowing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.celebrity_follow_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind profile views
        tvUsername = findViewById(R.id.tvUsername)
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvBio = findViewById(R.id.tvBio)
        tvLink = findViewById(R.id.tvLink)
        ivPfp = findViewById(R.id.pfp)
        ivPfpBottom = findViewById(R.id.bottom_nav_profile)

        tvFollowerCount = findViewById(R.id.tvFollowerCount)
        tvFollowingCount = findViewById<TextView>(R.id.tvFollowingCount)
        tvPostCount = findViewById<TextView>(R.id.tvPostCount)
        btnFollow = findViewById(R.id.btnFollow)

        // Current logged-in user
        currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

        // Target profile user
        targetUserId = intent.getStringExtra("userId") ?: return

        loadUserProfile(targetUserId)
        loadUserPosts(targetUserId)
        checkIfFollowing()

        // Bind RecyclerView
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts)
        recyclerViewPosts.layoutManager = GridLayoutManager(this, 3)
        postAdapter = ProfilePostAdapter(postsList)
        recyclerViewPosts.adapter = postAdapter

        tvFollowerCount.setOnClickListener {
            val intent = Intent(this, followers_page::class.java)
            intent.putExtra("userId", targetUserId)
            startActivity(intent)
        }

        tvFollowingCount.setOnClickListener {
            val intent = Intent(this, following_page::class.java)
            intent.putExtra("userId", targetUserId)
            startActivity(intent)
        }

        // --- Buttons & Nav setup ---
        val backButton = findViewById<ImageView>(R.id.back)
        val message = findViewById<MaterialButton>(R.id.message)

        val bottom_nav_home = findViewById<ImageView>(R.id.bottom_nav_home)
        val bottom_nav_search = findViewById<ImageView>(R.id.bottom_nav_search)
        val bottom_nav_create = findViewById<ImageView>(R.id.bottom_nav_create)
        val bottom_nav_likes = findViewById<ImageView>(R.id.bottom_nav_heart)

        // --- Nav click listeners ---
        backButton.setOnClickListener {
            startActivity(Intent(this, home_feed::class.java))
            finish()
        }
        bottom_nav_home.setOnClickListener {
            startActivity(Intent(this, home_feed::class.java))
            finish()
        }
        bottom_nav_search.setOnClickListener {
            startActivity(Intent(this, for_you_page::class.java))
        }
        bottom_nav_create.setOnClickListener {
            startActivity(Intent(this, select_photo_page::class.java))
        }
        bottom_nav_likes.setOnClickListener {
            startActivity(Intent(this, notis_page::class.java))
        }
        ivPfpBottom.setOnClickListener {
            startActivity(Intent(this, profile_page::class.java))
        }

        message.setOnClickListener {
            startActivity(Intent(this, all_chats_page::class.java))
        }

        // 🔹 Check request/follow status on page load
        val requestRef = FirebaseDatabase.getInstance()
            .getReference("notifications")
            .child(targetUserId)
            .child(currentUserId)

        requestRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    when (status) {
                        "pending" -> updateFollowButton("Requested")
                        "accepted" -> updateFollowButton("Following")
                        "rejected" -> updateFollowButton("Follow")
                        else -> updateFollowButton("Follow")
                    }
                } else {
                    updateFollowButton("Follow")
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // 🔹 Button click logic
        btnFollow.setOnClickListener {
            val currentText = btnFollow.text.toString()
            val dbRef = FirebaseDatabase.getInstance().getReference("users")
            val notifRef = FirebaseDatabase.getInstance().getReference("notifications")
            val requestRef = notifRef.child(targetUserId).child(currentUserId) // this is the "follow request" node

            when (currentText) {
                "Follow" -> {
                    // create request inside Realtime DB
                    val newNotif = Notification(
                        fromUserId = currentUserId,
                        type = "follow_request",
                        status = "pending",
                        timestamp = System.currentTimeMillis()
                    )
                    requestRef.setValue(newNotif)
                    updateFollowButton("Requested")

                }

                "Requested" -> {
                    // cancel request
                    requestRef.removeValue()
                    updateFollowButton("Follow")
                }

                "Following" -> {
                    // unfollow
                    dbRef.child(targetUserId).child("followers").child(currentUserId).removeValue()
                    dbRef.child(currentUserId).child("following").child(targetUserId).removeValue()

                    dbRef.child(targetUserId).child("stats").child("followerCount")
                        .setValue(ServerValue.increment(-1))
                    dbRef.child(currentUserId).child("stats").child("followingCount")
                        .setValue(ServerValue.increment(-1))

                    // also remove the request record just in case
                    requestRef.removeValue()

                    updateFollowButton("Follow")

                    tvFollowerCount.text = (tvFollowerCount.text.toString().toInt() - 1).toString()
                }
            }
        }

    }

    private fun loadUserProfile(userId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        dbRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // --- Load profile ---
                val profileSnapshot = snapshot.child("profile")
                val user = profileSnapshot.getValue(User::class.java)
                if (user != null) {
                    tvUsername.text = user.username
                    tvDisplayName.text = user.displayName
                    tvBio.text = user.bio
                    tvLink.text = user.website

                    // Decode profile picture
                    if (user.profilePictureUrl.isNotEmpty()) {
                        try {
                            val imageBytes = Base64.decode(user.profilePictureUrl, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            ivPfp.setImageBitmap(bitmap)
                            ivPfpBottom.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            ivPfp.setImageResource(R.drawable.placeholder_pfp)
                            ivPfpBottom.setImageResource(R.drawable.placeholder_pfp)
                        }
                    } else {
                        ivPfp.setImageResource(R.drawable.placeholder_pfp)
                        ivPfpBottom.setImageResource(R.drawable.placeholder_pfp)
                    }
                }

                // --- Load stats ---
                val statsSnapshot = snapshot.child("stats")
                val followerCount = statsSnapshot.child("followerCount").getValue(Int::class.java) ?: 0
                val followingCount = statsSnapshot.child("followingCount").getValue(Int::class.java) ?: 0
                val postCount = statsSnapshot.child("postCount").getValue(Int::class.java) ?: 0

                tvFollowerCount.text = followerCount.toString()
                tvFollowingCount.text = followingCount.toString()
                tvPostCount.text = postCount.toString()
            }
        }.addOnFailureListener {
            tvBio.text = "Failed to load user"
        }
    }

    private fun loadUserPosts(userId: String) {
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        val query = postsRef.orderByChild("userId").equalTo(userId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null) {
                        postsList.add(post)
                    }
                }
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors here
            }
        })
    }

    private fun checkIfFollowing() {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        val notiRef = FirebaseDatabase.getInstance().getReference("notifications").child(targetUserId)

        // First check if already following
        dbRef.child(targetUserId).child("followers").child(currentUserId)
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    isFollowing = true
                    updateFollowButton("Following")
                } else {
                    // Then check if request is pending
                    notiRef.orderByChild("fromUserId").equalTo(currentUserId)
                        .get().addOnSuccessListener { notifSnapshot ->
                            var pending = false
                            for (req in notifSnapshot.children) {
                                val status = req.child("status").getValue(String::class.java)
                                if (status == "pending") {
                                    pending = true
                                    break
                                }
                            }
                            if (pending) {
                                updateFollowButton("Requested")
                            } else {
                                updateFollowButton("Follow")
                            }
                        }
                }
            }
    }

    private fun updateFollowButton(state: String) {
        when (state) {
            "Follow" -> {
                btnFollow.text = "Follow"
                btnFollow.isEnabled = true
            }
            "Requested" -> {
                btnFollow.text = "Requested"
                btnFollow.isEnabled = true // allow canceling request
            }
            "Following" -> {
                btnFollow.text = "Following"
                btnFollow.isEnabled = true // allow unfollow
            }
        }
    }

    private fun loadUserStats(userId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        dbRef.child(userId).child("stats").get().addOnSuccessListener { snapshot ->
            val stats = snapshot.getValue(UserStats::class.java)
            stats?.let {
                tvFollowerCount.text = it.followerCount.toString()
                tvFollowingCount.text = it.followingCount.toString()
            }
        }
    }


}
