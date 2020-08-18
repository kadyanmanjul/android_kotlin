package com.joshtalks.joshskills.ui.assessment.listener

import com.joshtalks.joshskills.repository.local.model.assessment.Choice

interface OnChoiceClickListener {

    fun onChoiceClick(choice: Choice)

}
