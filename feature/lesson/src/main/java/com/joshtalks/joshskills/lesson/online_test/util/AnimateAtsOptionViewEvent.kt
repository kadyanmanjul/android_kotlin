package com.joshtalks.joshskills.lesson.online_test.util

import com.joshtalks.joshskills.lesson.online_test.vh.AtsOptionView
import com.nex3z.flowlayout.FlowLayout

class AnimateAtsOptionViewEvent(
    val fromLocation: IntArray,
    val height: Int,
    val width: Int,
    val atsOptionView: AtsOptionView,
    val optionLayout: FlowLayout? = null,
)