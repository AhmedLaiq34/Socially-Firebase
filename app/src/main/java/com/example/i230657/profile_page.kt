package com.example.i230657

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class profile_page : AppCompatActivity() {

    private lateinit var rvProfilePosts: RecyclerView
    private val postsList = ArrayList<Post>()
    private lateinit var postsAdapter: ProfilePostAdapter
    private lateinit var postsRef: DatabaseReference

    // Profile UI
    private lateinit var usernameText: TextView
    private lateinit var nameText: TextView
    private lateinit var bioText: TextView
    private lateinit var profilePic: CircleImageView

    // Stats UI
    private lateinit var postCountText: TextView
    private lateinit var followerCountText: TextView
    private lateinit var followingCountText: TextView

    private lateinit var iconMenu: ImageView


    // Firebase
    private lateinit var userRef: DatabaseReference
    private lateinit var statsRef: DatabaseReference
    private val auth = FirebaseAuth.getInstance()

    private lateinit var bottomNavProfile: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profile_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_main_profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Init Profile Views ---
        usernameText = findViewById(R.id.placeholder_username)
        nameText = findViewById(R.id.placeholder_Name)
        bioText = findViewById(R.id.placeholder_bio)
        profilePic = findViewById(R.id.image_profile_pic)

        // --- Init Stats Views ---
        postCountText = findViewById(R.id.text_posts_count)
        followerCountText = findViewById(R.id.text_followers_count)
        followingCountText = findViewById(R.id.text_following_count)

        followerCountText.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val intent = Intent(this, followers_page::class.java)
                intent.putExtra("userId", currentUser.uid) // pass current logged in user's id
                startActivity(intent)
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        followingCountText.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val intent = Intent(this, following_page::class.java)
                intent.putExtra("userId", currentUser.uid) // pass current logged in user's id
                startActivity(intent)
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Buttons & Navigation ---
        findViewById<MaterialButton>(R.id.btn_edit_profile).setOnClickListener {
            startActivity(Intent(this, edit_profile_page::class.java))
        }

        findViewById<CircleImageView>(R.id.highlight1)?.setOnClickListener {
            startActivity(Intent(this, highlight_page::class.java))
        }



        findViewById<ImageView>(R.id.bottom_nav_home).setOnClickListener {
            startActivity(Intent(this, home_feed::class.java))
        }
        findViewById<ImageView>(R.id.bottom_nav_search).setOnClickListener {
            startActivity(Intent(this, for_you_page::class.java))
        }
        findViewById<ImageView>(R.id.bottom_nav_create).setOnClickListener {
            startActivity(Intent(this, upload_page::class.java))
        }
        findViewById<ImageView>(R.id.bottom_nav_heart).setOnClickListener {
            startActivity(Intent(this, notifications_page::class.java))
        }





        iconMenu = findViewById(R.id.icon_menu)

        iconMenu.setOnClickListener {
            startActivity(Intent(this, logout_page::class.java))
            finish()
        }


        // --- Setup RecyclerView for posts ---
        rvProfilePosts = findViewById(R.id.rvProfilePosts)
        rvProfilePosts.layoutManager = GridLayoutManager(this, 3)
        postsAdapter = ProfilePostAdapter(postsList)
        rvProfilePosts.adapter = postsAdapter

        postsRef = FirebaseDatabase.getInstance().getReference("posts")

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("profile")
            statsRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("stats")

            loadUserProfile()
            loadUserStats()
            loadUserPosts()
            loadProfilePic()
        }
    }

    private fun loadUserPosts() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        postsRef.orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postsList.clear()
                    var postCount = 0

                    for (postSnap in snapshot.children) {
                        val post = postSnap.getValue(Post::class.java)
                        if (post != null) {
                            postsList.add(post)
                            postCount++
                        }
                    }

                    // Sort by newest
                    postsList.sortByDescending { it.createdAt }
                    postsAdapter.notifyDataSetChanged()

                    // 🔑 Update postCount in user stats
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(userId)
                        .child("stats")
                        .child("postCount")
                        .setValue(postCount)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@profile_page, "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadProfilePic(){

        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid


        bottomNavProfile = findViewById(R.id.image_bottom_nav_profile)

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
                            bottomNavProfile.setImageBitmap(bitmap) // ✅ set directly
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@profile_page, "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                }
            })
        }



    }


    private fun loadUserProfile() {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val username = snapshot.child("username").getValue(String::class.java) ?: ""
                    val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                    val profilePicBase64 = snapshot.child("profilePictureUrl").getValue(String::class.java) ?: ""

                    // Set text fields
                    nameText.text = "$firstName $lastName"
                    usernameText.text = username
                    bioText.text = bio

                    // Decode and set profile picture (Base64 → Bitmap)
                    if (profilePicBase64.isNotEmpty()) {
                        try {
                            val imageBytes = Base64.decode(profilePicBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            profilePic.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@profile_page, "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@profile_page, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserStats() {
        statsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val postCount = snapshot.child("postCount").getValue(Int::class.java) ?: 0
                    val followerCount = snapshot.child("followerCount").getValue(Int::class.java) ?: 0
                    val followingCount = snapshot.child("followingCount").getValue(Int::class.java) ?: 0

                    postCountText.text = postCount.toString()
                    followerCountText.text = followerCount.toString()
                    followingCountText.text = followingCount.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@profile_page, "Failed to load stats: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
