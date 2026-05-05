// FILE: DataScientist.kt
// KAHAN RAKHO: app → kotlin+java → com.example.mind → DataScientist.kt

package com.example.mind2career

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class DataScientist : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_datascientist)

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Course 1: IBM Data Science on Coursera
        findViewById<LinearLayout>(R.id.courseIBM).setOnClickListener {
            openUrl("https://www.coursera.org/professional-certificates/ibm-data-science")
        }

        // Course 2: Kaggle Learn
        findViewById<LinearLayout>(R.id.courseKaggle).setOnClickListener {
            openUrl("https://www.kaggle.com/learn")
        }

        // Course 3: Fast.ai
        findViewById<LinearLayout>(R.id.courseFastAi).setOnClickListener {
            openUrl("https://www.fast.ai")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}