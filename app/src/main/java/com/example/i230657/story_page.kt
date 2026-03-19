package com.example.i230657

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class story_page : AppCompatActivity() {

    private lateinit var storyUserPfp: CircleImageView
    private lateinit var storyUsername: TextView
    private lateinit var storyMainImage: ImageView
    private lateinit var topBar: LinearLayout
    private lateinit var progressActiveView: View
    private lateinit var progressRemainingView: View

    private lateinit var database: FirebaseDatabase
    private lateinit var storiesRef: DatabaseReference

    private val handler = Handler(Looper.getMainLooper())
    private var storyDuration = 10_000L // 10 seconds per story

    private var currentStoryIndex = 0
    private var userStoriesList: MutableList<Story> = mutableListOf()
    private var userId: String? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private var isAnimatingStory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.story_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainStoryLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        storyUserPfp = findViewById(R.id.storyUserPfp)
        storyUsername = findViewById(R.id.storyUsername)
        storyMainImage = findViewById(R.id.storyImageFull)
        topBar = findViewById(R.id.topBar)
        progressActiveView = findViewById(R.id.storyProgressActive1)
        progressRemainingView = findViewById(R.id.storyProgressRemaining1)

        val closeButton: ImageView = findViewById(R.id.closeButton)
        val cameraIcon: ImageView = findViewById(R.id.cameraIcon)
        val shareIcon: ImageView = findViewById(R.id.shareIcon)

        database = FirebaseDatabase.getInstance()
        storiesRef = database.getReference("stories")

        userId = intent.getStringExtra("userId")
        if (userId != null) {
            fetchUserStories(userId!!)
        } else {
            Toast.makeText(this, "Story not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        storyUserPfp.setOnClickListener {
            startActivity(Intent(this, celebrity_follow_page::class.java))
        }
        storyUsername.setOnClickListener {
            startActivity(Intent(this, celebrity_follow_page::class.java))
        }
        closeButton.setOnClickListener {
            closeStory()
        }
        cameraIcon.setOnClickListener {
            startActivity(Intent(this, camera_page::class.java))
        }
        shareIcon.setOnClickListener {
            startActivity(Intent(this, all_chats_page::class.java))
        }
    }

    private fun fetchUserStories(userId: String) {
        storiesRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userStories = snapshot.getValue(UserStories::class.java)
                    if (userStories != null && userStories.stories.isNotEmpty()) {
                        userStoriesList = userStories.stories
                        storyUsername.text = userStories.username
                        setProfilePicture(userStories.profilePictureBase64)
                        displayStory(userStoriesList[currentStoryIndex])
                    } else {
                        Toast.makeText(this@story_page, "No stories found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@story_page, "Story not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@story_page, "Failed to load story", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayStory(story: Story) {
        if (isAnimatingStory) return
        isAnimatingStory = true

        // Fade-out animation for previous story
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.duration = 300
        fadeOut.fillAfter = true

        // Fade-in animation for next story
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 400
        fadeIn.fillAfter = true

        fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation) {}
            override fun onAnimationRepeat(animation: android.view.animation.Animation) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation) {
                showDecodedImage(story)
                storyMainImage.startAnimation(fadeIn)
                startProgressBarAnimation()
                isAnimatingStory = false
            }
        })

        storyMainImage.startAnimation(fadeOut)

        // Mark story as viewed
        markStoryAsViewed(story.storyId)

        // Fade-in top bar
        topBar.alpha = 0f
        topBar.visibility = View.VISIBLE
        topBar.animate().alpha(1f).setDuration(400).start()
    }

    private fun showDecodedImage(story: Story) {
        if (!story.storyImageBase64.isNullOrEmpty()) {
            try {
                val cleaned = story.storyImageBase64.replace("\n", "").replace("\r", "").trim()
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                storyMainImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                storyMainImage.setImageResource(android.R.color.black)
            }
        } else {
            storyMainImage.setImageResource(android.R.color.black)
        }
    }

    private fun markStoryAsViewed(storyId: String) {
        if (currentUserId != null && userId != null) {
            storiesRef.child(userId!!)
                .child("stories")
                .orderByChild("storyId")
                .equalTo(storyId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (storySnap in snapshot.children) {
                            storySnap.ref.child("viewedBy").child(currentUserId!!).setValue(true)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun setProfilePicture(base64String: String?) {
        if (!base64String.isNullOrEmpty()) {
            try {
                val cleaned = base64String.replace("\n", "").replace("\r", "").trim()
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                storyUserPfp.setImageBitmap(bitmap)
            } catch (e: Exception) {
                storyUserPfp.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            storyUserPfp.setImageResource(R.drawable.placeholder_pfp)
        }
    }

    private fun startProgressBarAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = storyDuration
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            val params = progressActiveView.layoutParams as LinearLayout.LayoutParams
            params.weight = progress
            progressActiveView.layoutParams = params

            val remainingParams = progressRemainingView.layoutParams as LinearLayout.LayoutParams
            remainingParams.weight = 1 - progress
            progressRemainingView.layoutParams = remainingParams
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                handler.post {
                    currentStoryIndex++
                    if (currentStoryIndex < userStoriesList.size) {
                        resetProgressBar()
                        displayStory(userStoriesList[currentStoryIndex])
                    } else {
                        closeStory()
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        animator.start()
    }

    private fun resetProgressBar() {
        val params = progressActiveView.layoutParams as LinearLayout.LayoutParams
        params.weight = 0f
        progressActiveView.layoutParams = params

        val remainingParams = progressRemainingView.layoutParams as LinearLayout.LayoutParams
        remainingParams.weight = 1f
        progressRemainingView.layoutParams = remainingParams
    }

    private fun closeStory() {
        startActivity(Intent(this, home_feed::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
