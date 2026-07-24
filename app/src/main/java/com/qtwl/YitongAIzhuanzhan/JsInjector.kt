package com.qtwl.YitongAIzhuanzhan

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import org.json.JSONObject

object JsInjector {

    // ==================== 豆包专属 JS ====================

    private const val FILL_DOUBAO_JS = """
(function(text){
  var ta = document.querySelector('textarea[data-testid="chat_input_input"]')
        || document.querySelector('textarea[placeholder*="输入消息"]')
        || document.querySelector('textarea');
  if(!ta) return 'NO_INPUT';
  ta.focus();
  var setter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value').set;
  setter.call(ta, text);
  ta.dispatchEvent(new Event('input',{bubbles:true}));
  ta.dispatchEvent(new Event('change',{bubbles:true}));
  return 'FILLED';
})
"""

    private const val SEND_DOUBAO_JS = """
(function(){
  var ta = document.querySelector('textarea[data-testid="chat_input_input"]')
        || document.querySelector('textarea');
  if(ta){
    ta.focus();
    ta.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true}));
  }
  setTimeout(function(){
    var btn = document.querySelector('#flow-end-msg-send')
          || [...document.querySelectorAll('button')].find(function(b){
              return /发送/.test((b.getAttribute('aria-label')||'')) && b.getAttribute('aria-disabled')!=='true';
            });
    if(btn){
      btn.dispatchEvent(new PointerEvent('pointerdown',{bubbles:true}));
      btn.dispatchEvent(new PointerEvent('pointerup',{bubbles:true}));
      btn.click();
    }
  }, 350);
  return 'SENT';
})
"""

    private const val WATCH_DOUBAO_JS = """
(function(){
  if(window.__db_watch) return 'WATCHING';
  window.__db_watch = true;
  var stable = 0, last = '';
  var mo = new MutationObserver(function(){
    var loading = document.querySelector('.loading-spinner,[aria-busy=true]');
    var node = document.querySelector('.message-bubble:last-child .markdown-body')
            || document.querySelector('[data-testid="message_text_content"]:last-child');
    var txt = node ? node.innerText.trim() : '';
    if(!loading && txt && txt === last){ stable++; } else { stable = 0; last = txt; }
    if(stable >= 3 && last.length > 10){
      window.__db_reply = last;
      window.__db_watch = false;
      mo.disconnect();
    }
  });
  mo.observe(document.body,{childList:true,subtree:true,characterData:true});
  return 'WATCH_STARTED';
})
"""

    private const val GET_DOUBAO_REPLY_JS = """
(function(){
  var r = window.__db_reply || '';
  window.__db_reply = '';
  return r;
})
"""

    // ==================== 豆包专属 Kotlin 发送 ====================

    fun sendToDoubao(webView: WebView, text: String, onResult: (String) -> Unit) {
        val escaped = text.toJsonLiteral()
        val fillJs = "($FILL_DOUBAO_JS)($escaped)"
        webView.evaluateJavascript(fillJs) { result ->
            val clean = result?.trim()?.trim('"') ?: ""
            if (clean.contains("NO_INPUT")) {
                onResult("ERROR: 豆包输入框未找到")
                return@evaluateJavascript
            }
            val delay = (400..800).random().toLong()
            Handler(Looper.getMainLooper()).postDelayed({
                webView.evaluateJavascript(WATCH_DOUBAO_JS, null)
                webView.evaluateJavascript(SEND_DOUBAO_JS, null)
                Handler(Looper.getMainLooper()).postDelayed({
                    webView.evaluateJavascript(GET_DOUBAO_REPLY_JS) { reply ->
                        val cleanReply = reply?.trim()?.trim('"') ?: ""
                        if (cleanReply.isNotEmpty() && cleanReply != "null") {
                            onResult("REPLY:$cleanReply")
                        } else {
                            onResult("SENT")
                        }
                    }
                }, 3000)
            }, delay)
        }
    }

    // ==================== 统一分发入口 ====================

    fun fillAndSend(tag: String, webView: WebView, text: String, onResult: (String) -> Unit) {
        when (tag) {
            "doubao"  -> sendToDoubao(webView, text, onResult)
            "yuanbao" -> {
                injectJs(webView, getAutoChatScript(text)) { raw ->
                    onResult(parseGenericResult(raw))
                }
            }
            else      -> {
                injectJs(webView, getAutoChatScript(text)) { raw ->
                    onResult(parseGenericResult(raw))
                }
            }
        }
    }

    private fun parseGenericResult(raw: String): String {
        return try {
            val json = JSONObject(raw)
            val success = json.optBoolean("success", false)
            val method = json.optString("method", "")
            val error = json.optString("error", "")
            val platform = json.optString("platform", "")
            buildString {
                if (success) append("✅ 成功") else append("❌ 失败")
                if (platform.isNotEmpty()) append(" | 平台:$platform")
                if (method.isNotEmpty()) append(" | 方式:$method")
                if (error.isNotEmpty()) append(" | 错误:$error")
            }
        } catch (e: Exception) {
            "解析失败: ${e.message}"
        }
    }

    // ==================== 工具函数 ====================

    private fun String.toJsonLiteral(): String {
        return JSONObject().put("v", this).toString()
            .let { it.substring(5, it.length - 1) }
    }

    // ==================== 通用 JS 注入 ====================

    fun getAutoChatScript(message: String): String {
        val msg = message.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
        
        return """
(function() {
    try {
        var result = {success: false, error: '', method: '', inputFound: false, btnFound: false, platform: ''};
        var url = window.location.href;
        var isYuanbao = url.includes('yuanbao.tencent.com') || url.includes('yuanbao');
        if (isYuanbao) result.platform = 'yuanbao';
        
        function findInShadowDom(selector) {
            var el = document.querySelector(selector);
            if (el) return el;
            function walk(root) {
                if (!root) return null;
                var found = root.querySelector(selector);
                if (found) return found;
                var hosts = root.querySelectorAll('*');
                for (var i = 0; i < hosts.length; i++) {
                    var host = hosts[i];
                    if (host.shadowRoot) {
                        var f = walk(host.shadowRoot);
                        if (f) return f;
                    }
                }
                return null;
            }
            return walk(document);
        }
        
        if (isYuanbao) {
            var input = findInShadowDom('[data-slate-editor]') ||
                        findInShadowDom('[contenteditable="true"][data-slate-node="element"]') ||
                        findInShadowDom('.input-area textarea') ||
                        findInShadowDom('textarea[placeholder*="输入"]') ||
                        findInShadowDom('[data-testid*="input"]') ||
                        findInShadowDom('[role="textbox"]');
            
            if (!input) {
                var allEditable = document.querySelectorAll('[contenteditable], [data-slate-editor]');
                for (var i = 0; i < allEditable.length; i++) {
                    var el = allEditable[i];
                    if (el.offsetParent !== null && el.tabIndex !== -1 && !el.disabled) {
                        input = el; break;
                    }
                }
            }
            
            if (input) {
                result.inputFound = true;
                try { input.focus(); } catch(e) {}
                try { input.click(); } catch(e) {}
                try {
                    var r = input.getBoundingClientRect();
                    if (r.width === 0 || r.height === 0) {
                        input.scrollIntoView({behavior:'instant', block:'center'});
                    }
                } catch(e) {}
                try {
                    if (input.tagName === 'TEXTAREA' || input.tagName === 'INPUT') {
                        input.value = '';
                    } else if (input.isContentEditable) {
                        if (typeof input.innerHTML === 'string') input.innerHTML = '';
                        if (typeof input.textContent === 'string') input.textContent = '';
                    }
                } catch(e) {}
                
                var text = '$msg';
                var successInput = false;
                
                try {
                    if (input.isContentEditable) {
                        var sel = window.getSelection();
                        var range = document.createRange();
                        range.selectNodeContents(input);
                        range.collapse(false);
                        sel.removeAllRanges();
                        sel.addRange(range);
                        var textEvent = document.createEvent('TextEvent');
                        textEvent.initTextEvent('textInput', true, true, window, text, 0, 'en-US');
                        input.dispatchEvent(textEvent);
                        var textNode = document.createTextNode(text);
                        range.insertNode(textNode);
                        range.setStartAfter(textNode);
                        range.setEndAfter(textNode);
                        sel.removeAllRanges();
                        sel.addRange(range);
                        successInput = true;
                    }
                } catch(e1) {}
                
                if (!successInput) {
                    try {
                        var inputEvt = new InputEvent('input', {
                            bubbles: true, cancelable: true, data: text,
                            inputType: 'insertText', isComposing: false
                        });
                        if (input.tagName === 'TEXTAREA' || input.tagName === 'INPUT') {
                            input.value = text;
                        } else if (input.isContentEditable) {
                            var sel = window.getSelection();
                            var range = document.createRange();
                            range.selectNodeContents(input);
                            range.collapse(false);
                            sel.removeAllRanges();
                            sel.addRange(range);
                            var textNode = document.createTextNode(text);
                            range.insertNode(textNode);
                            range.setStartAfter(textNode);
                            range.setEndAfter(textNode);
                            sel.removeAllRanges();
                            sel.addRange(range);
                        }
                        input.dispatchEvent(inputEvt);
                        successInput = true;
                    } catch(e2) {}
                }
                
                if (!successInput) {
                    try {
                        if (input.isContentEditable) {
                            document.execCommand('insertText', false, text);
                        } else {
                            input.value = text;
                            var ev = new Event('input', {bubbles:true, cancelable:true});
                            input.dispatchEvent(ev);
                        }
                        successInput = true;
                    } catch(e3) {}
                }
                
                if (!successInput) {
                    try {
                        if (input.tagName === 'TEXTAREA' || input.tagName === 'INPUT') {
                            input.value = text;
                        } else if (input.isContentEditable) {
                            input.textContent = text;
                        }
                        var ev = new Event('input', {bubbles:true, cancelable:true});
                        input.dispatchEvent(ev);
                    } catch(e4) {}
                }
                
                try {
                    var changeEvt = new Event('change', {bubbles:true, cancelable:true});
                    input.dispatchEvent(changeEvt);
                } catch(e) {}
                
                var sendBtn = null;
                var selectors = [
                    'button[data-testid*="send"]',
                    'button[aria-label*="发送"]',
                    'button[aria-label*="Send"]',
                    'button[class*="send"]',
                    'button[class*="Send"]',
                    'button.send-btn',
                    '.send-button',
                    'button[type="submit"]',
                    'input[type="submit"]',
                    'button:has(svg)',
                    'button:has([class*="icon-send"])',
                    '[role="button"][aria-label*="发送"]',
                    'button[data-cy*="send"]'
                ];
                
                try {
                    if (input.parentNode) {
                        var nearby = input.parentNode.querySelector('[class*="send"], [aria-label*="发送"], button[type="submit"]');
                        if (nearby && nearby.offsetWidth > 0 && nearby.offsetHeight > 0) {
                            sendBtn = nearby;
                        }
                    }
                } catch(e) {}
                
                if (!sendBtn) {
                    for (var s = 0; s < selectors.length; s++) {
                        var btns = document.querySelectorAll(selectors[s]);
                        for (var i = 0; i < btns.length; i++) {
                            var btn = btns[i];
                            try {
                                var r = btn.getBoundingClientRect();
                                if (r.width > 5 && r.height > 5) {
                                    sendBtn = btn; break;
                                }
                            } catch(e) {}
                        }
                        if (sendBtn) break;
                    }
                }
                
                if (!sendBtn) {
                    var allBtns = document.querySelectorAll('button');
                    for (var i = 0; i < allBtns.length; i++) {
                        var btn = allBtns[i];
                        try {
                            var r = btn.getBoundingClientRect();
                            if (r.width === 0 || r.height === 0) continue;
                            var t = (btn.innerText || btn.textContent || '').trim().toLowerCase();
                            if (t.includes('发送') || t.includes('send') || t.includes('submit') || t === '→' || t === '>') {
                                sendBtn = btn; break;
                            }
                        } catch(e) {}
                    }
                }
                
                if (sendBtn) {
                    try { sendBtn.scrollIntoView({behavior:'instant', block:'center'}); } catch(e) {}
                    try { sendBtn.focus(); } catch(e) {}
                    try {
                        sendBtn.dispatchEvent(new MouseEvent('mousedown', {bubbles:true, cancelable:true, view:window, button:0}));
                        sendBtn.dispatchEvent(new MouseEvent('mouseup', {bubbles:true, cancelable:true, view:window, button:0}));
                        sendBtn.dispatchEvent(new MouseEvent('click', {bubbles:true, cancelable:true, view:window, button:0}));
                    } catch(e) {}
                    setTimeout(function() {
                        try { sendBtn.click(); } catch(e) {}
                        try {
                            var form = sendBtn.closest('form');
                            if (form) {
                                form.dispatchEvent(new Event('submit', {bubbles:true, cancelable:true}));
                                if (typeof form.submit === 'function') form.submit();
                            }
                        } catch(e) {}
                    }, 150);
                    result.method = 'click';
                    result.btnFound = true;
                    result.success = true;
                    result.btnText = (sendBtn.innerText || '').trim();
                    return JSON.stringify(result);
                }
                
                var keys = ['keydown','keypress','keyup'];
                for (var k = 0; k < keys.length; k++) {
                    try {
                        var ke = new KeyboardEvent(keys[k], {key:'Enter', code:'Enter', keyCode:13, which:13, bubbles:true, cancelable:true});
                        input.dispatchEvent(ke);
                    } catch(e) {}
                }
                result.method = 'enter';
                result.success = true;
                return JSON.stringify(result);
            }
        }
        
        var input = null;
        var selectors = [
            'textarea', '[contenteditable="true"]', 'input[type="text"]',
            'input[type="search"]', '.ql-editor', '.ProseMirror',
            '[data-testid="chat-input"]', '[data-role="editor"]',
            '[role="textbox"]', '[data-slate-editor]'
        ];
        for (var s = 0; s < selectors.length; s++) {
            var els = document.querySelectorAll(selectors[s]);
            for (var i = 0; i < els.length; i++) {
                var el = els[i];
                if ((el.offsetParent !== null || el.isContentEditable) && el.tabIndex !== -1 && !el.disabled) {
                    input = el; break;
                }
            }
            if (input) break;
        }
        if (!input) {
            var all = document.querySelectorAll('div, span, p, [contenteditable]');
            for (var i = 0; i < all.length; i++) {
                if (all[i].isContentEditable && all[i].offsetParent !== null && all[i].tabIndex !== -1 && !all[i].disabled) {
                    input = all[i]; break;
                }
            }
        }
        if (!input) { result.error = '找不到输入框'; return JSON.stringify(result); }
        result.inputFound = true;

        try { input.focus(); } catch(e) {}
        try {
            var rect = input.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) input.scrollIntoView({behavior:'instant', block:'center'});
        } catch(e) {}
        try { input.click(); } catch(e) {}
        
        if (input.tagName === 'TEXTAREA' || input.tagName === 'INPUT') { input.value = ''; } 
        else if (input.isContentEditable) { try { input.innerHTML = ''; } catch(e) {} }

        var text = '$msg';
        var successInput = false;
        
        try {
            var sel = window.getSelection();
            var range = document.createRange();
            range.selectNodeContents(input);
            range.collapse(false);
            sel.removeAllRanges();
            sel.addRange(range);
            var textEvent = document.createEvent('TextEvent');
            textEvent.initTextEvent('textInput', true, true, window, text, 0, 'en-US');
            input.dispatchEvent(textEvent);
            var textNode = document.createTextNode(text);
            range.insertNode(textNode);
            range.setStartAfter(textNode);
            range.setEndAfter(textNode);
            sel.removeAllRanges();
            sel.addRange(range);
            successInput = true;
        } catch(e) {}
        
        if (!successInput) {
            try {
                var inputEvt = new InputEvent('input', {bubbles:true, cancelable:true, data:text, inputType:'insertText', isComposing:false});
                if (input.tagName === 'TEXTAREA' || input.tagName === 'INPUT') input.value = text;
                else if (input.isContentEditable) {
                    var sel = window.getSelection(); var range = document.createRange(); range.selectNodeContents(input); range.collapse(false); sel.removeAllRanges(); sel.addRange(range); var tn = document.createTextNode(text); range.insertNode(tn); range.setStartAfter(tn); range.setEndAfter(tn); sel.removeAllRanges(); sel.addRange(range);
                }
                input.dispatchEvent(inputEvt);
                successInput = true;
            } catch(e) {}
        }
        
        if (!successInput) {
            try {
                if (input.tagName === 'TEXTAREA' || input.tagName === 'INPUT') input.value = text;
                else if (input.isContentEditable) input.textContent = text;
                var ev = new Event('input', {bubbles:true, cancelable:true});
                input.dispatchEvent(ev);
                successInput = true;
            } catch(e) {}
        }

        try {
            var changeEvt = new Event('change', {bubbles:true, cancelable:true});
            input.dispatchEvent(changeEvt);
        } catch(e) {}

        var sendBtn = null;
        var btnSelectors = [
            'button[class*="send"]', 'button[class*="Send"]', 'button[class*="submit"]',
            'button[aria-label*="发送"]', 'button[aria-label*="Send"]',
            '.send-btn', '.submit-btn', '.send-button', '.submit-button',
            'button[type="submit"]', 'input[type="submit"]',
            'button:has(svg)', 'button:has(i)', 'button:has(span)',
            '[role="button"]', '[data-testid="send-button"]'
        ];
        for (var s = 0; s < btnSelectors.length; s++) {
            var btns = document.querySelectorAll(btnSelectors[s]);
            for (var i = 0; i < btns.length; i++) {
                var btn = btns[i];
                try {
                    var r = btn.getBoundingClientRect();
                    if (r.width === 0 || r.height === 0) continue;
                    var t = (btn.innerText || btn.textContent || '').trim().toLowerCase();
                    if (t.includes('发送') || t.includes('send') || t.includes('submit') || t === '→' || t === '>' || t === '↑') {
                        sendBtn = btn; break;
                    }
                } catch(e) {}
            }
            if (sendBtn) break;
        }
        if (!sendBtn) {
            var allBtns = document.querySelectorAll('button, [role="button"], a[role="button"]');
            for (var i = 0; i < allBtns.length; i++) {
                var btn = allBtns[i];
                try {
                    var r = btn.getBoundingClientRect();
                    if (r.width === 0 || r.height === 0) continue;
                    var t = (btn.innerText || '').trim().toLowerCase();
                    if (t.includes('发送') || t.includes('send') || t.includes('submit')) {
                        sendBtn = btn; break;
                    }
                } catch(e) {}
            }
        }

        if (sendBtn) {
            try { sendBtn.scrollIntoView({behavior:'instant', block:'center'}); } catch(e) {}
            try { sendBtn.focus(); } catch(e) {}
            try {
                sendBtn.dispatchEvent(new MouseEvent('mousedown', {bubbles:true, cancelable:true, view:window, button:0}));
                sendBtn.dispatchEvent(new MouseEvent('mouseup', {bubbles:true, cancelable:true, view:window, button:0}));
                sendBtn.dispatchEvent(new MouseEvent('click', {bubbles:true, cancelable:true, view:window, button:0}));
            } catch(e) {}
            setTimeout(function() {
                try { sendBtn.click(); } catch(e) {}
                var form = sendBtn.closest('form');
                if (form) {
                    try { 
                        form.dispatchEvent(new Event('submit', {bubbles:true, cancelable:true})); 
                        if (typeof form.submit === 'function') form.submit();
                    } catch(e) {}
                }
            }, 150);

            result.method = 'click';
            result.btnFound = true;
            result.success = true;
            result.btnText = (sendBtn.innerText || '').trim();
            return JSON.stringify(result);
        }

        var enterEvents = ['keydown','keypress','keyup'];
        for (var i = 0; i < enterEvents.length; i++) {
            try {
                var ke = new KeyboardEvent(enterEvents[i], {key:'Enter', code:'Enter', keyCode:13, which:13, bubbles:true, cancelable:true});
                input.dispatchEvent(ke);
            } catch(e) {}
        }
        result.method = 'enter';
        result.success = true;
        return JSON.stringify(result);
    } catch(e) {
        return JSON.stringify({success: false, error: e.message, stack: e.stack});
    }
})();
""".trimIndent()
    }

    fun getExtractScript(): String {
        return """
(function() {
    try {
        var msgs = [];
        var selectors = [
            'p', 'div.message', 'div.chat-item', '.conversation-item', '.message-item',
            '.content', '.markdown-body', '.ds-markdown', '.message-content',
            '[class*="message"]', '[class*="chat"]', '[class*="response"]',
            '[data-testid*="message"]', '[data-message]', '.ai-message', '.bot-message',
            '. YuanbaoResponse', '[data-testid="assistant-content"]'
        ];
        var seen = new Set();
        for (var s = 0; s < selectors.length; s++) {
            var all = document.querySelectorAll(selectors[s]);
            for (var i = 0; i < all.length; i++) {
                var el = all[i];
                if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT' || el.isContentEditable) continue;
                var t = (el.innerText || el.textContent || '').trim();
                if (t.length > 5 && !seen.has(t)) {
                    seen.add(t);
                    msgs.push({tag: el.tagName, text: t.substring(0, 500)});
                }
            }
        }
        return JSON.stringify({success: true, title: document.title, url: window.location.href, messages: msgs, count: msgs.length});
    } catch(e) {
        return JSON.stringify({success: false, error: e.message});
    }
})();
""".trimIndent()
    }

    fun getDiagnoseScript(): String {
        return """
(function() {
    try {
        var r = {
            textareas: document.querySelectorAll('textarea').length,
            contenteditables: document.querySelectorAll('[contenteditable="true"]').length,
            buttons: document.querySelectorAll('button').length,
            inputs: document.querySelectorAll('input').length,
            title: document.title,
            url: window.location.href,
            viewportW: window.innerWidth,
            viewportH: window.innerHeight,
            userAgent: navigator.userAgent
        };
        var btnTexts = [];
        document.querySelectorAll('button').forEach(function(b) {
            var t = (b.innerText || b.textContent || '').trim();
            if (t.length > 0 && t.length < 30) btnTexts.push(t);
        });
        r.buttonTexts = btnTexts.slice(0, 20);
        var visTas = [];
        document.querySelectorAll('textarea').forEach(function(ta) {
            if (ta.offsetParent !== null) visTas.push(ta.placeholder || 'no-placeholder');
        });
        r.visibleTextareas = visTas;
        r.possibleInputs = [];
        ['textarea', '[contenteditable]', 'input[type="text"]', '[data-slate-editor]'].forEach(function(sel) {
            document.querySelectorAll(sel).forEach(function(el) {
                if (el.offsetParent !== null) {
                    r.possibleInputs.push({
                        tag: el.tagName,
                        placeholder: el.placeholder || '',
                        isContentEditable: el.isContentEditable,
                        valuePreview: (el.value || el.textContent || '').substring(0, 50)
                    });
                }
            });
        });
        return JSON.stringify(r);
    } catch(e) {
        return JSON.stringify({error: e.message});
    }
})();
""".trimIndent()
    }

    fun injectJs(webView: WebView, script: String, callback: ((String) -> Unit)? = null) {
        if (callback != null) {
            webView.evaluateJavascript(script) { raw ->
                val clean = if (raw != null && raw.startsWith("\"") && raw.endsWith("\"")) {
                    raw.substring(1, raw.length - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                } else raw ?: "null"
                callback(clean)
            }
        } else {
            webView.evaluateJavascript(script, null)
        }
    }

    fun autoSendMessage(webView: WebView, message: String, callback: ((Boolean, String) -> Unit)? = null) {
        webView.evaluateJavascript("(function(){return window.location.href})()") { url ->
            val tag = when {
                url?.contains("doubao") == true -> "doubao"
                url?.contains("yuanbao") == true -> "yuanbao"
                else -> "generic"
            }
            fillAndSend(tag, webView, message) { result ->
                val success = !result.startsWith("ERROR") && !result.startsWith("解析失败")
                callback?.let { it(success, result) }
            }
        }
    }

    fun extractChat(webView: WebView, callback: ((String) -> Unit)? = null) {
        injectJs(webView, getExtractScript(), callback)
    }
}