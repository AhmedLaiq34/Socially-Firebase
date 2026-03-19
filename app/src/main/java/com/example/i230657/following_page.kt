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

class following_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowingAdapter
    private lateinit var database: DatabaseReference
    private val followingList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.following_page)

        recyclerView = findViewById(R.id.Following)
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

        loadFollowing(userId)

    }

    private fun loadFollowing(userId: String) {
        val followingRef = database.child("users").child(userId).child("following")

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followingList.clear()
                if (snapshot.exists()) {
                    val followingIds = snapshot.children.map { it.key!! }

                    // For each followingId, fetch profile
                    for (fid in followingIds) {
                        database.child("users").child(fid).child("profile")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(data: DataSnapshot) {
                                    val user = data.getValue(User::class.java)
                                    if (user != null) {
                                        followingList.add(user)
                                        adapter = FollowingAdapter(followingList)
                                        recyclerView.adapter = adapter
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                } else {
                    Toast.makeText(this@following_page, "No following found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@following_page, "Failed to load following", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
