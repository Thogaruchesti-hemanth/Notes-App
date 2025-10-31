package com.example.NotesNest.editor;

import android.content.Context;
import android.util.TypedValue;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class CKEditorHelper {

    private final WebView webView;
    private ContentChangeListener listener;
    private final Context context;
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

        // ✅ Get theme colors
        int bgColor = getColorFromAttr(com.google.android.material.R.attr.colorSecondary);
        int textColor = getColorFromAttr(com.google.android.material.R.attr.colorPrimary);

        // Convert to CSS hex format
        String bgHex = String.format("#%06X", (0xFFFFFF & bgColor));
        String textHex = String.format("#%06X", (0xFFFFFF & textColor));

        String html = getEditorHtml(bgHex, textHex);
        webView.loadDataWithBaseURL(
                "file:///android_asset/",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private String getEditorHtml(String bgColor, String textColor) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<style>" +
                "html, body {margin:0; padding:0; height:100%; background:" + bgColor + ";}" +
                "#editorContainer {" +
                "display:flex; flex-direction:column; height:100%; " +
                "padding:8px; box-sizing:border-box;" +
                "}" +
                "#editor {" +
                "flex:1; border:1px solid #1D1E1D; padding:8px;" +
                "font-family:Arial,sans-serif; color:" + textColor + ";" +
                "background:" + bgColor + "; overflow-y:auto;" +
                "border-radius:6px;" +
                "}" +
                "#editor:focus{outline:none;}" +
                "#editor[placeholder]:empty:before {content: attr(placeholder); color:#999;}" +
                "#editor[placeholder]:empty:focus:before {content:'';}" +
                "</style></head><body>" +
                "<div id='editorContainer'>" +
                "<div id='editor' contenteditable='true' placeholder='Start writing your note...'></div>" +
                "</div>" +
                "<script>" +
                "var editor=document.getElementById('editor');" +
                "function execCommand(command){document.execCommand(command,false,null);editor.focus();}" +
                "function insertCheckbox(){var html='<input type=\"checkbox\" style=\"margin-right:8px;\">';" +
                "document.execCommand('insertHTML',false,html);editor.focus();}" +
                "function getContent(){return editor.innerHTML;}" +
                "function setContent(content){editor.innerHTML=content;}" +
                "function setBackgroundColor(color){" +
                "editor.style.backgroundColor=color;" +
                "}" +
                "editor.addEventListener('input',function(){" +
                "Android.onContentChanged(editor.innerHTML);" +
                "});" +
                "</script></body></html>";
    }

    /**
     * Set HTML content safely into editor
     */
    public void setContent(String html) {
        if (html == null) html = "";
        String escaped = html.replace("'", "\\'");
        webView.post(() -> webView.evaluateJavascript("setContent('" + escaped + "');", null));
    }

    /**
     * Get HTML content from editor
     */
    public void getContent(ContentCallback callback) {
        webView.evaluateJavascript("getContent();", value -> {
            if (value != null && value.length() > 2) {
                callback.onResult(value.substring(1, value.length() - 1)
                        .replace("\\u003C", "<")
                        .replace("\\n", "")
                        .replace("\\\"", "\""));
            } else {
                callback.onResult("");
            }
        });
    }

    /**
     * Set background color for editor
     */
    public void setBackgroundColor(String color) {
        currentBackgroundColor = color;
        String jsCode = "setBackgroundColor('" + color + "');";
        webView.post(() -> webView.evaluateJavascript(jsCode, null));
    }

    /**
     * Execute formatting commands (bold, italic, etc.)
     */
    public void executeCommand(String command) {
        webView.evaluateJavascript("execCommand('" + command + "');", null);
    }

    public void setContentChangeListener(ContentChangeListener listener) {
        this.listener = listener;
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

    public interface ContentCallback {
        void onResult(String html);
    }

    private class JSInterface {
        @JavascriptInterface
        public void onContentChanged(String html) {
            if (listener != null) listener.onContentChanged(html);
        }
    }

}