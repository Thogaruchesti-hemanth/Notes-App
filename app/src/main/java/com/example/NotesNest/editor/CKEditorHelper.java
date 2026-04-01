package com.example.NotesNest.editor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class CKEditorHelper {

    private final WebView webView;
    private final Context context;
    private final Queue<Runnable> pendingActions = new ArrayDeque<>();
    private boolean isEditorReady = false;
    private EditorReadyListener editorReadyListener;
    private OnFormatStateChangeListener formatStateChangeListener;

    public CKEditorHelper(Context context, WebView webView) {
        this.webView = webView;
        this.context = context;
        setupEditor();
    }

    public static int getThemeColor(Context context, int attrResId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }

    public void setOnFormatStateChangeListener(OnFormatStateChangeListener listener) {
        this.formatStateChangeListener = listener;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupEditor() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onFormatStateChanged(boolean bold, boolean italic, String listType, String headingLevel) {
                if (formatStateChangeListener != null) {
                    formatStateChangeListener.onFormatStateChanged(bold, italic, listType, headingLevel);
                }
            }

            @JavascriptInterface
            public void onContentChanged() {
                // Optional: Keep your existing content change listener
            }
        }, "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                markEditorReady();
            }
        });

        int bgColor = getColorFromAttr(com.google.android.material.R.attr.colorSecondary);
        String bgHex = String.format("#%06X", (0xFFFFFF & bgColor));

        String html = getEditorHtml(bgHex);
        webView.loadDataWithBaseURL(
                "file:///android_asset/",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private void markEditorReady() {
        isEditorReady = true;

        while (!pendingActions.isEmpty()) {
            Objects.requireNonNull(pendingActions.poll()).run();
        }

        if (editorReadyListener != null) {
            editorReadyListener.onReady();
        }
    }

    public void setOnEditorReadyListener(EditorReadyListener listener) {
        this.editorReadyListener = listener;
        if (isEditorReady) listener.onReady();
    }

    private String getEditorHtml(String bgColor) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" +
                "<meta name='viewport' content='width=device-width, initial-scale=1'>" +

                "<style>" +
                "html, body {margin:0; padding:0; height:100%; background:" + bgColor + ";}" +
                "#editorContainer { display:flex; flex-direction:column; height:100%; padding:8px; box-sizing:border-box; }" +
                "#editor { flex:1; border:1px solid #1D1E1D; padding:8px; font-family:Arial,sans-serif; " +
                "color:#000000; background:" + bgColor + "; overflow-y:auto; border-radius:6px; }" +
                "#editor:focus{outline:none;}" +
                "#editor[placeholder]:empty:before {content: attr(placeholder); color:#999;}" +
                "#editor[placeholder]:empty:focus:before {content:'';}" +
                "#editor h1 { font-size:2em; font-weight:bold; margin:0.67em 0; }" +
                "#editor h2 { font-size:1.5em; font-weight:bold; margin:0.75em 0; }" +
                "</style></head><body>" +

                "<div id='editorContainer'>" +
                "<div id='editor' contenteditable='true' placeholder='Start writing your note...'></div>" +
                "</div>" +

                "<script>" +
                "var editor = document.getElementById('editor');" +

                // Generic command
                "function execCommand(cmd) { " +
                "  document.execCommand(cmd, false, null); " +
                "  editor.focus(); " +
                "  checkFormatState();" +
                "}" +

                // Restore caret to end of element
                "function restoreCaret(element) {" +
                "  let sel = window.getSelection();" +
                "  if (element) {" +
                "    let range = document.createRange();" +
                "    range.selectNodeContents(element);" +
                "    range.collapse(false);" +
                "    sel.removeAllRanges();" +
                "    sel.addRange(range);" +
                "  }" +
                "}" +

                // Get current block element
                "function getCurrentBlock() {" +
                "  let sel = window.getSelection();" +
                "  if (!sel.rangeCount) return null;" +
                "  let node = sel.anchorNode;" +
                "  while (node && node !== editor) {" +
                "    if (node.nodeType === 1 && (node.tagName === 'P' || node.tagName === 'DIV' || " +
                "        node.tagName === 'H1' || node.tagName === 'H2' || node.tagName === 'LI')) {" +
                "      return node;" +
                "    }" +
                "    node = node.parentNode;" +
                "  }" +
                "  return null;" +
                "}" +

                // Toggle heading
                "function toggleHeading(level) {" +
                "  let block = getCurrentBlock();" +
                "  if (!block) {" +
                "    document.execCommand('formatBlock', false, '<' + level + '>');" +
                "    setTimeout(() => { checkFormatState(); editor.focus(); }, 10);" +
                "    return;" +
                "  }" +
                "  " +
                "  if (block.tagName.toLowerCase() === level) {" +
                "    document.execCommand('formatBlock', false, '<p>');" +
                "  } else {" +
                "    document.execCommand('formatBlock', false, '<' + level + '>');" +
                "  }" +
                "  setTimeout(() => { checkFormatState(); editor.focus(); }, 10);" +
                "}" +

                // UL Toggle
                "function toggleList() {" +
                "  document.execCommand('insertUnorderedList', false, null);" +
                "  setTimeout(() => {" +
                "    let node = window.getSelection().anchorNode;" +
                "    while (node && node.tagName !== 'LI') node = node.parentNode;" +
                "    if (node) restoreCaret(node);" +
                "    checkFormatState();" +
                "    editor.focus();" +
                "  }, 10);" +
                "}" +

                // OL Toggle
                "function toggleNumberList() {" +
                "  document.execCommand('insertOrderedList', false, null);" +
                "  setTimeout(() => {" +
                "    let node = window.getSelection().anchorNode;" +
                "    while (node && node.tagName !== 'LI') node = node.parentNode;" +
                "    if (node) restoreCaret(node);" +
                "    checkFormatState();" +
                "    editor.focus();" +
                "  }, 10);" +
                "}" +

                // Detect OL / UL
                "function getListType() {" +
                "  let sel = window.getSelection();" +
                "  if (!sel.rangeCount) return '';" +
                "  let node = sel.anchorNode;" +
                "  while (node && node !== editor) {" +
                "    if (node.tagName === 'UL') return 'ul';" +
                "    if (node.tagName === 'OL') return 'ol';" +
                "    node = node.parentNode;" +
                "  }" +
                "  return '';" +
                "}" +

                // Get heading level
                "function getHeadingLevel() {" +
                "  let sel = window.getSelection();" +
                "  if (!sel.rangeCount) return '';" +
                "  let node = sel.anchorNode;" +
                "  while (node && node !== editor) {" +
                "    if (node.tagName === 'H1') return 'h1';" +
                "    if (node.tagName === 'H2') return 'h2';" +
                "    node = node.parentNode;" +
                "  }" +
                "  return '';" +
                "}" +

                // Check format state
                "function checkFormatState() {" +
                "  if (Android && Android.onFormatStateChanged) {" +
                "    Android.onFormatStateChanged(" +
                "      document.queryCommandState('bold')," +
                "      document.queryCommandState('italic')," +
                "      getListType()," +
                "      getHeadingLevel()" +
                "    );" +
                "  }" +
                "}" +

                // Event listeners
                "editor.addEventListener('keyup', checkFormatState);" +
                "editor.addEventListener('mouseup', checkFormatState);" +
                "editor.addEventListener('input', function() {" +
                "  checkFormatState();" +
                "  if (Android && Android.onContentChanged) {" +
                "    Android.onContentChanged(editor.innerHTML);" +
                "  }" +
                "});" +
                "document.addEventListener('selectionchange', checkFormatState);" +

                // Insert checkbox
                "function insertCheckbox() {" +
                "  var html = '<input type=\"checkbox\" style=\"margin-right:8px;\">';" +
                "  document.execCommand('insertHTML', false, html);" +
                "  editor.focus();" +
                "}" +

                // Set / Get content
                "function getContent() { return editor.innerHTML; }" +
                "function setContent(content) { " +
                "  editor.innerHTML = content; " +
                "  setTimeout(checkFormatState, 50);" +
                "}" +

                "function setBackgroundColor(color) { editor.style.backgroundColor = color; }" +

                "</script></body></html>";
    }

    /**
     * Set HTML content safely into editor
     */
    public void setContent(String html) {
        final String safeHtml = (html == null) ? "" : html;
        runWhenReady(() -> {
            String escaped = safeHtml
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "");
            webView.evaluateJavascript("setContent('" + escaped + "');", null);
        });
    }

    /**
     * Get HTML content from editor
     */
    public void getContent(ValueCallback<String> callback) {
        runWhenReady(() ->
                webView.evaluateJavascript("getContent();", value -> {
                    if (value != null && value.length() > 2) {
                        callback.onReceiveValue(
                                value.substring(1, value.length() - 1)
                                        .replace("\\u003C", "<")
                                        .replace("\\u003E", ">")
                                        .replace("\\n", "")
                                        .replace("\\\"", "\"")
                        );
                    } else {
                        callback.onReceiveValue("");
                    }
                })
        );
    }

    /**
     * Set background color for editor
     */
    public void setBackgroundColor(String color) {
        String jsCode = "setBackgroundColor('" + color + "');";
        runWhenReady(() -> webView.evaluateJavascript(jsCode, null));
    }

    /**
     * Toggle bold formatting
     */
    public void toggleBold() {
        runWhenReady(() -> webView.evaluateJavascript("execCommand('bold');", null));
    }

    /**
     * Toggle italic formatting
     */
    public void toggleItalic() {
        runWhenReady(() -> webView.evaluateJavascript("execCommand('italic');", null));
    }

    /**
     * Toggle bullet list
     */
    public void toggleBulletList() {
        runWhenReady(() -> webView.evaluateJavascript("toggleList();", null));
    }

    /**
     * Toggle numbered list
     */
    public void toggleNumberedList() {
        runWhenReady(() -> webView.evaluateJavascript("toggleNumberList();", null));
    }

    /**
     * Toggle heading (h1 or h2)
     */
    public void toggleHeading(String level) {
        runWhenReady(() -> webView.evaluateJavascript("toggleHeading('" + level + "');", null));
    }

    private void runWhenReady(Runnable action) {
        if (isEditorReady) {
            webView.post(action);
        } else {
            pendingActions.add(() -> webView.post(action));
        }
    }

    private int getColorFromAttr(int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public interface EditorReadyListener {
        void onReady();
    }

    public interface OnFormatStateChangeListener {
        void onFormatStateChanged(boolean bold, boolean italic, String listType, String headingLevel);
    }
}