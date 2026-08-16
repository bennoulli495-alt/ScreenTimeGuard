package com.familyguard.screentime.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.familyguard.screentime.R
import com.familyguard.screentime.data.AppRepository
import com.familyguard.screentime.util.Constants

/**
 * One screen, two modes:
 *  - First-time setup: no password exists yet, so only "new password" +
 *    "confirm" are shown.
 *  - Change: a password already exists, so the current password must be
 *    entered correctly before a new one is accepted — unless this screen
 *    was reached via a successful recovery-question check, in which case
 *    that requirement is waived for this one visit.
 */
class PasswordActivity : AppCompatActivity() {

    private lateinit var repository: AppRepository

    private lateinit var textTitle: TextView
    private lateinit var textSubtitle: TextView
    private lateinit var editCurrentPassword: EditText
    private lateinit var textForgotPassword: TextView
    private lateinit var editNewPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var textError: TextView
    private lateinit var btnSave: Button

    private var skipCurrentCheck = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)

        repository = AppRepository(this)
        skipCurrentCheck = intent.getBooleanExtra(Constants.EXTRA_SKIP_CURRENT_PASSWORD_CHECK, false)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textTitle = findViewById(R.id.textPasswordScreenTitle)
        textSubtitle = findViewById(R.id.textPasswordScreenSubtitle)
        editCurrentPassword = findViewById(R.id.editCurrentPassword)
        textForgotPassword = findViewById(R.id.textForgotPassword)
        editNewPassword = findViewById(R.id.editNewPassword)
        editConfirmPassword = findViewById(R.id.editConfirmPassword)
        textError = findViewById(R.id.textPasswordError)
        btnSave = findViewById(R.id.btnSavePassword)

        val requireCurrentPassword = repository.storage.hasPasswordSet() && !skipCurrentCheck

        if (requireCurrentPassword) {
            textTitle.text = "Change admin password"
            textSubtitle.text = "Enter your current password, then choose a new one."
            editCurrentPassword.visibility = View.VISIBLE
            textForgotPassword.visibility = View.VISIBLE
        } else if (skipCurrentCheck) {
            textTitle.text = "Set a new password"
            textSubtitle.text = "Recovery verified. Choose a new admin password."
            editCurrentPassword.visibility = View.GONE
            textForgotPassword.visibility = View.GONE
        } else {
            textTitle.text = "Set admin password"
            textSubtitle.text = "This password unlocks a restricted app for the rest of the day."
            editCurrentPassword.visibility = View.GONE
            textForgotPassword.visibility = View.GONE
        }

        textForgotPassword.setOnClickListener {
            startActivity(android.content.Intent(this, RecoveryQuestionActivity::class.java))
        }

        btnSave.setOnClickListener { attemptSave(requireCurrentPassword) }
    }

    private fun attemptSave(requireCurrentPassword: Boolean) {
        textError.visibility = View.GONE

        if (requireCurrentPassword) {
            val current = editCurrentPassword.text.toString()
            if (!repository.storage.verifyPassword(current)) {
                showError("Current password is incorrect.")
                return
            }
        }

        val newPassword = editNewPassword.text.toString()
        val confirm = editConfirmPassword.text.toString()

        if (newPassword.length < 4) {
            showError("New password must be at least 4 characters.")
            return
        }
        if (newPassword != confirm) {
            showError("Passwords don't match.")
            return
        }

        repository.storage.setPassword(newPassword)
        Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showError(message: String) {
        textError.text = message
        textError.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
