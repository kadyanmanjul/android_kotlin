package com.joshtalks.joshskills.core.extension

fun List<*>.deepEquals(other: List<*>) =
    this.size == other.size && this.mapIndexed { index, element -> element == other[index] }
        .all { it }

fun List<*>.getFirstMisMatchedIndex(other: List<*>) {
    val mIndex = -1
    if (this.size == other.size && this.mapIndexed { index, element -> element == other[index] }.all { it }){
        return
    }

}


