import org.dicthub.plugin.com_bing_translator.BingTranslationProvider
import org.dicthub.plugin.com_bing_translator.BingTranslationRenderer
import org.dicthub.plugin.shared.util.AjaxHttpClient

@JsName("create_plugin_com_bing_translator")
fun create_plugin_com_bing_translator(): BingTranslationProvider {

    return BingTranslationProvider(AjaxHttpClient, BingTranslationRenderer())
}
