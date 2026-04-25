package com.example.mind2career

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PesonalityAssesment : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private var questionList = mutableListOf<questionItem>()
    private var interpreter: Interpreter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pesonality_assesment)

        recyclerView = findViewById(R.id.recyclerQuestions)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadQuestions()
        recyclerView.adapter = QuestionAdapter(questionList)

        loadModel()

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            handleSubmit()
        }
    }

    // ================= CSV LOAD =================
    private fun loadQuestions() {
        try {
            val files = assets.list("")
            Log.d("ASSETS", files?.joinToString() ?: "No files")

            val inputStream = assets.open("personalityQuestion.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            reader.readLine()

            reader.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.size >= 6) {
                    val q = tokens[0].replace("\"", "").trim()
                    val options = tokens.subList(1, 6).map { it.replace("\"", "").trim() }
                    questionList.add(questionItem(q, options))
                }
            }

            reader.close()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "CSV Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ================= MODEL LOAD =================
    private fun loadModel() {
        try {
            interpreter = Interpreter(loadModelFile())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Model load failed", Toast.LENGTH_LONG).show()
            interpreter = null
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("personality_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel

        return channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    // ================= SUBMIT =================
    private fun handleSubmit() {

        if (questionList.isEmpty()) {
            Toast.makeText(this, "Questions not loaded", Toast.LENGTH_SHORT).show()
            return
        }

        val answers = questionList.map { it.selectedAnswer }

        if (answers.contains(0)) {
            Toast.makeText(this, "Answer all questions", Toast.LENGTH_SHORT).show()
            return
        }

        if (interpreter == null) {
            saveResultToFirebase("Balanced")
            return
        }

        runModel(answers)
    }

    // ================= MODEL RUN =================
    private fun runModel(answers: List<Int>) {

        try {
            val interpreter = this.interpreter ?: return

            val inputSize = interpreter.getInputTensor(0).shape().last()
            val input = Array(1) { FloatArray(inputSize) }

            for (i in 0 until inputSize) {
                input[0][i] = if (i < answers.size)
                    answers[i].toFloat() / 5f
                else 0f
            }

            val outputSize = interpreter.getOutputTensor(0).shape().last()
            val output = Array(1) { FloatArray(outputSize) }

            interpreter.run(input, output)

            val result = output[0]

            val traits = listOf(
                "Agreeableness",
                "Balanced",
                "Conscientiousness",
                "Extraversion",
                "Neuroticism",
                "Openness"
            )

            val index = result.indices.maxByOrNull { result[it] } ?: 0
            val predicted = traits.getOrElse(index) { "Balanced" }

            saveResultToFirebase(predicted)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Model error, using default", Toast.LENGTH_SHORT).show()
            saveResultToFirebase("Balanced")
        }
    }

    // ================= FIREBASE SAVE =================
    private fun saveResultToFirebase(result: String) {

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "personality" to result
        )

        db.collection("users").document(user.uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                openNextScreen()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ================= NEXT SCREEN =================
    private fun openNextScreen() {
        startActivity(Intent(this, AcademicTestActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter?.close()
    }
}