package com.example.mind2career

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
    private lateinit var btnStartJourney : Button
    private lateinit var btnSavePdf      : Button

    private var detailsExpanded = false

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            Toast.makeText(this, "Your journey begins now! 🚀", Toast.LENGTH_SHORT).show()
            // TODO: navigate to next screen
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

                renderUI(roadmap, personality, grade, field, percentage, score, total, skills)
            }
            .addOnFailureListener { e ->
                showError("Error: ${e.localizedMessage ?: "Unknown"}")
            }
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

        // Skills chips
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

    private fun makeSkillChip(text: String): TextView = TextView(this).apply {
        this.text = text
        setPadding(24, 8, 24, 8)
        textSize = 12f
        setTextColor(Color.parseColor("#1565C0"))
        background = ContextCompat.getDrawable(context, R.drawable.chip_bg_need)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 8, 0) }
    }

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
            setTextColor(Color.parseColor("#1565C0"))
        })
        addView(TextView(context).apply {
            text = skillName   // ✅ ab bilkul clear hai
            textSize = 13f
            setTextColor(Color.parseColor("#1565C0"))
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
        h.tvNum.text   = "${s.stepNumber}"
        h.tvTitle.text = s.title
        h.tvDesc.text  = s.description
        h.tvDur.text   = s.duration
    }

    override fun getItemCount() = steps.size
}
