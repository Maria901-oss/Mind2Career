package com.example.mind2career

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class BusinessAnalyst : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_business)

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Udemy
        findViewById<LinearLayout>(R.id.courseUdemy).setOnClickListener {
            openUrl("https://www.udemy.com/course/the-business-intelligence-analyst-course-2018/")
        }

        // Coursera
        findViewById<LinearLayout>(R.id.courseCoursera).setOnClickListener {
            openUrl("https://www.coursera.org/learn/business-analysis-fundamentals")
        }

        // Microsoft Learn Power BI
        findViewById<LinearLayout>(R.id.courseMicrosoft).setOnClickListener {
            openUrl("https://learn.microsoft.com/en-us/training/powerplatform/power-bi")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}