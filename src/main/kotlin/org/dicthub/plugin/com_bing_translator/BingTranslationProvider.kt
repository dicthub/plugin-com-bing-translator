package org.dicthub.plugin.com_bing_translator

import org.dicthub.plugin.shared.util.*
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.browser.localStorage
import kotlin.js.*


const val ID = "plugin-com-bing-translator"


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

        val translateWithNewToken = fun(resolve: (String) -> Unit) {
            getBingContext().then { newContext ->
                console.info("Translated using new BingContext: ${newContext.domain}, ${newContext.token}")
                saveContextToCache(newContext)
                translateWithContext(newContext, query, true).then(resolve)
            }
        }

        return Promise { resolve, _ ->
            loadCachedContext()?.let { cachedContext ->
                // Try cached token first; if failed try to refresh and get a new token
                translateWithContext(cachedContext, query, false).then(resolve).catch {
                    translateWithNewToken(resolve)
                }
            } ?: run {
                translateWithNewToken(resolve)
            }
        }
    }

    private val domainStorageKey = "plugin-bing-domain"
    private val tokenStorageKey = "plugin-bing-token"

    private fun loadCachedContext(): BingContext? {
        val domain = localStorage[domainStorageKey] ?: return null
        val token = localStorage[tokenStorageKey] ?: return null
        console.info("Translated using cached BingContext: $domain, $token")
        return BingContext(domain = domain, token = token)
    }

    private fun saveContextToCache(context: BingContext) {
        localStorage[domainStorageKey] = context.domain
        localStorage[tokenStorageKey] = context.token
    }

    private fun translateWithContext(context: BingContext, query: Query, renderFailureMessage: Boolean): Promise<String> {
        val quickTranslation = getQuickTranslation(context, query).convert { parseQuickTranslation(it) }
        val detailTranslation = getDetailTranslation(context, query).convert { parseDetailTranslation(it) }

        val sourceUrl = sourceUrl(context, query)

        return Promise { resolve, reject ->
            allPromises(quickTranslation, detailTranslation).then {
                val t = BingTranslation(
                        sourceUrl = sourceUrl,
                        from = bingLangCode(query.getFrom()),
                        to = bingLangCode(query.getTo()),
                        query = query.getText(),
                        queryVoice = voiceUrl(context, query.getText(), bingLangCode(query.getFrom())),
                        translation = it.first,
                        translationVoice = voiceUrl(context, it.first, bingLangCode(query.getTo())),
                        details = it.second
                )
                resolve(renderer.render(t))
            }.catch {
                if (renderFailureMessage) {
                    resolve(renderFailure(id(), sourceUrl(context, query), query, it))
                } else {
                    reject(it)
                }
            }
        }
    }

    private fun sourceUrl(context: BingContext, q: Query) =
            "https://${context.domain}/translate/?from=${bingLangCode(q.getFrom())}&to=${bingLangCode(q.getTo())}&text=${q.getText()}"

    private fun voiceUrl(context: BingContext, text: String, lang: String) =
            "https://${context.domain}/tspeak?&format=audio%2Fmp3&language=${bingLangCode(lang)}&IG=${context.token}&IID=translator.5038.1&options=female&text=${encodeURIComponent(text)}"

    private fun getQuickTranslation(context: BingContext, query: Query) =
            httpClient.post("https://${context.domain}/ttranslatev3?isVertical=1&IG=${context.token}&IID=translator.5038.1", mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                    buildQueryData(query.getFrom(), query.getTo(), query.getText()))

    private fun getDetailTranslation(context: BingContext, query: Query) =
            httpClient.post("https://${context.domain}/tlookupv3?isVertical=1&IG=${context.token}&IID=translator.5038.1", mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                    buildFormData(query.getFrom(), query.getTo(), query.getText()))

    private fun parseQuickTranslation(result: String) =
            JSON.parse<Array<Json>>(result).getOrNull(0)?.get("translations")?.let { it as? Array<Json> }
                    ?.getOrNull(0)?.get("text")?.toString() ?: throw TranslationNotFoundException()

    @Suppress("UNCHECKED_CAST")
    private fun parseDetailTranslation(result: String): List<Detail> {
        val translations = try {
            JSON.parse<Array<Json>>(result).getOrNull(0)?.get("translations") as? Array<Json> ?: return emptyList()
        } catch (e: Throwable) {
            return emptyList()
        }

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

    private fun buildQueryData(from: String, to: String, query: String) =
            "fromLang=${bingLangCode(from)}&to=${bingLangCode(to)}&text=${encodeURIComponent(query)}"

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
        "zh-CN" to "zh-Hans",
        "zh-TW" to "zh-Hant"
)


private fun bingLangCode(langCode: String) = langCodeMap[langCode] as? String ?: langCode

external fun encodeURIComponent(str: String): String