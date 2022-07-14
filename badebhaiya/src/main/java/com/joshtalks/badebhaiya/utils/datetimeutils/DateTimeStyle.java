package com.joshtalks.badebhaiya.utils.datetimeutils;


import android.content.Context;
import java.util.Date;

public enum DateTimeStyle {
    /**
     * Style full e.g Tuesday, June 13, 2017
     */
    FULL,
    /**
     * Style long e.g June 13, 2017
     */
    LONG,
    /**
     * Style medium e.g Jun 13, 2017
     */
    MEDIUM,
    /**
     * Style for time e.g Sun, Mar 6
     */
    SEMI_MEDIUM,
    /**
     * Style short e.g 06/13/17
     */
    SHORT,
    /**
     * Style short e.g 06/13/17
     */
    SMALL,
    /**
     * Style for ago time e.g 3h ago
     */
    AGO_SHORT_STRING,
    /**
     * Style for ago time e.g 3 hours ago
     */
    AGO_FULL_STRING
}
