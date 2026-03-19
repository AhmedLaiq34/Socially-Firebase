package com.example.i230657
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig


class call_page : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    lateinit var mMuteBtn: ImageView
    lateinit var EndCallBtn: ImageView

    lateinit var statusText: TextView

    lateinit var displayName: TextView

    lateinit var profileImage: CircleImageView



    private var mMuted = false

    private val APP_ID = "c73657650c704870b5b75abb8e831e37"

    private val CHANNEL = "myChannel"

    private val TOKEN = ""

    private var mRtcEngine: RtcEngine? = null

    lateinit var speakerBtn: ImageView
    private var isSpeakerOn = false


    private val mRtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                statusText.text = "Joined channel: $channel, uid: $uid"
                Log.d("AgoraCall", "✅ Joined channel: $channel, uid: $uid")
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                statusText.text = "User joined: $uid"
                Log.d("AgoraCall", "👤 Remote user joined: $uid")
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                statusText.text = "User left: $uid"
                Log.d("AgoraCall", "❌ Remote user left: $uid, reason: $reason")
                // End call after short delay
                statusText.postDelayed({ onCallEnded(null) }, 1500)
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            runOnUiThread {
                statusText.text = "Left channel"
                Log.d("AgoraCall", "📴 Left channel")
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                statusText.text = "Error: $err"
                Log.e("AgoraCall", "⚠️ Agora error: $err")
            }
        }

        override fun onConnectionLost() {
            runOnUiThread {
                statusText.text = "Connection lost"
                Log.e("AgoraCall", "⚠️ Connection lost")
            }
        }

        override fun onConnectionInterrupted() {
            runOnUiThread {
                statusText.text = "Connection interrupted"
                Log.e("AgoraCall", "⚠️ Connection interrupted")
            }
        }
    }


    private fun initializeAndJoinChannel() {
        try {
            mRtcEngine = RtcEngine.create(baseContext, APP_ID, mRtcEventHandler)
        } catch (e: Exception) {
            println("Exception: " + e.message)
        }

        // ✅ Important setup
        mRtcEngine?.setChannelProfile(io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION)
        mRtcEngine?.enableAudio()

        // Show calling status immediately when starting
        statusText.text = "Calling..."

        val uid = (1000..9999).random() // random unique UID per device
        mRtcEngine!!.joinChannel(TOKEN, CHANNEL, "", uid)
    }



    private val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    private val PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(permission),
                requestCode)
            return false
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_page)


        mMuteBtn = findViewById<ImageView>(R.id.mute_btn)

        EndCallBtn = findViewById<ImageView>(R.id.end_call_btn)

        statusText = findViewById<TextView>(R.id.status_text)

        displayName = findViewById<TextView>(R.id.placeholder_name)

        profileImage = findViewById<CircleImageView>(R.id.pfp)

        EndCallBtn.setOnClickListener {
            onCallEnded(it)
        }
        mMuteBtn.setOnClickListener {
            onLocalAudioMuteClicked(it)
        }

        database = FirebaseDatabase.getInstance().reference

        // Get userId from intent
        val userId = intent.getStringExtra("userId")
        if (userId != null) {
            fetchUserProfile(userId)
        }

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)){
            initializeAndJoinChannel()
        }

        speakerBtn = findViewById(R.id.speaker_btn)

        speakerBtn.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            mRtcEngine?.setEnableSpeakerphone(isSpeakerOn)
        }


    }

    //onDestroy
    override fun onDestroy() {
        super.onDestroy()
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null

    }

    fun onCallEnded(view: View?){
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
        finish()
    }

    fun onLocalAudioMuteClicked(view: View) {
        mMuted = !mMuted
        mRtcEngine?.muteLocalAudioStream(mMuted)

        val res = if (mMuted) R.drawable.mic_muted else R.drawable.mic_icon
        mMuteBtn.setImageResource(res)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID_RECORD_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeAndJoinChannel()
        }
    }

    private fun fetchUserProfile(userId: String) {
        database.child("users").child(userId).child("profile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                        val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                        val fullName = "$firstName $lastName"

                        val profilePicBase64 = snapshot.child("profilePictureUrl").getValue(String::class.java) ?: ""

                        // Update UI
                        displayName.text = fullName

                        if (profilePicBase64.isNotEmpty()) {
                            try {
                                val decodedBytes = Base64.decode(profilePicBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                profileImage.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                profileImage.setImageResource(R.drawable.placeholder_pfp) // fallback image
                            }
                        } else {
                            profileImage.setImageResource(R.drawable.placeholder_pfp) // fallback
                        }

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("❌ Failed to fetch profile: ${error.message}")
                }
            })
    }







}
