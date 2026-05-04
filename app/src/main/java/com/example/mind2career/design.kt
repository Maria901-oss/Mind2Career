package com.example.mind2career

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class DesignActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_design)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.layoutGoogle).setOnClickListener {
            openUrl("https://www.coursera.org/professional-certificates/google-ux-design")
        }

        findViewById<LinearLayout>(R.id.layoutFigma).setOnClickListener {
            openUrl("https://www.figma.com/resources/learn-design/")
        }

        findViewById<LinearLayout>(R.id.layoutDribbble).setOnClickListener {
            openUrl("https://dribbble.com/learn")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}