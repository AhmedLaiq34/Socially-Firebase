package com.example.i230657

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class for_you_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ForYouPageAdapter
    private val postsList = ArrayList<Post>()
    private val postsRef = FirebaseDatabase.getInstance().getReference("posts")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.for_you_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewForYou)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = ForYouPageAdapter(postsList)
        recyclerView.adapter = adapter

        loadAllPosts()

        // --- Bottom nav & search setup (already done by you) ---
        setupNavigation()
    }

    private fun loadAllPosts() {
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null && post.imagesBase64.isNotEmpty()) {
                        postsList.add(post)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@for_you_page, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupNavigation() {
        val searchBarLayout = findViewById<LinearLayout>(R.id.searchBarLayout)
        searchBarLayout.setOnClickListener {
            startActivity(Intent(this, search_page::class.java))
        }
        findViewById<ImageView>(R.id.bottomNavHome).setOnClickListener {
            startActivity(Intent(this, home_feed::class.java))
        }
        findViewById<ImageView>(R.id.bottomNavCreate).setOnClickListener {
            startActivity(Intent(this, upload_page::class.java))
        }
        findViewById<ImageView>(R.id.bottomNavLikes).setOnClickListener {
            startActivity(Intent(this, notifications_page::class.java))
        }


        val bottomNavProfile = findViewById<ImageView>(R.id.bottomNavProfile)

        bottomNavProfile.setOnClickListener {
            startActivity(Intent(this, profile_page::class.java))
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

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
                    Toast.makeText(this@for_you_page, "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
