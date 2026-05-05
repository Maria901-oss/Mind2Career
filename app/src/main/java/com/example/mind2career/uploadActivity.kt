package com.example.mind2career

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

/**
 * UploadActivity — Sirf ek baar use karo
 *
 * Steps:
 * 1. Yeh file apni project mein add karo
 * 2. AndroidManifest.xml mein add karo:
 *    <activity android:name=".UploadActivity"/>
 * 3. Academic_Dataset.json → app/src/main/assets/ mein rakho
 * 4. App run karo, seedha is activity kholo:
 *    Intent(this, UploadActivity::class.java) start karo kisi bhi jagah se
 * 5. "UPLOAD DATA" button dabao — wait karo
 * 6. "All data uploaded successfully!" message aaye toh ho gaya
 * 7. Yeh activity aur manifest entry hata do
 */
class UploadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Simple UI — no XML needed
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val tvTitle = TextView(this).apply {
            text     = "Firebase Data Uploader"
            textSize = 20f
            gravity  = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val tvInfo = TextView(this).apply {
            text     = "Yeh button sirf EK BAAR dabao.\nSaare grades, fields aur questions\nFirebase mein upload ho jayenge."
            textSize = 14f
            gravity  = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val progressBar = ProgressBar(this).apply {
            visibility = android.view.View.GONE
            setPadding(0, 0, 0, 16)
        }

        val tvStatus = TextView(this).apply {
            text     = ""
            textSize = 14f
            gravity  = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        val btnUpload = Button(this).apply {
            text    = "UPLOAD DATA TO FIREBASE"
            setPadding(32, 16, 32, 16)
        }

        btnUpload.setOnClickListener {
            btnUpload.isEnabled = false
            btnUpload.text      = "Uploading..."
            progressBar.visibility = android.view.View.VISIBLE
            tvStatus.text       = "Uploading grades..."

            DataUploader(this).uploadAll { success, msg ->
                runOnUiThread {
                    progressBar.visibility = android.view.View.GONE
                    tvStatus.text = msg
                    if (success) {
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                        btnUpload.text = "Done! ✓"
                    } else {
                        tvStatus.setTextColor(android.graphics.Color.RED)
                        btnUpload.isEnabled = true
                        btnUpload.text = "Retry"
                    }
                }
            }
        }

        layout.addView(tvTitle)
        layout.addView(tvInfo)
        layout.addView(progressBar)
        layout.addView(btnUpload)
        layout.addView(tvStatus)
        setContentView(layout)
    }
}
