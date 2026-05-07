package com.example.mind2career

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// HistoryActivity.kt
//
// Lists every recommendation that was ever generated for the signed-in user.
// Tapping an entry loads the full roadmap in JourneyProgressActivity.
// ─────────────────────────────────────────────────────────────────────────────
class HistoryActivity : AppCompatActivity() {

    private val db   by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var progressBar  : ProgressBar
    private lateinit var rvHistory    : RecyclerView
    private lateinit var tvEmpty      : TextView
    private lateinit var btnBack      : View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.history)
        bindViews()
        btnBack.setOnClickListener { finish() }
        loadHistory()
    }

    private fun bindViews() {
        progressBar = findViewById(R.id.historyProgressBar)
        rvHistory   = findViewById(R.id.rvHistory)
        tvEmpty     = findViewById(R.id.tvHistoryEmpty)
        btnBack     = findViewById(R.id.btnHistoryBack)
    }

    private fun loadHistory() {
        val uid = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE
        rvHistory.visibility   = View.GONE
        tvEmpty.visibility     = View.GONE

        db.collection("users").document(uid)
            .collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                if (snapshot == null || snapshot.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        HistoryItem(
                            docId          = doc.id,
                            timestamp      = doc.getLong("timestamp") ?: 0L,
                            personality    = doc.getString("personality") ?: "",
                            grade          = doc.getString("grade") ?: "",
                            field          = doc.getString("field") ?: "",
                            percentage     = (doc.getLong("percentage") ?: 0L).toInt(),
                            score          = (doc.getLong("score") ?: 0L).toInt(),
                            total          = (doc.getLong("total") ?: 0L).toInt(),
                            primaryCareer  = doc.getString("primaryCareer") ?: "",
                            estimatedTimeline = doc.getString("estimatedTimeline") ?: "",
                            performanceLabel  = doc.getString("performanceLabel") ?: "",
                            skillsToLearn     = (doc.get("skillsToLearn") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList(),
                            skills            = (doc.get("skills") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList(),
                            whyThisCareer     = doc.getString("whyThisCareer") ?: "",
                            personalityInsight= doc.getString("personalityInsight") ?: "",
                            recommendedCareers= (doc.get("recommendedCareers") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList(),
                            strongSubjects    = (doc.get("strongSubjects") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList(),
                            weakSubjects      = (doc.get("weakSubjects") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList(),
                            roadmapStepsJson  = doc.getString("roadmapStepsJson") ?: "[]"
                        )
                    } catch (e: Exception) { null }
                }

                if (items.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                rvHistory.visibility   = View.VISIBLE
                rvHistory.layoutManager = LinearLayoutManager(this)
                rvHistory.adapter = HistoryAdapter(items) { item ->
                    openJourneyFromHistory(item)
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                tvEmpty.text           = "Could not load history. Try again later."
                tvEmpty.visibility     = View.VISIBLE
            }
    }

    // ── Reconstruct roadmap from history item and open JourneyProgressActivity
    private fun openJourneyFromHistory(item: HistoryItem) {
        val stepsType = object : TypeToken<List<RoadmapStep>>() {}.type
        val steps: List<RoadmapStep> = try {
            Gson().fromJson(item.roadmapStepsJson, stepsType)
        } catch (e: Exception) { emptyList() }

        val roadmap = CareerRoadmap(
            recommendedCareers  = item.recommendedCareers,
            primaryCareer       = item.primaryCareer,
            whyThisCareer       = item.whyThisCareer,
            roadmapSteps        = steps,
            skillsToLearn       = item.skillsToLearn,
            skillsYouHave       = item.skills,
            strongSubjects      = item.strongSubjects,
            weakSubjects        = item.weakSubjects,
            personalityInsight  = item.personalityInsight,
            estimatedTimeline   = item.estimatedTimeline,
            performanceLabel    = item.performanceLabel,
            performanceColor    = "#2196F3"
        )

        val intent = Intent(this, JourneyProgressActivity::class.java).apply {
            putExtra("roadmapJson",  Gson().toJson(roadmap))
            putExtra("personality",  item.personality)
            putExtra("grade",        item.grade)
            putExtra("field",        item.field)
            putExtra("percentage",   item.percentage)
        }
        startActivity(intent)
    }
}

// ── Data class for one history row ────────────────────────────────────────────
data class HistoryItem(
    val docId             : String,
    val timestamp         : Long,
    val personality       : String,
    val grade             : String,
    val field             : String,
    val percentage        : Int,
    val score             : Int,
    val total             : Int,
    val primaryCareer     : String,
    val estimatedTimeline : String,
    val performanceLabel  : String,
    val skillsToLearn     : List<String>,
    val skills            : List<String>,
    val whyThisCareer     : String,
    val personalityInsight: String,
    val recommendedCareers: List<String>,
    val strongSubjects    : List<String>,
    val weakSubjects      : List<String>,
    val roadmapStepsJson  : String
)

// ── History list adapter ──────────────────────────────────────────────────────
class HistoryAdapter(
    private val items    : List<HistoryItem>,
    private val onOpen   : (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryVH>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.getDefault())

    inner class HistoryVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvDate        : TextView = v.findViewById(R.id.tvHistoryDate)
        val tvCareer      : TextView = v.findViewById(R.id.tvHistoryCareer)
        val tvPersonality : TextView = v.findViewById(R.id.tvHistoryPersonality)
        val tvGradeField  : TextView = v.findViewById(R.id.tvHistoryGradeField)
        val tvPerf        : TextView = v.findViewById(R.id.tvHistoryPerf)
        val tvTimeline    : TextView = v.findViewById(R.id.tvHistoryTimeline)
        val cardRoot      : View     = v.findViewById(R.id.historyCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        HistoryVH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false))

    override fun onBindViewHolder(h: HistoryVH, i: Int) {
        val item = items[i]
        h.tvDate.text        = dateFormat.format(Date(item.timestamp))
        h.tvCareer.text      = item.primaryCareer
        h.tvPersonality.text = "🧠 ${item.personality}"
        h.tvGradeField.text  = "🎓 ${item.grade}  •  ${item.field}"
        h.tvPerf.text        = "${item.performanceLabel}  (${item.percentage}%  —  ${item.score}/${item.total})"
        h.tvTimeline.text    = "⏱ ${item.estimatedTimeline}"
        h.cardRoot.setOnClickListener { onOpen(item) }
    }

    override fun getItemCount() = items.size
}
