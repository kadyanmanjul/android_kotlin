package com.joshtalks.joshskills.quizgame.util

const val FAVOURITE_FRAGMENT = "FAVOURITE_FRAGMENT"
const val RANDOM_PARTNER_FRAGMENT = "RANDOM_PARTNER_FRAGMENT"
const val GROUPS_STACK = "GROUPS_BACK_STACK"

const val CHOICE_FRAGMENT = "CHOICE_FRAGMENT"

const val CHANGE_USER_STATUS = "User status changed to inactive , data deleted from UserStatusRedis"

const val CALL_DURATION_RESPONSE = "Call duration saved successfully"
const val NO_MATCHING_USER_FOUND = "No matching user found"
const val NO_FPP_FOUND = "No favourite practice partner found"

//Both Team Selected
const val START_TIME: String = "start_time"
const val ROOM_ID: String = "room_id"
const val CHANNEL_NAME = "channelName"
const val USER_DETAILS: String = "userDetails"
const val MESSAGE = "Your partner left the you have to play alone"

//FavouritePartnerFragment
const val IN_ACTIVE: String = "Inactive"
const val ACTIVE: String = "Active"
const val IN_GAME: String = "In Game"
const val SEARCHING: String = "Searching"
const val USER_ALREADY_JOIN: String = "User has already joined the game"
const val USER_LEFT_THE_GAME: String = "User has left the game"
const val PARTNER_LEFT_THE_GAME: String = "Partner has left the game please try again"
const val TEAM_CREATED: String = "Team created successfully"
const val TRUE: String = "true"
const val FALSE: String = "false"

//QuestionFragment
const val BOTH_TEAM_SELECTED: String =
    "Both teams selected answer before 7 seconds , show response and animations to all the users in room"
const val CALL_TIME: String = "callTime"
const val FROM_TYPE: String = "fromType"
const val QUESTION_COUNT: String = "7"
const val LAST_ROUND: String = "Last Round"
const val ROUND_2X_BOUNCE: String = "2X BONUS!"
const val RANDOM: String = "Random"
const val TIME_DATA: String = "Time"

//RandomPartnerFragment
const val FOUR_USER_FOUND_MSG: String = "4 users found in Redis successfully"
const val NO_OPPONENT_FOUND = "No Opponent Team Found Please Retry"
const val USER_DELETED_SUCCESSFULLY = "User deleted successfully"


//WinScreenFragment
const val YOU_WON: String = "You Won!"
const val WINNER: String = "Winner"
const val TEAM_SCORE: String = "team_score"
const val OPPONENT_TEAM_MARKS: String = "opponent_team_marks"
const val TEAM_ID: String = "team_id"
const val OPPONENT_WON: String = "Opponent Won!"
const val ZERO: String = "0"
const val FIVE: String = "5"
const val DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_FPP: String =
    "Data deleted successfully from firebase"
const val DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_AND_RADIUS: String =
    "Data deleted successfully from firebase and redis"
