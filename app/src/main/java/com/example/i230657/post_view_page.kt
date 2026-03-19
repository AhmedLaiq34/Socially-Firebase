package com.example.i230657

import User
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class post_view_page : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var postsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference

    private lateinit var profileImage: CircleImageView
    private lateinit var username: TextView
    private lateinit var postImagesPager: ViewPager2
    private lateinit var likesCounter: TextView
    private lateinit var caption: TextView
    private lateinit var timestamp: TextView
    private lateinit var likeIcon: ImageView
    private lateinit var commentIcon: ImageView
    private lateinit var shareIcon: ImageView
    private lateinit var saveIcon: ImageView
    private lateinit var progressBar: ProgressBar

    private var postId: String = ""
    private var currentPost: Post? = null
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_view_page)

        // Get postId from intent
        postId = intent.getStringExtra("postId") ?: ""

        if (postId.isEmpty()) {
            Toast.makeText(this, "Invalid post", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        postsRef = database.getReference("posts")
        usersRef = database.getReference("users")

        // Initialize views
        initializeViews()
        setupClickListeners()

        // Fetch post data
        fetchPostData()
    }

    private fun initializeViews() {
        profileImage = findViewById(R.id.postProfileImage)
        username = findViewById(R.id.tvUsername)
        postImagesPager = findViewById(R.id.vpPostImages)
        likesCounter = findViewById(R.id.likesCounter)
        caption = findViewById(R.id.postCaption)
        timestamp = findViewById(R.id.postTimestamp)
        likeIcon = findViewById(R.id.postLikeIcon)
        commentIcon = findViewById(R.id.postCommentIcon)
        shareIcon = findViewById(R.id.postShareIcon)
        saveIcon = findViewById(R.id.postSaveIcon)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.back).setOnClickListener {
            finish()
        }

        likeIcon.setOnClickListener {
            handleLikeClick()
        }

        commentIcon.setOnClickListener {
            val intent = Intent(this, comments_page::class.java)
            intent.putExtra("postId", postId)
            startActivity(intent)
        }

        shareIcon.setOnClickListener {
            handleShareClick()
        }

        username.setOnClickListener {
            navigateToProfile()
        }

        profileImage.setOnClickListener {
            navigateToProfile()
        }
    }

    private fun fetchPostData() {
        progressBar.visibility = View.VISIBLE

        postsRef.child(postId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentPost = snapshot.getValue(Post::class.java)

                if (currentPost != null) {
                    fetchUserData(currentPost!!.userId)
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@post_view_page, "Post not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@post_view_page, "Failed to load post", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserData(userId: String) {
        usersRef.child(userId).child("profile").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
                progressBar.visibility = View.GONE

                if (currentUser != null && currentPost != null) {
                    displayPost()
                } else {
                    Toast.makeText(this@post_view_page, "User not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@post_view_page, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayPost() {
        currentPost?.let { post ->
            currentUser?.let { user ->
                // Set username
                username.text = user.username

                // Set profile picture
                if (user.profilePictureUrl.isNotEmpty()) {
                    try {
                        val profileBytes = Base64.decode(user.profilePictureUrl, Base64.DEFAULT)
                        val profileBitmap = BitmapFactory.decodeByteArray(profileBytes, 0, profileBytes.size)
                        profileImage.setImageBitmap(profileBitmap)
                    } catch (e: Exception) {
                        profileImage.setImageResource(R.drawable.placeholder_pfp)
                    }
                } else {
                    profileImage.setImageResource(R.drawable.placeholder_pfp)
                }

                // Set post images
                if (post.imagesBase64.isNotEmpty()) {
                    val pagerAdapter = ImagePagerAdapter(post.imagesBase64)
                    postImagesPager.adapter = pagerAdapter
                } else {
                    postImagesPager.adapter = ImagePagerAdapter(listOf(""))
                }

                // Set caption with username
                val spannable = android.text.SpannableStringBuilder()
                spannable.append(user.username)

                spannable.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    0,
                    user.username.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                spannable.setSpan(
                    object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: View) {
                            navigateToProfile()
                        }

                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            ds.color = android.graphics.Color.BLACK
                        }
                    },
                    0,
                    user.username.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                if (post.caption.isNotEmpty()) {
                    spannable.append(" ")
                    spannable.append(post.caption)
                }

                caption.text = spannable
                caption.movementMethod = android.text.method.LinkMovementMethod.getInstance()

                // Set likes counter
                likesCounter.text = post.likes.toString()

                // Update like icon based on current user's like status
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null && post.likedBy.containsKey(currentUserId)) {
                    likeIcon.setImageResource(R.drawable.red_like)
                } else {
                    likeIcon.setImageResource(R.drawable.like)
                }

                // Set timestamp
                timestamp.text = getTimeAgo(post.createdAt)
            }
        }
    }

    private fun handleLikeClick() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val post = currentPost ?: return

        postsRef.child(postId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val p = currentData.getValue(Post::class.java) ?: return Transaction.success(currentData)

                val updatedLikedBy = p.likedBy.toMutableMap()
                if (updatedLikedBy.containsKey(currentUserId)) {
                    updatedLikedBy.remove(currentUserId)
                    p.likes = (p.likes - 1).coerceAtLeast(0)
                } else {
                    updatedLikedBy[currentUserId] = true
                    p.likes += 1
                }

                p.likedBy = updatedLikedBy
                currentData.value = p
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (committed) {
                    val updatedPost = currentData?.getValue(Post::class.java)
                    if (updatedPost != null) {
                        currentPost = updatedPost
                        likesCounter.text = updatedPost.likes.toString()

                        if (updatedPost.likedBy.containsKey(currentUserId)) {
                            likeIcon.setImageResource(R.drawable.red_like)
                        } else {
                            likeIcon.setImageResource(R.drawable.like)
                        }
                    }
                }
            }
        })
    }

    private fun handleShareClick() {
        val post = currentPost ?: return
        val user = currentUser ?: return

        val firstImage = if (post.imagesBase64.isNotEmpty()) {
            post.imagesBase64[0]
        } else {
            ""
        }

        val dialog = SharePostDialogFragment.newInstance(
            postId = post.postId,
            userId = user.userId,
            userProfilePicture = user.profilePictureUrl,
            postCaption = post.caption,
            postImage = firstImage,
            timestamp = System.currentTimeMillis()
        )
        dialog.show(supportFragmentManager, "SharePostDialog")
    }

    private fun navigateToProfile() {
        val user = currentUser ?: return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (user.userId == currentUserId) {
            val intent = Intent(this, profile_page::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, celebrity_follow_page::class.java)
            intent.putExtra("userId", user.userId)
            startActivity(intent)
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            weeks < 4 -> "${weeks}w ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}