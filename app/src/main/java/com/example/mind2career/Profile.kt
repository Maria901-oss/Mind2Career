package com.example.mind2career

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import java.io.ByteArrayOutputStream
import java.io.File

class Profile : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private var cameraUri: Uri? = null

    private val auth = FirebaseAuth.getInstance()

    // ===================== IMAGE PICKERS =====================

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { setCircularImageAndSave(it) }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraUri != null) setCircularImageAndSave(cameraUri!!)
            else Toast.makeText(this, "Image did not set from Camera", Toast.LENGTH_SHORT).show()
        }

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(this, "Allow Camera permission", Toast.LENGTH_SHORT).show()
        }

    // ===================== ON CREATE =====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        profileImage = findViewById(R.id.profileImage)
        tvUserName   = findViewById(R.id.tvUserName)
        tvUserEmail  = findViewById(R.id.tvUserEmail)

        loadUserInfo()
        loadSavedProfileImage()

        profileImage.setOnClickListener { showImagePickerDialog() }

        findViewById<LinearLayout>(R.id.layoutPersonalInfo).setOnClickListener {
            showPersonalInfoDialog()
        }

        findViewById<LinearLayout>(R.id.layoutSupport).setOnClickListener {
            showSupportDialog()
        }



        // ✅ Privacy Policy → PrivacyPolicyActivity
        findViewById<LinearLayout>(R.id.layoutPrivacyPolicy).setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        // ✅ Terms & Conditions → TermsConditionsActivity
        findViewById<LinearLayout>(R.id.layoutTermsConditions).setOnClickListener {
            startActivity(Intent(this, TermsConditionsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.layoutLogout).setOnClickListener {
            showLogoutDialog()
        }

        setupBottomNav()
    }

    // ===================== FIREBASE USER INFO =====================

    private fun loadUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            tvUserName.text  = if (!user.displayName.isNullOrEmpty()) user.displayName else "User"
            tvUserEmail.text = user.email ?: "Email not found"
        } else {
            tvUserName.text  = "User"
            tvUserEmail.text = "Logged out"
        }
    }

    // ===================== SAVE / LOAD PROFILE IMAGE =====================

    private fun saveProfileImageToPrefs(bitmap: Bitmap) {
        try {
            val stream = ByteArrayOutputStream()
            val scaled = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val encoded = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
            getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("profile_image", encoded)
                .apply()
        } catch (e: Exception) {
            Toast.makeText(this, "Image not saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedProfileImage() {
        try {
            val encoded = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
                .getString("profile_image", null) ?: return
            val bytes  = Base64.decode(encoded, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return
            val drawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
            drawable.isCircular = true
            profileImage.setImageDrawable(drawable)
            profileImage.background = null
        } catch (e: Exception) {
            // no saved image — default rehne do
        }
    }

    // ===================== PERSONAL INFO DIALOG =====================

    private fun showPersonalInfoDialog() {
        val user = auth.currentUser ?: return

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val p = (20 * resources.displayMetrics.density).toInt()
        layout.setPadding(p, p, p, p)

        val etName = EditText(this)
        etName.hint = "Write your Name"
        etName.setText(if (!user.displayName.isNullOrEmpty()) user.displayName else "")
        layout.addView(etName)

        val etEmail = EditText(this)
        etEmail.hint = "Email"
        etEmail.setText(user.email ?: "")
        etEmail.isEnabled = false
        etEmail.alpha = 0.5f
        layout.addView(etEmail)

        AlertDialog.Builder(this)
            .setTitle("Personal Information")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Please fill name field", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val updates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName).build()
                user.updateProfile(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        tvUserName.text = newName
                        Toast.makeText(this, "Name updated ✅", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Update failed ❌", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ===================== SUPPORT DIALOG =====================

    private fun showSupportDialog() {
        AlertDialog.Builder(this)
            .setTitle("Support")
            .setMessage("Contact our support team for any query.")
            .setPositiveButton("📧 Send Email") { _, _ ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")  // ✅ fixed: "mail to:" → "mailto:"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@mind2career.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "App Support Request")
                }
                try { startActivity(intent) }
                catch (e: Exception) {
                    Toast.makeText(this, "Email app not found", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ===================== LOGIN & SECURITY DIALOG =====================

    private fun showLoginSecurityDialog() {
        val email = auth.currentUser?.email
        AlertDialog.Builder(this)
            .setTitle("Login & Security")
            .setMessage("Send password reset email to \"$email\"?")
            .setPositiveButton("Reset") { _, _ ->
                if (!email.isNullOrEmpty()) {
                    auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                        if (task.isSuccessful)
                            Toast.makeText(this, "Reset email sent ✅", Toast.LENGTH_LONG).show()
                        else
                            Toast.makeText(this, "Email not sent ❌", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ===================== LOGOUT =====================

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Do you really want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    // ===================== CIRCULAR IMAGE =====================

    private fun showImagePickerDialog() {
        val options = arrayOf("📷  Camera", "🖼️  Gallery", "❌  Cancel")
        AlertDialog.Builder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> cameraPermission.launch(Manifest.permission.CAMERA)
                    1 -> galleryLauncher.launch("image/*")
                    2 -> dialog.dismiss()
                }
            }.show()
    }

    private fun openCamera() {
        val imageFile = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
        cameraUri = FileProvider.getUriForFile(
            this, "${applicationContext.packageName}.provider", imageFile
        )
        cameraLauncher.launch(cameraUri!!)
    }

    private fun setCircularImageAndSave(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                val circular = getCircularBitmap(bitmap)
                val drawable = RoundedBitmapDrawableFactory.create(resources, circular)
                drawable.isCircular = true
                profileImage.setImageDrawable(drawable)
                profileImage.background = null
                saveProfileImageToPrefs(circular)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size    = minOf(bitmap.width, bitmap.height)
        val squared = Bitmap.createBitmap(
            bitmap, (bitmap.width - size) / 2, (bitmap.height - size) / 2, size, size
        )
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint  = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        squared.recycle()
        return output
    }

    // ===================== BOTTOM NAV =====================

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java)); finish(); true
                }
                R.id.nav_ranking -> {
                    startActivity(Intent(this, RankingScreen::class.java)); finish(); true
                }
                R.id.nav_transition -> {
                    startActivity(Intent(this, transitionScreen::class.java)); finish(); true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) {
            Toast.makeText(this, "Browser not opening", Toast.LENGTH_SHORT).show()
        }
    }
}