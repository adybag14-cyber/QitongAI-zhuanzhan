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

  // ===== Request-scoped reply watcher =====
  window.__replyWatchers = window.__replyWatchers || {};

  window.__cancelReplyWatcher = function (requestId) {
    var watcher = window.__replyWatchers[String(requestId)];
    if (!watcher) return false;
    if (watcher.timer) clearInterval(watcher.timer);
    if (watcher.observer) watcher.observer.disconnect();
    delete window.__replyWatchers[String(requestId)];
    return true;
  };

  window.__watchReply = function (options) {
    options = options || {};
    var requestId = String(options.requestId || Date.now());
    window.__cancelReplyWatcher(requestId);

    var replySelectors = options.replySelectors || [];
    var loadingSelectors = options.loadingSelectors || [];
    var fallbackSelectors = [
      '[data-role="assistant"]',
      '[data-message-author-role="assistant"]',
      '[data-testid*="assistant"]',
      '[class*="assistant-message"]',
      '[class*="message-assistant"]',
      '.ds-markdown', '.qwen-markdown', '.markdown-body', '.prose'
    ];
    var baselineCount = Number(options.baselineCount || 0);
    var baselineText = String(options.baselineText || '').trim();
    var sentMessage = String(options.sentMessage || '').trim();
    var timeoutMs = Math.max(10000, Number(options.timeoutMs || 150000));
    var deadline = Date.now() + timeoutMs;
    var lastText = '';
    var stablePolls = 0;
    var checking = false;

    function roots() {
      var result = [document], queue = [document];
      while (queue.length) {
        var root = queue.shift();
        var all = root.querySelectorAll ? root.querySelectorAll('*') : [];
        for (var i = 0; i < all.length; i++) {
          if (all[i].shadowRoot) {
            result.push(all[i].shadowRoot);
            queue.push(all[i].shadowRoot);
          }
        }
      }
      return result;
    }

    function visible(el) {
      if (!el) return false;
      var style = getComputedStyle(el);
      var rect = el.getBoundingClientRect();
      return !el.hidden && style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0;
    }

    function isUserMessage(el) {
      return !!(el.closest && el.closest(
        '[data-role="user"],[data-message-author-role="user"],[data-testid*="user"],'+
        '[class*="user-message"],[class*="message-user"],[class*="human-message"]'
      ));
    }

    function collectElements(selectors) {
      var rs = roots(), elements = [], seen = new Set();
      for (var s = 0; s < selectors.length; s++) {
        for (var r = 0; r < rs.length; r++) {
          var nodes = [];
          try { nodes = rs[r].querySelectorAll(selectors[s]); } catch (ignore) {}
          for (var n = 0; n < nodes.length; n++) {
            var el = nodes[n];
            if (!seen.has(el) && visible(el) && !isUserMessage(el) &&
                el.tagName !== 'TEXTAREA' && el.tagName !== 'INPUT' && !el.isContentEditable) {
              seen.add(el);
              elements.push(el);
            }
          }
        }
      }
      elements.sort(function (a, b) {
        if (a === b) return 0;
        var relation = a.compareDocumentPosition ? a.compareDocumentPosition(b) : 0;
        if (relation & Node.DOCUMENT_POSITION_FOLLOWING) return -1;
        if (relation & Node.DOCUMENT_POSITION_PRECEDING) return 1;
        return a.getBoundingClientRect().top - b.getBoundingClientRect().top;
      });
      return elements;
    }

    function snapshot() {
      var elements = collectElements(replySelectors);
      if (!elements.length) elements = collectElements(fallbackSelectors);
      var texts = [], seenText = new Set();
      for (var i = 0; i < elements.length; i++) {
        var text = (elements[i].innerText || elements[i].textContent || '').replace(/\u00a0/g, ' ').trim();
        if (text && !seenText.has(text)) {
          seenText.add(text);
          texts.push(text);
        }
      }

      var loading = false, rs = roots();
      for (var s = 0; s < loadingSelectors.length; s++) {
        for (var r = 0; r < rs.length; r++) {
          var nodes = [];
          try { nodes = rs[r].querySelectorAll(loadingSelectors[s]); } catch (ignore) {}
          for (var n = 0; n < nodes.length; n++) if (visible(nodes[n])) loading = true;
        }
      }
      return { text: texts.length ? texts[texts.length - 1] : '', count: texts.length, loading: loading };
    }

    function finish(text) {
      window.__cancelReplyWatcher(requestId);
      try {
        if (window.Android && window.Android.onReplyForRequest) {
          window.Android.onReplyForRequest(requestId, text);
        }
      } catch (error) {
        console.warn('[SlateFiller] reply bridge failed:', error);
      }
    }

    function check() {
      if (checking) return;
      checking = true;
      try {
        if (Date.now() >= deadline) {
          window.__cancelReplyWatcher(requestId);
          return;
        }
        var current = snapshot();
        var text = String(current.text || '').trim();
        var isNew = text && text !== sentMessage &&
          (current.count > baselineCount || text !== baselineText);
        if (isNew && text === lastText) stablePolls += 1;
        else {
          lastText = isNew ? text : lastText;
          stablePolls = 0;
        }
        var required = current.loading ? 10 : 3;
        if (isNew && stablePolls >= required) finish(text);
      } finally {
        checking = false;
      }
    }

    var watcher = {
      timer: setInterval(check, 800),
      observer: new MutationObserver(function () {
        setTimeout(check, 120);
      })
    };
    window.__replyWatchers[requestId] = watcher;
    watcher.observer.observe(document.documentElement || document.body, {
      childList: true,
      subtree: true,
      characterData: true
    });
    check();
    return true;
  };

  console.log('[SlateFiller] ready');

})(window);