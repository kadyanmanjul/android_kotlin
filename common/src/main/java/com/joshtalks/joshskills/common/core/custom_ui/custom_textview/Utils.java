package com.joshtalks.joshskills.common.core.custom_ui.custom_textview;

import android.util.Log;

class Utils {

    static String getRegexByAutoLinkMode(AutoLinkMode anAutoLinkMode, String customRegex) {
        switch (anAutoLinkMode) {
            case MODE_HASHTAG:
                return RegexParser.HASHTAG_PATTERN;
            case MODE_MENTION:
                return RegexParser.MENTION_PATTERN;
            case MODE_URL:
                return RegexParser.URL_PATTERN;
            case MODE_PHONE:
                return RegexParser.PHONE_PATTERN;
            case MODE_EMAIL:
                return RegexParser.EMAIL_PATTERN;
            case MODE_CUSTOM:
                if (!Utils.isValidRegex(customRegex)) {
                    Log.e(JoshTextView.TAG, "Your custom regex is null, returning URL_PATTERN");
                    return RegexParser.URL_PATTERN;
                } else {
                    return customRegex;
                }
            default:
                return RegexParser.URL_PATTERN;
        }
    }

    private static boolean isValidRegex(String regex) {
        return regex != null && !regex.isEmpty() && regex.length() > 2;
    }

}
