package com.example.i230657

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler

class video_call_page : AppCompatActivity() {

    private val APP_ID = "c73657650c704870b5b75abb8e831e37"
    private val TOKEN = ""
    private val CHANNEL = "myChannel"

    private var mRtcEngine: RtcEngine? = null
    private lateinit var endCallBtn: ImageView
    private lateinit var switchCameraBtn: ImageView
    private lateinit var statusText: TextView

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private lateinit var micBtn: ImageView
    private var isMicMuted = false

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                statusText.text = "Joined channel: $channel"
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                statusText.text = "User joined: $uid"
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                statusText.text = "User left: $uid"
                removeRemoteVideo()
                statusText.postDelayed({ onCallEnded(null) }, 1500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_call_page)

        endCallBtn = findViewById(R.id.end_call_btn)
        switchCameraBtn = findViewById(R.id.switch_camera_btn)
        statusText = findViewById(R.id.status_text)

        endCallBtn.setOnClickListener { onCallEnded(it) }
        switchCameraBtn.setOnClickListener { mRtcEngine?.switchCamera() }

        if (checkPermissions()) {
            startVideoCall()
        }

        micBtn = findViewById(R.id.mic)
        micBtn.setOnClickListener {
            isMicMuted = !isMicMuted
            mRtcEngine?.muteLocalAudioStream(isMicMuted)
            val res = if (isMicMuted) R.drawable.muted_mic_white else R.drawable.mic_white
            micBtn.setImageResource(res)
        }
    }

    private fun startVideoCall() {
        initAgoraEngine()
        enableVideo()
        setupLocalVideo()
        joinChannel()
    }

    private fun initAgoraEngine() {
        val config = RtcEngineConfig().apply {
            mContext = applicationContext
            mAppId = APP_ID
            mEventHandler = mRtcEventHandler
        }

        try {
            mRtcEngine = RtcEngine.create(config)
        } catch (_: Exception) { }
    }

    private fun enableVideo() {
        mRtcEngine?.apply {
            enableVideo()
            startPreview()
        }
    }

    private fun setupLocalVideo() {
        val container: FrameLayout = findViewById(R.id.local_video_container)
        container.removeAllViews()

        val surfaceView = SurfaceView(baseContext).apply {
            setZOrderMediaOverlay(true)
            setZOrderOnTop(true)
        }

        container.addView(surfaceView)
        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
        container.post { mRtcEngine?.startPreview() }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container: FrameLayout = findViewById(R.id.remote_video_container)
        container.removeAllViews()
        val surfaceView = SurfaceView(baseContext).apply { setZOrderMediaOverlay(true) }
        container.addView(surfaceView)
        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun removeRemoteVideo() {
        val container: FrameLayout = findViewById(R.id.remote_video_container)
        container.removeAllViews()
    }

    private fun joinChannel() {
        val uid = (1000..9999).random()
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishCameraTrack = true
            publishMicrophoneTrack = true
        }
        mRtcEngine?.joinChannel(TOKEN, CHANNEL, uid, options)
    }

    private fun checkPermissions(): Boolean {
        for (perm in REQUESTED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startVideoCall()
            }
        }
    }

    private fun onCallEnded(view: View?) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val receiverId = intent.getStringExtra("userId")
        if (currentUserId != null && receiverId != null) {
            val ref = FirebaseDatabase.getInstance().getReference("calls")
            ref.child("${currentUserId}_${receiverId}_video").removeValue()
            ref.child("${receiverId}_${currentUserId}_video").removeValue()
        }
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        endCall()
    }

    private fun endCall() {
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
    }
}
