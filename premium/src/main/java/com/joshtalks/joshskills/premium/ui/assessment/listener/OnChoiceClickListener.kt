package com.joshtalks.joshskills.premium.ui.assessment.listener

import com.joshtalks.joshskills.premium.repository.local.model.assessment.Choice

interface OnChoiceClickListener {

    fun onChoiceClick(choice: Choice)

}
