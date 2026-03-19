package com.example.i230657

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class notis_page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.notis_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tabYou = findViewById<TextView>(R.id.tab_you_text)
        tabYou.setOnClickListener {
            val intent = Intent(this, notis_page_you::class.java)
            startActivity(intent)
        }

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
