package com.handoff.app.core.extraction

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Platform
import com.handoff.app.core.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume

@Serializable
internal data class WebViewResult(
    val title: String? = null,
    val messages: List<WebViewMessage> = emptyList(),
)

@Serializable
internal data class WebViewMessage(
    val role: String? = null,
    val blocks: List<Block> = emptyList(),
)

/**
 * Strategy 2: hidden WebView for pages whose conversation only exists after JS
 * renders it. Waits for the DOM to stabilize, then runs an injected extractor
 * (built from [WebViewConfig]) that returns messages as JSON.
 */
class WebViewExtractor {

    suspend fun extract(
        context: Context,
        url: String,
        platform: Platform,
        config: WebViewConfig,
    ): Conversation = withContext(Dispatchers.Main) {
        val rawJson = withTimeoutOrNull(config.maxWaitMs.toLong()) {
            runOnWebView(context, url, config)
        } ?: throw ExtractionError.Timeout("WebView extraction exceeded ${config.maxWaitMs}ms")

        if (rawJson == CANCELLED) throw ExtractionError.Timeout("WebView run was cancelled")

        val parsed = parseResult(rawJson)
            ?: throw ExtractionError.ParseFailed("Extractor output was not valid JSON")

        val messages = parsed.messages.mapNotNull { it.toMessage() }
        if (messages.isEmpty()) {
            throw ExtractionError.ParseFailed("No messages found in rendered page")
        }
        Conversation(
            platform = platform,
            title = parsed.title?.trim().takeUnless { it.isNullOrEmpty() } ?: "Shared conversation",
            url = url,
            messages = messages,
        )
    }

    internal fun parseResult(rawJson: String): WebViewResult? = runCatching {
        var element = Json.parseToJsonElement(rawJson)
        // evaluateJavascript hands JS strings back JSON-encoded (quotes + escapes), so the
        // extractor's JSON.stringify output arrives double-wrapped; unwrap once.
        if (element is JsonPrimitive && element.isString) {
            element = Json.parseToJsonElement(element.content)
        }
        Json.decodeFromJsonElement(WebViewResult.serializer(), element)
    }.getOrNull()

    private fun WebViewMessage.toMessage(): Message? {
        val blocks = blocks.filter { block ->
            when (block) {
                is Block.Text -> block.text.isNotBlank()
                is Block.Code -> block.content.isNotBlank()
                is Block.ListBlock -> block.items.isNotEmpty()
                else -> true
            }
        }
        if (blocks.isEmpty()) return null
        val role = role?.let { JsonMessageParser.normalizeRole(it) } ?: Role.ASSISTANT
        return Message(role = role, blocks = blocks)
    }

    // ---- WebView driving ------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun runOnWebView(
        context: Context,
        url: String,
        config: WebViewConfig,
    ): String = suspendCancellableCoroutine { continuation ->
        val main = Handler(Looper.getMainLooper())
        var webView: WebView? = null
        var finished = false
        var cancelled = false

        fun cleanupAndFinish(result: String?) {
            if (finished) return
            finished = true
            main.post {
                webView?.apply {
                    stopLoading()
                    (parent as? android.view.ViewGroup)?.removeView(this)
                    destroy()
                }
                webView = null
                if (!cancelled) {
                    continuation.resume(result ?: CANCELLED)
                }
            }
        }

        continuation.invokeOnCancellation {
            cancelled = true
            main.post {
                webView?.apply {
                    stopLoading()
                    (parent as? android.view.ViewGroup)?.removeView(this)
                    destroy()
                }
                webView = null
            }
        }

        var stableCount = 0
        var lastCount = -1

        fun pollMessages() {
            val view = webView ?: return
            view.evaluateJavascript(countJs(config.messageSelector)) { countRaw ->
                if (finished || cancelled) return@evaluateJavascript
                val count = countRaw?.toIntOrNull() ?: 0
                stableCount = if (count == lastCount && count > 0) stableCount + 1 else 0
                lastCount = count
                if (count > 0 && stableCount >= config.stabilityPolls.coerceAtLeast(1)) {
                    view.evaluateJavascript(buildExtractorJs(config)) { result ->
                        cleanupAndFinish(result)
                    }
                } else {
                    if (config.scrollForLazy && count > 0) {
                        view.evaluateJavascript("window.scrollTo(0, document.body.scrollHeight);", null)
                    }
                    main.postDelayed({ pollMessages() }, config.pollIntervalMs.toLong())
                }
            }
        }

        var readyPolls = 0
        fun waitForReady() {
            val view = webView ?: return
            if (finished || cancelled) return
            readyPolls++
            view.evaluateJavascript(readyJs(config.readySelector)) { ready ->
                if (finished || cancelled) return@evaluateJavascript
                if (ready == "true" || readyPolls > 16) {
                    pollMessages()
                } else {
                    main.postDelayed({ waitForReady() }, config.pollIntervalMs.toLong())
                }
            }
        }

        webView = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.blockNetworkImage = true
            settings.loadsImagesAutomatically = false
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): WebResourceResponse? {
                    val resourceUrl = request?.url?.toString().orEmpty()
                    return if (resourceUrl.endsWith(".mp4") ||
                        resourceUrl.endsWith(".webm") ||
                        resourceUrl.endsWith(".woff2")
                    ) {
                        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    } else {
                        null
                    }
                }

                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                    main.post { waitForReady() }
                }
            }
            loadUrl(url)
        }
    }

    private val CANCELLED = "{\"cancelled\":true}"

    // ---- Injected JS ------------------------------------------------------------

    internal fun readyJs(readySelector: String): String =
        "(function(){try{return !!document.querySelector(${jsString(readySelector)})}catch(e){return false}})()"

    internal fun countJs(messageSelector: String): String =
        "(function(){try{return document.querySelectorAll(${jsString(messageSelector)}).length}catch(e){return 0}})()"

    internal fun jsString(value: String): String = JsonPrimitive(value).toString()

    /**
     * The injected extractor: picks message containers, resolves roles, strips
     * UI junk, splits code/headings/lists, and returns compact JSON.
     */
    internal fun buildExtractorJs(config: WebViewConfig): String {
        val cfgJson = Json.encodeToString(WebViewConfig.serializer(), config)
        return """
        (function(){
        var CFG = $cfgJson;
        function cls(el){
          if (!el) return '';
          if (typeof el.className === 'string') return el.className;
          if (el.className && el.className.baseVal !== undefined) return el.className.baseVal;
          return '';
        }
        function roleOf(el){
          if (CFG.roleAttr){ var a = el.getAttribute(CFG.roleAttr); if (a) return a.toLowerCase(); }
          var c = cls(el) + ' ' + (el.tagName ? el.tagName.toLowerCase() : '');
          var rules = CFG.roleByClass || [];
          for (var i=0;i<rules.length;i++){
            if (c.indexOf(rules[i].contains) !== -1) return rules[i].role;
          }
          return CFG.defaultRole || null;
        }
        function cleanClone(root){
          var clone = root.cloneNode(true);
          var junk = clone.querySelectorAll('button,svg,script,style,noscript,textarea,input,select,[class*="copy"],[class*="Copy"],[class*="toolbar"],[class*="footer"],[class*="avatar"],[class*="timestamp"],[class*="menu"],[data-testid*="copy"]');
          for (var i=0;i<junk.length;i++){ var n=junk[i]; if(n.parentNode) n.parentNode.removeChild(n); }
          return clone;
        }
        function collectCode(clone, out){
          var codes = [];
          var pres = clone.querySelectorAll('pre');
          for (var i=0;i<pres.length;i++){
            var pre = pres[i];
            var codeEl = pre.querySelector('code') || pre;
            var c = cls(codeEl) + ' ' + cls(pre);
            var m = c.match(/(?:language|lang)-([A-Za-z0-9_+-]+)/);
            var content = (codeEl.textContent || '').replace(/\s+$/,'');
            if (content.trim()){
              codes.push({type:'code', language: m ? m[1] : '', content: content});
              var marker = clone.ownerDocument.createTextNode('\u0000CODE' + (codes.length-1) + '\u0000');
              if (pre.parentNode) pre.parentNode.replaceChild(marker, pre); else pre.parentNode = null;
            } else if (pre.parentNode) {
              pre.parentNode.removeChild(pre);
            }
          }
          return codes;
        }
        function walk(node, out, codes){
          if (node.nodeType === 3){
            var raw = node.textContent || '';
            var codeMarker = raw.match(/^\u0000CODE(\d+)\u0000$/);
            if (codeMarker){
              var cb = codes[parseInt(codeMarker[1], 10)];
              if (cb) out.push(cb);
              return;
            }
            var t = raw.replace(/\s+/g,' ').trim();
            if (t) out.push({type:'text', text:t});
            return;
          }
          if (node.nodeType !== 1) return;
          var tag = node.tagName.toLowerCase();
          if (tag === 'pre' || tag === 'code') return;
          if (/^h[1-6]${'$'}/.test(tag)){
            var t = (node.textContent || '').replace(/\s+/g,' ').trim();
            if (t) out.push({type:'heading', level: parseInt(tag.substring(1),10), text:t});
            return;
          }
          if (tag === 'ul' || tag === 'ol'){
            var items = [];
            var lis = node.querySelectorAll('li');
            for (var i=0;i<lis.length;i++){
              var it = (lis[i].textContent || '').replace(/\s+/g,' ').trim();
              if (it) items.push(it);
            }
            if (items.length) out.push({type:'list', ordered: tag==='ol', items: items});
            return;
          }
          if (tag === 'img' || tag === 'picture'){
            var alt = node.getAttribute ? node.getAttribute('alt') : null;
            out.push({type:'image', alt: alt || 'image'});
            return;
          }
          if (tag === 'br'){ out.push({type:'text', text:' '}); return; }
          for (var c=0;c<node.childNodes.length;c++){ walk(node.childNodes[c], out, codes); }
        }
        function mergeText(blocks){
          var merged = [];
          for (var i=0;i<blocks.length;i++){
            var b = blocks[i];
            var prev = merged.length ? merged[merged.length-1] : null;
            if (b.type === 'text' && prev && prev.type === 'text'){
              prev.text = (prev.text + ' ' + b.text).replace(/\s+/g,' ');
            } else { merged.push(b); }
          }
          return merged;
        }
        var containers = document.querySelectorAll(CFG.messageSelector);
        var messages = [];
        for (var i=0;i<containers.length;i++){
          var el = containers[i];
          var out = [];
          var codes = collectCode(cleanClone(el), out);
          var body = CFG.textSelector ? el.querySelector(CFG.textSelector) : null;
          var root = cleanClone(body || el);
          walk(root, out, codes);
          var blocks = mergeText(out);
          if (!blocks.length && el.querySelector('img')){
            var im = el.querySelector('img');
            blocks = [{type:'image', alt: (im && im.getAttribute('alt')) || 'image'}];
          }
          if (blocks.length){
            messages.push({role: roleOf(el), blocks: blocks});
          }
        }
        var title = document.title || '';
        try {
          if (CFG.titleSelector){
            var tEl = document.querySelector(CFG.titleSelector);
            if (tEl) title = tEl.textContent || title;
          }
        } catch(e) {}
        return JSON.stringify({title: title, messages: messages});
        })()
        """.trimIndent()
    }
}
