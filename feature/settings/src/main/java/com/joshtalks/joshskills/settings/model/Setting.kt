package com.joshtalks.joshskills.settings.model

import androidx.annotation.DrawableRes

data class Setting(
    @DrawableRes
    val icon: Int,
    val title: String,
    val subheading: String? = null,
    val showSwitch: Boolean = false,
    val onClick: (() -> Unit?)? = null,
    val isDisabled: Boolean = false,
    val onSwitch: ((Boolean) -> Unit)? = null,
    val isSwitchChecked: Boolean = false,
    val isVisible: Boolean = true
)
