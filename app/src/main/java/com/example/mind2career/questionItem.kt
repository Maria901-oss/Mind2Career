package com.example.mind2career

data class questionItem( val question: String,
                         val options: List<String>,
                         var selectedAnswer: Int = 0)
