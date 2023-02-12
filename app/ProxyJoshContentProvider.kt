//import android.content.ContentProvider
//import android.content.ContentValues
//import android.database.Cursor
//import android.database.MatrixCursor
//import android.net.Uri
//import android.util.Log
//
//class ProxyJoshContentProvider : ContentProvider() {
//    override fun onCreate(): Boolean {
//        return true
//    }
//
//    override fun query(
//        uri: Uri,
//        projection: Array<out String>?,
//        selection: String?,
//        selectionArgs: Array<out String>?,
//        sortOrder: String?
//    ): Cursor? {
//        API_HEADER -> {
//            val apiHeader = ApiHeader(
//                token = "JWT " + PrefManager.getStringValue(API_TOKEN),
//                versionName = BuildConfig.VERSION_NAME,
//                versionCode = BuildConfig.VERSION_CODE.toString(),
//                userAgent = "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString(),
//                acceptLanguage = PrefManager.getStringValue(USER_LOCALE)
//            )
//
//            val cursor = MatrixCursor(
//                arrayOf(
//                    AUTHORIZATION,
//                    APP_VERSION_NAME,
//                    APP_VERSION_CODE,
//                    APP_USER_AGENT,
//                    APP_ACCEPT_LANGUAGE
//                )
//            )
//            cursor.addRow(
//                arrayOf(
//                    apiHeader.token,
//                    apiHeader.versionName,
//                    apiHeader.versionCode,
//                    apiHeader.userAgent,
//                    apiHeader.acceptLanguage
//                )
//            )
//            Log.d(TAG, "query: Api Header --> $apiHeader")
//            return cursor
//        }
//        MENTOR_ID -> {
//            val cursor = MatrixCursor(arrayOf(MENTOR_ID_COLUMN))
//            cursor.addRow(arrayOf(Mentor.getInstance().getId()))
//            return cursor
//        }
//        CURRENT_ACTIVITY -> {
//            val cursor = MatrixCursor(arrayOf(CURRENT_ACTIVITY_COLUMN))
//            try {
////                    if(AppObjectController.joshApplication.isAppVisible())
////                        cursor.addRow(arrayOf(ActivityLifecycleCallback.currentActivity::class.java.simpleName))
////                    else
////                        cursor.addRow(arrayOf("NA"))
//            } catch (e : Exception) {
//                e.printStackTrace()
//                cursor.addRow(arrayOf("NA"))
//            }
//            return cursor
//        }
//        COURSE_ID -> {
//            val cursor = MatrixCursor(arrayOf(COURSE_ID_COLUMN))
//            cursor.addRow(arrayOf(PrefManager.getStringValue(CURRENT_COURSE_ID,false, DEFAULT_COURSE_ID)))
//            return cursor
//        }
//        IS_COURSE_BOUGHT_OR_FREE_TRIAL -> {
//            val cursor = MatrixCursor(arrayOf(FREE_TRIAL_OR_COURSE_BOUGHT_COLUMN))
//            val shouldHaveTapAction = when {
//                PrefManager.getBoolValue(IS_COURSE_BOUGHT) -> {
//                    true
//                }
//                PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false) -> {
//                    !PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED, defValue = true)
//                }
//                else -> {
//                    false
//                }
//            }
//            Log.d(TAG, "query:IS_COURSE_BOUGHT_OR_FREE_TRIAL ${
//                PrefManager.getBoolValue(
//                    IS_COURSE_BOUGHT
//                )} ${PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false)} $shouldHaveTapAction")
//            cursor.addRow(arrayOf(shouldHaveTapAction.toString()))
//            return cursor
//        }
//        IS_FT_ENDED_OR_BLOCKED -> {
//            val cursor = MatrixCursor(arrayOf(FT_ENDED_OR_BLOCKED_COLUMN))
//            val isBlockedOrFtEnded = when {
//                isBlocked() -> true
//                PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false) -> {
//                    PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED, defValue = false)
//                }
//                else -> false
//            }
//            cursor.addRow(arrayOf(isBlockedOrFtEnded.toString()))
//            return cursor
//        }
//        MENTOR_NAME -> {
//            val cursor = MatrixCursor(arrayOf(MENTOR_NAME_COLUMN))
//            if (PrefManager.getStringValue(USER_NAME)!= EMPTY)
//                cursor.addRow(arrayOf(PrefManager.getStringValue(USER_NAME)))
//            else
//                cursor.addRow(arrayOf(User.getInstance().firstName))
//            return cursor
//        }
//        MENTOR_PROFILE -> {
//            val cursor = MatrixCursor(arrayOf(MENTOR_PROFILE_COLUMN))
//            if (PrefManager.getStringValue(USER_PROFILE)!= EMPTY)
//                cursor.addRow(arrayOf(PrefManager.getStringValue(USER_PROFILE)))
//            else
//                cursor.addRow(arrayOf(User.getInstance().photo))
//            return cursor
//        }
//        DEVICE_ID -> {
//            val cursor = MatrixCursor(arrayOf(DEVICE_ID_COLUMN))
//            cursor.addRow(arrayOf(Utils.getDeviceId()))
//            return cursor
//        }
//        NOTIFICATION_DATA -> {
//            val cursor = MatrixCursor(arrayOf(NOTIFICATION_TITLE_COLUMN, NOTIFICATION_SUBTITLE_COLUMN))
//            val isBlockedOrFtEnded = if (PrefManager.getBoolValue(IS_FREE_TRIAL, defValue = false)) {
//                when {
//                    isBlocked() -> true
//                    else -> PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED, defValue = false)
//                }
//            } else false
//            return try {
//                cursor.addRow(
//                    arrayOf(
//                        getNotificationTitle(
//                            PrefManager.getStringValue(CURRENT_COURSE_ID, defaultValue = DEFAULT_COURSE_ID),
//                            isBlockedOrFtEnded
//                        ),
//                        getNotificationBody(
//                            PrefManager.getStringValue(CURRENT_COURSE_ID, defaultValue = DEFAULT_COURSE_ID),
//                            isBlockedOrFtEnded
//                        )
//                    )
//                )
//                cursor
//            } catch (e: Exception) {
//                cursor.addRow(
//                    arrayOf(
//                        getNotificationTitle(DEFAULT_COURSE_ID, isBlockedOrFtEnded),
//                        getNotificationBody(DEFAULT_COURSE_ID, isBlockedOrFtEnded)
//                    )
//                )
//                cursor
//            }
//        }
//    }
//
//    override fun getType(uri: Uri): String? {
//        TODO("Not yet implemented")
//    }
//
//    override fun insert(uri: Uri, values: ContentValues?): Uri? {
//        TODO("Not yet implemented")
//    }
//
//    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
//        TODO("Not yet implemented")
//    }
//
//    override fun update(
//        uri: Uri,
//        values: ContentValues?,
//        selection: String?,
//        selectionArgs: Array<out String>?
//    ): Int {
//        TODO("Not yet implemented")
//    }
//}