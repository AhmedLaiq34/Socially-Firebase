package com.example.i230657

import User
import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class search_page : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchAdapter
    private lateinit var database: DatabaseReference
    private lateinit var clearTextView: TextView

    private var allUsers = mutableListOf<User>()

    private lateinit var allFilter: TextView
    private lateinit var followerFilter: TextView
    private lateinit var followingFilter: TextView

    private var currentFilter = "all"

    // For current user's data
    private var currentUserId: String? = null
    private val followersList = mutableSetOf<String>()
    private val followingList = mutableSetOf<String>()




    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_page)

        searchEditText = findViewById(R.id.search_bar)
        recyclerView = findViewById(R.id.rvSearchResults)
        clearTextView = findViewById(R.id.tvClear)

        adapter = SearchAdapter(this, allUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().reference.child("users")

        loadUsers()

        // Clear button functionality
        clearTextView.setOnClickListener {
            searchEditText.text.clear()
        }

        // Handle clicks on end drawable of EditText
        searchEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd: Drawable? = searchEditText.compoundDrawablesRelative[2]
                if (drawableEnd != null &&
                    event.rawX >= (searchEditText.right - drawableEnd.bounds.width() - searchEditText.paddingEnd)
                ) {
                    searchEditText.text.clear()
                    searchEditText.performClick() // 👈 accessibility fix
                    return@setOnTouchListener true
                }
            }
            false
        }


        // Search filter
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        allFilter = findViewById(R.id.all_filter)
        followerFilter = findViewById(R.id.follower_filter)
        followingFilter = findViewById(R.id.following_filter)

// Suppose you are using FirebaseAuth for logged in user
        currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

// Load followers/following
        if (currentUserId != null) {
            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUserId!!)
                .child("followers")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        followersList.clear()
                        for (child in snapshot.children) {
                            followersList.add(child.key!!)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUserId!!)
                .child("following")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        followingList.clear()
                        for (child in snapshot.children) {
                            followingList.add(child.key!!)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        allFilter.setOnClickListener {
            currentFilter = "all"
            updateFilterUI()
            filter(searchEditText.text.toString())
            recyclerView.scrollToPosition(0)
        }

        followerFilter.setOnClickListener {
            currentFilter = "followers"
            updateFilterUI()
            filter(searchEditText.text.toString())
            recyclerView.scrollToPosition(0)
        }

        followingFilter.setOnClickListener {
            currentFilter = "following"
            updateFilterUI()
            filter(searchEditText.text.toString())
            recyclerView.scrollToPosition(0)
        }



    }

    private fun updateFilterUI() {
        allFilter.setTextColor(
            if (currentFilter == "all") getColor(R.color.brown) else getColor(android.R.color.darker_gray)
        )
        followerFilter.setTextColor(
            if (currentFilter == "followers") getColor(R.color.brown) else getColor(android.R.color.darker_gray)
        )
        followingFilter.setTextColor(
            if (currentFilter == "following") getColor(R.color.brown) else getColor(android.R.color.darker_gray)
        )
    }


    private fun loadUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                for (userSnap in snapshot.children) {
                    val profileSnap = userSnap.child("profile")
                    val user = profileSnap.getValue(User::class.java)
                    if (user != null) {
                        allUsers.add(user)
                    }
                }
                adapter.updateList(allUsers)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filter(query: String) {
        var filteredList = allUsers.filter {
            it.username.contains(query, ignoreCase = true) ||
                    it.displayName.contains(query, ignoreCase = true)
        }

        when (currentFilter) {
            "followers" -> filteredList = filteredList.filter { followersList.contains(it.userId) }
            "following" -> filteredList = filteredList.filter { followingList.contains(it.userId) }
        }

        adapter.updateList(filteredList)
    }

}
