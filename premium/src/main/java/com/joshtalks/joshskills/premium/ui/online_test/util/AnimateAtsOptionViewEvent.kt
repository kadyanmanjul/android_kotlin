package com.joshtalks.joshskills.premium.ui.online_test.util

import com.joshtalks.joshskills.premium.ui.online_test.vh.AtsOptionView
import com.nex3z.flowlayout.FlowLayout

class AnimateAtsOptionViewEvent(
    val fromLocation: IntArray,
    val height: Int,
    val width: Int,
    val atsOptionView: AtsOptionView,
    val optionLayout: FlowLayout? = null,
)