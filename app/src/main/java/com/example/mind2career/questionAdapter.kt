package com.example.mind2career

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuestionAdapter(
    private val questions: List<questionItem>
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
        val radioGroup: RadioGroup = itemView.findViewById(R.id.rgOptions)
        val rb1: RadioButton = itemView.findViewById(R.id.rb1)
        val rb2: RadioButton = itemView.findViewById(R.id.rb2)
        val rb3: RadioButton = itemView.findViewById(R.id.rb3)
        val rb4: RadioButton = itemView.findViewById(R.id.rb4)
        val rb5: RadioButton = itemView.findViewById(R.id.rb5)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.personalityquestions, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val questionItem = questions[position]

        holder.tvQuestion.text = "${position + 1}. ${questionItem.question}"

        holder.rb1.text = questionItem.options[0]
        holder.rb2.text = questionItem.options[1]
        holder.rb3.text = questionItem.options[2]
        holder.rb4.text = questionItem.options[3]
        holder.rb5.text = questionItem.options[4]

        holder.radioGroup.setOnCheckedChangeListener(null)

        when (questionItem.selectedAnswer) {
            1 -> holder.rb1.isChecked = true
            2 -> holder.rb2.isChecked = true
            3 -> holder.rb3.isChecked = true
            4 -> holder.rb4.isChecked = true
            5 -> holder.rb5.isChecked = true
            else -> holder.radioGroup.clearCheck()
        }

        holder.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            questionItem.selectedAnswer = when (checkedId) {
                R.id.rb1 -> 1
                R.id.rb2 -> 2
                R.id.rb3 -> 3
                R.id.rb4 -> 4
                R.id.rb5 -> 5
                else -> 0
            }
        }
    }

    override fun getItemCount(): Int {
        return questions.size
    }
}
