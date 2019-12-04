package com.joshtalks.joshskills.core

const val ARG_PHONE_NUMBER = "phone_number"
const val INDIA_COUNTRY_CODE = "+91"
const val EDITTEXT = "edittext"

const val RC_ACCOUNT_KIT = 16

enum class SignUpStepStatus(val dayNumber: Int) {
    SignUpStepFirst(1), SignUpStepSecond(2), SignUpStepThird(3)
}


const val REDIRECT_URL = "http://english.joshtalks.org/"


const val MESSAGE_CHAT_SIZE_LIMIT = 2048
const val EMPTY = ""

const val IMAGE_PATTERN = "([^\\s]+(\\.(?i)(jpg|png|gif|bmp)|WEBP|webp|JPEG|PNG|Jpeg)$)"
const val VIDEO_PATTERN = "([^\\s]+(\\.(?i)(mp4|MP4)$)"

val IMAGE_REGEX= Regex(pattern = IMAGE_PATTERN)

const val MINIMUM_VIDEO_DOWNLOAD_PROGRESS = 20
const val MAXIMUM_VIDEO_DOWNLOAD_PROGRESS = 50






