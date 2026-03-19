package com.example.i230657

import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
class logout_page : AppCompatActivity() {

    lateinit var returnText: TextView
    lateinit var sociallyCircle: RelativeLayout

    lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.logout_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()


        returnText = findViewById(R.id.returnText)
        sociallyCircle = findViewById(R.id.sociallyCircle)
        returnText.setOnClickListener {
            finish()
        }
        sociallyCircle.setOnClickListener {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
            logoutUser()
        }


    }

    private fun logoutUser() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            val userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("profile")

            // 1️⃣ Set online = false, lastSeen, and clear fcmToken
            val updates = mapOf<String, Any>(
                "online" to false,
                "lastSeen" to System.currentTimeMillis(),
                "fcmToken" to "" // clear the FCM token on logout
            )

            userRef.updateChildren(updates).addOnCompleteListener {
                if (it.isSuccessful) {
                    // 2️⃣ Log out from FirebaseAuth
                    FirebaseAuth.getInstance().signOut()

                    // 3️⃣ Navigate to login screen
                    val intent = Intent(this, login_page::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error logging out: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Already logged out
            val intent = Intent(this, login_page::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }


}