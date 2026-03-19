package com.example.i230657

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class story_draft : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.story_draft)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var close = findViewById<ImageView>(R.id.close)
        close.setOnClickListener {
            finish()
        }

        var your_stories = findViewById<ImageView>(R.id.your_stories)
        your_stories.setOnClickListener {
            var intent = Intent(this, own_story_page::class.java)
            startActivity(intent)

        }

        var close_friends = findViewById<ImageView>(R.id.close_friends)
        close_friends.setOnClickListener {
            var intent = Intent(this, own_story_page::class.java)
            startActivity(intent)
        }

        var next = findViewById<ImageView>(R.id.next)
        next.setOnClickListener {
            var intent = Intent(this, own_story_page::class.java)
            startActivity(intent)
        }
    }
}