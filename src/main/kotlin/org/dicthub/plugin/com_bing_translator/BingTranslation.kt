package org.dicthub.plugin.com_bing_translator

data class BingTranslation(
        val sourceUrl: String,
        val from: String,
        val to: String,
        val query: String,
        val queryVoice: String,
        val translation: String,
        val translationVoice: String,
        val details: List<Detail>
)

data class Detail(
        val poc: String,
        val meanings: List<Meaning>
)

data class Meaning(
        val meaning: String,
        val examples: List<String>
)