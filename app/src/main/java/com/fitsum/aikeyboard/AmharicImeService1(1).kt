package com.fitsum.aikeyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout

/**
 * AmharicImeService
 * Custom Android Keyboard Service that intercepts typed Latin keys,
 * computes Ge'ez script via GeEzTransliterationEngine, and updates
 * the active InputConnection via setComposingText & commitText.
 */
class AmharicImeService : InputMethodService(),
    CandidateViewManager.CandidateClickListener,
    KeyboardView.OnKeyboardActionListener {

    private lateinit var transliterationEngine: GeEzTransliterationEngine
    private var candidateViewManager: CandidateViewManager? = null

    private lateinit var keyboardView: KeyboardView
    private lateinit var keyboard: Keyboard

    // Internal state buffers
    private val wordBuffer = StringBuilder()
    private var isGeezMode = true
    private var autoSpaceOnCommit = true

    override fun onCreate() {
        super.onCreate()
        transliterationEngine = GeEzTransliterationEngine()
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        keyboard = Keyboard(this, R.xml.qwerty_keyboard)
        view.keyboard = keyboard
        view.setOnKeyboardActionListener(this)
        keyboardView = view
        return view
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

    // ---------------------------------------------------------------
    // KeyboardView.OnKeyboardActionListener implementation
    // ---------------------------------------------------------------

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            keyboardView.isShifted = !keyboardView.isShifted
            return
        }

        var code = primaryCode
        if (code in 97..122 && keyboardView.isShifted) {
            code -= 32 // convert to uppercase, e.g. 't' -> 'T' for special families
        }

        onKeyTyped(code)

        // Non-sticky shift: revert after one character, like standard mobile keyboards
        if (keyboardView.isShifted) {
            keyboardView.isShifted = false
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    // ---------------------------------------------------------------
    // Core typing logic
    // ---------------------------------------------------------------

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
            candidateViewManager?.updateCandidates(emptyList(), null)
            return
        }

        val result = transliterationEngine.transliterate(wordBuffer.toString())

        // setComposingText renders an underlined preview inside the active text field
        ic.setComposingText(result.geezText, 1)

        // Update candidate bar above keyboard
        candidateViewManager?.updateCandidates(result.candidates, result.activeOrderFamily)
    }

    /**
     * Handles backspace key press.
     */
    private fun handleBackspace(ic: InputConnection) {
        if (wordBuffer.isNotEmpty()) {
            wordBuffer.deleteCharAt(wordBuffer.length - 1)
            updateComposingText(ic)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    /**
     * Handles space key press.
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
        candidateViewManager?.updateCandidates(emptyList(), null)
    }

    private fun toggleTransliterationMode() {
        currentInputConnection?.let { commitCurrentWordBuffer(it) }
        isGeezMode = !isGeezMode
    }

    companion object {
        const val KEYCODE_DELETE = -5
        const val KEYCODE_SPACE = 32
        const val KEYCODE_ENTER = 10
        const val KEYCODE_MODE_SWITCH = -2
    }
}
