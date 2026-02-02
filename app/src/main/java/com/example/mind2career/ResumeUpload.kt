package com.example.mind2career

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class ResumeUpload : AppCompatActivity() {

    private val PICK_PDF = 301

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resume_upload)

        findViewById<Button>(R.id.uploadButton).setOnClickListener {
            openPdfPicker()
        }
    }

    private fun openPdfPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_PDF)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PDF && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                extractAndSaveResume(uri)
            }
        }
    }

    private fun extractAndSaveResume(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)
            val text = PDFTextStripper().getText(document)
            document.close()

            val extractedData = extractResumeData(text)
            saveToFirestore(extractedData)

        } catch (_: Exception) {
            // Silent fail, no Toast
        }
    }

    private fun extractResumeData(text: String): HashMap<String, Any> {
        val data = HashMap<String, Any>()

        // Extract skills dynamically: words that start with capital letters and are not common words
        val skillRegex = Regex("\\b([A-Z][a-zA-Z0-9#+]+)\\b")
        val skills = skillRegex.findAll(text)
            .map { it.value }
            .filter { it.length > 1 } // remove single letters
            .toSet()
            .toList()

        // Extract education dynamically
        val educationRegex = Regex(
            "\\b(Bachelor|Master|BS|BSc|MS|MSc|Intermediate|FSc|High School|Matric)\\b",
            RegexOption.IGNORE_CASE
        )
        val education = educationRegex.findAll(text)
            .map { it.value }
            .toSet()
            .toList()

        data["skills"] = skills
        data["education"] = education

        return data
    }

    private fun saveToFirestore(data: HashMap<String, Any>) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("resumes")
            .document(userId)
            .set(data)
    }
}
