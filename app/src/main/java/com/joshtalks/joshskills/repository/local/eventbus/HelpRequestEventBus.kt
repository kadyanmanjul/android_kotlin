package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.TypeOfHelpModel

data class HelpRequestEventBus(var typeOfHelpModel: TypeOfHelpModel)