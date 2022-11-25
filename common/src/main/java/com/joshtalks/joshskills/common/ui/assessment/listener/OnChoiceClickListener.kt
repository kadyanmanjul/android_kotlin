package com.joshtalks.joshskills.common.ui.assessment.listener

import com.joshtalks.joshskills.common.repository.local.model.assessment.Choice

interface OnChoiceClickListener {

    fun onChoiceClick(choice: Choice)

}
