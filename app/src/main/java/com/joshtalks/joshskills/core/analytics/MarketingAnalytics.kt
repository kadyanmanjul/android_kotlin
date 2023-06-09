package com.joshtalks.joshskills.core.analytics

import android.os.Bundle
import android.util.Log
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.*
import java.math.BigDecimal


object MarketingAnalytics {

    fun completeRegistrationAnalytics(
        userExist: Boolean,
        registrationMethod: RegistrationMethods
    ) {
        JoshSkillExecutors.BOUNDED.submit {
            if (userExist) {
                BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.LOGIN)
            } else {
                val params = Bundle().apply {
                    putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                }
                AppObjectController.facebookEventLogger.logEvent(
                    AppEventsConstants.EVENT_NAME_CUSTOMIZE_PRODUCT,
                    params
                )
                FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.SIGN_UP,params)
                BranchEvent(BRANCH_STANDARD_EVENT.COMPLETE_STREAM).addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.COMPLETE_STREAM)
            }
        }
    }

    fun callComplete20Min() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_VIEWED_CONTENT,params)

            BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM.name)
                .setDescription(BranchEventName.CALL_COMPLETED_20MIN.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.VIEW_ITEM,params)
        }
    }

    fun initPurchaseEvent(
        data: MutableMap<String, String>,
        amount: Double,
        currency: String
    ) {
        JoshSkillExecutors.BOUNDED.submit {

            val context = AppObjectController.joshApplication
            val testId = data["test_id"]

            //fb init event
            val params = Bundle()
            //params.putString(AppEventsConstants.EVENT_PARAM_CONTENT, mPaymentDetailsResponse.email)
            params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, testId)
            params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "E-learning")
            params.putInt(AppEventsConstants.EVENT_PARAM_NUM_ITEMS, 1)
            params.putInt(AppEventsConstants.EVENT_PARAM_PAYMENT_INFO_AVAILABLE, 1)
            params.putString(
                AppEventsConstants.EVENT_PARAM_CURRENCY,
                currency
            )
            params.putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(
                AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT,
                amount,
                params
            )

            // branch init event
            val buo = BranchUniversalObject()
                .setCanonicalIdentifier(testId.toString())
                .setTitle("joshskills")
                .setContentMetadata(
                    ContentMetadata()
                        .setPrice(amount, CurrencyType.INR)
                        .setQuantity(1.0)
                        .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
                )

            BranchEvent(BRANCH_STANDARD_EVENT.INITIATE_PURCHASE)
                .setCurrency(CurrencyType.INR)
                .setDescription("Customer init purchase ")
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .addContentItems(buo)
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.ADD_PAYMENT_INFO,params)
        }
    }

    fun callComplete5Min() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_ACHIEVED_LEVEL,params)

            BranchEvent(BRANCH_STANDARD_EVENT.ACHIEVE_LEVEL)
                .setDescription(BranchEventName.CALL_COMPLETED_5MIN.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.ADD_SHIPPING_INFO,params)
        }
    }

    fun callComplete5MinForFirstTime() {
        val context = AppObjectController.joshApplication

        JoshSkillExecutors.BOUNDED.submit {
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }

            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SELECT_ITEM,params)

            BranchEvent(BRANCH_STANDARD_EVENT.INVITE)
                .setCustomerEventAlias("inbox_screen")
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)
        }
    }

    fun openInboxPage() {
        val context = AppObjectController.joshApplication
        // Facebook Event
        AppEventsLogger.activateApp(context)
        val params = Bundle().apply {
            putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
        }
        val facebookEventLogger = AppEventsLogger.newLogger(context)
        facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_SCHEDULE,params)

        // Branch Events
        BranchEvent(AppEventsConstants.EVENT_NAME_SCHEDULE)
            .setCustomerEventAlias("inbox_screen")
            .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            .logEvent(context)

        FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.LOGIN,params)
    }

    fun coursePurchased(
        amount: BigDecimal,
        logFacebook: Boolean = false,
        testId: String = EMPTY,
        courseName: String = EMPTY,
        juspayPaymentId: String = EMPTY
    ) {
        JoshSkillExecutors.BOUNDED.submit {
            var guestMentorId = EMPTY
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, testId)
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, courseName)
            bundle.putDouble(FirebaseAnalytics.Param.VALUE, amount.toDouble())
            bundle.putString(FirebaseAnalytics.Param.TRANSACTION_ID, juspayPaymentId)
            bundle.putString(FirebaseAnalytics.Param.CURRENCY, CurrencyType.INR.name)
            bundle.putString(ParamKeys.DEVICE_ID.name , Utils.getDeviceId())
            FirebaseAnalytics.getInstance(AppObjectController.joshApplication)
                .logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)

            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                guestMentorId = Mentor.getInstance().getId()
            }

            val extras: HashMap<String, String> = HashMap()
            extras["test_id"] = testId
            extras["payment_id"] = juspayPaymentId
            extras["currency"] = CurrencyType.INR.name
            extras["amount"] = amount.toString()
            extras["course_name"] = courseName
            extras["device_id"] = Utils.getDeviceId()
            extras["guest_mentor_id"] = guestMentorId
            BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.PURCHASE, extras)
        }
    }

    fun logAchievementLevelEvent(achievementLevel: Int) {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication

            val params = Bundle()
            params.putString(AppEventsConstants.EVENT_PARAM_LEVEL, achievementLevel.toString())
            params.putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_CONTACT, params)

            BranchEvent(AppEventsConstants.EVENT_NAME_CONTACT)
                .setCustomerEventAlias("achieve_level")
                .addCustomDataProperty("level", achievementLevel.toString())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT,params)
        }
    }

    fun logLessonCompletedEvent(lessonNumber: Int,lessonId: Int) {
        MixPanelTracker.publishEvent(MixPanelEvent.LESSON_COMPLETE)
            .addParam(ParamKeys.LESSON_ID,lessonId)
            .addParam(ParamKeys.LESSON_NUMBER,lessonNumber)
            .push()

        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_SUBMIT_APPLICATION,params)

            BranchEvent(BRANCH_STANDARD_EVENT.COMPLETE_TUTORIAL)
                .setDescription("User has completed his lesson")
                .addCustomDataProperty("lesson_number",lessonNumber.toString())
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE,params)
        }
    }

    fun callInitiated() {
        JoshSkillExecutors.BOUNDED.submit {
            //Facebook event
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_SEARCHED,params)

            //Branch event
            BranchEvent(BRANCH_STANDARD_EVENT.SEARCH)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Event.SEARCH, Utils.getDeviceId())
            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        }
    }

    fun callInitiatedForFirstTime() {
        val context = AppObjectController.joshApplication
        JoshSkillExecutors.BOUNDED.submit {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Event.SELECT_PROMOTION, Utils.getDeviceId())
            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.REMOVE_FROM_CART, bundle)
            BranchEvent(BRANCH_STANDARD_EVENT.RESERVE)
                .setCustomerEventAlias("inbox_screen")
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)
        }
    }

    fun openAppFirstTime(){
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_ACTIVATED_APP,params)

            BranchEvent(BRANCH_STANDARD_EVENT.VIEW_CART)
                .setDescription(BranchEventName.APP_OPENED_FIRST_TIME.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(AppObjectController.joshApplication)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.APP_OPEN, params)
        }
    }

    fun logSpeakingSectionCompleted() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,params)

            BranchEvent(BRANCH_STANDARD_EVENT.COMPLETE_REGISTRATION)
                .setDescription(BranchEventName.SPEAKING_COMPLETED.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.LEVEL_UP,params)
        }
    }

    fun openPreCheckoutPage() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_DONATE,params)

            BranchEvent(AppEventsConstants.EVENT_NAME_DONATE)
                .setDescription(BranchEventName.OPENED_PRE_CHECKOUT_PAGE.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT,params)

        }
    }


    fun freeTrialEndEvent(){
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_START_TRIAL,params)

            BranchEvent(BRANCH_STANDARD_EVENT.START_TRIAL)
                .setDescription(BranchEventName.START_TRIAL.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.LEVEL_START,params)
        }
    }
}
