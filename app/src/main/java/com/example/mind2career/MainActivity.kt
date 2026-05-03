package com.example.mind2career

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User already logged in — seedha home
            startActivity(Intent(this, onboarding4::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val getstarted=findViewById<Button>(R.id.btnGetStarted)
        getstarted.setOnClickListener {
            val intent= Intent(this, onboarding2::class.java)
            startActivity(intent)
        }
    }
}