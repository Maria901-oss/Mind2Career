package com.example.mind2career

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView


class home_activity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // SharedPreferences
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isTestDone = prefs.getBoolean("test_done", false)

        // Cards
        val cardTest = findViewById<MaterialCardView>(R.id.card1)
        val cardSkill = findViewById<MaterialCardView>(R.id.card2)
        val cardCareer = findViewById<MaterialCardView>(R.id.card3)
        val cardRate = findViewById<MaterialCardView>(R.id.card4)

        // TEST → Personality Assessment
        cardTest.setOnClickListener {
            startActivity(Intent(this, personality::class.java))
        }

        // SKILL GAP → Skill screen
        cardSkill.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // CAREER PLAN
        cardCareer.setOnClickListener {
            if (isTestDone) {
                startActivity(Intent(this, RecommendationScreen::class.java))
            } else {
                Toast.makeText(
                    this,
                    "Please complete the personality assessment before viewing the career plan.",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this, PesonalityAssesment::class.java))
            }
        }

        // RATE US → Play Store
        cardRate.setOnClickListener {

            val dialogView = layoutInflater.inflate(R.layout.dialog_rate, null)

            val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.ratingBar)
            val btnSubmit = dialogView.findViewById<android.widget.Button>(R.id.btnSubmitRating)

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            btnSubmit.setOnClickListener {

                val rating = ratingBar.rating

                if (rating == 0f) {

                    Toast.makeText(this, "Please select rating", Toast.LENGTH_SHORT).show()

                } else {

                    val userId = auth.currentUser?.uid ?: "anonymous"

                    val ratingData = hashMapOf(
                        "userId" to userId,
                        "rating" to rating,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("AppRatings")
                        .document(userId)
                        .set(ratingData)
                        .addOnSuccessListener {

                            Toast.makeText(
                                this,
                                "Thanks for rating: $rating ⭐",
                                Toast.LENGTH_SHORT
                            ).show()

                            dialog.dismiss()
                        }
                        .addOnFailureListener {

                            Toast.makeText(
                                this,
                                "Failed to submit rating",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }

            dialog.show()
        }

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
// Home par aate hi home item selected dikhao
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener {item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    if (this !is home_activity) {
                        val intent = Intent(this, home_activity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                    true
                }

                R.id.nav_ranking -> {
                    startActivity(Intent(this, RankingScreen::class.java))
                    finish()
                    true
                }

                R.id.nav_transition -> {
                    startActivity(Intent(this, transitionScreen::class.java))
                    finish()
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }
    }
}