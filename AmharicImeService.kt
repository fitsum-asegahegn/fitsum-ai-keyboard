package com.ethiopia.keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import com.ethiopia.keyboard.R
import com.ethiopia.keyboard.engine.GeEzTransliterationEngine
import com.ethiopia.keyboard.ui.CandidateViewManager

/**
 * AmharicImeService
 * Custom Android Keyboard Service that intercept typed Latin keys,
 * computes Ge'ez script via GeEzTransliterationEngine, and updates
 * the active InputConnection via setComposingText & commitText.
 */
class AmharicImeService : InputMethodService(), CandidateViewManager.CandidateClickListener {

    private lateinit var transliterationEngine: GeEzTransliterationEngine
    private lateinit var candidateViewManager: CandidateViewManager
    
    // Internal state buffers
    private val wordBuffer = StringBuilder()
    private var isGeezMode = true
    private var autoSpaceOnCommit = true

    override fun onCreate() {
        super.onCreate()
        transliterationEngine = GeEzTransliterationEngine()
    }

    override fun onCreateInputView(): View {
        val keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null)
        // Set up keyboard key event listeners here (QWERTY soft layout or Ethiopian Fidel keycaps)
        setupKeyListeners(keyboardView)
        return keyboardView
    }

    override fun onCreateCandidatesView(): View {
        val candidatesLayout = layoutInflater.inflate(R.layout.candidate_bar, null) as LinearLayout
        candidateViewManager = CandidateViewManager(candidatesLayout, this)
        return candidatesLayout
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        clearComposingBuffer()
        setCandidatesViewShown(true)
    }

    /**
     * Intercepts soft or hardware key presses
     */
    fun onKeyTyped(primaryCode: Int) {
        val ic: InputConnection = currentInputConnection ?: return

        when (primaryCode) {
            KEYCODE_DELETE -> handleBackspace(ic)
            KEYCODE_SPACE -> handleSpace(ic)
            KEYCODE_ENTER -> handleEnter(ic)
            KEYCODE_MODE_SWITCH -> toggleTransliterationMode()
            else -> {
                val codeChar = primaryCode.toChar()
                if (codeChar.isLetter()) {
                    if (isGeezMode) {
                        wordBuffer.append(codeChar)
                        updateComposingText(ic)
                    } else {
                        // Direct Latin typing mode
                        ic.commitText(codeChar.toString(), 1)
                    }
                } else {
                    // Number or punctuation -> auto-commit word buffer first
                    commitCurrentWordBuffer(ic)
                    ic.commitText(codeChar.toString(), 1)
                }
            }
        }
    }

    /**
     * Updates setComposingText on active target app field
     */
    private fun updateComposingText(ic: InputConnection) {
        if (wordBuffer.isEmpty()) {
            ic.finishComposingText()
            candidateViewManager.updateCandidates(emptyList(), null)
            return
        }

        val result = transliterationEngine.transliterate(wordBuffer.toString())
        
        // setComposingText renders an underlined preview inside the active text field
        ic.setComposingText(result.geezText, 1)

        // Update candidate bar above keyboard
        candidateViewManager.updateCandidates(result.candidates, result.activeOrderFamily)
    }

    /**
     * Handles backspace key press.
     * If word buffer has content, remove last character from buffer.
     * Otherwise, pass deleteSurroundingText(1, 0) to target field.
     */
    private fun handleBackspace(ic: InputConnection) {
        if (wordBuffer.isNotEmpty()) {
            wordBuffer.deleteCharAt(wordBuffer.length - 1)
            updateComposingText(ic)
        } else {
            // No composing text -> delete character directly behind cursor in external app
            ic.deleteSurroundingText(1, 0)
        }
    }

    /**
     * Handles space key press.
     * Commits the current Ge'ez word to target field, then adds a space.
     */
    private fun handleSpace(ic: InputConnection) {
        if (wordBuffer.isNotEmpty()) {
            val result = transliterationEngine.transliterate(wordBuffer.toString())
            val textToCommit = if (autoSpaceOnCommit) "${result.primaryCandidate} " else result.primaryCandidate
            ic.commitText(textToCommit, 1)
            clearComposingBuffer()
        } else {
            ic.commitText(" ", 1)
        }
    }

    private fun handleEnter(ic: InputConnection) {
        commitCurrentWordBuffer(ic)
        ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
    }

    /**
     * Callback triggered when user taps a candidate from the suggestion bar
     */
    override fun onCandidateSelected(candidateText: String) {
        val ic: InputConnection = currentInputConnection ?: return
        val textToCommit = if (autoSpaceOnCommit) "$candidateText " else candidateText
        ic.commitText(textToCommit, 1)
        clearComposingBuffer()
    }

    /**
     * Callback triggered when user taps a 7-order syllable family key (e.g., 'ሳ')
     */
    override fun onSyllableSelected(syllable: String) {
        val ic: InputConnection = currentInputConnection ?: return
        ic.commitText(syllable, 1)
        clearComposingBuffer()
    }

    private fun commitCurrentWordBuffer(ic: InputConnection) {
        if (wordBuffer.isNotEmpty()) {
            val result = transliterationEngine.transliterate(wordBuffer.toString())
            ic.commitText(result.primaryCandidate, 1)
            clearComposingBuffer()
        }
    }

    private fun clearComposingBuffer() {
        wordBuffer.clear()
        candidateViewManager.updateCandidates(emptyList(), null)
    }

    private fun toggleTransliterationMode() {
        currentInputConnection?.let { commitCurrentWordBuffer(it) }
        isGeezMode = !isGeezMode
    }

    private fun setupKeyListeners(view: View) {
        // Wire keycap buttons to onKeyTyped(code)
    }

    companion object {
        const val KEYCODE_DELETE = -5
        const val KEYCODE_SPACE = 32
        const val KEYCODE_ENTER = 10
        const val KEYCODE_MODE_SWITCH = -2
    }
}
