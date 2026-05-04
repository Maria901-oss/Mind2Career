package com.example.mind2career

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SoftwareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_softwareengineering)

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Coursera
        findViewById<LinearLayout>(R.id.layoutCoursera).setOnClickListener {
            openUrl("https://www.coursera.org/specializations/software-development")
        }

        // freeCodeCamp
        findViewById<LinearLayout>(R.id.layoutFreeCodeCamp).setOnClickListener {
            openUrl("https://www.freecodecamp.org")
        }

        // Harvard CS50
        findViewById<LinearLayout>(R.id.layoutCS50).setOnClickListener {
            openUrl("https://cs50.harvard.edu")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}