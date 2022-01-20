package com.joshtalks.joshskills.repository.server

data class LanguageSelectionResponse(
    var availableLanguages: List<ChooseLanguages> = listOf(
        ChooseLanguages("111", "Hindi (Hindi)", "111"),
        ChooseLanguages("111", "Bangla (Bangla)", "111"),
    )
)

data class ChooseLanguages(
    var testId: String = "",
    var languageName: String = "",
    var languageCode: String = "",
)