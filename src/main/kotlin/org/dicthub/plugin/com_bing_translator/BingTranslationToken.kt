package org.dicthub.plugin.com_bing_translator

import org.dicthub.plugin.shared.util.HttpAsyncClient
import kotlin.js.Promise


val iggRegex = Regex("IG:\"([\\w\\d]+)\"")

fun newTokenPromise(httpAsyncClient: HttpAsyncClient): Promise<String> {
    return Promise { resolve, reject ->
        httpAsyncClient.get("https://www.bing.com/translator").then { htmlContent ->
            iggRegex.find(htmlContent)?.groupValues?.get(1)
                    ?.let(resolve)
                    ?: run {
                        reject(IllegalStateException("No IG found in html content"))
                    }
        }.catch(reject)
    }
}
