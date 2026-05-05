package com.example.mind2career

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class DigitalMarketing : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_digital_marketing)

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Google Skillshop
        findViewById<LinearLayout>(R.id.courseGoogle).setOnClickListener {
            openUrl("https://skillshop.withgoogle.com/")
        }

        // Coursera Digital Marketing
        findViewById<LinearLayout>(R.id.courseCoursera).setOnClickListener {
            openUrl("https://www.coursera.org/specializations/digital-marketing")
        }

        // HubSpot Academy
        findViewById<LinearLayout>(R.id.courseHubspot).setOnClickListener {
            openUrl("https://academy.hubspot.com/courses/digital-marketing")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}