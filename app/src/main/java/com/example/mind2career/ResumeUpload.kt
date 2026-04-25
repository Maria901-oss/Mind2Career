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
import com.google.firebase.firestore.SetOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class ResumeUpload : AppCompatActivity() {

    private val PICK_PDF = 301
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // ALL SKILLS COMBINED
    private val allSkillsList = listOf(
        "python","java","kotlin","swift","javascript","typescript",
        "c++","c#","php","ruby","golang","rust","scala","dart",
        "html","css","react","angular","vue","nodejs","django","flask",
        "laravel","spring boot","android development","flutter",
        "machine learning","data science","artificial intelligence",
        "tensorflow","pytorch","firebase","sql","mongodb",
        "aws","azure","docker","kubernetes","linux",
        "cybersecurity","ethical hacking","networking","ccna",
        "git","github","figma","canva","photoshop",
        "leadership","communication","teamwork","problem solving",
        "project management","digital marketing","seo","content writing",
        "accounting","finance","teaching","research"
    )

    // REMOVE THESE FROM SKILLS
    private val ignoreWords = listOf(
        "english","urdu","arabic","chinese","french",
        "german","spanish","ielts","toefl"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resume_upload)

        PDFBoxResourceLoader.init(applicationContext)

        findViewById<Button>(R.id.uploadButton).setOnClickListener {
            openPdfPicker()
        }
    }

    private fun openPdfPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        startActivityForResult(intent, PICK_PDF)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PDF && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            extractAndSave(uri)
        }
    }

    private fun extractAndSave(uri: Uri) {

        try {
            val btn = findViewById<Button>(R.id.uploadButton)
            btn.isEnabled = false
            btn.text = "Processing..."

            val inputStream = contentResolver.openInputStream(uri)
                ?: throw Exception("Cannot open file")

            val document = PDDocument.load(inputStream)
            val text = PDFTextStripper().getText(document)
            document.close()

            if (text.isBlank()) {
                Toast.makeText(this, "PDF empty or scanned", Toast.LENGTH_LONG).show()
                resetButton()
                return
            }

            val skills = extractSkills(text)

            saveToFirestore(skills)

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            resetButton()
        }
    }

    private fun extractSkills(text: String): List<String> {

        val lowerText = text.lowercase()

        return allSkillsList
            .filter { skill ->
                lowerText.contains(skill) &&
                        !ignoreWords.contains(skill)
            }
            .map { skill ->
                skill.split(" ").joinToString(" ") {
                    it.replaceFirstChar { ch -> ch.uppercase() }
                }
            }
            .distinct()   // remove duplicates
            .sorted()
    }

    private fun saveToFirestore(skills: List<String>) {

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            resetButton()
            return
        }

        firestore.collection("users")
            .document(userId)
            .set(
                mapOf("skills" to skills),
                SetOptions.merge()
            )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "${skills.size} skills saved",
                    Toast.LENGTH_SHORT
                ).show()

                // MOVE TO NEXT SCREEN
                startActivity(Intent(this, AfterAssesment::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
                resetButton()
            }
    }

    private fun resetButton() {
        val btn = findViewById<Button>(R.id.uploadButton)
        btn.isEnabled = true
        btn.text = "Upload Resume"
    }
}