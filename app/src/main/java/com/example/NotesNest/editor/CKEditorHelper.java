package com.example.NotesNest.editor;

import android.content.Context;
import android.util.TypedValue;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayDeque;
import java.util.Queue;

public class CKEditorHelper {

    private final WebView webView;
    private final Context context;
    private final Queue<Runnable> pendingActions = new ArrayDeque<>();
    private ContentChangeListener listener;
    private boolean isEditorReady = false;
    private EditorReadyListener editorReadyListener;
    private String currentBackgroundColor; // Default white

    public CKEditorHelper(Context context, WebView webView) {
        this.webView = webView;
        this.context = context;

        currentBackgroundColor = String.format("#%06X", (0xFFFFFF & getThemeColor(context, com.google.android.material.R.attr.colorSecondary)));
        setupEditor();
    }

    public static int getThemeColor(Context context, int attrResId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data; // returns the actual color int (dynamic)
    }

    private void setupEditor() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);

        webView.addJavascriptInterface(new JSInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                markEditorReady();
            }
        });

        // ✅ Get theme colors
        int bgColor = getColorFromAttr(com.google.android.material.R.attr.colorSecondary);

        // Convert to CSS hex format
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
            pendingActions.poll().run();
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
                "color:#000000;" + "; background:" + bgColor + "; overflow-y:auto; border-radius:6px; }" +
                "#editor:focus{outline:none;}" +
                "#editor[placeholder]:empty:before {content: attr(placeholder); color:#999;}" +
                "#editor[placeholder]:empty:focus:before {content:'';}" +
                "</style></head><body>" +

                "<div id='editorContainer'>" +
                "<div id='editor' contenteditable='true' placeholder='Start writing your note...'></div>" +
                "</div>" +

                "<script>" +
                "var editor = document.getElementById('editor');" +

                // Generic command
                "function execCommand(cmd) { document.execCommand(cmd, false, null); editor.focus(); }" +

                // Restore caret to end of LI
                "function restoreCaret() {" +
                "  let sel = window.getSelection();" +
                "  let node = sel.anchorNode;" +
                "  while (node && node.tagName !== 'LI') node = node.parentNode;" +
                "  if (node) {" +
                "     let range = document.createRange();" +
                "     range.selectNodeContents(node);" +
                "     range.collapse(false);" +
                "     sel.removeAllRanges();" +
                "     sel.addRange(range);" +
                "  }" +
                "}" +

                // UL Toggle
                "function toggleList() {" +
                " document.execCommand('insertUnorderedList', false, null);" +
                " setTimeout(() => {" +
                "   restoreCaret();" +
                "   notifyListChange();" +
                "   editor.focus();" +
                " }, 10);" +
                "}" +

                // OL Toggle
                "function toggleNumberList() {" +
                " document.execCommand('insertOrderedList', false, null);" +
                " setTimeout(() => {" +
                "   restoreCaret();" +
                "   notifyListChange();" +
                "   editor.focus();" +
                " }, 10);" +
                "}" +

                // Detect OL / UL
                "function getListType() {" +
                " let sel = window.getSelection();" +
                " if (!sel.rangeCount) return '';" +
                " let node = sel.anchorNode;" +
                " while (node && node !== editor) {" +
                "   if (node.tagName === 'UL') return 'ul';" +
                "   if (node.tagName === 'OL') return 'ol';" +
                "   node = node.parentNode;" +
                " }" +
                " return '';" +
                "}" +

                // Notify Android
                "function notifyListChange() {" +
                " if (Android && Android.onListTypeChanged) {" +
                "   Android.onListTypeChanged(getListType());" +
                " }" +
                "}" +

                // Selection listeners
                "editor.addEventListener('keyup', notifyListChange);" +
                "editor.addEventListener('mouseup', notifyListChange);" +

                // Input → Android
                "editor.addEventListener('input', function() {" +
                " if (Android && Android.onContentChanged) {" +
                "    Android.onContentChanged(editor.innerHTML);" +
                " }" +
                "});" +

                // Insert checkbox
                "function insertCheckbox() {" +
                " var html = '<input type=\"checkbox\" style=\"margin-right:8px;\">';" +
                " document.execCommand('insertHTML', false, html);" +
                " editor.focus();" +
                "}" +

                // Set / Get content
                "function getContent() { return editor.innerHTML; }" +
                "function setContent(content) { editor.innerHTML = content; notifyListChange(); }" +

                "function setBackgroundColor(color) { editor.style.backgroundColor = color; }" +

                "</script></body></html>";
    }


    /**
     * Set HTML content safely into editor
     */
    public void setContent(String html) {
        final String safeHtml = (html == null) ? "" : html;

        runWhenReady(() -> {
            String escaped = safeHtml.replace("'", "\\'");
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
        currentBackgroundColor = color;
        String jsCode = "setBackgroundColor('" + color + "');";
        runWhenReady(() ->
                webView.evaluateJavascript("setBackgroundColor('" + color + "');", null)
        );
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

    /**
     * Listener for live content changes
     */
    public interface ContentChangeListener {
        void onContentChanged(String newHtml);
    }

    public interface EditorReadyListener {
        void onReady();
    }

    private class JSInterface {
        @JavascriptInterface
        public void onContentChanged(String html) {
            if (listener != null) listener.onContentChanged(html);
        }
    }

}