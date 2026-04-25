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

class AcademicTestActivity : AppCompatActivity() {

    private lateinit var spinnerGrade: Spinner
    private lateinit var spinnerField: Spinner
    private lateinit var questionsContainer: LinearLayout
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val gradeList = mutableListOf<Map<String, String>>()
    private val fieldList = mutableListOf<Map<String, String>>()
    private val questionList = mutableListOf<Map<String, String>>()
    private val radioGroups = mutableListOf<RadioGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_academic_screen)

        spinnerGrade = findViewById(R.id.spinnerGrade)
        spinnerField = findViewById(R.id.spinnerField)
        questionsContainer = findViewById(R.id.questionsContainer)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)

        loadGrades()

        btnSubmit.setOnClickListener { submitTest() }
    }

    private fun loadGrades() {
        db.collection("grades").get()
            .addOnSuccessListener { result ->
                gradeList.clear()
                for (doc in result) {
                    gradeList.add(
                        mapOf(
                            "id" to doc.id,
                            "name" to (doc.getString("name") ?: "")
                        )
                    )
                }
                setupGradeSpinner()
            }
    }

    private fun setupGradeSpinner() {
        val names = listOf("-- Select Grade --") + gradeList.map { it["name"]!! }
        spinnerGrade.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, names)

        spinnerGrade.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    if (pos == 0) return
                    loadFields(gradeList[pos - 1]["id"]!!)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun loadFields(gradeId: String) {
        db.collection("fields")
            .whereEqualTo("gradeId", gradeId)
            .get()
            .addOnSuccessListener { result ->
                fieldList.clear()
                for (doc in result) {
                    fieldList.add(
                        mapOf(
                            "id" to doc.id,
                            "name" to (doc.getString("name") ?: "")
                        )
                    )
                }
                setupFieldSpinner()
            }
    }

    private fun setupFieldSpinner() {
        val names = listOf("-- Select Field --") + fieldList.map { it["name"]!! }
        spinnerField.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, names)

        spinnerField.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    if (pos == 0) return
                    loadQuestions(fieldList[pos - 1]["id"]!!)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun loadQuestions(fieldId: String) {

        db.collection("academic_questions")
            .whereEqualTo("fieldId", fieldId)
            .get()
            .addOnSuccessListener { result ->

                questionList.clear()
                radioGroups.clear()
                questionsContainer.removeAllViews()

                for (doc in result) {
                    questionList.add(
                        mapOf(
                            "question" to (doc.getString("question") ?: ""),
                            "optionA" to (doc.getString("optionA") ?: ""),
                            "optionB" to (doc.getString("optionB") ?: ""),
                            "optionC" to (doc.getString("optionC") ?: ""),
                            "optionD" to (doc.getString("optionD") ?: ""),
                            "correct" to (doc.getString("correct") ?: "")
                        )
                    )
                }

                renderQuestions()
                btnSubmit.visibility = View.VISIBLE
            }
    }

    private fun renderQuestions() {

        questionList.forEachIndexed { index, q ->

            val view = LayoutInflater.from(this)
                .inflate(R.layout.academic_questions, questionsContainer, false)

            val tv = view.findViewById<TextView>(R.id.tvQuestion)
            val rg = view.findViewById<RadioGroup>(R.id.radioGroup)

            view.findViewById<RadioButton>(R.id.rbOptionA).text = q["optionA"]
            view.findViewById<RadioButton>(R.id.rbOptionB).text = q["optionB"]
            view.findViewById<RadioButton>(R.id.rbOptionC).text = q["optionC"]
            view.findViewById<RadioButton>(R.id.rbOptionD).text = q["optionD"]

            tv.text = "${index + 1}. ${q["question"]}"

            radioGroups.add(rg)
            questionsContainer.addView(view)
        }
    }

    private fun submitTest() {

        val userId = auth.currentUser?.uid ?: return

        var score = 0

        questionList.forEachIndexed { index, q ->
            val selected = when (radioGroups[index].checkedRadioButtonId) {
                R.id.rbOptionA -> "a"
                R.id.rbOptionB -> "b"
                R.id.rbOptionC -> "c"
                R.id.rbOptionD -> "d"
                else -> ""
            }

            if (selected == q["correct"]) score++
        }

        val total = questionList.size
        val percentage = (score * 100) / total

        val gradeName = gradeList[spinnerGrade.selectedItemPosition - 1]["name"]
        val fieldName = fieldList[spinnerField.selectedItemPosition - 1]["name"]

        val data = hashMapOf(
            "grade" to gradeName,
            "field" to fieldName,
            "score" to score,
            "total" to total,
            "percentage" to percentage
        )

        btnSubmit.isEnabled = false
        btnSubmit.text = "Saving..."

        db.collection("users")
            .document(userId)
            .set(data, SetOptions.merge())   // ✅ FIX HERE
            .addOnSuccessListener {
                startActivity(Intent(this, ResumeUpload::class.java))
                finish()
            }
            .addOnFailureListener {
                btnSubmit.isEnabled = true
                btnSubmit.text = "Submit"
                Toast.makeText(this, "Error saving result", Toast.LENGTH_SHORT).show()
            }
    }
}