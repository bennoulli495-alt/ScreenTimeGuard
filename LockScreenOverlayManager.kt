package com.familyguard.screentime.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.familyguard.screentime.R
import com.familyguard.screentime.data.AppRepository

/**
 * Owns the lifecycle of the full-screen SYSTEM_ALERT_WINDOW overlay used to
 * block a restricted app. One instance lives inside AppMonitorService.
 */
class LockScreenOverlayManager(
    private val context: Context,
    private val repository: AppRepository,
    private val onUnlocked: () -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    val isShowing: Boolean
        get() = overlayView != null

    fun show() {
        if (overlayView != null) return // already showing

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.overlay_lock_screen, null)

        val editPassword = view.findViewById<EditText>(R.id.editOverlayPassword)
        val textError = view.findViewById<TextView>(R.id.textOverlayError)
        val btnUnlock = view.findViewById<Button>(R.id.btnOverlayUnlock)

        val attemptUnlock = {
            val candidate = editPassword.text.toString()
            if (repository.unlockSessionWithPassword(candidate)) {
                textError.visibility = View.INVISIBLE
                hide()
                onUnlocked()
            } else {
                textError.visibility = View.VISIBLE
                editPassword.text.clear()
            }
        }

        btnUnlock.setOnClickListener { attemptUnlock() }
        editPassword.setOnEditorActionListener { _, _, _ ->
            attemptUnlock()
            true
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Deliberately focusable (i.e. FLAG_NOT_FOCUSABLE is NOT set) so the
        // password EditText can receive keyboard input.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )

        windowManager.addView(view, params)
        overlayView = view

        // Auto-focus the password field and raise the keyboard.
        editPassword.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editPassword, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hide() {
        val view = overlayView ?: return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        windowManager.removeView(view)
        overlayView = null
    }
}
