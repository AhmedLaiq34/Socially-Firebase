package com.example.i230657

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class select_photo_page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.select_photo_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnCancel = findViewById<TextView>(R.id.btn_cancel)
        btnCancel.setOnClickListener {
            finish()
        }

        val btnNext = findViewById<TextView>(R.id.btn_next)
        btnNext.setOnClickListener {
            val intent = Intent(this, story_draft::class.java)
            startActivity(intent)
        }
    }
}
