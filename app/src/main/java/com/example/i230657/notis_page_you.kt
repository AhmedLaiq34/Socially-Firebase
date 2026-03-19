package com.example.i230657

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class notis_page_you : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.notis_page_you)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tabFollowing = findViewById<TextView>(R.id.tab_following_text)
        tabFollowing.setOnClickListener {
            val intent = Intent(this, notis_page::class.java)
            startActivity(intent)
        }

        val message1 = findViewById<MaterialButton>(R.id.Message1)
        val message2 = findViewById<MaterialButton>(R.id.Message2)
        val message3 = findViewById<MaterialButton>(R.id.Message3)
        val message4 = findViewById<MaterialButton>(R.id.Message4)

        val messageClickListener = {
            val intent = Intent(this, chat_page::class.java)
            startActivity(intent)
        }

        message1.setOnClickListener { messageClickListener() }
        message2.setOnClickListener { messageClickListener() }
        message3.setOnClickListener { messageClickListener() }
        message4.setOnClickListener { messageClickListener() }

        val navHome = findViewById<ImageView>(R.id.bottom_nav_home)
        val navSearch = findViewById<ImageView>(R.id.bottom_nav_search)
        val navCreate = findViewById<ImageView>(R.id.bottom_nav_create)
        val navProfile = findViewById<ImageView>(R.id.bottom_nav_profile)

        navHome.setOnClickListener {
            val intent = Intent(this, home_feed::class.java)
            startActivity(intent)
        }

        navSearch.setOnClickListener {
            val intent = Intent(this, for_you_page::class.java)
            startActivity(intent)
        }

        navCreate.setOnClickListener {
            val intent = Intent(this, select_photo_page::class.java)
            startActivity(intent)
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, profile_page::class.java)
            startActivity(intent)
        }
    }
}
