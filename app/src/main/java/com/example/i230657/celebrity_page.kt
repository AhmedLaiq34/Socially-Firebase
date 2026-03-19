package com.example.i230657

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.hdodenhof.circleimageview.CircleImageView

class celebrity_page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.celebrity_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var backButton = findViewById<ImageView>(R.id.back)
        var pfp = findViewById<ImageView>(R.id.pfp)
        var h1 = findViewById<CircleImageView>(R.id.h1)
        var h2 = findViewById<CircleImageView>(R.id.h2)
        var h3 = findViewById<CircleImageView>(R.id.h3)
        var h4 = findViewById<CircleImageView>(R.id.h4)
        var h5 = findViewById<CircleImageView>(R.id.h5)
        var message = findViewById<com.google.android.material.button.MaterialButton>(R.id.message)

        var bottom_nav_home = findViewById<ImageView>(R.id.bottom_nav_home)
        var bottom_nav_search = findViewById<ImageView>(R.id.bottom_nav_search)
        var bottom_nav_create = findViewById<ImageView>(R.id.bottom_nav_create)
        var bottom_nav_likes = findViewById<ImageView>(R.id.bottom_nav_heart)
        var bottom_nav_profile = findViewById<ImageView>(R.id.bottom_nav_profile)

        backButton.setOnClickListener {
            finish()
        }

        bottom_nav_home.setOnClickListener {
            finish()
        }

        bottom_nav_search.setOnClickListener {
            var intent = Intent(this, for_you_page::class.java)
            startActivity(intent)

        }

        bottom_nav_create.setOnClickListener {
            var intent = Intent(this, select_photo_page::class.java)
            startActivity(intent)
        }

        bottom_nav_likes.setOnClickListener {
            var intent = Intent(this, notis_page::class.java)
            startActivity(intent)
        }

        bottom_nav_profile.setOnClickListener {
            var intent = Intent(this, profile_page::class.java)
            startActivity(intent)
        }

        pfp.setOnClickListener {
            var intent = Intent(this, story_page::class.java)
            startActivity(intent)
        }

        h1.setOnClickListener {
            var intent = Intent(this, highlight_page::class.java)
            startActivity(intent)
        }

        h2.setOnClickListener {
            var intent = Intent(this, highlight_page::class.java)
            startActivity(intent)
        }

        h3.setOnClickListener {
            var intent = Intent(this, highlight_page::class.java)
            startActivity(intent)
        }


        h4.setOnClickListener {
            var intent = Intent(this, highlight_page::class.java)
            startActivity(intent)
        }

        h5.setOnClickListener {
            var intent = Intent(this, highlight_page::class.java)
            startActivity(intent)
        }

        message.setOnClickListener {
            var intent = Intent(this, chat_page::class.java)
            startActivity(intent)
        }

        var follow = findViewById<com.google.android.material.button.MaterialButton>(R.id.follow)

        follow.setOnClickListener {
            var intent = Intent(this, celebrity_follow_page::class.java)
            startActivity(intent)
        }






    }
}