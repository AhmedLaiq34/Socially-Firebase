package com.example.i230657

import User
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class comments_page : AppCompatActivity() {

    private lateinit var rvComments: RecyclerView
    private lateinit var etAddComment: EditText
    private lateinit var ivSendComment: ImageView
    private lateinit var civCurrentUser: CircleImageView

    private lateinit var backButton: ImageView

    private lateinit var adapter: CommentAdapter
    private val commentsList = ArrayList<Pair<Comment, User>>()
    private val usersMap = HashMap<String, User>()

    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var commentsRef: DatabaseReference

    private lateinit var postId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.comments_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get postId from intent
        postId = intent.getStringExtra("postId") ?: ""
        if (postId.isEmpty()) {
            Toast.makeText(this, "No post specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Find views
        rvComments = findViewById<RecyclerView>(R.id.rvComments)
        etAddComment = findViewById<EditText>(R.id.etAddComment)
        ivSendComment = findViewById<ImageView>(R.id.ivSendComment)
        civCurrentUser = findViewById<CircleImageView>(R.id.civCurrentUser)
        backButton = findViewById<ImageView>(R.id.back_button)

        backButton.setOnClickListener {
            finish()
        }


        // Initialize Firebase references
        database = FirebaseDatabase.getInstance()
        usersRef = database.getReference("users")
        commentsRef = database.getReference("comments") // store all comments under "comments"

        // Setup RecyclerView
        rvComments.layoutManager = LinearLayoutManager(this)
        adapter = CommentAdapter(commentsList)
        rvComments.adapter = adapter

        loadUsers()
        setupCurrentUserProfile()
        setupSendComment()
    }

    private fun loadUsers() {
        // Load all users to map for username/profile image lookup
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersMap.clear()
                for (userSnap in snapshot.children) {
                    val profileSnap = userSnap.child("profile")
                    val user = profileSnap.getValue(User::class.java)
                    if (user != null) {
                        usersMap[userSnap.key!!] = user
                    }
                }
                loadComments()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@comments_page, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadComments() {
        // Listen for changes under comments/{postId}/
        commentsRef.child(postId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentsList.clear()
                for (commentSnap in snapshot.children) {
                    val comment = commentSnap.getValue(Comment::class.java)
                    if (comment != null) {
                        val user = usersMap[comment.userId]
                        if (user != null) {
                            commentsList.add(Pair(comment, user))
                        }
                    }
                }
                // Sort by createdAt ascending (oldest first)
                commentsList.sortBy { it.first.createdAt }
                adapter.notifyDataSetChanged()
                rvComments.scrollToPosition(commentsList.size - 1) // scroll to latest
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@comments_page, "Failed to load comments", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupCurrentUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Fetch current user's profile from the usersMap if already loaded
            val user = usersMap[currentUser.uid]
            if (user != null && user.profilePictureUrl.isNotEmpty()) {
                val profileBytes = android.util.Base64.decode(user.profilePictureUrl, android.util.Base64.DEFAULT)
                val profileBitmap = android.graphics.BitmapFactory.decodeByteArray(profileBytes, 0, profileBytes.size)
                civCurrentUser.setImageBitmap(profileBitmap)
            } else {
                // If user not in usersMap yet, fetch from database directly
                usersRef.child(currentUser.uid).child("profile").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val fetchedUser = snapshot.getValue(User::class.java)
                        if (fetchedUser != null && fetchedUser.profilePictureUrl.isNotEmpty()) {
                            val profileBytes = android.util.Base64.decode(fetchedUser.profilePictureUrl, android.util.Base64.DEFAULT)
                            val profileBitmap = android.graphics.BitmapFactory.decodeByteArray(profileBytes, 0, profileBytes.size)
                            civCurrentUser.setImageBitmap(profileBitmap)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Optional: fallback to placeholder
                        civCurrentUser.setImageResource(R.drawable.placeholder_pfp)
                    }
                })
            }
        } else {
            civCurrentUser.setImageResource(R.drawable.placeholder_pfp)
        }
    }


    private fun setupSendComment() {
        ivSendComment.setOnClickListener {
            val text = etAddComment.text.toString().trim()
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (text.isEmpty()) return@setOnClickListener
            if (currentUser == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create comment
            val commentId = commentsRef.child(postId).push().key!!
            val comment = Comment(
                commentId = commentId,
                postId = postId,
                userId = currentUser.uid,
                text = text,
                createdAt = System.currentTimeMillis()
            )

            // Save comment to Firebase
            commentsRef.child(postId).child(commentId).setValue(comment)
                .addOnSuccessListener {
                    etAddComment.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
