package com.example.NotesNest.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlListConverter {

    public static String convertHtmlLists(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return htmlContent;
        }

        String result = htmlContent;

        // Convert <ul> → bullets
        result = convertUnorderedLists(result);

        // Convert <ol> → numbered lists
        result = convertOrderedLists(result);

        // Remove remaining raw <ul>/<ol> tags
        result = cleanupListTags(result);

        return result.trim();
    }

    // ------------------------------------------------------------------
    //               UL → BULLETS (•)
    // ------------------------------------------------------------------
    private static String convertUnorderedLists(String content) {
        Pattern ulPattern = Pattern.compile("(?i)<ul[^>]*>(.*?)</ul>", Pattern.DOTALL);
        Matcher matcher = ulPattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String block = matcher.group(1);
            String bulletList = convertLiToBullets(block);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(bulletList));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertLiToBullets(String block) {
        Pattern liPattern = Pattern.compile("(?i)<li[^>]*>(.*?)</li>", Pattern.DOTALL);
        Matcher matcher = liPattern.matcher(block);

        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String item = cleanFormatting(matcher.group(1));
            sb.append("• ").append(item).append("<br>");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    //               OL → NUMBERED (1. , 2. , 3.)
    // ------------------------------------------------------------------
    private static String convertOrderedLists(String content) {
        Pattern olPattern = Pattern.compile("(?i)<ol[^>]*>(.*?)</ol>", Pattern.DOTALL);
        Matcher matcher = olPattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String block = matcher.group(1);
            String numberedList = convertLiToNumbered(block);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(numberedList));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertLiToNumbered(String block) {
        Pattern liPattern = Pattern.compile("(?i)<li[^>]*>(.*?)</li>", Pattern.DOTALL);
        Matcher matcher = liPattern.matcher(block);

        StringBuilder sb = new StringBuilder();
        int index = 1;

        while (matcher.find()) {
            String item = cleanFormatting(matcher.group(1));
            sb.append(index).append(". ").append(item).append("<br>");
            index++;
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    //               PRESERVE FORMATTING
    // ------------------------------------------------------------------
    private static String cleanFormatting(String input) {
        if (input == null) return "";

        String text = input;

        // preserve line breaks
        text = text
                .replaceAll("(?i)<br\\s*/?>", "<br>")
                .replaceAll("(?i)<p[^>]*>", "<br>")
                .replaceAll("(?i)</p>", "<br>");

        // remove all tags except b, i, strong, em
        text = text.replaceAll("(?i)<(?!/?(b|i|strong|em)\\b)[^>]+>", "");

        // avoid double <br>
        text = text.replaceAll("(?i)(<br>\\s*){2,}", "<br>");

        return text.trim();
    }

    // ------------------------------------------------------------------
    //               REMOVE LEFTOVER LIST TAGS
    // ------------------------------------------------------------------
    private static String cleanupListTags(String content) {
        return content
                .replaceAll("(?i)</?ul[^>]*>", "")
                .replaceAll("(?i)</?ol[^>]*>", "")
                .trim();
    }
}
