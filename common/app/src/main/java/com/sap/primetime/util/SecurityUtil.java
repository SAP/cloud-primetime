package com.sap.primetime.util;

import org.jsoup.Jsoup;

public class SecurityUtil {
    public static String textOnly(String html) {
        if (html == null) {
            return null;
        }
        String text = Jsoup.parse(html.replaceAll("\n", "br2n")).text();
        text = text.replaceAll("br2n", "\n");

        return text;
    }

}
