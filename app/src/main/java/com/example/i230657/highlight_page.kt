package com.example.i230657

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.hdodenhof.circleimageview.CircleImageView

class highlight_page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.highlight_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var pfp = findViewById<CircleImageView>(R.id.pfp)
        var close = findViewById<ImageView>(R.id.close)
        var send = findViewById<ImageView>(R.id.send)
        var post = findViewById<ImageView>(R.id.post)
        var create = findViewById<ImageView>(R.id.create)

        pfp.setOnClickListener {
            val intent = Intent(this, profile_page::class.java)
            startActivity(intent)
        }

        close.setOnClickListener {
            finish()
        }

        send.setOnClickListener {
            val intent = Intent(this, all_chats_page::class.java)
            startActivity(intent)
        }

        post.setOnClickListener {
            val intent = Intent(this, select_photo_page::class.java)
            startActivity(intent)
        }

        create.setOnClickListener {
            val intent = Intent(this, select_photo_page::class.java)
            startActivity(intent)
        }

    }



}