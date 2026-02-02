package com.example.mind2career

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PesonalityAssesment : AppCompatActivity() {

    private lateinit var questionList: List<questionItem>
    private lateinit var questionAdapter: QuestionAdapter
    private lateinit var recyclerView: RecyclerView

    private val NUM_PERSONALITY_CLASSES = 5 // set according to your model output

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pesonality_assesment)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerQuestions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        questionList = loadQuestionsFromCSV(this)
        questionAdapter = QuestionAdapter(questionList)
        recyclerView.adapter = questionAdapter

        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        btnSubmit.setOnClickListener {
            handleSubmit()
        }
    }

    private fun loadQuestionsFromCSV(context: Context): List<questionItem> {
        val questionsList = mutableListOf<questionItem>()

        val inputStream = context.assets.open("personality_questions.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))

        reader.readLine() // skip header

        reader.forEachLine { line ->
            val tokens = line.split(",")
            if (tokens.size >= 6) {
                val questionText = tokens[0].replace("\"", "").trim()
                val options = tokens.subList(1, 6).map { it.replace("\"", "").trim() }
                questionsList.add(questionItem(questionText, options))
            }
        }

        reader.close()
        return questionsList
    }

    private fun handleSubmit() {
        val answers = questionList.map { it.selectedAnswer }

        if (answers.contains(0)) {
            Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show()
            return
        }

        runModel(answers)
    }

    // Function to load model as MappedByteBuffer
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("personality_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val channel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun runModel(answers: List<Int>) {
        val input = FloatArray(answers.size) { i -> answers[i].toFloat() / 5f }

        // Load model using MappedByteBuffer
        val tfliteModel = loadModelFile()
        val interpreter = Interpreter(tfliteModel)

        val output = FloatArray(NUM_PERSONALITY_CLASSES)
        interpreter.run(input, output)

        val personalityTraits = listOf("Agreeableness", "Balanced", "Conscientiousness",
            "Extraversion", "Neuroticism", "Openness", "Hybrid_Extraversion_Agreeableness",
            "Hybrid_Extraversion_Conscientiousness", "Hybrid_Extraversion_Neuroticism",
            "Hybrid_Extraversion_Openness", "Hybrid_Agreeableness_Conscientiousness",
            "Hybrid_Agreeableness_Neuroticism", "Hybrid_Agreeableness_Openness",
            "Hybrid_Conscientiousness_Neuroticism", "Hybrid_Conscientiousness_Openness",
            "Hybrid_Neuroticism_Openness", "Hybrid_Extraversion_Agreeableness_Openness",
            "Hybrid_Extraversion_Agreeableness_Conscientiousness", "Hybrid_Extraversion_Agreeableness_Neuroticism",
            "Hybrid_Extraversion_Conscientiousness_Neuroticism", "Hybrid_Extraversion_Conscientiousness_Openness",
            "Hybrid_Extraversion_Neuroticism_Openness", "Hybrid_Agreeableness_Conscientiousness_Neuroticism",
            "Hybrid_Agreeableness_Conscientiousness_Openness", "Hybrid_Agreeableness_Neuroticism_Openness",
            "Hybrid_Conscientiousness_Neuroticism_Openness", "Hybrid_Extraversion_Agreeableness_Conscientiousness_Neuroticism",
            "Hybrid_Extraversion_Agreeableness_Conscientiousness_Openness", "Hybrid_Extraversion_Agreeableness_Neuroticism_Openness",
            "Hybrid_Extraversion_Conscientiousness_Neuroticism_Openness", "Hybrid_Agreeableness_Conscientiousness_Neuroticism_Openness",
            "Hybrid_Extraversion_Agreeableness_Conscientiousness_Neuroticism_Openness")
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: 0
        val predictedPersonality = personalityTraits[maxIndex]

        saveResultToFirebase(predictedPersonality)
    }

    private fun saveResultToFirebase(predictedPersonality: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf<String, Any>()
        data["predictedPersonality"] = predictedPersonality

        db.collection("users").document(userId)
            .set(data)
            .addOnSuccessListener {
                openNextScreen()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openNextScreen() {
        val intent = Intent(this, AcademicScreen::class.java)
        startActivity(intent)
        finish()
    }
}


