package com.example.i230657

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging


class login_page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_page)

        // Setup system window padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        val usernameField = findViewById<EditText>(R.id.usernameField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val signUpText = findViewById<TextView>(R.id.signUpText)

        // When user clicks login button
        loginButton.setOnClickListener {
            val emailOrUsername = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (emailOrUsername.isEmpty()) {
                usernameField.error = "Enter email or username"
                usernameField.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordField.error = "Enter password"
                passwordField.requestFocus()
                return@setOnClickListener
            }

            // Check if user entered email or username
            if (Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
                // Login using email directly
                loginWithEmail(emailOrUsername, password)
            } else {
                // Login using username (convert to email using database lookup)
                loginWithUsername(emailOrUsername, password)
            }
        }

        // Go to signup page
        signUpText.setOnClickListener {
            startActivity(Intent(this, signup_page::class.java))
        }

    }

    private fun loginWithEmail(email: String, password: String) {
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        loginButton.isEnabled = false
        loginButton.text = "Logging in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loginButton.isEnabled = true
                loginButton.text = "Login"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val userRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(userId)
                            .child("profile")

                        // ✅ Get FCM token and update profile
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { tokenTask ->
                                val updates = mutableMapOf<String, Any>(
                                    "online" to true,
                                    "lastSeen" to System.currentTimeMillis()
                                )

                                if (tokenTask.isSuccessful) {
                                    val token = tokenTask.result
                                    Log.d("FCM", "Token: $token")
                                    updates["fcmToken"] = token
                                }

                                userRef.updateChildren(updates)
                            }
                    }

                    // Move to switch accounts page
                    val intent = Intent(this, switch_accounts_page::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = task.exception?.message ?: "Login failed"
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun loginWithUsername(username: String, password: String) {
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        loginButton.isEnabled = false
        loginButton.text = "Checking username..."

        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
        val ref = database.reference.child("usernameLookup").child(username.lowercase())

        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val userId = snapshot.value.toString()
                val userRef = database.reference.child("users").child(userId).child("profile").child("email")

                userRef.get().addOnSuccessListener { emailSnapshot ->
                    val email = emailSnapshot.value?.toString()
                    if (email != null) {
                        // Now login using email
                        loginWithEmail(email, password)
                    } else {
                        loginButton.isEnabled = true
                        loginButton.text = "Login"
                        Toast.makeText(this, "Email not found for username", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    loginButton.isEnabled = true
                    loginButton.text = "Login"
                    Toast.makeText(this, "Error fetching email", Toast.LENGTH_SHORT).show()
                }
            } else {
                loginButton.isEnabled = true
                loginButton.text = "Login"
                Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            loginButton.isEnabled = true
            loginButton.text = "Login"
            Toast.makeText(this, "Error checking username", Toast.LENGTH_SHORT).show()
        }
    }
}

