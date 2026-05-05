package com.example.mind2career

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File

class Profile : AppCompatActivity() {

    private lateinit var profileImage: ImageView

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { profileImage.setImageURI(it) }
        }

    private var cameraUri: Uri? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraUri != null) {
                profileImage.setImageURI(cameraUri)
            } else {
                Toast.makeText(this, "Camera failed", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_screen)

        profileImage = findViewById(R.id.profileImage)

        profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        findViewById<LinearLayout>(R.id.layoutPersonalInfo).setOnClickListener {
            Toast.makeText(this, "Personal Information", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.layoutSupport).setOnClickListener {
            Toast.makeText(this, "Support", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.layoutLoginSecurity).setOnClickListener {
            Toast.makeText(this, "Login & Security", Toast.LENGTH_SHORT).show()
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

        // BOTTOM NAV FIX
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }

                R.id.nav_ranking -> {
                    startActivity(Intent(this, RankingScreen::class.java))
                    true
                }

                R.id.nav_transition -> {
                    startActivity(Intent(this, transitionScreen::class.java))
                    true
                }

                R.id.nav_profile -> {
                    true
                }

                else -> false
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> cameraPermission.launch(Manifest.permission.CAMERA)
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
            "${applicationContext.packageName}.provider",
            imageFile
        )

        cameraLauncher.launch(cameraUri!!)
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}