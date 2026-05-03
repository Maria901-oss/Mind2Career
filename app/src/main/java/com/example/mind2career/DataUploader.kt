package com.example.mind2career

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

class DataUploader(private val context: Context) {

    private val db  = FirebaseFirestore.getInstance()
    private val TAG = "DataUploader"

    fun uploadAll(onComplete: (Boolean, String) -> Unit) {
        uploadGrades { gOk ->
            if (!gOk) { onComplete(false, "Grades failed"); return@uploadGrades }
            uploadFields { fOk ->
                if (!fOk) { onComplete(false, "Fields failed"); return@uploadFields }
                uploadQuestions { qOk ->
                    onComplete(qOk, if (qOk) "All uploaded successfully" else "Questions failed")
                }
            }
        }
    }

    // ── GRADES ───────────────────────────────────────────────────────────────

    private fun uploadGrades(onDone: (Boolean) -> Unit) {
        val grades = listOf(
            "matric"       to "Matric",
            "intermediate" to "Intermediate",
            // FIX 1: Dataset mein key "bachelors" hai, "undergraduate" nahi
            "bachelors"    to "Bachelors"
        )

        val batch = db.batch()
        grades.forEach { (id, name) ->
            batch.set(db.collection("grades").document(id), mapOf("name" to name))
        }

        batch.commit()
            .addOnSuccessListener { Log.d(TAG, "Grades uploaded"); onDone(true) }
            .addOnFailureListener { Log.e(TAG, "Grades failed: ${it.message}"); onDone(false) }
    }

    // ── FIELDS ───────────────────────────────────────────────────────────────

    private fun uploadFields(onDone: (Boolean) -> Unit) {

        // FIX 2: gradeId "bachelors" hona chahiye "undergraduate" nahi
        //        aur bachelor fields ka case bhi exact match karo (SE, ITC uppercase)
        val fields = listOf(
            // Matric
            Triple("matric_physics",   "matric", "Physics"),
            Triple("matric_chemistry", "matric", "Chemistry"),
            Triple("matric_english",   "matric", "English"),
            Triple("matric_biology",   "matric", "Biology"),
            Triple("matric_computer",  "matric", "Computer Science"),
            Triple("matric_maths",     "matric", "Maths"),
            // Intermediate
            Triple("intermediate_physics",   "intermediate", "Physics"),
            Triple("intermediate_chemistry", "intermediate", "Chemistry"),
            Triple("intermediate_biology",   "intermediate", "Biology"),
            Triple("intermediate_maths",     "intermediate", "Maths"),
            Triple("intermediate_computer",  "intermediate", "Computer Science"),
            // Bachelors — field IDs dataset keys se exactly match karein
            Triple("bachelors_cn",  "bachelors", "Computer Networks"),
            Triple("bachelors_os",  "bachelors", "Operating Systems"),
            Triple("bachelors_dsa", "bachelors", "Data Structures & Algorithms"),
            Triple("bachelors_SE",  "bachelors", "Software Engineering"),
            Triple("bachelors_ITC", "bachelors", "Introduction to Computing")
        )

        val batch = db.batch()
        fields.forEach { (id, gradeId, name) ->
            batch.set(
                db.collection("fields").document(id),
                mapOf("gradeId" to gradeId, "name" to name)
            )
        }

        batch.commit()
            .addOnSuccessListener { Log.d(TAG, "Fields uploaded"); onDone(true) }
            .addOnFailureListener { Log.e(TAG, "Fields failed: ${it.message}"); onDone(false) }
    }

    // ── QUESTIONS ─────────────────────────────────────────────────────────────

    private fun uploadQuestions(onDone: (Boolean) -> Unit) {

        val json = try {
            context.assets.open("Academic_Dataset.json")
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Asset load failed: ${e.message}"); onDone(false); return
        }

        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed: ${e.message}"); onDone(false); return
        }

        val all = mutableListOf<Map<String, Any>>()

        root.keys().forEach { gradeKey ->
            val gradeObj = root.optJSONObject(gradeKey) ?: return@forEach

            gradeObj.keys().forEach { fieldKey ->
                val questionsArr = gradeObj.optJSONArray(fieldKey) ?: return@forEach

                // FIX 3: fieldId exact dataset key se match karo (case-sensitive: SE, ITC)
                val fieldId = "${gradeKey}_${fieldKey}"

                for (i in 0 until questionsArr.length()) {
                    val q = questionsArr.optJSONObject(i) ?: continue

                    val question = q.optString("question", "").trim()
                    if (question.isEmpty()) continue

                    // FIX 4: Dataset mein options [{text, isCorrect}] format mein hain
                    //         "answer" field nahi hai — isCorrect flag se correct nikalo
                    val optionsArr: JSONArray = q.optJSONArray("options") ?: continue
                    val parsedOptions = mutableListOf<Map<String, Any>>()

                    for (j in 0 until optionsArr.length()) {
                        val opt = optionsArr.optJSONObject(j) ?: continue
                        parsedOptions.add(
                            mapOf(
                                "text"      to opt.optString("text", ""),
                                "isCorrect" to opt.optBoolean("isCorrect", false)
                            )
                        )
                    }

                    if (parsedOptions.isEmpty()) continue

                    all.add(
                        mapOf(
                            "question" to question,
                            "fieldId"  to fieldId,
                            "options"  to parsedOptions   // proper maps, not strings
                        )
                    )
                }
            }
        }

        Log.d(TAG, "Total questions parsed: ${all.size}")

        if (all.isEmpty()) {
            Log.e(TAG, "No questions found — check asset file name and format")
            onDone(false)
            return
        }

        uploadChunks(all, 0, onDone)
    }

    // ── CHUNKED UPLOAD ────────────────────────────────────────────────────────

    private fun uploadChunks(
        list: List<Map<String, Any>>,
        start: Int,
        onDone: (Boolean) -> Unit
    ) {
        if (start >= list.size) {
            Log.d(TAG, "All ${list.size} questions uploaded successfully")
            onDone(true)
            return
        }

        val end   = minOf(start + 499, list.size)   // 499 safe limit (Firestore max = 500)
        val batch = db.batch()

        for (i in start until end) {
            batch.set(db.collection("academic_questions").document(), list[i])
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Chunk uploaded: $start → $end")
                uploadChunks(list, end, onDone)
            }
            .addOnFailureListener {
                Log.e(TAG, "Chunk failed ($start→$end): ${it.message}")
                onDone(false)
            }
    }
}