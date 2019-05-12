package org.dicthub.plugin.com_bing_translator

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.dicthub.plugin.shared.util.builtinSourceIcon
import org.dicthub.plugin.shared.util.renderSource
import kotlin.random.Random

class BingTranslationRenderer {

    fun render(t: BingTranslation): String {

        val stringBuilder = StringBuilder()
        val container = stringBuilder.appendHTML()

        container.div(classes = "t-result") {

            div(classes = "alert alert-info") {
                em(classes = "translation-lang") {
                    +"[${t.from}]"
                }
                strong {
                    +t.query
                }
                +" "
                span(classes = "translation-voice") {
                    audio {
                        src = t.queryVoice
                    }
                }
            }

            ul(classes = "list-group") {
                li(classes = "list-group-item") {
                    em(classes = "translation-lang") {
                        +"[${t.to}]"
                    }
                    +t.translation
                    +" "
                    span(classes = "translation-voice") {
                        audio {
                            src = t.translationVoice
                        }
                    }

                    if (t.details.isNotEmpty()) {
                        val detailId = "googleTranslationDetail${Random.nextInt()}"
                        a(classes = "btn btn-light btn-sm mb-2", href = "#$detailId") {
                            role = "button"
                            attributes["data-toggle"] = "collapse"
                            +"\uD83D\uDCDA"
                        }
                        div(classes = "collapse") {
                            id = detailId
                            t.details.forEach { detail ->
                                div {
                                    div { i(classes = "translation-poc") { +detail.poc } }
                                    ul(classes = "list-group") {
                                        detail.meanings.forEach { meaning ->
                                            li(classes = "list-group-item small") {
                                                span (classes = "translation-primary") { +"${meaning.meaning}: " }
                                                span (classes = "translation-secondary") { +meaning.examples.joinToString(", ") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        renderSource(container, t.sourceUrl, builtinSourceIcon(ID))

        return stringBuilder.toString()
    }
}