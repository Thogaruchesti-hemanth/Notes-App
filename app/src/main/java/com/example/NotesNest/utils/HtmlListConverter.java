package com.example.NotesNest.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlListConverter {

    public static String convertHtmlLists(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return htmlContent;
        }

        String result = htmlContent;

        // Convert <input type="checkbox"> → Unicode checkboxes
        result = convertInputCheckboxes(result);

        // Convert <ul> with checkbox class → checkboxes
        result = convertChecklists(result);

        // Convert <ul> → bullets
        result = convertUnorderedLists(result);

        // Convert <ol> → numbered lists
        result = convertOrderedLists(result);

        // Remove remaining raw <ul>/<ol> tags
        result = cleanupListTags(result);

        return result.trim();
    }

    // ------------------------------------------------------------------
    //               INPUT CHECKBOXES ( <input type="checkbox"> )
    // ------------------------------------------------------------------
    private static String convertInputCheckboxes(String content) {
        // Regex to find <input type="checkbox"> with optional checked attribute
        Pattern checkboxPattern = Pattern.compile("<input[^>]*type=\"checkbox\"[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = checkboxPattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group();
            String replacement = "\u2610 "; // Unicode Ballot Box
            if (tag.toLowerCase(Locale.ROOT).contains("checked")) {
                replacement = "\u2611 "; // Unicode Ballot Box with Check
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // ------------------------------------------------------------------
    //               CHECKLISTS ( ☐ or ☑ )
    // ------------------------------------------------------------------
    private static String convertChecklists(String content) {
        // Look for <ul> with a class that indicates it's a checklist
        Pattern checklistPattern = Pattern.compile("(?i)<ul[^>]*class=\"[^\"]*todo-list[^\"]*\"[^>]*>(.*?)</ul>", Pattern.DOTALL);
        Matcher matcher = checklistPattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        boolean found = false;
        while (matcher.find()) {
            found = true;
            String block = matcher.group(1);
            String checklist = convertLiToCheckboxes(block);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(checklist));
        }

        if (!found) {
            // Fallback: search for li items that have data-checked attribute if the ul didn't have the class
            return convertLiWithDataChecked(content);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertLiToCheckboxes(String block) {
        Pattern liPattern = Pattern.compile("(?i)<li[^>]*>(.*?)</li>", Pattern.DOTALL);
        Matcher matcher = liPattern.matcher(block);

        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String fullLi = matcher.group(0);
            String itemText = cleanFormatting(matcher.group(1));
            
            String checkbox = "\u2610 ";
            if (fullLi.contains("checked") || fullLi.contains("data-checked=\"true\"")) {
                checkbox = "\u2611 ";
            }
            
            sb.append(checkbox).append(itemText).append("<br>");
        }
        return sb.toString();
    }

    private static String convertLiWithDataChecked(String content) {
        // Some editors use <li data-checked="true"> inside a normal <ul>
        Pattern liPattern = Pattern.compile("(?i)<li[^>]*data-checked=\"(true|false)\"[^>]*>(.*?)</li>", Pattern.DOTALL);
        Matcher matcher = liPattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String isChecked = matcher.group(1);
            String itemText = cleanFormatting(matcher.group(2));
            String checkbox = "true".equals(isChecked) ? "\u2611 " : "\u2610 ";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(checkbox + itemText + "<br>"));
        }
        matcher.appendTail(sb);
        return sb.toString();
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
