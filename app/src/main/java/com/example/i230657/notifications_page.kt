package com.example.i230657

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener



class notifications_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowRequestAdapter
    private val requests = mutableListOf<Notification>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var bottomNavHome: ImageView
    private lateinit var bottomNavCreate: ImageView
    private lateinit var bottomNavProfile: ImageView
    private lateinit var bottomNavSearch: ImageView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notifications_page)

        recyclerView = findViewById(R.id.rvComments)
        adapter = FollowRequestAdapter(requests,
            onAccept = { notif -> handleAccept(notif) },
            onReject = { notif -> handleReject(notif) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchNotifications()

        backButton = findViewById<ImageView>(R.id.back_button)

        backButton.setOnClickListener {
            finish()
        }

        setupBottomNavigation()
    }

    private fun fetchNotifications() {
        val notifRef = FirebaseDatabase.getInstance()
            .getReference("notifications").child(currentUserId)

        notifRef.orderByChild("status").equalTo("pending")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requests.clear()
                    for (notifSnap in snapshot.children) {
                        val notif = notifSnap.getValue(Notification::class.java)
                        notif?.id = notifSnap.key   // keep ID for later
                        if (notif != null) requests.add(notif)
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun handleAccept(notif: Notification) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")

        // Add follower/following relationship
        dbRef.child(currentUserId).child("followers").child(notif.fromUserId).setValue(true)
        dbRef.child(notif.fromUserId).child("following").child(currentUserId).setValue(true)

        // Increment stats
        dbRef.child(currentUserId).child("stats").child("followerCount")
            .setValue(ServerValue.increment(1))
        dbRef.child(notif.fromUserId).child("stats").child("followingCount")
            .setValue(ServerValue.increment(1))

        // Update notification status
        FirebaseDatabase.getInstance().getReference("notifications")
            .child(currentUserId)
            .child(notif.id!!)   // use notif.id, not fromUserId
            .child("status")
            .setValue("accepted")
    }

    private fun handleReject(notif: Notification) {
        FirebaseDatabase.getInstance().getReference("notifications")
            .child(currentUserId)
            .child(notif.id!!)   // use notif.id, not fromUserId
            .child("status")
            .setValue("rejected")
    }

    private fun setupBottomNavigation(){
        bottomNavHome = findViewById(R.id.bottom_nav_home)
        bottomNavCreate = findViewById(R.id.bottom_nav_create)
        bottomNavSearch = findViewById(R.id.bottom_nav_search)
        bottomNavProfile = findViewById(R.id.bottom_nav_profile)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        bottomNavHome.setOnClickListener{
            startActivity(Intent(this, home_feed::class.java))
        }
        bottomNavCreate.setOnClickListener {
            startActivity(Intent(this, upload_page::class.java))
        }
        bottomNavSearch.setOnClickListener {
            startActivity(Intent(this, for_you_page::class.java))
        }
        bottomNavProfile.setOnClickListener {
            startActivity(Intent(this, profile_page::class.java))
        }

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
                    Toast.makeText(this@notifications_page, "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                }
            })
        }

    }





}
