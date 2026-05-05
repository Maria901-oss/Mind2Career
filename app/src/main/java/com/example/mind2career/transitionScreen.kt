package com.example.mind2career

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView

class transitionScreen : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transition_screen)

        val cardSoftwareEngineering = findViewById<CardView>(R.id.cardSoftwareEngineering)
        val cardDigitalMarketing = findViewById<CardView>(R.id.cardDigitalMarketing)
        val cardDataScientist = findViewById<CardView>(R.id.cardDataScientist)
        val cardBusinessAnalyst = findViewById<CardView>(R.id.cardBusinessAnalyst)
        val cardTeacher = findViewById<CardView>(R.id.cardTeacher)
        val cardDesign = findViewById<CardView>(R.id.cardDesign)

        cardSoftwareEngineering.setOnClickListener {
            val intent=Intent(this, softwareEngineering::class.java)
            startActivity(intent)
        }

        cardDigitalMarketing.setOnClickListener {
            startActivity(Intent(this, DigitalMarketing::class.java))
        }

        cardDataScientist.setOnClickListener {
            startActivity(Intent(this, DataScientist::class.java))
        }

        cardBusinessAnalyst.setOnClickListener {
            startActivity(Intent(this, BusinessAnalyst::class.java))
        }

        cardTeacher.setOnClickListener {
            startActivity(Intent(this, TeacherActivity::class.java))
        }

        cardDesign.setOnClickListener {
            startActivity(Intent(this, DesignActivity::class.java))
        }
        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

// Transition screen hai toh transition item selected dikhao
        bottomNav.selectedItemId = R.id.nav_transition

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, home_activity::class.java))
                    true
                }
                R.id.nav_ranking -> {
                    startActivity(Intent(this, RankingScreen::class.java))
                    true
                }
                R.id.nav_transition -> {
                    true // Already here
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile::class.java))
                    true
                }
                else -> false
            }
        }
    }
}