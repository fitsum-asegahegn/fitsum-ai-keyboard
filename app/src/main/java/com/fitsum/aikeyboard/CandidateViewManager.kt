package com.fitsum.aikeyboard

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fitsum.aikeyboard.R

/**
 * CandidateViewManager
 * Manages rendering of transliteration candidates and 7-order syllable family popups above the IME keyboard.
 */
class CandidateViewManager(
    private val container: LinearLayout,
    private val listener: CandidateClickListener
) {

    interface CandidateClickListener {
        fun onCandidateSelected(candidateText: String)
        fun onSyllableSelected(syllable: String)
    }

    fun updateCandidates(candidates: List<String>, orderFamily: Array<String>?) {
        container.removeAllViews()

        if (candidates.isEmpty() && orderFamily == null) {
            container.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE

        // Render Primary Candidates (e.g. ሰላም, ሰላሜ, selam)
        candidates.forEachIndexed { index, candidate ->
            val itemView = TextView(container.context).apply {
                text = candidate
                textSize = if (index == 0) 18f else 16f
                setPadding(20, 10, 20, 10)
                setTextColor(if (index == 0) 0xFFFFFFFF.toInt() else 0xFF94A3B8.toInt())
                if (index == 0) {
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                setOnClickListener {
                    listener.onCandidateSelected(candidate)
                }
            }

            container.addView(itemView)
        }

        // Render 7-Order Syllable Quick Grid if a consonant was typed (e.g., ሰ ሱ ሲ ሳ ሴ ስ ሶ)
        if (orderFamily != null && orderFamily.isNotEmpty()) {
            val divider = View(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(2, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                    setMargins(12, 8, 12, 8)
                }
                setBackgroundColor(0xFF334155.toInt())
            }
            container.addView(divider)

            orderFamily.forEach { syllable ->
                val syllableBtn = TextView(container.context).apply {
                    text = syllable
                    textSize = 16f
                    setPadding(16, 8, 16, 8)
                    setTextColor(0xFF0EA5E9.toInt())
                    setOnClickListener {
                        listener.onSyllableSelected(syllable)
                    }
                }
                container.addView(syllableBtn)
            }
        }
    }
}
