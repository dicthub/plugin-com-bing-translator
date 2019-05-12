package org.dicthub.plugin.com_bing_translator

import org.w3c.dom.url.URL
import org.w3c.xhr.XMLHttpRequest
import kotlin.js.Promise


/**
 * Contextual information for calling Bing translation API.
 * The information is from https://www.bing.com/translator html
 *
 * @property domain Bing forces redirects to cn.bing.com for CN users, `cn.bing.com` for CN user and `bing.com` for others.
 * @property token `IG` field in bing translation html.
 */
data class BingContext(
        val domain: String,
        val token: String
)

private const val DEFAULT_URL = "https://www.bing.com/translator"
private val iggRegex = Regex("IG:\"([\\w\\d]+)\"")

fun getBingContext(): Promise<BingContext> {
    return Promise { resolve, reject ->
        val xhr = XMLHttpRequest()
        xhr.addEventListener("load", { _ ->
            val url = URL(xhr.responseURL)
            val htmlContent = xhr.responseText
            iggRegex.find(htmlContent)?.groupValues?.get(1)?.let { token ->
                resolve(BingContext(domain = url.hostname, token = token))
            }?:run {
                reject(IllegalStateException("No IG found in html content"))
            }
        })
        xhr.addEventListener("error", { event ->
            reject(IllegalStateException("Failed to fetch url, error event: $event"))
        })
        xhr.open("GET", DEFAULT_URL)
        xhr.send()
    }
}
