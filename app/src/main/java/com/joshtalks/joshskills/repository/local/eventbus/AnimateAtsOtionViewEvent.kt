package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomLayout
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomWord

class AnimateAtsOtionViewEvent(
    val fromLocation: IntArray,
    val height: Int,
    val width: Int,
    val customWord: CustomWord,
    val optionLayout: CustomLayout? = null
)

