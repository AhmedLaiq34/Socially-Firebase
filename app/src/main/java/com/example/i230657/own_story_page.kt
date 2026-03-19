package com.example.i230657

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.hdodenhof.circleimageview.CircleImageView

class own_story_page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.own_story_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var pfp = findViewById<CircleImageView>(R.id.pfp)
        pfp.setOnClickListener {
            var intent = Intent(this, profile_page::class.java)
        }

        var close = findViewById<ImageView>(R.id.close)
        close.setOnClickListener {
            //go to home feed
            var intent = Intent(this, home_feed::class.java)
            startActivity(intent)
        }

        var create = findViewById<ImageView>(R.id.create)
        create.setOnClickListener {
            var intent = Intent(this, select_photo_page::class.java)
            startActivity(intent)
        }



    }

}