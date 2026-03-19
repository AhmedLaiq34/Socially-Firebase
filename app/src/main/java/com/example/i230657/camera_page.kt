package com.example.i230657

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageView

class camera_page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.camera_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainCameraLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.rightArrow).setOnClickListener {
            val intent = Intent(this, story_draft::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.galleryButton).setOnClickListener {
            val intent = Intent(this, select_photo_page::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.captureButton).setOnClickListener {
            val intent = Intent(this, story_draft::class.java)
            startActivity(intent)
        }
    }
}
