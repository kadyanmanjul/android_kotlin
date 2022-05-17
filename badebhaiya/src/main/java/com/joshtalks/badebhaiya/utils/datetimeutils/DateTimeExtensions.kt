package com.joshtalks.badebhaiya.utils.datetimeutils

fun Int.minutesToMilliseconds(): Long{
    return (1000 * 60 * this).toLong()
}