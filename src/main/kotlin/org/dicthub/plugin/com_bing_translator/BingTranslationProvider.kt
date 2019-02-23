package org.dicthub.plugin.com_bing_translator

import org.dicthub.plugin.shared.util.*
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json


const val ID = "plugin-com-bing-translator"

const val BASE_URL = "https://www.bing.com/translator"

class BingTranslationProvider constructor(
        private val httpClient: HttpAsyncClient,
        private val renderer: BingTranslationRenderer)
    : TranslationProvider {

    override fun id() = ID

    override fun meta() = createMeta(
            name = "Bing microsoft translator",
            description = "Bing multiple language translation",
            source = "Bing Microsoft Translator",
            sourceUrl = "https://www.bing.com/translator",
            author = "DictHub",
            authorUrl = "https://github.com/willings/DictHub"
    )

    override fun canTranslate(query: Query): Boolean {

        // Skip checking supported lang
        return langCodeMap[query.getFrom()] is String && langCodeMap[query.getTo()] is String
    }

    override fun translate(query: Query): Promise<String> {

        return Promise { resolve, _ ->
            newTokenPromise(httpClient).then { token ->
                getQuickTranslation(query)(token).map { parseQuickTranslation(it) }.then { quickTranslation ->
                    getDetailTranslation(query)(token).map { parseDetailTranslation(it) }.then { details ->
                        val t = BingTranslation(
                                from = bingLangCode(query.getFrom()),
                                to = bingLangCode(query.getTo()),
                                query = query.getText(),
                                queryVoice = voiceUrl(token, query.getText(), bingLangCode(query.getFrom())),
                                translation = quickTranslation,
                                translationVoice = voiceUrl(token, quickTranslation, bingLangCode(query.getTo())),
                                details = details
                        )
                        resolve(renderer.render(t))
                    }.catch {
                        resolve(renderFailure(id(), sourceUrl(query), query, it))
                    }
                }.catch {
                    resolve(renderFailure(id(), sourceUrl(query), query, it))
                }
            }.catch {
                resolve(renderFailure(id(), sourceUrl(query), query, it))
            }
        }
    }

    private fun voiceUrl(token: String, text: String, lang: String) =
            "https://www.bing.com/tspeak?&format=audio%2Fmp3&language=${bingLangCode(lang)}&IG=$token&IID=translator.5038.1&options=female&text=${encodeURIComponent(text)}"

    private fun getQuickTranslation(query: Query) = { ig: String ->
        httpClient.post("https://www.bing.com/ttranslate?&category=&IG=$ig&IID=translator.5038.1", mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                buildFormData(query.getFrom(), query.getTo(), query.getText()))
    }

    private fun getDetailTranslation(query: Query) = { ig: String ->
        httpClient.post("https://www.bing.com/ttranslationlookup?&IG=$ig&IID=translator.5038.1", mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                buildFormData(query.getFrom(), query.getTo(), query.getText()))
    }

    private fun parseQuickTranslation(result: String) =
            JSON.parse<Json>(result)["translationResponse"].toString()

    @Suppress("UNCHECKED_CAST")
    private fun parseDetailTranslation(result: String): List<Detail> {
        val translations = JSON.parse<Json>(result)["translations"] as? Array<Json> ?: return emptyList()
        return translations.groupBy { it["posTag"] }.entries.map { pocGroup ->
            val poc = pocGroup.key as String
            val meanings = pocGroup.value.map { data ->
                val meaning = data["normalizedTarget"] as String
                val examples = data["backTranslations"]?.let { it as? Array<Json> }?.map { it["normalizedText"] as String }
                        ?: emptyList()
                Meaning(meaning = meaning, examples = examples)
            }
            Detail(poc = poc, meanings = meanings)
        }
    }

    private fun buildFormData(from: String, to: String, query: String) =
            "from=${bingLangCode(from)}&to=${bingLangCode(to)}&text=${encodeURIComponent(query)}"
}

private val langCodeMap = json(
        "af" to "af",
        "ar" to "ar",
        "bg" to "bg",
        "bn" to "bn-BD",
        "bs" to "bs-Latn",
        "ca" to "ca",
        "cs" to "cs",
        "cy" to "cy",
        "da" to "da",
        "de" to "de",
        "el" to "el",
        "en" to "en",
        "es" to "es",
        "et" to "et",
        "fa" to "fa",
        "fi" to "fi",
        "fj" to "fj",
        "fr" to "fr",
        "he" to "he",
        "hi" to "hi",
        "hr" to "hr",
        "ht" to "ht",
        "hu" to "hu",
        "id" to "id",
        "is" to "is",
        "it" to "it",
        "ja" to "ja",
        "ko" to "ko",
        "lt" to "lt",
        "lv" to "lv",
        "mg" to "mg",
        "ms" to "ms",
        "mt" to "mt",
        "nl" to "nl",
        "no" to "no",
        "pl" to "pl",
        "pt" to "pt",
        "ro" to "ro",
        "ru" to "ru",
        "sk" to "sk",
        "sl" to "sl",
        "sm" to "sm",
        "sr" to "sr-Latn",
        "sv" to "sv",
        "sw" to "sw",
        "ta" to "ta",
        "te" to "te",
        "th" to "th",
        "tl" to "fil",
        "to" to "to",
        "tr" to "tr",
        "ty" to "ty",
        "uk" to "uk",
        "ur" to "ur",
        "vi" to "vi",
        "zh-CN" to "zh-CHS",
        "zh-TW" to "zh-CHT"
)


private fun bingLangCode(langCode: String) = langCodeMap[langCode] as? String ?: langCode

private fun sourceUrl(q: Query) = "$BASE_URL/#${bingLangCode(q.getFrom())}/${bingLangCode(q.getTo())}/${q.getText()}"

external fun encodeURIComponent(str: String): String