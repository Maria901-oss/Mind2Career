package com.example.mind2career

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class RecommendationScreen : AppCompatActivity() {

    private lateinit var tvPersonality: TextView
    private lateinit var tvPercentage: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvStrengths: TextView
    private lateinit var tvWeaknesses: TextView
    private lateinit var tvSkills: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private var personalityInfo: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recommendation_screen)

        tvPersonality = findViewById(R.id.tvPersonality)
        tvPercentage  = findViewById(R.id.tvPercentage)
        tvDescription = findViewById(R.id.tvDescription)
        tvStrengths   = findViewById(R.id.tvStrengths)
        tvWeaknesses  = findViewById(R.id.tvWeaknesses)
        tvSkills      = findViewById(R.id.tvSkills)

        loadPersonalityInfo()
        loadUserData()
    }

    // ── Load personality_info.json from assets ────────────────────────────────
    private fun loadPersonalityInfo() {
        try {
            val json = assets.open("personality_info.json")
                .bufferedReader()
                .use { it.readText() }
            personalityInfo = JSONObject(json)
            Log.d("INFO_JSON", "Loaded ${personalityInfo!!.length()} entries")
        } catch (e: Exception) {
            Log.e("INFO_JSON", "Failed: ${e.message}")
        }
    }

    // ── Firestore fetch ───────────────────────────────────────────────────────
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

                Log.d("USER_DATA", doc.data.toString())

                val personality = doc.getString("personality") ?: "Not available"
                val percentage  = doc.getLong("percentage") ?: 0L

                @Suppress("UNCHECKED_CAST")
                val skills = doc.get("skills") as? List<String> ?: emptyList()

                // Basic fields
                tvPersonality.text = "Personality: $personality"
                tvPercentage.text  = "Score: $percentage%"
                tvSkills.text = if (skills.isNotEmpty())
                    skills.joinToString("\n• ", prefix = "• ")
                else
                    "Not available yet"

                // Description / Strengths / Weaknesses from JSON
                showPersonalityDetails(personality)
            }
            .addOnFailureListener { e ->
                Log.e("RECOMMENDATION", "Load failed: ${e.message}")
                tvPersonality.text = "Failed to load data"
            }
    }

    // ── Fill description / strengths / weaknesses ─────────────────────────────
    private fun showPersonalityDetails(personality: String) {
        val info = personalityInfo

        if (info == null || !info.has(personality)) {
            Log.w("INFO_JSON", "No entry for: $personality")
            tvDescription.text = ""
            tvStrengths.text   = "Not available"
            tvWeaknesses.text  = "Not available"
            return
        }

        val entry = info.getJSONObject(personality)

        tvDescription.text = entry.optString("description", "")
        tvStrengths.text   = entry.optString("strengths",   "Not available")
        tvWeaknesses.text  = entry.optString("weaknesses",  "Not available")

        Log.d("INFO_JSON", "Details loaded for: $personality")
    }
}
