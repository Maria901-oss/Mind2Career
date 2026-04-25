package com.example.mind2career

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class DataUploader(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "DataUploader"

    fun uploadAll(onComplete: (Boolean, String) -> Unit) {

        uploadGrades { gOk ->
            if (!gOk) {
                onComplete(false, "Grades failed")
                return@uploadGrades
            }

            uploadFields { fOk ->
                if (!fOk) {
                    onComplete(false, "Fields failed")
                    return@uploadFields
                }

                uploadQuestions { qOk ->
                    onComplete(qOk, if (qOk) "All uploaded successfully" else "Questions failed")
                }
            }
        }
    }

    // ================= GRADES =================
    private fun uploadGrades(onDone: (Boolean) -> Unit) {

        val grades = listOf(
            "matric" to "Matric",
            "intermediate" to "Intermediate",
            "undergraduate" to "Undergraduate"
        )

        val batch = db.batch()

        grades.forEach {
            val ref = db.collection("grades").document(it.first)
            batch.set(ref, mapOf("name" to it.second))
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Grades uploaded")
                onDone(true)
            }
            .addOnFailureListener {
                Log.e(TAG, "Grades failed: ${it.message}")
                onDone(false)
            }
    }

    // ================= FIELDS =================
    private fun uploadFields(onDone: (Boolean) -> Unit) {

        val subjectToGrades = mapOf(
            "physics" to listOf("matric", "intermediate"),
            "chemistry" to listOf("matric", "intermediate"),
            "english" to listOf("matric"),
            "biology" to listOf("matric", "intermediate"),
            "computer" to listOf("matric", "intermediate"),
            "maths" to listOf("matric", "intermediate"),
            "cn" to listOf("undergraduate"),
            "os" to listOf("undergraduate"),
            "dsa" to listOf("undergraduate"),
            "SE" to listOf("undergraduate"),
            "ITC" to listOf("undergraduate")
        )

        val subjectNames = mapOf(
            "cn" to "Computer Networks",
            "os" to "Operating Systems",
            "dsa" to "Data Structures",
            "SE" to "Software Engineering",
            "ITC" to "Introduction to Computing"
        )

        val batch = db.batch()

        subjectToGrades.forEach { (subject, grades) ->

            grades.forEach { grade ->

                val id = "${grade}_${subject.lowercase()}"

                val ref = db.collection("fields").document(id)

                batch.set(ref, mapOf(
                    "gradeId" to grade,
                    "name" to (subjectNames[subject] ?: subject)
                ))
            }
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Fields uploaded")
                onDone(true)
            }
            .addOnFailureListener {
                Log.e(TAG, "Fields failed: ${it.message}")
                onDone(false)
            }
    }

    // ================= QUESTIONS =================
    private fun uploadQuestions(onDone: (Boolean) -> Unit) {

        val json = try {
            context.assets.open("Academic_Dataset.json")
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "JSON load failed")
            onDone(false)
            return
        }

        val root = JSONObject(json)
        val all = mutableListOf<Map<String, Any>>()

        fun safeOptions(q: JSONObject): List<String> {
            val arr = q.optJSONArray("options")

            return if (arr != null && arr.length() >= 4) {
                listOf(
                    arr.optString(0),
                    arr.optString(1),
                    arr.optString(2),
                    arr.optString(3)
                )
            } else {
                listOf(
                    q.optString("optionA", ""),
                    q.optString("optionB", ""),
                    q.optString("optionC", ""),
                    q.optString("optionD", "")
                )
            }
        }

        fun process(gradeId: String, prefix: String, obj: JSONObject) {

            obj.keys().forEach { subject ->

                val arr = obj.optJSONArray(subject) ?: return@forEach

                for (i in 0 until arr.length()) {

                    val q = arr.optJSONObject(i) ?: continue

                    val question = q.optString("question")

                    val options = safeOptions(q)

                    if (question.isBlank()) continue

                    val correct = q.optString("answer", "a").lowercase()

                    all.add(
                        mapOf(
                            "gradeId" to gradeId,
                            "fieldId" to "${prefix}_${subject.lowercase()}",
                            "question" to question,
                            "optionA" to options[0],
                            "optionB" to options[1],
                            "optionC" to options[2],
                            "optionD" to options[3],
                            "correct" to correct
                        )
                    )
                }
            }
        }

        root.optJSONObject("matric")?.let {
            process("matric", "matric", it)
        }

        root.optJSONObject("intermediate")?.let {
            process("intermediate", "intermediate", it)
        }

        val bachKey = listOf("bachelors", "Bachelors", "undergraduate")
            .firstOrNull { root.has(it) }

        if (bachKey != null) {
            root.optJSONObject(bachKey)?.let {
                process("undergraduate", "undergraduate", it)
            }
        }

        Log.d(TAG, "Total questions: ${all.size}")

        uploadChunks(all, 0, onDone)
    }

    private fun uploadChunks(
        list: List<Map<String, Any>>,
        start: Int,
        onDone: (Boolean) -> Unit
    ) {

        if (start >= list.size) {
            onDone(true)
            return
        }

        val end = minOf(start + 500, list.size)
        val batch = db.batch()

        for (i in start until end) {
            batch.set(db.collection("academic_questions").document(), list[i])
        }

        batch.commit()
            .addOnSuccessListener {
                uploadChunks(list, end, onDone)
            }
            .addOnFailureListener {
                Log.e(TAG, "Chunk failed: ${it.message}")
                onDone(false)
            }
    }
}