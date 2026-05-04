package com.example.mind2career

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardSoftwareEngineering = findViewById<CardView>(R.id.cardSoftwareEngineering)
        val cardDigitalMarketing = findViewById<CardView>(R.id.cardDigitalMarketing)
        val cardDataScientist = findViewById<CardView>(R.id.cardDataScientist)
        val cardBusinessAnalyst = findViewById<CardView>(R.id.cardBusinessAnalyst)
        val cardTeacher = findViewById<CardView>(R.id.cardTeacher)
        val cardDesign = findViewById<CardView>(R.id.cardDesign)

        cardSoftwareEngineering.setOnClickListener {
            startActivity(Intent(this, SoftwareActivity::class.java))
        }

        cardDigitalMarketing.setOnClickListener {
            startActivity(Intent(this, digitalmarketing::class.java))
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
    }
}