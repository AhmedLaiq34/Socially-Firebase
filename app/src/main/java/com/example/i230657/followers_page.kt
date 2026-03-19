package com.example.i230657

import User
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class followers_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowersAdapter
    private lateinit var database: DatabaseReference
    private val followersList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.followers_page)

        recyclerView = findViewById(R.id.Followers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Firebase reference
        database = FirebaseDatabase.getInstance().reference

        // Get userId from intent
        val userId = intent.getStringExtra("userId")
        if (userId == null) {
            Toast.makeText(this, "No userId passed", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Back button
        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener { finish() }

        loadFollowers(userId)
    }

    private fun loadFollowers(userId: String) {
        val followersRef = database.child("users").child(userId).child("followers")

        followersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followersList.clear()
                if (snapshot.exists()) {
                    val followerIds = snapshot.children.map { it.key!! }

                    // For each followerId, fetch profile
                    for (fid in followerIds) {
                        database.child("users").child(fid).child("profile")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(data: DataSnapshot) {
                                    val user = data.getValue(User::class.java)
                                    if (user != null) {
                                        followersList.add(user)
                                        adapter = FollowersAdapter(followersList)
                                        recyclerView.adapter = adapter
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                } else {
                    Toast.makeText(this@followers_page, "No followers found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@followers_page, "Failed to load followers", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
