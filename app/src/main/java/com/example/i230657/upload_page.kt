package com.example.i230657

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class upload_page : AppCompatActivity() {

    private lateinit var ivSelected: ImageView
    private lateinit var tvPrompt: TextView
    private lateinit var etCaption: EditText
    private lateinit var btnUpload: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var postTab: LinearLayout
    private lateinit var storyTab: LinearLayout
    private lateinit var postTabText: TextView
    private lateinit var storyTabText: TextView
    private lateinit var postTabIndicator: View
    private lateinit var storyTabIndicator: View

    // Instead of single image
    private val selectedImageUris = mutableListOf<Uri>()

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.upload_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.storyTabText).setOnClickListener {
            val intent = Intent(this, upload_page_story::class.java)
            startActivity(intent)
        }

        // find views
        ivSelected = findViewById(R.id.iv_selectedImage)
        tvPrompt = findViewById(R.id.tv_selectPrompt)
        etCaption = findViewById(R.id.et_caption)
        btnUpload = findViewById(R.id.btn_upload)
        btnCancel = findViewById(R.id.cancelButton)

        postTab = findViewById(R.id.postTab)
        storyTab = findViewById(R.id.storyTab)
        postTabText = findViewById(R.id.postTabText)
        storyTabText = findViewById(R.id.storyTabText)
        postTabIndicator = findViewById(R.id.postTabIndicator)
        storyTabIndicator = findViewById(R.id.storyTabIndicator)

        // open gallery (allow multiple)
        findViewById<FrameLayout>(R.id.imagePickerContainer).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnUpload.setOnClickListener {
            val caption = etCaption.text.toString().trim()
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentUser == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val imageBase64List = selectedImageUris.map { uri ->
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                }

                val postId = FirebaseDatabase.getInstance().reference.child("posts").push().key!!
                val post = Post(
                    postId = postId,
                    userId = currentUser.uid,
                    caption = caption,
                    imagesBase64 = imageBase64List, // store multiple images
                    likes = 0,
                    createdAt = System.currentTimeMillis()
                )

                val db = FirebaseDatabase.getInstance().reference

                db.child("posts").child(postId)
                    .setValue(post)
                    .addOnSuccessListener {
                        // ✅ Ensure stats exists, then increment postCount
                        val statsRef = db.child("users").child(currentUser.uid).child("stats")

                        statsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) {
                                    // Create stats if missing
                                    val userStats = mapOf(
                                        "postCount" to 0,
                                        "followerCount" to 0,
                                        "followingCount" to 0
                                    )
                                    statsRef.setValue(userStats)
                                }

                                // Now safely increment postCount
                                statsRef.child("postCount")
                                    .runTransaction(object : Transaction.Handler {
                                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                                            val currentCount = currentData.getValue(Int::class.java) ?: 0
                                            currentData.value = currentCount + 1
                                            return Transaction.success(currentData)
                                        }

                                        override fun onComplete(
                                            error: DatabaseError?,
                                            committed: Boolean,
                                            snapshot: DataSnapshot?
                                        ) {
                                            if (error == null && committed) {
                                                Toast.makeText(this@upload_page, "Post uploaded!", Toast.LENGTH_SHORT).show()
                                                finish()
                                            } else {
                                                Toast.makeText(this@upload_page, "Failed to update post count", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    })
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@upload_page, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }

            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // cancel button
        btnCancel.setOnClickListener {
            selectedImageUris.clear()
            ivSelected.setImageDrawable(null)
            ivSelected.visibility = View.GONE
            tvPrompt.visibility = View.VISIBLE
            etCaption.text.clear()
            Toast.makeText(this, "Upload canceled", Toast.LENGTH_SHORT).show()
        }

        postTab.setOnClickListener { activatePostTab() }
        storyTab.setOnClickListener { activateStoryTab() }
    }

    // handle multi-image selection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUris.clear()
            data?.let {
                if (it.clipData != null) {
                    val count = it.clipData!!.itemCount
                    for (i in 0 until count) {
                        selectedImageUris.add(it.clipData!!.getItemAt(i).uri)
                    }
                } else if (it.data != null) {
                    selectedImageUris.add(it.data!!)
                }
            }

            if (selectedImageUris.isNotEmpty()) {
                ivSelected.setImageURI(selectedImageUris[0]) // preview first image
                ivSelected.visibility = View.VISIBLE
                tvPrompt.visibility = View.GONE
            }
        }
    }

    private fun activatePostTab() {
        postTabText.setTextColor(getColor(R.color.brown))
        storyTabText.setTextColor(getColor(android.R.color.darker_gray))
        postTabIndicator.visibility = View.VISIBLE
        storyTabIndicator.visibility = View.INVISIBLE
    }

    private fun activateStoryTab() {
        storyTabText.setTextColor(getColor(R.color.brown))
        postTabText.setTextColor(getColor(android.R.color.darker_gray))
        storyTabIndicator.visibility = View.VISIBLE
        postTabIndicator.visibility = View.INVISIBLE
    }
}
