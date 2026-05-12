package com.example.mind2career

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class RecommendationScreen : AppCompatActivity() {

    private val db   by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // ── State views ───────────────────────────────────────────────────────────
    private lateinit var progressBar    : ProgressBar
    private lateinit var contentScroll  : ScrollView
    private lateinit var errorLayout    : LinearLayout
    private lateinit var tvErrorMessage : TextView
    private lateinit var btnRetry       : Button

    // ── Hero card ─────────────────────────────────────────────────────────────
    private lateinit var tvPersonalityType    : TextView
    private lateinit var tvResultPercent      : TextView
    private lateinit var llSkillsHave         : LinearLayout
    private lateinit var tvGradeField         : TextView
    private lateinit var tvDetailsToggle      : TextView
    private lateinit var tvPersonalityInsight : TextView

    // ── Journey steps ─────────────────────────────────────────────────────────
    private lateinit var tvPrimaryCareer : TextView
    private lateinit var tvWhyCareer     : TextView
    private lateinit var llSkillsNeed    : LinearLayout
    private lateinit var rvRoadmapSteps  : RecyclerView
    private lateinit var tvAlt1          : TextView
    private lateinit var tvTimeline      : TextView
    private lateinit var tvAlt2          : TextView

    // ── Subject cards ─────────────────────────────────────────────────────────
    private lateinit var cardStrong       : View
    private lateinit var cardWeak         : View
    private lateinit var llStrongSubjects : LinearLayout
    private lateinit var llWeakSubjects   : LinearLayout

    // ── CTA ───────────────────────────────────────────────────────────────────
    private lateinit var btnStartJourney  : Button
    private lateinit var btnGoHome        : Button
    private lateinit var btnHistory       : Button

    private var detailsExpanded = false

    // Keep roadmap in memory so we can pass it to JourneyProgressActivity
    private var currentRoadmap: CareerRoadmap? = null
    private var currentPersonality: String = ""
    private var currentGrade: String = ""
    private var currentField: String = ""
    private var currentPercentage: Int = 0

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recommendation_screen)
        bindViews()
        setupListeners()
        fetchAndGenerate()
    }

    private fun bindViews() {
        progressBar           = findViewById(R.id.progressBar)
        contentScroll         = findViewById(R.id.contentScroll)
        errorLayout           = findViewById(R.id.errorLayout)
        tvErrorMessage        = findViewById(R.id.tvErrorMessage)
        btnRetry              = findViewById(R.id.btnRetry)

        tvPersonalityType     = findViewById(R.id.tvPersonalityType)
        tvResultPercent       = findViewById(R.id.tvResultPercent)
        llSkillsHave          = findViewById(R.id.llSkillsHave)
        tvGradeField          = findViewById(R.id.tvGradeField)
        tvDetailsToggle       = findViewById(R.id.tvDetailsToggle)
        tvPersonalityInsight  = findViewById(R.id.tvPersonalityInsight)

        tvPrimaryCareer       = findViewById(R.id.tvPrimaryCareer)
        tvWhyCareer           = findViewById(R.id.tvWhyCareer)
        llSkillsNeed          = findViewById(R.id.llSkillsNeed)
        rvRoadmapSteps        = findViewById(R.id.rvRoadmapSteps)

        tvAlt1                = findViewById(R.id.tvAlt1)
        tvTimeline            = findViewById(R.id.tvTimeline)
        tvAlt2                = findViewById(R.id.tvAlt2)

        cardStrong            = findViewById(R.id.cardStrong)
        cardWeak              = findViewById(R.id.cardWeak)
        llStrongSubjects      = findViewById(R.id.llStrongSubjects)
        llWeakSubjects        = findViewById(R.id.llWeakSubjects)

        btnStartJourney       = findViewById(R.id.btnStartJourney)
        btnGoHome             = findViewById(R.id.btnGoHome)
        btnHistory            = findViewById(R.id.btnHistory)
    }

    private fun setupListeners() {
        btnRetry.setOnClickListener { fetchAndGenerate() }
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        tvDetailsToggle.setOnClickListener {
            detailsExpanded = !detailsExpanded
            tvPersonalityInsight.visibility =
                if (detailsExpanded) View.VISIBLE else View.GONE
            tvDetailsToggle.text =
                if (detailsExpanded) "DETAILS ▴" else "DETAILS ▾"
        }

        btnStartJourney.setOnClickListener {
            val roadmap = currentRoadmap ?: return@setOnClickListener
            val intent = Intent(this, JourneyProgressActivity::class.java).apply {
                putExtra("roadmapJson", Gson().toJson(roadmap))
                putExtra("personality", currentPersonality)
                putExtra("grade", currentGrade)
                putExtra("field", currentField)
                putExtra("percentage", currentPercentage)
            }
            startActivity(intent)
        }

        btnGoHome.setOnClickListener {
            // Go back to the root / home activity
            val intent = Intent(this, home_activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    // ── Firebase fetch ────────────────────────────────────────────────────────
    private fun fetchAndGenerate() {
        val uid = auth.currentUser?.uid ?: run {
            showError("Not logged in. Please sign in again.")
            return
        }
        showLoading()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc == null || !doc.exists()) {
                    showError("No profile found. Complete your quiz first.")
                    return@addOnSuccessListener
                }

                val personality = doc.getString("personality")  ?: "Balanced"
                val grade       = doc.getString("grade")        ?: "Bachelors"
                val field       = doc.getString("field")        ?: "Software Engineering"
                val percentage  = (doc.getLong("percentage")    ?: 50L).toInt()
                val score       = (doc.getLong("score")         ?: 0L).toInt()
                val total       = (doc.getLong("total")         ?: 10L).toInt()

                @Suppress("UNCHECKED_CAST")
                val skills: List<String> = (doc.get("skills") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()

                val roadmap = RecommendationEngine.generateRoadmap(
                    personality = personality,
                    grade       = grade,
                    field       = field,
                    percentage  = percentage,
                    skills      = skills
                )

                // Store for navigation and history
                currentRoadmap     = roadmap
                currentPersonality = personality
                currentGrade       = grade
                currentField       = field
                currentPercentage  = percentage

                // Save this recommendation to Firestore history
                saveToHistory(uid, roadmap, personality, grade, field, percentage, score, total, skills)

                renderUI(roadmap, personality, grade, field, percentage, score, total, skills)
            }
            .addOnFailureListener { e ->
                showError("Error: ${e.localizedMessage ?: "Unknown"}")
            }
    }

    // ── Save recommendation to history ────────────────────────────────────────
    private fun saveToHistory(
        uid: String,
        roadmap: CareerRoadmap,
        personality: String,
        grade: String,
        field: String,
        percentage: Int,
        score: Int,
        total: Int,
        skills: List<String>
    ) {
        val historyEntry = hashMapOf(
            "timestamp"       to System.currentTimeMillis(),
            "personality"     to personality,
            "grade"           to grade,
            "field"           to field,
            "percentage"      to percentage,
            "score"           to score,
            "total"           to total,
            "skills"          to skills,
            "primaryCareer"   to roadmap.primaryCareer,
            "whyThisCareer"   to roadmap.whyThisCareer,
            "estimatedTimeline" to roadmap.estimatedTimeline,
            "performanceLabel"  to roadmap.performanceLabel,
            "recommendedCareers" to roadmap.recommendedCareers,
            "skillsToLearn"   to roadmap.skillsToLearn,
            "strongSubjects"  to roadmap.strongSubjects,
            "weakSubjects"    to roadmap.weakSubjects,
            "personalityInsight" to roadmap.personalityInsight,
            "roadmapStepsJson" to Gson().toJson(roadmap.roadmapSteps)
        )

        db.collection("users").document(uid)
            .collection("history")
            .add(historyEntry)
        // Silently ignore failures — history is non-critical
    }

    // ── Render full UI ────────────────────────────────────────────────────────
    private fun renderUI(
        roadmap     : CareerRoadmap,
        personality : String,
        grade       : String,
        field       : String,
        percentage  : Int,
        score       : Int,
        total       : Int,
        skills      : List<String>
    ) {
        // Hero card
        tvPersonalityType.text    = personality
        tvResultPercent.text      = "$percentage%"
        tvPersonalityInsight.text = roadmap.personalityInsight
        tvGradeField.text         = "🎓  $grade — $field   ($score/$total correct)"

        // Skills chips — NOW using @color/button (blue) color
        llSkillsHave.removeAllViews()
        skills.take(5).forEach { llSkillsHave.addView(makeSkillChip(it)) }
        if (skills.isEmpty()) llSkillsHave.addView(makeEmptyNote("No skills added yet"))

        // Step 1: Career
        tvPrimaryCareer.text = roadmap.primaryCareer
        tvWhyCareer.text     = roadmap.whyThisCareer

        // Step 2: Skills to learn
        llSkillsNeed.removeAllViews()
        if (roadmap.skillsToLearn.isEmpty()) {
            llSkillsNeed.addView(makeEmptyNote("You already have the key skills!"))
        } else {
            roadmap.skillsToLearn.forEach { llSkillsNeed.addView(makeSkillRow(it)) }
        }

        // Step 3: Roadmap steps
        rvRoadmapSteps.layoutManager            = LinearLayoutManager(this)
        rvRoadmapSteps.adapter                  = RoadmapStepsAdapter(roadmap.roadmapSteps)
        rvRoadmapSteps.isNestedScrollingEnabled = false

        // Step 4: Growth path card
        val alts    = roadmap.recommendedCareers.drop(1)
        tvAlt1.text = if (alts.isNotEmpty()) alts[0] else roadmap.primaryCareer
        tvTimeline.text = "Target reached in ${roadmap.estimatedTimeline}"
        tvAlt2.text = "ESTIMATED SALARY: View details →"

        // Strong subjects
        if (roadmap.strongSubjects.isNotEmpty()) {
            cardStrong.visibility = View.VISIBLE
            llStrongSubjects.removeAllViews()
            roadmap.strongSubjects.forEach {
                llStrongSubjects.addView(makeSubjectChip(it, true))
            }
        } else {
            cardStrong.visibility = View.GONE
        }

        // Weak subjects
        if (roadmap.weakSubjects.isNotEmpty()) {
            cardWeak.visibility = View.VISIBLE
            llWeakSubjects.removeAllViews()
            roadmap.weakSubjects.forEach {
                llWeakSubjects.addView(makeSubjectChip(it, false))
            }
        } else {
            cardWeak.visibility = View.GONE
        }

        showContent()
    }

    // ── View factories ────────────────────────────────────────────────────────

    // Personality "Key Skills" chips — @color/button (blue) text
    private fun makeSkillChip(text: String): TextView = TextView(this).apply {
        this.text = text
        setPadding(24, 8, 24, 8)
        textSize = 12f
        setTextColor(ContextCompat.getColor(context, R.color.button))
        background = ContextCompat.getDrawable(context, R.drawable.chip_bg_need)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 8, 0) }
    }

    // "Skills to Learn" rows — @color/button (blue) text
    private fun makeSkillRow(skillName: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(14, 10, 14, 10)
        background = ContextCompat.getDrawable(context, R.drawable.chip_bg_need)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, 8) }

        addView(TextView(context).apply {
            text = "✓  "
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.button))
        })
        addView(TextView(context).apply {
            text = skillName
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.button))
        })
    }

    private fun makeSubjectChip(text: String, isStrong: Boolean): TextView = TextView(this).apply {
        this.text = text
        setPadding(22, 8, 22, 8)
        textSize = 12f
        setTextColor(
            if (isStrong) Color.parseColor("#1B5E20")
            else Color.parseColor("#BF360C")
        )
        background = ContextCompat.getDrawable(
            context,
            if (isStrong) R.drawable.chip_bg_have else R.drawable.chip_bg_weak
        )
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 10, 0) }
    }

    private fun makeEmptyNote(msg: String): TextView = TextView(this).apply {
        text = msg
        textSize = 13f
        setTextColor(Color.parseColor("#888888"))
    }

    // ── State helpers ─────────────────────────────────────────────────────────
    private fun showLoading() {
        progressBar.visibility   = View.VISIBLE
        contentScroll.visibility = View.GONE
        errorLayout.visibility   = View.GONE
    }
    private fun showContent() {
        progressBar.visibility   = View.GONE
        contentScroll.visibility = View.VISIBLE
        errorLayout.visibility   = View.GONE
    }
    private fun showError(msg: String) {
        progressBar.visibility   = View.GONE
        contentScroll.visibility = View.GONE
        errorLayout.visibility   = View.VISIBLE
        tvErrorMessage.text      = msg
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// RoadmapStepsAdapter
// ─────────────────────────────────────────────────────────────────────────────
class RoadmapStepsAdapter(
    private val steps: List<RoadmapStep>
) : RecyclerView.Adapter<RoadmapStepsAdapter.StepVH>() {

    inner class StepVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNum  : TextView = v.findViewById(R.id.tvStepNumber)
        val tvTitle: TextView = v.findViewById(R.id.tvStepTitle)
        val tvDesc : TextView = v.findViewById(R.id.tvStepDescription)
        val tvDur  : TextView = v.findViewById(R.id.tvStepDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StepVH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_roadmap_step, parent, false))

    override fun onBindViewHolder(h: StepVH, i: Int) {
        val s = steps[i]
        // Step number — @color/button (blue) as per requirement
        h.tvNum.text   = "${s.stepNumber}"
        h.tvNum.setTextColor(
            ContextCompat.getColor(h.itemView.context, R.color.button)
        )
        h.tvTitle.text = s.title
        h.tvDesc.text  = s.description
        h.tvDur.text   = s.duration
    }

    override fun getItemCount() = steps.size
}
