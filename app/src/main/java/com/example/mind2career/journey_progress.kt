package com.example.mind2career

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
import com.google.gson.reflect.TypeToken

// ─────────────────────────────────────────────────────────────────────────────
// JourneyProgressActivity.kt
//
// Screen shown after "Start the Journey" is tapped.
// Shows each roadmap step with a checkbox — user can mark steps as done.
// Progress is saved to Firestore so it persists across sessions.
// ─────────────────────────────────────────────────────────────────────────────
class JourneyProgressActivity : AppCompatActivity() {

    private val db   by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var tvCareerTitle  : TextView
    private lateinit var tvSubtitle     : TextView
    private lateinit var progressBar    : ProgressBar
    private lateinit var tvProgressText : TextView
    private lateinit var rvSteps        : RecyclerView
    private lateinit var tvMotivation   : TextView
    private lateinit var btnBack        : View

    private var roadmap: CareerRoadmap? = null
    private val completedSteps = mutableSetOf<Int>() // stepNumber values that are done
    private var progressDocId  = ""                  // Firestore doc key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_journey_progress)

        bindViews()

        // Parse roadmap from intent
        val json = intent.getStringExtra("roadmapJson") ?: ""
        roadmap = try { Gson().fromJson(json, CareerRoadmap::class.java) } catch (e: Exception) { null }

        val personality = intent.getStringExtra("personality") ?: "Balanced"
        val grade       = intent.getStringExtra("grade")       ?: ""
        val field       = intent.getStringExtra("field")       ?: ""
        val percentage  = intent.getIntExtra("percentage", 0)

        progressDocId = "journey_${field}_${grade}".replace(" ", "_").lowercase()

        if (roadmap == null) {
            Toast.makeText(this, "Could not load roadmap.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvCareerTitle.text = roadmap!!.primaryCareer
        tvSubtitle.text    = "$personality  •  $grade  •  $percentage%"

        btnBack.setOnClickListener { finish() }

        loadProgressThenRender()
    }

    private fun bindViews() {
        tvCareerTitle  = findViewById(R.id.tvJourneyCareer)
        tvSubtitle     = findViewById(R.id.tvJourneySubtitle)
        progressBar    = findViewById(R.id.journeyProgressBar)
        tvProgressText = findViewById(R.id.tvJourneyProgressText)
        rvSteps        = findViewById(R.id.rvJourneySteps)
        tvMotivation   = findViewById(R.id.tvMotivation)
        btnBack        = findViewById(R.id.btnJourneyBack)
    }

    // ── Load saved progress from Firestore ────────────────────────────────────
    private fun loadProgressThenRender() {
        val uid = auth.currentUser?.uid ?: return renderSteps()
        db.collection("users").document(uid)
            .collection("journey_progress").document(progressDocId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val saved = (doc.get("completedSteps") as? List<*>)
                        ?.filterIsInstance<Long>()?.map { it.toInt() } ?: emptyList()
                    completedSteps.addAll(saved)
                }
                renderSteps()
            }
            .addOnFailureListener { renderSteps() }
    }

    // ── Render RecyclerView ───────────────────────────────────────────────────
    private fun renderSteps() {
        val steps = roadmap?.roadmapSteps ?: return
        updateProgressUI(steps.size)

        rvSteps.layoutManager            = LinearLayoutManager(this)
        rvSteps.isNestedScrollingEnabled = false
        rvSteps.adapter = JourneyStepsAdapter(
            steps        = steps,
            completedSet = completedSteps,
            onToggle     = { stepNumber, isDone ->
                if (isDone) completedSteps.add(stepNumber)
                else completedSteps.remove(stepNumber)
                updateProgressUI(steps.size)
                saveProgress()
            }
        )
    }

    // ── Update progress bar + text + motivation ───────────────────────────────
    private fun updateProgressUI(totalSteps: Int) {
        val done    = completedSteps.size
        val percent = if (totalSteps > 0) (done * 100) / totalSteps else 0

        progressBar.max      = 100
        progressBar.progress = percent
        tvProgressText.text  = "$done / $totalSteps steps completed  ($percent%)"

        tvMotivation.text = when {
            percent == 100 -> "🎉 Incredible! You've completed your entire roadmap!"
            percent >= 75  -> "🔥 Almost there — you're crushing it!"
            percent >= 50  -> "💪 You're halfway through — keep going!"
            percent >= 25  -> "🚀 Great start — momentum is building!"
            done == 0      -> "👇 Tap a step to mark it done as you go!"
            else           -> "✅ Step $done done — on your way to ${ roadmap?.primaryCareer ?: "your goal" }!"
        }
    }

    // ── Save progress to Firestore ────────────────────────────────────────────
    private fun saveProgress() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("journey_progress").document(progressDocId)
            .set(mapOf(
                "completedSteps" to completedSteps.toList(),
                "careerTitle"    to (roadmap?.primaryCareer ?: ""),
                "updatedAt"      to System.currentTimeMillis()
            ))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// JourneyStepsAdapter
// ─────────────────────────────────────────────────────────────────────────────
class JourneyStepsAdapter(
    private val steps        : List<RoadmapStep>,
    private val completedSet : Set<Int>,
    private val onToggle     : (stepNumber: Int, isDone: Boolean) -> Unit
) : RecyclerView.Adapter<JourneyStepsAdapter.JourneyVH>() {

    // Track local state per item so toggling is instant (no full rebind needed)
    private val localDone = mutableMapOf<Int, Boolean>().apply {
        steps.forEach { put(it.stepNumber, completedSet.contains(it.stepNumber)) }
    }

    inner class JourneyVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNum      : TextView  = v.findViewById(R.id.tvJourneyStepNum)
        val tvTitle    : TextView  = v.findViewById(R.id.tvJourneyStepTitle)
        val tvDesc     : TextView  = v.findViewById(R.id.tvJourneyStepDesc)
        val tvDuration : TextView  = v.findViewById(R.id.tvJourneyStepDuration)
        val checkBox   : CheckBox  = v.findViewById(R.id.cbStepDone)
        val cardRoot   : View      = v.findViewById(R.id.journeyStepCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        JourneyVH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journey_step, parent, false))

    override fun onBindViewHolder(h: JourneyVH, position: Int) {
        val step = steps[position]
        val done = localDone[step.stepNumber] ?: false

        // Step number — @color/button blue
        h.tvNum.text = "${step.stepNumber}"
        h.tvNum.setTextColor(ContextCompat.getColor(h.itemView.context, R.color.button))

        h.tvTitle.text    = step.title
        h.tvDesc.text     = step.description
        h.tvDuration.text = "⏱ ${step.duration}"

        // Visual "done" state
        applyDoneState(h, done)

        // Prevent listener double-fire during rebind
        h.checkBox.setOnCheckedChangeListener(null)
        h.checkBox.isChecked = done
        h.checkBox.setOnCheckedChangeListener { _, isChecked ->
            localDone[step.stepNumber] = isChecked
            applyDoneState(h, isChecked)
            onToggle(step.stepNumber, isChecked)
        }

        // Tap card = toggle checkbox
        h.cardRoot.setOnClickListener {
            h.checkBox.isChecked = !h.checkBox.isChecked
        }
    }

    private fun applyDoneState(h: JourneyVH, done: Boolean) {
        h.cardRoot.alpha  = if (done) 0.65f else 1f
        h.tvTitle.paintFlags = if (done)
            h.tvTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        else
            h.tvTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
        h.tvTitle.setTextColor(
            if (done) Color.parseColor("#9E9E9E") else Color.parseColor("#1A1A2E")
        )
    }

    override fun getItemCount() = steps.size
}
