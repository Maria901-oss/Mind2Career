package com.example.mind2career

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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

    // ── 32 personality classes — exact order from label_encoder.pkl ──────────
    private val PERSONALITY_LABELS = listOf(
        "Achiever", "Adventurer", "Advocate", "Balanced", "Builder",
        "Caretaker", "Charmer", "Companion", "Connector", "Creator",
        "Dreamer", "Dynamo", "Empath", "Executor", "Explorer",
        "Expressionist", "Harmonizer", "Humanist", "Idealist", "Innovator",
        "Leader", "Luminary", "Mystic", "Nurturer", "Perfectionist",
        "Performer", "Seeker", "Sensitive", "Socializer", "Striver",
        "Visionary", "Worrier"
    )

    // ── StandardScaler values from scaler.pkl (full precision) ───────────────
    // These MUST match Python training — wrong values = wrong predictions
    private val SCALER_MEAN = doubleArrayOf(
        3.661830357142857,  3.2075892857142856, 4.348214285714286,
        4.229910714285714,  2.892857142857143,  4.260044642857143,
        3.2421875,          4.193080357142857,  3.8861607142857144,
        2.7075892857142856, 4.295758928571429,  3.3794642857142856,
        3.9073660714285716, 4.012276785714286,  3.84375,
        2.580357142857143,  2.970982142857143,  3.953125,
        2.9174107142857144, 3.786830357142857,  4.301339285714286,
        2.8392857142857144, 3.8136160714285716, 3.443080357142857,
        4.087053571428571,  3.1573660714285716, 2.642857142857143,
        3.6908482142857144, 3.3292410714285716, 4.030133928571429,
        2.986607142857143,  2.7667410714285716, 3.8984375
    )

    private val SCALER_SCALE = doubleArrayOf(
        0.9650907839272432,  1.297915736820077,   0.8151448318788358,
        0.840279498075018,   1.171515676204052,   0.8675766731080896,
        1.21396510916596,    0.8084356436793846,  1.0046701177996138,
        1.0390738884215547,  0.8255300542415127,  1.125487068598139,
        0.9962605215032837,  0.884429393428948,   1.0774292527321028,
        1.1270884809443982,  1.1096399511146369,  1.0361928351025484,
        1.279948551934604,   1.0079397957892062,  0.9322103107435751,
        1.0115720741620389,  1.0896601513572912,  1.1797666887869402,
        0.8719396874547422,  1.1362588322423954,  1.1561724325884748,
        1.1316100232934665,  1.174446052899102,   0.924726301163289,
        1.3320563812414707,  1.2748638249796365,  1.0197589288059528
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    // ─────────────── CSV LOAD ───────────────────────────────────────────────
    private fun loadQuestions() {
        try {
            val inputStream = assets.open("personalityQuestion.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // skip header row

            reader.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.size >= 6) {
                    val q = tokens[0].replace("\"", "").trim()
                    val options = tokens.subList(1, 6).map { it.replace("\"", "").trim() }
                    questionList.add(questionItem(q, options))
                }
            }
            reader.close()
            Log.d("CSV", "Loaded ${questionList.size} questions")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "CSV Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ─────────────── MODEL LOAD ─────────────────────────────────────────────
    private fun loadModel() {
        try {
            interpreter = Interpreter(loadModelFile())
            val inShape  = interpreter!!.getInputTensor(0).shape()
            val outShape = interpreter!!.getOutputTensor(0).shape()
            Log.d("MODEL", "Loaded — input: ${inShape.toList()}, output: ${outShape.toList()}")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Model load failed: ${e.message}", Toast.LENGTH_LONG).show()
            interpreter = null
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fd = assets.openFd("personality_model.tflite")
        val stream = FileInputStream(fd.fileDescriptor)
        return stream.channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
    }

    // ─────────────── SUBMIT ─────────────────────────────────────────────────
    private fun handleSubmit() {
        if (questionList.isEmpty()) {
            Toast.makeText(this, "Questions not loaded", Toast.LENGTH_SHORT).show()
            return
        }

        val answers = questionList.map { it.selectedAnswer }

        // selectedAnswer must be 1–5 (not 0).
        // If your QuestionAdapter stores 0-based index (0–4), change to:
        //     if (answers.contains(-1))   and initialise selectedAnswer as -1
        if (answers.contains(0)) {
            Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("ANSWERS", "Raw answers: $answers")

        if (interpreter == null) {
            Log.w("MODEL", "Interpreter null — saving fallback Balanced")
            saveResultToFirebase("Balanced")
            return
        }

        runModel(answers)
    }

    // ─────────────── MODEL RUN ──────────────────────────────────────────────
    /**
     * WHY "Balanced" was always saved before:
     *   Old code used  answer / 5f  → all inputs became ~0.6
     *   The model saw almost identical input for every user and always
     *   picked the same class.
     *
     * Fix: apply the same StandardScaler used during Python training:
     *   z = (answer - mean) / scale
     * This shifts values to roughly [-2, +2] so the model can distinguish users.
     */
    private fun runModel(answers: List<Int>) {
        try {
            val interp = interpreter ?: return

            val inputSize  = interp.getInputTensor(0).shape().last()   // 33
            val outputSize = interp.getOutputTensor(0).shape().last()  // 32

            // ── Apply StandardScaler ──
            val input = Array(1) { FloatArray(inputSize) }
            for (i in 0 until inputSize) {
                val raw   = if (i < answers.size) answers[i].toDouble() else 3.0
                val mean  = if (i < SCALER_MEAN.size)  SCALER_MEAN[i]  else 3.0
                val scale = if (i < SCALER_SCALE.size) SCALER_SCALE[i] else 1.0
                input[0][i] = ((raw - mean) / scale).toFloat()
            }
            Log.d("MODEL", "Scaled input (first 5): ${input[0].take(5)}")

            // ── Run model ──
            val output = Array(1) { FloatArray(outputSize) }
            interp.run(input, output)

            val probs = output[0]

            // ── Log top 3 for debugging ──
            val sortedIndices = probs.indices.sortedByDescending { probs[it] }
            val top3 = sortedIndices.take(3).joinToString(" | ") {
                "${PERSONALITY_LABELS.getOrElse(it) { "?" }} ${"%.1f".format(probs[it] * 100)}%"
            }
            Log.d("MODEL", "Top 3: $top3")

            val predicted = PERSONALITY_LABELS.getOrElse(sortedIndices.first()) { "Balanced" }
            saveResultToFirebase(predicted)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MODEL", "Runtime error: ${e.message}")
            Toast.makeText(this, "Model error — using default", Toast.LENGTH_SHORT).show()
            saveResultToFirebase("Balanced")
        }
    }

    // ─────────────── FIREBASE SAVE ──────────────────────────────────────────
    private fun saveResultToFirebase(result: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            return
        }

        val data = hashMapOf("personality" to result)

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FIREBASE", "Saved personality: $result")
                Toast.makeText(this, "Saved: $result", Toast.LENGTH_SHORT).show()
                openNextScreen()
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Save failed: ${e.message}")
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ─────────────── NEXT SCREEN ────────────────────────────────────────────
    private fun openNextScreen() {
        startActivity(Intent(this, AcademicTestActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter?.close()
    }
}
