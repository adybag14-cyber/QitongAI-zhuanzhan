package com.qtwl.YitongAIzhuanzhan

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import org.json.JSONObject
import java.util.Random

/**
 * 真人输入模拟器
 * 逐字输入，带随机间隔，模拟真实打字
 */
object HumanLikeInput {
    
    private val random = Random()
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * 真人化输入：逐字打字
     */
    fun typeLikeHuman(webView: WebView, text: String, onDone: () -> Unit) {
        val script = """
            (function(){
                const tx = document.querySelector('textarea')
                    || document.querySelector('input[type=text]')
                    || document.querySelector('div[contenteditable=true]');
                if(!tx) return 'NO_INPUT';
                tx.focus();
                return 'READY:' + text.length;
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(script) { result ->
            val cleanResult = result?.trim('"') ?: ""
            if (cleanResult.startsWith("READY")) {
                typeCharByChar(webView, text, 0, onDone)
            } else {
                onDone()
            }
        }
    }
    
    /**
     * 逐字输入
     */
    private fun typeCharByChar(webView: WebView, text: String, idx: Int, onDone: () -> Unit) {
        if (idx >= text.length) { 
            onDone()
            return 
        }
        
        val ch = text[idx].toString()
        val chJson = JSONObject().put("c", ch).toString().let { 
            it.substring(8, it.length - 1) 
        }
        
        val js = """
            (function(){
                const tx = document.querySelector('textarea')
                    || document.querySelector('div[contenteditable=true]');
                if(!tx) return;
                const c = $chJson;
                if(tx.tagName==='TEXTAREA'){
                    tx.value = tx.value + c;
                } else {
                    tx.innerText = tx.innerText + c;
                }
                tx.dispatchEvent(new InputEvent('input',{bubbles:true,data:c}));
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js, null)
        
        // 随机打字间隔 30~180ms
        val delay = (30 + random.nextInt(150)).toLong()
        handler.postDelayed({
            typeCharByChar(webView, text, idx + 1, onDone)
        }, delay)
    }
    
    /**
     * 快速输入（一次性填入，不带延迟）
     */
    fun typeFast(webView: WebView, text: String, onDone: () -> Unit) {
        val msg = text.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
        
        val script = """
            (function(){
                const tx = document.querySelector('textarea')
                    || document.querySelector('input[type=text]')
                    || document.querySelector('div[contenteditable=true]');
                if(!tx) return 'NO_INPUT';
                tx.focus();
                if(tx.tagName==='TEXTAREA' || tx.tagName==='INPUT'){
                    tx.value = '$msg';
                } else {
                    tx.textContent = '$msg';
                }
                tx.dispatchEvent(new InputEvent('input',{bubbles:true,data:'$msg'}));
                tx.dispatchEvent(new Event('change',{bubbles:true}));
                return 'OK';
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(script) { result ->
            onDone()
        }
    }
    
    /**
     * 真人化点击（带随机延迟）
     */
    fun clickLikeHuman(webView: WebView, selector: String, onDone: () -> Unit) {
        val script = """
            (function(){
                const el = document.querySelector('$selector');
                if(!el) return 'NOT_FOUND';
                el.scrollIntoView({behavior:'instant', block:'center'});
                el.dispatchEvent(new MouseEvent('mousedown',{bubbles:true,cancelable:true,view:window,button:0}));
                el.dispatchEvent(new MouseEvent('mouseup',{bubbles:true,cancelable:true,view:window,button:0}));
                el.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true,view:window,button:0}));
                return 'OK';
            })();
        """.trimIndent()
        
        // 随机延迟 100~500ms
        val delay = (100 + random.nextInt(400)).toLong()
        handler.postDelayed({
            webView.evaluateJavascript(script) { result ->
                onDone()
            }
        }, delay)
    }
    
    /**
     * 真人化发送（输入 + 点击发送按钮）
     */
    fun sendLikeHuman(webView: WebView, message: String, onResult: (Boolean, String) -> Unit) {
        // 先输入
        typeFast(webView, message) {
            // 随机延迟 200~800ms 后点击发送
            val delay = (200 + random.nextInt(600)).toLong()
            handler.postDelayed({
                clickSendButton(webView, onResult)
            }, delay)
        }
    }
    
    /**
     * 点击发送按钮
     */
    private fun clickSendButton(webView: WebView, onResult: (Boolean, String) -> Unit) {
        val script = """
            (function(){
                const selectors = [
                    'button[type="submit"]',
                    'button.send-btn',
                    '.send-button',
                    'button[aria-label*="发送"]',
                    'button[aria-label*="Send"]',
                    'button:has(svg)',
                    'button'
                ];
                for(var i=0; i<selectors.length; i++){
                    const btns = document.querySelectorAll(selectors[i]);
                    for(var j=0; j<btns.length; j++){
                        const btn = btns[j];
                        const r = btn.getBoundingClientRect();
                        if(r.width > 5 && r.height > 5){
                            btn.scrollIntoView({behavior:'instant', block:'center'});
                            btn.dispatchEvent(new MouseEvent('mousedown',{bubbles:true,cancelable:true,view:window,button:0}));
                            btn.dispatchEvent(new MouseEvent('mouseup',{bubbles:true,cancelable:true,view:window,button:0}));
                            btn.dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true,view:window,button:0}));
                            return 'OK:' + (btn.innerText || btn.textContent || '').trim();
                        }
                    }
                }
                // 回车发送
                const input = document.querySelector('textarea') || document.querySelector('[contenteditable]');
                if(input){
                    var keys = ['keydown','keypress','keyup'];
                    for(var k=0; k<keys.length; k++){
                        var ke = new KeyboardEvent(keys[k],{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true});
                        input.dispatchEvent(ke);
                    }
                    return 'ENTER';
                }
                return 'NO_BUTTON';
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(script) { result ->
            val clean = result?.trim('"') ?: ""
            if (clean.startsWith("OK")) {
                onResult(true, "发送成功")
            } else if (clean == "ENTER") {
                onResult(true, "回车发送")
            } else {
                onResult(false, "找不到发送按钮")
            }
        }
    }
}