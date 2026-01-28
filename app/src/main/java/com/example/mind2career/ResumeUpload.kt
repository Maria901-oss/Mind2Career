package com.example.mind2career

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
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

            val extractedData = extractEducationSkillsAddress(text)
            saveToFirestore(extractedData)

        } catch (e: Exception) {
            Toast.makeText(this, "PDF read failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractEducationSkillsAddress(text: String): HashMap<String, Any> {
        val data = HashMap<String, Any>()

        val skillKeywords = listOf(
            "Java", "Kotlin", "Python", "C++",
            "Android", "Firebase", "SQL", "Machine Learning"
        )

        val educationKeywords = listOf(
            "BS", "BSc", "Bachelor", "MS", "MSc",
            "Intermediate", "FSc", "Matric"
        )

        val skills = skillKeywords.filter {
            text.contains(it, ignoreCase = true)
        }

        val education = educationKeywords.filter {
            text.contains(it, ignoreCase = true)
        }

        val addressRegex = Regex(
            "(House|Street|Road|Sector|Block|City|Pakistan)[^\\n]+",
            RegexOption.IGNORE_CASE
        )

        val address = addressRegex.find(text)?.value ?: "not found"

        data["skills"] = skills
        data["education"] = education
        data["address"] = address

        return data
    }

    private fun saveToFirestore(data: HashMap<String, Any>) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("resumes")
            .document(userId)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Resume data saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Database error", Toast.LENGTH_SHORT).show()
            }
    }
}
