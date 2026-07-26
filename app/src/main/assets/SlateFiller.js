/**
 * SlateFiller — 四级降级输入引擎
 * 通过 WebView.evaluateJavascript 注入（绕 CSP）
 */
(function (window) {
  if (window.__slateFiller) return;
  window.__slateFiller = true;

  // ===== 工具函数 =====

  // React Fiber 反查 Slate editor 实例
  function getSlateEditor(el) {
    const keys = Object.keys(el);
    const fiberKey = keys.find(k =>
      k.startsWith('__reactFiber') || k.startsWith('__reactInternalInstance')
    );
    if (!fiberKey) return null;
    let fiber = el[fiberKey];
    while (fiber) {
      if (fiber.stateNode && fiber.stateNode.props && fiber.stateNode.props.editor) {
        return fiber.stateNode.props.editor;
      }
      fiber = fiber.return;
    }
    return null;
  }

  // 递归穿透 Shadow DOM 查找
  function deepQuery(sel, root) {
    root = root || document;
    let results = [];
    const direct = root.querySelectorAll(sel);
    results.push(...direct);
    const all = root.querySelectorAll('*');
    all.forEach(el => {
      if (el.shadowRoot) {
        results = results.concat(deepQuery(sel, el.shadowRoot));
      }
    });
    return results;
  }

  // 找输入框（优先级：Slate > contenteditable > textarea）
  function findInput() {
    // Slate editor
    const slate = document.querySelector('[data-slate-editor]');
    if (slate) return { el: slate, type: 'slate' };

    // contenteditable
    const ce = document.querySelector('div[contenteditable="true"], [contenteditable=""]');
    if (ce) return { el: ce, type: 'contenteditable' };

    // textarea
    const ta = document.querySelector('textarea');
    if (ta) return { el: ta, type: 'textarea' };

    return null;
  }

  // ===== 四级输入策略 =====

  // 策略1：Slate editor.insertText
  function fillViaSlateEditor(el, text) {
    const editor = getSlateEditor(el);
    if (!editor) return false;
    try {
      if (editor.focus) editor.focus();
      editor.insertText(text);
      if (editor.onChange) editor.onChange();
      return true;
    } catch (e) {
      console.warn('[SlateFiller] slate insertText failed:', e);
      return false;
    }
  }

  // 策略2：Clipboard paste
  function fillViaPaste(el, text) {
    try {
      el.focus();
      const dataTransfer = new DataTransfer();
      dataTransfer.setData('text/plain', text);
      const pasteEvent = new ClipboardEvent('paste', {
        bubbles: true,
        cancelable: true,
        clipboardData: dataTransfer
      });
      const selectAll = new KeyboardEvent('keydown', {
        key: 'a', code: 'KeyA', ctrlKey: true, bubbles: true
      });
      el.dispatchEvent(selectAll);
      const delEvent = new KeyboardEvent('keydown', {
        key: 'Delete', code: 'Delete', bubbles: true
      });
      el.dispatchEvent(delEvent);
      const handled = el.dispatchEvent(pasteEvent);
      if (!handled) {
        document.execCommand('insertText', false, text);
      }
      return true;
    } catch (e) {
      console.warn('[SlateFiller] paste failed:', e);
      return false;
    }
  }

  // 策略3：beforeinput + input 事件
  function fillViaBeforeInput(el, text) {
    try {
      el.focus();
      if (el.setSelectionRange) {
        el.setSelectionRange(el.value.length, el.value.length);
      }
      const beforeInput = new InputEvent('beforeinput', {
        bubbles: true,
        cancelable: true,
        inputType: 'insertText',
        data: text,
        isComposing: false
      });
      const beforeResult = el.dispatchEvent(beforeInput);
      const inputEvent = new InputEvent('input', {
        bubbles: true,
        cancelable: true,
        inputType: 'insertText',
        data: text
      });
      el.dispatchEvent(inputEvent);
      if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT') {
        const start = el.selectionStart || 0;
        const end = el.selectionEnd || 0;
        el.value = el.value.substring(0, start) + text + el.value.substring(end);
        el.dispatchEvent(new Event('change', { bubbles: true }));
      } else {
        document.execCommand('insertText', false, text);
      }
      return beforeResult;
    } catch (e) {
      console.warn('[SlateFiller] beforeinput failed:', e);
      return false;
    }
  }

  // 策略4：execCommand 兜底
  function fillViaExecCommand(el, text) {
    try {
      el.focus();
      document.execCommand('insertText', false, text);
      return true;
    } catch (e) {
      console.warn('[SlateFiller] execCommand failed:', e);
      return false;
    }
  }

  // ===== 主入口 =====
  window.__fillText = function (text) {
    const input = findInput();
    if (!input) return JSON.stringify({ ok: false, reason: 'NO_INPUT' });

    const { el, type } = input;

    if (type === 'slate') {
      if (fillViaSlateEditor(el, text)) {
        return JSON.stringify({ ok: true, method: 'slate_editor' });
      }
    }

    if (fillViaPaste(el, text)) {
      return JSON.stringify({ ok: true, method: 'paste' });
    }
    if (fillViaBeforeInput(el, text)) {
      return JSON.stringify({ ok: true, method: 'beforeinput' });
    }
    if (fillViaExecCommand(el, text)) {
      return JSON.stringify({ ok: true, method: 'execCommand' });
    }

    return JSON.stringify({ ok: false, reason: 'ALL_FAILED' });
  };

  // ===== 发送按钮 =====
  window.__clickSend = function () {
    const candidates = deepQuery('[aria-label*="发送" i], [aria-label*="send" i], [data-testid*="send" i], button[type="submit"]');
    const btn = candidates.find(el => {
      const txt = (el.textContent || '') + (el.getAttribute('aria-label') || '');
      return /发送|send|submit/i.test(txt);
    });

    if (btn) {
      const button = btn.tagName === 'BUTTON' ? btn : btn.closest('button');
      if (button) {
        button.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, cancelable: true }));
        button.dispatchEvent(new PointerEvent('pointerup', { bubbles: true, cancelable: true }));
        button.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
        return JSON.stringify({ ok: true, method: 'button_click' });
      }
    }

    const input = findInput();
    if (input) {
      input.el.focus();
      input.el.dispatchEvent(new KeyboardEvent('keydown', {
        key: 'Enter', code: 'Enter', keyCode: 13,
        which: 13, bubbles: true, cancelable: true
      }));
      input.el.dispatchEvent(new KeyboardEvent('keyup', {
        key: 'Enter', code: 'Enter', keyCode: 13,
        which: 13, bubbles: true, cancelable: true
      }));
      return JSON.stringify({ ok: true, method: 'enter_key' });
    }

    return JSON.stringify({ ok: false, reason: 'NO_SEND_BUTTON' });
  };

  // ===== 回复监听（贪婪匹配） =====
  window.__watchReply = function () {
    if (window.__replyWatcher) return;
    window.__replyWatcher = true;

    var lastText = '';
    var stableCount = 0;
    var lastChangeTime = Date.now();

    function getAllText() {
      var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
      var texts = [];
      var node;
      while (node = walker.nextNode()) {
        var p = node.parentElement;
        if (!p) continue;
        var s = window.getComputedStyle(p);
        if (s.display === 'none' || s.visibility === 'hidden') continue;
        var t = node.textContent.trim();
        if (t.length > 20) texts.push(t);
      }
      return texts.sort(function(a,b){ return b.length - a.length; }).slice(0,5);
    }

    function checkReply() {
      var texts = getAllText();
      if (texts.length === 0) return;
      var longest = texts[0];
      if (longest === lastText) {
        stableCount++;
      } else {
        lastText = longest;
        stableCount = 0;
        lastChangeTime = Date.now();
      }
      var elapsed = Date.now() - lastChangeTime;
      if (stableCount >= 3 && longest.length > 30 && elapsed > 2000) {
        try {
          if (window.Android && window.Android.onReply) {
            window.Android.onReply(longest);
          }
        } catch(e) {}
        window.__replyWatcher = false;
      }
    }

    setInterval(checkReply, 800);
    var mo = new MutationObserver(function(){
      lastChangeTime = Date.now();
      stableCount = 0;
    });
    mo.observe(document.body, {childList:true, subtree:true, characterData:true});
  };

  console.log('[SlateFiller] ready');

})(window);