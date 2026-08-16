package com.familyguard.screentime.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.familyguard.screentime.R
import com.familyguard.screentime.util.Constants
import com.familyguard.screentime.util.PasswordUtils

/**
 * Shown when the admin taps "Forgot password?" on PasswordActivity. The
 * question and its correct answer are fixed (not user-configurable) and the
 * answer is never stored or compared as plaintext — only its hash is.
 *
 * A correct answer forwards to PasswordActivity with a flag that lets a new
 * password be set without re-entering the (forgotten) current one.
 */
class RecoveryQuestionActivity : AppCompatActivity() {

    private lateinit var editAnswer: EditText
    private lateinit var textError: TextView
    private lateinit var btnVerify: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery_question)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<TextView>(R.id.textRecoveryQuestion).text = Constants.RECOVERY_QUESTION
        editAnswer = findViewById(R.id.editRecoveryAnswer)
        textError = findViewById(R.id.textRecoveryError)
        btnVerify = findViewById(R.id.btnVerifyAnswer)

        val attemptVerify = {
            val candidateHash = PasswordUtils.normalizedHash(editAnswer.text.toString())
            if (candidateHash == Constants.RECOVERY_ANSWER_HASH) {
                textError.visibility = View.INVISIBLE
                val intent = Intent(this, PasswordActivity::class.java).apply {
                    putExtra(Constants.EXTRA_SKIP_CURRENT_PASSWORD_CHECK, true)
                }
                startActivity(intent)
                finish()
            } else {
                textError.visibility = View.VISIBLE
                editAnswer.text.clear()
            }
        }

        btnVerify.setOnClickListener { attemptVerify() }
        editAnswer.setOnEditorActionListener { _, _, _ ->
            attemptVerify()
            true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
