package com.example.i230657

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView

class switch_accounts_page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.switch_accounts_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.switchAccountsRootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fetch and display logged-in user's profile picture
        loadProfilePicture()

        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        loginButton.setOnClickListener {
            val intent = Intent(this, home_feed::class.java)
            startActivity(intent)
        }

        val signupRedirectText = findViewById<TextView>(R.id.signupRedirectText)
        signupRedirectText.setOnClickListener {
            val intent = Intent(this, signup_page::class.java)
            startActivity(intent)
        }
    }

    private fun loadProfilePicture() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val profileImageView = findViewById<CircleImageView>(R.id.signupProfileImage)
            val userRef = FirebaseDatabase.getInstance()
                .reference
                .child("users")
                .child(currentUser.uid)
                .child("profile")
                .child("profilePictureUrl")

            userRef.get().addOnSuccessListener { snapshot ->
                val base64String = snapshot.value?.toString()
                if (!base64String.isNullOrEmpty()) {
                    try {
                        val cleanedString = base64String.replace("\n", "").replace("\r", "")
                        val decodedBytes = Base64.decode(cleanedString, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        profileImageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to load profile picture", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error fetching profile picture", Toast.LENGTH_SHORT).show()
            }
        }

        if (currentUser != null) {


            val userRef = FirebaseDatabase.getInstance()
                .reference
                .child("users")
                .child(currentUser.uid)
                .child("profile")
                .child("username")

            userRef.get().addOnSuccessListener { snapshot ->
                val username = snapshot.value?.toString()
                if (!username.isNullOrEmpty()) {
                    val usernamePreview = findViewById<TextView>(R.id.signupUsernamePreview)
                    usernamePreview.text = username
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error fetching username", Toast.LENGTH_SHORT).show()
            }
        }



    }
}
