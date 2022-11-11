package com.joshtalks.joshskills.base
import java.util.regex.Pattern

fun String.eval(durationInSec : Long) : Boolean {
    // Pattern : >100,<8 or ==20 etc
    val pattern = Pattern.compile("^(LT|GT|LTET|GTET|ET)(\\d+)(,(LT|GT|LTET|GTET|ET)(\\d+))?\$")
    val matcher = pattern.matcher(this)
    /**
     * GT100,LT8
     * Group 1 - "GT"
     * Group 2 - "100"
     * Group 3 - ",LT8"
     * Group 4 - "LT"
     * Group 5 - "8"
     */
    return if(matcher.find()) {
        val isFirstConditionPassed = durationInSec.isConditionValid(matcher.group(1), matcher.group(2).toLong())
        if(isFirstConditionPassed.not())
            false
        else if (matcher.group(4).hasValidExpression())
            durationInSec.isConditionValid(matcher.group(4), matcher.group(5).toLong())
        else true
    } else {
        false
    }
}

private fun String?.hasValidExpression() : Boolean {
    return this != null
}

private fun Long.isConditionValid(condition : String, value : Long) = when(condition) {
        "ET" -> {
            this == value
        }
        "GT" -> {
            this > value
        }
        "LT" -> {
            this < value
        }
        "LTET" -> {
            this <= value
        }
        "GTET" -> {
            this >= value
        }
        else -> false
}