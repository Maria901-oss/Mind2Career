package com.example.mind2career

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class profile : AppCompatActivity() {

    private lateinit var profileImage: ImageView

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { profileImage.setImageURI(it) }
        }

    private var cameraUri: Uri? = null
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) cameraUri?.let { profileImage.setImageURI(it) }
        }

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_screen)

        profileImage = findViewById(R.id.profileImage)
        profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        findViewById<LinearLayout>(R.id.layoutPersonalInfo).setOnClickListener {
            android.widget.Toast.makeText(this, "Personal Information", android.widget.Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.layoutSupport).setOnClickListener {
            android.widget.Toast.makeText(this, "Support", android.widget.Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.layoutLoginSecurity).setOnClickListener {
            android.widget.Toast.makeText(this, "Login & Security", android.widget.Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.layoutPrivacyPolicy).setOnClickListener {
            openUrl("https://yourapp.com/privacy-policy")
        }

        findViewById<LinearLayout>(R.id.layoutTermsConditions).setOnClickListener {
            openUrl("https://yourapp.com/terms")
        }

        findViewById<LinearLayout>(R.id.layoutLogout).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Camera se photo lo", "Gallery se choose karo", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> cameraPermission.launch(android.Manifest.permission.CAMERA)
                    1 -> galleryLauncher.launch("image/*")
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openCamera() {
        val imageFile = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
        cameraUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            imageFile
        )
        cameraLauncher.launch(cameraUri!!)
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Kya aap waqai log out karna chahte hain?")
            .setPositiveButton("Haan") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Nahi", null)
            .show()
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}