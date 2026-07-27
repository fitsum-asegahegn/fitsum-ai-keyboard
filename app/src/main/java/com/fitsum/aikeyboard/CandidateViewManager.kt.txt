package com.ethiopia.keyboard.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ethiopia.keyboard.R

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
            val itemView = LayoutInflater.from(container.context).inflate(
                R.layout.item_candidate, container, false
            ) as TextView

            itemView.text = candidate
            if (index == 0) {
                itemView.setTypeface(itemView.typeface, android.graphics.Typeface.BOLD)
                itemView.textSize = 18f
            }

            itemView.setOnClickListener {
                listener.onCandidateSelected(candidate)
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
