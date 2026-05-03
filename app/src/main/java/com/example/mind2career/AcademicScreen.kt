package com.example.mind2career

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONObject

class AcademicTestActivity : AppCompatActivity() {

    private lateinit var spinnerGrade: Spinner
    private lateinit var spinnerField: Spinner
    private lateinit var questionsContainer: LinearLayout
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val gradeList    = mutableListOf<Map<String, String>>()
    private val fieldList    = mutableListOf<Map<String, String>>()
    private val questionList = mutableListOf<Map<String, Any>>()
    private val radioGroups  = mutableListOf<RadioGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academic_screen)

        spinnerGrade       = findViewById(R.id.spinnerGrade)
        spinnerField       = findViewById(R.id.spinnerField)
        questionsContainer = findViewById(R.id.questionsContainer)
        btnSubmit          = findViewById(R.id.btnSubmit)
        progressBar        = findViewById(R.id.progressBar)

        btnSubmit.visibility = View.GONE
        loadGrades()
        btnSubmit.setOnClickListener { submitTest() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  KEY FIX: Option ko safely parse karo
    //  Firebase se option 2 tarah aa sakta hai:
    //    1. Proper Map  →  { text: "baseband signal", isCorrect: true }
    //    2. String      →  "{\"text\":\"baseband signal\",\"isCorrect\":true}"
    // ─────────────────────────────────────────────────────────────────────────
    private fun parseOption(raw: Any?): Pair<String, Boolean> {
        return when (raw) {
            is Map<*, *> -> {
                val text      = raw["text"]?.toString() ?: ""
                val isCorrect = raw["isCorrect"] as? Boolean ?: false
                Pair(text, isCorrect)
            }
            is String -> {
                try {
                    val obj       = JSONObject(raw)
                    val text      = obj.optString("text", raw)
                    val isCorrect = obj.optBoolean("isCorrect", false)
                    Pair(text, isCorrect)
                } catch (e: Exception) {
                    Pair(raw, false)
                }
            }
            else -> Pair(raw?.toString() ?: "", false)
        }
    }

    // ── GRADES ───────────────────────────────────────────────────────────────

    private fun loadGrades() {
        progressBar.visibility = View.VISIBLE
        db.collection("grades").get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                gradeList.clear()
                for (doc in result) {
                    gradeList.add(mapOf("id" to doc.id, "name" to (doc.getString("name") ?: "")))
                }
                setupGradeSpinner()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load grades", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupGradeSpinner() {
        val names = listOf("-- Select Grade --") + gradeList.map { it["name"]!! }
        spinnerGrade.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinnerGrade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (pos == 0) return
                spinnerField.adapter = ArrayAdapter(
                    this@AcademicTestActivity,
                    android.R.layout.simple_spinner_item,
                    listOf("-- Select Field --")
                )
                questionsContainer.removeAllViews()
                btnSubmit.visibility = View.GONE
                loadFields(gradeList[pos - 1]["id"]!!)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // ── FIELDS ───────────────────────────────────────────────────────────────

    private fun loadFields(gradeId: String) {
        progressBar.visibility = View.VISIBLE
        db.collection("fields")
            .whereEqualTo("gradeId", gradeId)
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                fieldList.clear()
                for (doc in result) {
                    fieldList.add(mapOf("id" to doc.id, "name" to (doc.getString("name") ?: "")))
                }
                setupFieldSpinner()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load fields", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupFieldSpinner() {
        val names = listOf("-- Select Field --") + fieldList.map { it["name"]!! }
        spinnerField.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinnerField.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (pos == 0) return
                loadQuestions(fieldList[pos - 1]["id"]!!)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // ── QUESTIONS ─────────────────────────────────────────────────────────────

    private fun loadQuestions(fieldId: String) {
        progressBar.visibility = View.VISIBLE
        questionsContainer.removeAllViews()
        btnSubmit.visibility = View.GONE

        db.collection("academic_questions")
            .whereEqualTo("fieldId", fieldId)
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                questionList.clear()
                radioGroups.clear()

                for (doc in result) {
                    // options array (new) ya optionA/B/C/D (old) dono handle
                    val options: List<Any> = when {
                        doc.get("options") != null -> {
                            @Suppress("UNCHECKED_CAST")
                            doc.get("options") as? List<Any> ?: emptyList()
                        }
                        doc.get("optionA") != null -> {
                            listOfNotNull(
                                doc.get("optionA"),
                                doc.get("optionB"),
                                doc.get("optionC"),
                                doc.get("optionD")
                            )
                        }
                        else -> emptyList()
                    }

                    questionList.add(
                        mapOf(
                            "question" to (doc.getString("question") ?: ""),
                            "options"  to options
                        )
                    )
                }

                if (questionList.isEmpty()) {
                    Toast.makeText(this, "No questions found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                questionList.shuffle()   // Questions randomize
                renderQuestions()
                btnSubmit.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load questions", Toast.LENGTH_SHORT).show()
            }
    }

    private fun renderQuestions() {
        questionsContainer.removeAllViews()
        radioGroups.clear()

        questionList.forEachIndexed { index, q ->
            val view = LayoutInflater.from(this)
                .inflate(R.layout.academic_questions, questionsContainer, false)

            val tv = view.findViewById<TextView>(R.id.tvQuestion)
            val rg = view.findViewById<RadioGroup>(R.id.radioGroup)

            tv.text = "${index + 1}. ${q["question"]}"

            @Suppress("UNCHECKED_CAST")
            val rawOptions = (q["options"] as? List<Any>) ?: emptyList()

            // KEY FIX: parse (string ya map) + shuffle
            val parsedOptions = rawOptions
                .map { parseOption(it) }
                .toMutableList()
                .also { it.shuffle() }   // Options randomize

            rg.removeAllViews()
            parsedOptions.forEach { (text, isCorrect) ->
                val rb = RadioButton(this)
                rb.text = text
                rb.tag  = isCorrect   // scoring ke liye tag mein store
                rg.addView(rb)
            }

            radioGroups.add(rg)
            questionsContainer.addView(view)
        }
    }

    // ── SUBMIT ────────────────────────────────────────────────────────────────

    private fun submitTest() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val unanswered = radioGroups.indexOfFirst { it.checkedRadioButtonId == -1 }
        if (unanswered != -1) {
            Toast.makeText(this, "Please answer question ${unanswered + 1}", Toast.LENGTH_SHORT).show()
            return
        }

        var score = 0
        radioGroups.forEach { rg ->
            val selected = rg.findViewById<RadioButton>(rg.checkedRadioButtonId)
            if (selected?.tag as? Boolean == true) score++
        }

        val total      = questionList.size
        val percentage = if (total > 0) (score * 100) / total else 0

        val gradeName = gradeList.getOrNull(spinnerGrade.selectedItemPosition - 1)?.get("name") ?: ""
        val fieldName = fieldList.getOrNull(spinnerField.selectedItemPosition - 1)?.get("name") ?: ""

        val data = hashMapOf(
            "grade"      to gradeName,
            "field"      to fieldName,
            "score"      to score,
            "total"      to total,
            "percentage" to percentage
        )

        btnSubmit.isEnabled    = false
        btnSubmit.text         = "Saving..."
        progressBar.visibility = View.VISIBLE

        db.collection("users").document(userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                startActivity(Intent(this, ResumeUpload::class.java))
                finish()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled = true
                btnSubmit.text      = "Submit"
                Toast.makeText(this, "Error saving result. Try again.", Toast.LENGTH_SHORT).show()
            }
    }
}
