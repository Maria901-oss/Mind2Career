package com.example.mind2career

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class home_activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
            startActivity(Intent(this, PesonalityAssesment::class.java))
        }

        // SKILL GAP → Skill screen
        cardSkill.setOnClickListener {
            startActivity(Intent(this, SkillGapScreen::class.java))
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
            val uri = Uri.parse("market://details?id=$packageName")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {

            when (item.itemId) {

                R.id.nav_home -> {
                    true
                }

                R.id.nav_ranking -> {
                    startActivity(Intent(this, RankingScreen::class.java))
                    true
                }

                R.id.nav_transition -> {
                    startActivity(Intent(this, TransitionScreen::class.java))
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileScreen::class.java))
                    true
                }

                else -> false
            }
        }
    }
}