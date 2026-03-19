package com.example.i230657

import User
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.util.Calendar

class signup_page : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase

    private lateinit var usernameInput: EditText
    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var createAccountBtn: MaterialButton
    private lateinit var profileCameraIcon: ImageView
    private lateinit var backBtn: ImageView

    private lateinit var bioInput: EditText
    private lateinit var websiteInput: EditText
    private lateinit var phoneNumberInput: EditText



    private lateinit var profileImageView: CircleImageView

    private var selectedImageBitmap: Bitmap? = null
    private var isPasswordVisible = false
    private var selectedDate: String = ""

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                try {
                    val inputStream = contentResolver.openInputStream(it)
                    selectedImageBitmap = BitmapFactory.decodeStream(inputStream)

                    // Compress image to reasonable size
                    selectedImageBitmap = compressBitmap(selectedImageBitmap!!)

                    // Show selected image in CircleImageView
                    profileImageView.setImageBitmap(selectedImageBitmap)

                    Toast.makeText(this, "Profile picture selected", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signup_page)

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupMainContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views with explicit types
        backBtn = findViewById<ImageView>(R.id.signupBackBtn)
        usernameInput = findViewById<EditText>(R.id.Username_input)
        firstNameInput = findViewById<EditText>(R.id.FirstName_input)
        lastNameInput = findViewById<EditText>(R.id.LastName_input)
        dobInput = findViewById<EditText>(R.id.Dob_input)
        emailInput = findViewById<EditText>(R.id.Email_input)
        passwordInput = findViewById<EditText>(R.id.Password_input)
        createAccountBtn = findViewById<MaterialButton>(R.id.createAccountButton)
        profileCameraIcon = findViewById<ImageView>(R.id.signupProfileCameraIcon)
        profileImageView = findViewById<CircleImageView>(R.id.signupProfileImage)

        bioInput = findViewById(R.id.Bio_input)
        websiteInput = findViewById(R.id.Website_input)
        phoneNumberInput = findViewById(R.id.Phone_input)


        // Make DOB field non-editable and show date picker on click
        dobInput.inputType = InputType.TYPE_NULL
        dobInput.isFocusable = false
        dobInput.setOnClickListener {
            showDatePicker()
        }

        // Setup listeners
        backBtn.setOnClickListener {
            val intent = Intent(this, login_page::class.java)
            startActivity(intent)
            finish()
        }

        profileCameraIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        createAccountBtn.setOnClickListener {
            validateAndSignUp()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format: DD/MM/YYYY
                selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                dobInput.setText(selectedDate)
            },
            year - 18, // Default to 18 years ago
            month,
            day
        )

        // Set minimum date (e.g., 100 years ago)
        calendar.set(year - 100, month, day)
        datePickerDialog.datePicker.minDate = calendar.timeInMillis

        // Set maximum date (e.g., 13 years ago - minimum age requirement)
        calendar.set(year - 13, month, day)
        datePickerDialog.datePicker.maxDate = calendar.timeInMillis

        datePickerDialog.show()
    }

    private fun validateAndSignUp() {
        val username = usernameInput.text.toString().trim()
        val firstName = firstNameInput.text.toString().trim()
        val lastName = lastNameInput.text.toString().trim()
        val dob = dobInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        // Validation
        if (username.isEmpty()) {
            usernameInput.error = "Username is required"
            usernameInput.requestFocus()
            return
        }

        if (username.length < 3) {
            usernameInput.error = "Username must be at least 3 characters"
            usernameInput.requestFocus()
            return
        }

        if (username.contains(" ")) {
            usernameInput.error = "Username cannot contain spaces"
            usernameInput.requestFocus()
            return
        }

        if (firstName.isEmpty()) {
            firstNameInput.error = "First name is required"
            firstNameInput.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            lastNameInput.error = "Last name is required"
            lastNameInput.requestFocus()
            return
        }

        if (dob.isEmpty()) {
            dobInput.error = "Date of birth is required"
            Toast.makeText(this, "Please select your date of birth", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            emailInput.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email"
            emailInput.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            passwordInput.requestFocus()
            return
        }

        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            passwordInput.requestFocus()
            return
        }

        // Show loading
        createAccountBtn.isEnabled = false
        createAccountBtn.text = "Creating Account..."

        // Check if username is available
        checkUsernameAvailability(username, firstName, lastName, dob, email, password)
    }

    private fun checkUsernameAvailability(
        username: String,
        firstName: String,
        lastName: String,
        dob: String,
        email: String,
        password: String
    ) {
        mDatabase.reference.child("usernameLookup")
            .child(username.lowercase())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Username is taken
                        createAccountBtn.isEnabled = true
                        createAccountBtn.text = "Create an Account"
                        usernameInput.error = "Username is already taken"
                        usernameInput.requestFocus()
                        Toast.makeText(this@signup_page, "Username is already taken", Toast.LENGTH_SHORT).show()
                    } else {
                        // Username is available
                        createFirebaseAccount(username, firstName, lastName, dob, email, password)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    createAccountBtn.isEnabled = true
                    createAccountBtn.text = "Create an Account"
                    Toast.makeText(this@signup_page, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createFirebaseAccount(
        username: String,
        firstName: String,
        lastName: String,
        dob: String,
        email: String,
        password: String
    ) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = mAuth.currentUser?.uid
                    if (userId != null) {
                        // Create user profile in database
                        val displayName = "$firstName $lastName"

                        val user = User(
                            userId = userId,
                            email = email,
                            username = username,
                            displayName = displayName,
                            firstName = firstName,
                            lastName = lastName,
                            dateOfBirth = dob,
                            phoneNumber = phoneNumberInput.text.toString().trim(),
                            bio = bioInput.text.toString().trim(),
                            website = websiteInput.text.toString().trim(),
                            profilePictureUrl = "",
                            accountPrivate = false,
                            createdAt = System.currentTimeMillis(),
                            isOnline = true,
                            lastSeen = System.currentTimeMillis()
                        )



                        // If profile picture was selected, convert to Base64
                        if (selectedImageBitmap != null) {
                            user.profilePictureUrl = bitmapToBase64(selectedImageBitmap!!)
                        }

                        saveUserToDatabase(user)

                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                val userId = mAuth.currentUser?.uid
                                if (userId != null && token != null) {
                                    mDatabase.reference.child("users")
                                        .child(userId)
                                        .child("profile")
                                        .child("fcmToken")
                                        .setValue(token)
                                }
                            }
                        }
                    }
                } else {
                    createAccountBtn.isEnabled = true
                    createAccountBtn.text = "Create an Account"
                    val errorMsg = task.exception?.message ?: "Sign up failed"
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val maxWidth = 400
        val maxHeight = 400

        val width = bitmap.width
        val height = bitmap.height

        val ratioBitmap = width.toFloat() / height.toFloat()
        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioBitmap < 1) {
            finalWidth = (maxHeight * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveUserToDatabase(user: User) {
        val userId = user.userId

        // Save user profile
        mDatabase.reference.child("users").child(userId).child("profile").setValue(user)
            .addOnSuccessListener {
                // Save user stats
                val stats = hashMapOf(
                    "postCount" to 0,
                    "followerCount" to 0,
                    "followingCount" to 0
                )
                mDatabase.reference.child("users").child(userId).child("stats").setValue(stats)

                // Save username lookup
                mDatabase.reference.child("usernameLookup").child(user.username.lowercase()).setValue(userId)

                createAccountBtn.isEnabled = true
                createAccountBtn.text = "Create an Account"
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to next screen
                val intent = Intent(this, switch_accounts_page::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                createAccountBtn.isEnabled = true
                createAccountBtn.text = "Create an Account"
                Toast.makeText(this, "Failed to create profile: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}