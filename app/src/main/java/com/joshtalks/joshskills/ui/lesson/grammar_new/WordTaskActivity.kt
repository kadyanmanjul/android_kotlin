package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.ArrangeTheSentenceViewBinding
import java.util.ArrayList
import java.util.Collections
import java.util.Random

class WordTaskActivity : AppCompatActivity() {

    var questionModel: QuestionModel = QuestionModel(
        "तुम एक लड़के हो और वह एक लड़की है",
        "You are a boy and she is a girl"
    )
    var words = ArrayList<String>()
    var answers = arrayListOf(
        "She eats apple",
        "He eats",
        "You are a woman",
        "You are a boy and she is a girl",
        "What happened",
        "I am a boy"
    )
    var random = Random()
    var progressBarValue = 0

    private lateinit var binding: ArrangeTheSentenceViewBinding
    var tvQuestion: TextView? = null
    var progressBar: ProgressBar? = null
    private var customWord: CustomWord? = null
    private var customLayout: CustomLayout? = null
    var context: Context = this@WordTaskActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.arrange_the_sentence_view)
        binding.handler = this
        tvQuestion = findViewById(R.id.question)
        progressBar = findViewById(R.id.task_progress_bar)
        initCustomLayout()
        initData()
    }

    private inner class TouchListener : View.OnTouchListener {
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            if (motionEvent.action == MotionEvent.ACTION_DOWN && !customLayout!!.isEmpty()) {
                customWord = view as CustomWord
                customWord!!.changeViewGroup(customLayout!!, binding.sentenceLine)
                checkChildCount()
                return true
            }
            return false
        }
    }

    private fun initData() {
        binding.checkButton!!.isEnabled = false
        progressBarValue = 0
        if (PrefManager.getIntValue("progressBarValue") != 0) {
            progressBarValue = PrefManager.getIntValue("progressBarValue")
            progressBar!!.progress = progressBarValue
        }
        tvQuestion?.setText(questionModel.question)
        randomizeCustomWords()
        checkAnswer()
    }

    private fun initCustomLayout() {
        customLayout = CustomLayout(this)
        customLayout!!.setGravity(Gravity.CENTER)
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        binding.answerContainer!!.addView(customLayout, params)
    }

    private fun checkAnswer() {
        binding.checkButton!!.setOnClickListener {
            val answer = StringBuilder()
            if (binding.checkButton!!.text == "check") {
                for (i in 0 until binding.sentenceLine!!.childCount) {
                    customWord = binding.sentenceLine!!.getChildAt(i) as CustomWord
                    answer.append(customWord?.getText().toString() + " ")
                }
                if (answer.toString() == questionModel.answer.toString() + " ") {
                    Toast.makeText(
                        this@WordTaskActivity,
                        "You Are Correct!",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressBarValue += 10
                    progressBar!!.progress = progressBarValue
                    PrefManager.put("progressBarValue", progressBarValue)
                    lockViews()
                } else {
                    Toast.makeText(
                        this@WordTaskActivity,
                        """
                            That's Not Correct. 
                            ${questionModel.answer}
                            """.trimIndent(),
                        Toast.LENGTH_SHORT
                    ).show()
                    if (progressBarValue > 10) {
                        progressBarValue -= 10
                    } else {
                        progressBarValue = 0
                    }
                    progressBar!!.progress = progressBarValue
                    PrefManager.put("progressBarValue", progressBarValue)
                    lockViews()
                }
                binding.checkButton!!.text = "continue"
                binding.checkButton!!.background = getDrawable(R.drawable.rect_with_green_bound)
                binding.checkButton!!.setTextColor(resources.getColor(R.color.white))
            } else if (binding.checkButton!!.text == "continue") {
                if (progressBarValue < 100) {
                    // TODO - Goto next task
                } else {
                    progressBarValue = 0
                    // TODO - showlessonCompleted
                    PrefManager.put("progressBarValue", progressBarValue)
                }
            }
        }
    }

    private fun checkChildCount() {
        if (binding.sentenceLine!!.childCount > 0) {
            binding.checkButton!!.background = getDrawable(R.drawable.rect_with_green_bound)
            binding.checkButton!!.setTextColor(resources.getColor(R.color.white))
            binding.checkButton!!.isEnabled = true
        } else {
            binding.checkButton!!.background = getDrawable(R.drawable.grey_rounded_bg)
            binding.checkButton!!.setTextColor(resources.getColor(R.color.grey_61))
            binding.checkButton!!.isEnabled = false
        }
    }

    private fun lockViews() {
        for (i in 0 until binding.sentenceLine!!.childCount) {
            customWord = binding.sentenceLine!!.getChildAt(i) as CustomWord
            customWord!!.setEnabled(false)
        }
        for (i in 0 until customLayout!!.getChildCount()) {
            customWord = customLayout!!.getChildAt(i) as CustomWord
            customWord!!.setEnabled(false)
        }
    }

    private fun randomizeCustomWords() {
        val wordsFromSentence: Array<String> = questionModel.answer.split(" ").toTypedArray()
        Collections.addAll(words, *wordsFromSentence)
        val sentenceWordsCount = wordsFromSentence.size

        //Declare how many words left to complete our layout
        val leftSize = 15 - sentenceWordsCount

        //Pick a random number from "leftSize" and add 2
        val leftRandom = random.nextInt(leftSize) + 2
        while (words.size - leftSize < leftRandom) {
            addArrayWords()
        }
        Collections.shuffle(words)
//        for (word in words) {
//            val customWord = CustomWord(applicationContext, word)
//            customWord.setOnTouchListener(TouchListener())
//            customLayout!!.push(customWord)
//        }
    }

    private fun addArrayWords() {
        val wordsFromAnswerArray =
            answers[random.nextInt(answers.size)].split(" ".toRegex()).toTypedArray()
        for (i in 0..1) {
            val word = wordsFromAnswerArray[random.nextInt(wordsFromAnswerArray.size)]
            if (!words.contains(word)) {
                words.add(word)
            }
        }
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Are you sure about that?")
            .setMessage("All progress in this lesson will be lost.")
            .setPositiveButton("QUIT", DialogInterface.OnClickListener { dialog, which ->
                progressBarValue = 0
                PrefManager.put("progressBarValue", progressBarValue)
                finish()
            })
            .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            .show()
    }

    override fun onStop() {
        progressBarValue = 0
        PrefManager.put("progressBarValue", progressBarValue)
        finish()
        super.onStop()
    }

    companion object {
        private const val TAG = "WordTaskActivity"
    }
}
