package com.fitsum.aikeyboard

/**
 * GeEzTransliterationEngine
 * Production-ready offline transliteration engine for Amharic / Ge'ez script.
 * Converts Latin phonetic input (e.g., "selam") into Ge'ez script ("ሰላም").
 *
 * Runs 100% locally with zero external network API calls.
 */
class GeEzTransliterationEngine {

    data class OrderFamily(
        val consonant: String,
        val latinBase: String,
        val orders: Array<String>, // 1st to 7th order
        val labialized: Array<String> = emptyArray()
    )

    data class TransliterationResult(
        val geezText: String,
        val primaryCandidate: String,
        val candidates: List<String>,
        val englishTranslations: List<String> = emptyList(),
        val activeOrderFamily: Array<String>? = null
    )

    companion object {
        // Complete 33 Fidel Consonant Families in Ge'ez
        val FIDEL_MATRIX = listOf(
            OrderFamily("ሀ", "h", arrayOf("ሀ", "ሁ", "ሂ", "ሃ", "ሄ", "ህ", "ሆ"), arrayOf("ኋ")),
            OrderFamily("ለ", "l", arrayOf("ለ", "ሉ", "ሊ", "ላ", "ሌ", "ል", "ሎ"), arrayOf("ሏ")),
            OrderFamily("ሐ", "hh", arrayOf("ሐ", "ሑ", "ሒ", "ሓ", "ሔ", "ሕ", "ሖ"), arrayOf("ሗ")),
            OrderFamily("መ", "m", arrayOf("መ", "ሙ", "ሚ", "ማ", "ሜ", "ም", "ሞ"), arrayOf("ሟ")),
            OrderFamily("ሠ", "ss", arrayOf("ሠ", "ሡ", "ሢ", "ሣ", "ሤ", "ሥ", "ሦ"), arrayOf("ሧ")),
            OrderFamily("ረ", "r", arrayOf("ረ", "ሩ", "ሪ", "ራ", "ሬ", "ር", "ሮ"), arrayOf("ሯ")),
            OrderFamily("ሰ", "s", arrayOf("ሰ", "ሱ", "ሲ", "ሳ", "ሴ", "ስ", "ሶ"), arrayOf("ሷ")),
            OrderFamily("ሸ", "sh", arrayOf("ሸ", "ሹ", "ሺ", "ሻ", "ሼ", "ሽ", "ሾ"), arrayOf("ሿ")),
            OrderFamily("ቀ", "q", arrayOf("ቀ", "ቁ", "ቂ", "ቃ", "ቄ", "ቅ", "ቆ"), arrayOf("ቋ", "ቈ", "ቊ", "ቌ", "ቍ")),
            OrderFamily("በ", "b", arrayOf("በ", "ቡ", "ቢ", "ባ", "ቤ", "ብ", "ቦ"), arrayOf("ቧ")),
            OrderFamily("ቨ", "v", arrayOf("ቨ", "ቩ", "ቪ", "ቫ", "ቬ", "ቭ", "ቮ"), arrayOf("ቯ")),
            OrderFamily("ተ", "t", arrayOf("ተ", "ቱ", "ቲ", "ታ", "ቴ", "ት", "ቶ"), arrayOf("ቷ")),
            OrderFamily("ቸ", "ch", arrayOf("ቸ", "ቹ", "ቺ", "ቻ", "ቼ", "ች", "ቾ"), arrayOf("ቿ")),
            OrderFamily("ኀ", "hu", arrayOf("ኀ", "ኁ", "ኂ", "ኃ", "ኄ", "ኅ", "ኆ"), arrayOf("ኋ")),
            OrderFamily("ነ", "n", arrayOf("ነ", "ኑ", "ኒ", "ና", "ኔ", "ን", "ኖ"), arrayOf("ኗ")),
            OrderFamily("ኘ", "gn", arrayOf("ኘ", "ኙ", "ኚ", "ኛ", "ጜ", "ኝ", "ኞ"), arrayOf("፟")),
            OrderFamily("አ", "a", arrayOf("አ", "ኡ", "ኢ", "ኣ", "ኤ", "እ", "ኦ")),
            OrderFamily("ከ", "k", arrayOf("ከ", "ኩ", "ኪ", "ካ", "ኬ", "ክ", "ኮ"), arrayOf("ኳ", "ኰ", "ኲ", "ኴ", "ኵ")),
            OrderFamily("ኸ", "kh", arrayOf("ኸ", "ኹ", "ኺ", "ኻ", "ኼ", "ኽ", "ኾ"), arrayOf("ዃ")),
            OrderFamily("ወ", "w", arrayOf("ወ", "ዉ", "ዊ", "ዋ", "ዌ", "ው", "ዎ")),
            OrderFamily("ዐ", "aa", arrayOf("ዐ", "ዑ", "ዒ", "ዓ", "ዔ", "ዕ", "ዖ")),
            OrderFamily("ዘ", "z", arrayOf("ዘ", "ዙ", "ዚ", "ዛ", "ዜ", "ዝ", "ዞ"), arrayOf("ዟ")),
            OrderFamily("ዥ", "zh", arrayOf("ዥ", "ዡ", "ዢ", "ዣ", "ዤ", "ዥ", "ዦ"), arrayOf("ዧ")),
            OrderFamily("የ", "y", arrayOf("የ", "ዩ", "ዪ", "ያ", "ዬ", "ይ", "ዮ")),
            OrderFamily("ደ", "d", arrayOf("ደ", "ዱ", "ዲ", "ዳ", "ዴ", "ድ", "ዶ"), arrayOf("ዷ")),
            OrderFamily("ጀ", "j", arrayOf("ጀ", "ጁ", "ጂ", "ጃ", "ጄ", "ጅ", "ጆ"), arrayOf("ጇ")),
            OrderFamily("ገ", "g", arrayOf("ገ", "ጉ", "ጊ", "ጋ", "ጌ", "ግ", "ጎ"), arrayOf("ጓ", "ጐ", "ጒ", "ጔ", "ጕ")),
            OrderFamily("ጠ", "T", arrayOf("ጠ", "ጡ", "ጢ", "ጣ", "ጤ", "ጥ", "ጦ"), arrayOf("ጧ")),
            OrderFamily("ጨ", "CH", arrayOf("ጨ", "ጩ", "ጪ", "ጫ", "ጬ", "ጭ", "ጮ"), arrayOf("ጯ")),
            OrderFamily("ጰ", "P", arrayOf("ጰ", "ጱ", "ጲ", "ጳ", "ጴ", "ጵ", "ጶ"), arrayOf("ጷ")),
            OrderFamily("ጸ", "ts", arrayOf("ጸ", "ጹ", "ጺ", "ጻ", "ጼ", "ጽ", "ጾ"), arrayOf("ጿ")),
            OrderFamily("ፀ", "tz", arrayOf("ፀ", "ፁ", "ፁ", "ፃ", "ፄ", "ፅ", "ፆ")),
            OrderFamily("ፈ", "f", arrayOf("ፈ", "ፉ", "ፊ", "ፋ", "ፌ", "ፍ", "ፎ"), arrayOf("ፏ")),
            OrderFamily("ፐ", "p", arrayOf("ፐ", "ፑ", "ፒ", "ፓ", "ፔ", "ፕ", "ፖ"), arrayOf("ፗ"))
        )

        private val CONSONANT_MAP = FIDEL_MATRIX.associateBy { it.latinBase }

        private val ALIAS_MAP = mapOf(
            "sh" to "sh", "ch" to "ch", "gn" to "gn", "ny" to "gn",
            "ts" to "ts", "tz" to "tz", "kh" to "kh", "zh" to "zh",
            "hh" to "hh", "ss" to "ss", "hu" to "hu", "aa" to "aa",
            "C" to "CH", "T" to "T", "P" to "P", "S" to "ts", "Z" to "zh"
        )

        private val COMMON_DICTIONARY = mapOf(
            "selam" to listOf("ሰላም", "ሰላሜ", "ሰላምህ", "ሰላምሽ"),
            "dehna" to listOf("ደህና", "ደኅና", "ደህነነት"),
            "ityopya" to listOf("ኢትዮጵያ", "ኢትዮጵያዊ", "ኢትዮጵያውያን"),
            "bunna" to listOf("ቡና", "ቡናማ", "ቡናችን"),
            "ameseginalew" to listOf("አመሰግናለሁ", "እናመሰግናለን"),
            "addis" to listOf("አዲስ", "አዲሱ", "አዲሷ"),
            "abeba" to listOf("አበባ", "አበባዎች")
        )

        private val ENGLISH_TRANSLATION_MAP = mapOf(
            "selam" to listOf("Hello", "Peace"),
            "dehna" to listOf("Fine", "Well"),
            "ityopya" to listOf("Ethiopia"),
            "bunna" to listOf("Coffee"),
            "ameseginalew" to listOf("Thank you"),
            "addis" to listOf("New"),
            "abeba" to listOf("Flower")
        )
    }

    /**
     * Converts Latin input buffer to Ge'ez script and candidate predictions.
     */
    fun transliterate(input: String): TransliterationResult {
        if (input.isEmpty()) {
            return TransliterationResult("", "", emptyList())
        }

        val result = StringBuilder()
        var i = 0
        var lastFamily: OrderFamily? = null

        while (i < input.length) {
            val char = input[i]

            if (!char.isLetter()) {
                result.append(char)
                i++
                lastFamily = null
                continue
            }

            // Greedy 3-character matching (e.g. "chw", "twa", "mwa")
            if (i + 2 < input.length) {
                val tri = input.substring(i, i + 3).lowercase()
                if (tri.endsWith("wa") || tri.endsWith("ua")) {
                    val baseKey = tri.substring(0, 1)
                    val resolvedKey = ALIAS_MAP[baseKey] ?: baseKey
                    val family = CONSONANT_MAP[resolvedKey]
                    if (family != null && family.labialized.isNotEmpty()) {
                        result.append(family.labialized[0])
                        i += 3
                        lastFamily = family
                        continue
                    }
                }
            }

            // Greedy 2-character matching (e.g. "sh", "ch", "gn", "ts", "kh", "zh")
            var matchedKey: String? = null
            var consumed = 0

            if (i + 1 < input.length) {
                val duo = input.substring(i, i + 2)
                val resolvedDuo = ALIAS_MAP[duo] ?: ALIAS_MAP[duo.lowercase()] ?: duo.lowercase()
                if (CONSONANT_MAP.containsKey(resolvedDuo)) {
                    matchedKey = resolvedDuo
                    consumed = 2
                }
            }

            if (matchedKey == null) {
                val single = input[i].toString()
                val resolvedSingle = ALIAS_MAP[single] ?: ALIAS_MAP[single.lowercase()] ?: single.lowercase()
                if (CONSONANT_MAP.containsKey(resolvedSingle)) {
                    matchedKey = resolvedSingle
                    consumed = 1
                }
            }

            if (matchedKey != null) {
                val family = CONSONANT_MAP[matchedKey]!!
                i += consumed

                // Evaluate next vowel for order 1..7 selection
                if (i < input.length) {
                    val nextChar = input[i].lowercaseChar()

                    // Check for labialized "wa" / "ua"
                    if ((nextChar == 'w' || nextChar == 'u') && i + 1 < input.length && input[i + 1].lowercaseChar() == 'a') {
                        if (family.labialized.isNotEmpty()) {
                            result.append(family.labialized[0])
                            i += 2
                            lastFamily = family
                            continue
                        }
                    }

                    when (nextChar) {
                        'u' -> { result.append(family.orders[1]); i++ } // 2nd order (ሱ)
                        'i' -> { result.append(family.orders[2]); i++ } // 3rd order (ሲ)
                        'a' -> { result.append(family.orders[3]); i++ } // 4th order (ሳ)
                        'e' -> {
                            if (i + 1 < input.length && (input[i + 1].lowercaseChar() == 'e' || input[i + 1].lowercaseChar() == 'y')) {
                                result.append(family.orders[4]); i += 2 // 5th order (ሴ)
                            } else {
                                result.append(family.orders[0]); i++ // 1st order (ሰ)
                            }
                        }
                        'o' -> { result.append(family.orders[6]); i++ } // 7th order (ሶ)
                        else -> { result.append(family.orders[5]) } // 6th order default (ስ)
                    }
                } else {
                    result.append(family.orders[5]) // 6th order default (ስ)
                }
                lastFamily = family
            } else {
                // Standalone vowel handling (e.g. 'a', 'e', 'i', 'u', 'o')
                val vowelFamily = CONSONANT_MAP["a"]!!
                when (char.lowercaseChar()) {
                    'e' -> result.append(vowelFamily.orders[0]) // አ
                    'u' -> result.append(vowelFamily.orders[1]) // ኡ
                    'i' -> result.append(vowelFamily.orders[2]) // ኢ
                    'a' -> result.append(vowelFamily.orders[3]) // ኣ
                    'o' -> result.append(vowelFamily.orders[6]) // ኦ
                    else -> result.append(char)
                }
                i++
                lastFamily = null
            }
        }

        val primary = result.toString()
        val candidatesList = mutableListOf<String>()
        candidatesList.add(primary)

        COMMON_DICTIONARY[input.lowercase()]?.forEach { dictWord ->
            if (!candidatesList.contains(dictWord)) {
                candidatesList.add(dictWord)
            }
        }

        if (!candidatesList.contains(input)) {
            candidatesList.add(input)
        }

        val englishList = ENGLISH_TRANSLATION_MAP[input.lowercase()] ?: emptyList()

        return TransliterationResult(
            geezText = primary,
            primaryCandidate = primary,
            candidates = candidatesList,
            englishTranslations = englishList,
            activeOrderFamily = lastFamily?.orders
        )
    }
}
