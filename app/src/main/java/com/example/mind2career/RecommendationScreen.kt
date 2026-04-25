package com.example.mind2career

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecommendationScreen : AppCompatActivity() {

    private lateinit var tvPersonality: TextView
    private lateinit var tvPercentage: TextView
    private lateinit var tvSkills: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation_screen)

        tvPersonality = findViewById(R.id.tvPersonality)
        tvPercentage = findViewById(R.id.tvPercentage)
        tvSkills = findViewById(R.id.tvSkills)

        loadUserData()
    }

    private fun loadUserData() {

        val userId = auth.currentUser?.uid

        if (userId == null) {
            tvPersonality.text = "User not logged in"
            return
        }

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {
                    tvPersonality.text = "No data found"
                    return@addOnSuccessListener
                }

                // DEBUG (optional)
                Log.d("USER_DATA", doc.data.toString())

                val personality = doc.getString("personality") ?: "Not available"
                val percentage = doc.getLong("percentage") ?: 0

                // ✅ FIXED: direct fetch
                val skills = doc.get("skills") as? List<*> ?: emptyList<Any>()

                tvPersonality.text = "Personality: $personality"
                tvPercentage.text = "Percentage: $percentage%"

                tvSkills.text = if (skills.isNotEmpty()) {
                    "Skills: ${skills.joinToString(", ")}"
                } else {
                    "Skills: Not found"
                }
            }
            .addOnFailureListener {
                tvPersonality.text = "Failed to load data"
            }
    }
}