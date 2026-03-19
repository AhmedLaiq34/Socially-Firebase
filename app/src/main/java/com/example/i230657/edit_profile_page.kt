package com.example.i230657

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.io.InputStream

class edit_profile_page : AppCompatActivity() {

    private lateinit var profileImage: CircleImageView
    private lateinit var auth: FirebaseAuth

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.edit_profile_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        profileImage = findViewById(R.id.profile_image)

        // Make sure this TextView has id="@+id/change_photo_text" in XML
        val changePhoto = findViewById<TextView>(R.id.change_photo_text)
        changePhoto.setOnClickListener {
            openImagePicker()
        }

        findViewById<TextView>(R.id.cancel_text).setOnClickListener { finish() }
        findViewById<TextView>(R.id.done_text).setOnClickListener { finish() }

        // Load existing profile picture (base64) if present
        loadCurrentProfilePic()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    // deprecated startActivityForResult used for brevity — it's fine here
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                // Convert picked image to base64 + compressed bitmap
                val pair = uriToBase64AndBitmap(imageUri)
                if (pair != null) {
                    val (base64String, bitmap) = pair
                    saveProfilePicBase64(base64String, bitmap)
                } else {
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Reads the image Uri → returns Pair(base64String, compressedBitmap)
     * - compresses to JPEG (70% quality) and NO_WRAP base64 (no line breaks).
     * - resizes if needed to avoid huge DB strings.
     */
    private fun uriToBase64AndBitmap(uri: Uri): Pair<String, Bitmap>? {
        try {
            val input: InputStream? = contentResolver.openInputStream(uri)
            if (input == null) return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            val resized = resizeBitmapIfNeeded(original, 800) // max dimension 800 px
            val baos = ByteArrayOutputStream()
            // compress as JPEG to reduce size
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos)
            val bytes = baos.toByteArray()
            // NO_WRAP to avoid newline characters in the string
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            return Pair(base64, resized)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Save base64 string exactly to:
     * users/{userId}/profile/profilePictureUrl
     */
    private fun saveProfilePicBase64(base64String: String, bitmapToShow: Bitmap) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val profilePicRef = FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(userId)
            .child("profile")
            .child("profilePictureUrl")

        profilePicRef.setValue(base64String)
            .addOnSuccessListener {
                // Immediately update UI
                profileImage.setImageBitmap(bitmapToShow)
                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Loads existing base64 picture from database (if any) and shows it.
     */
    private fun loadCurrentProfilePic() {
        val userId = auth.currentUser?.uid ?: return
        val profilePicRef = FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(userId)
            .child("profile")
            .child("profilePictureUrl")

        profilePicRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val base64 = snapshot.getValue(String::class.java)
                if (!base64.isNullOrEmpty()) {
                    try {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        profileImage.setImageBitmap(bmp)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // ignore or show a small toast
            }
        })
    }

    private fun resizeBitmapIfNeeded(src: Bitmap, maxDim: Int): Bitmap {
        val width = src.width
        val height = src.height
        val max = maxOf(width, height)
        if (max <= maxDim) return src
        val scale = maxDim.toFloat() / max.toFloat()
        val newW = (width * scale).toInt()
        val newH = (height * scale).toInt()
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }
}
