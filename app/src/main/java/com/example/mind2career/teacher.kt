package com.example.mind2career

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class TeacherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher)

        // Back button
        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Coursera
        findViewById<LinearLayout>(R.id.layoutCoursera).setOnClickListener {
            openUrl("https://www.coursera.org/learn/foundations-of-teaching")
        }

        // edX
        findViewById<LinearLayout>(R.id.layoutEdx).setOnClickListener {
            openUrl("https://www.edx.org/learn/teaching")
        }

        // Khan Academy
        findViewById<LinearLayout>(R.id.layoutKhan).setOnClickListener {
            openUrl("https://www.khanacademy.org/teacher")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}