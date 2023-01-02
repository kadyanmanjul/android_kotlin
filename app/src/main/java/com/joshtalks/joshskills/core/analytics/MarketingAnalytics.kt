package com.joshtalks.joshskills.core.analytics

import android.os.Bundle
import android.util.Log
import com.facebook.appevents.AppEventsConstants
import com.google.firebase.analytics.FirebaseAnalytics
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.BranchLog
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

            BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM.name)
                .setDescription(BranchEventName.CALL_COMPLETED_20MIN.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.VIEW_ITEM,params)
        }
    }

    fun callComplete20MinForFreeTrial() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }

            BranchEvent(BRANCH_STANDARD_EVENT.CLICK_AD)
                .setDescription(BranchEventName.CLICK_AD.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.GENERATE_LEAD,params)
        }
    }

    fun callComplete10MinForFreeTrial(){
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }

            BranchEvent(BRANCH_STANDARD_EVENT.INITIATE_STREAM)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.POST_SCORE,params)
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
        val params = Bundle().apply {
            putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
        }
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
            val branchResponse = BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.PURCHASE, extras)
            //if (branchResponse) {
            Log.e("sagar", "addLiveDataObservable4:$branchResponse" )
               val response =  AppObjectController.appDatabase.branchLogDao().inertBranchEntry(
                    BranchLog(
                        amount.toDouble(),
                        courseName,
                        testId,
                        juspayPaymentId,
                        0
                    )
                )
                Log.e("sagar", "coursePurchased: $response")
            }
            //}
        }

    fun logAchievementLevelEvent(achievementLevel: Int) {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication

            val params = Bundle()
            params.putString(AppEventsConstants.EVENT_PARAM_LEVEL, achievementLevel.toString())
            params.putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())

            BranchEvent(AppEventsConstants.EVENT_NAME_CONTACT)
                .setCustomerEventAlias("achieve_level")
                .addCustomDataProperty("level", achievementLevel.toString())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT,params)
        }
    }

    fun logAchievementLevelEventFOrFreeTrial(achievementLevel: Int) {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }

            BranchEvent(BRANCH_STANDARD_EVENT.SHARE)
                .setCustomerEventAlias("share")
                .addCustomDataProperty("level", achievementLevel.toString())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.SHARE,params)
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

            BranchEvent(BRANCH_STANDARD_EVENT.COMPLETE_TUTORIAL)
                .setDescription("User has completed his lesson")
                .addCustomDataProperty("lesson_number",lessonNumber.toString())
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE,params)
        }
    }

    fun logLessonCompletedEventForFreeTrial(lessonNumber: Int) {

        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            BranchEvent(BRANCH_STANDARD_EVENT.RATE)
                .setDescription("User has completed his lesson")
                .addCustomDataProperty("lesson_number",lessonNumber.toString())
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.REFUND,params)
        }
    }

    fun callInitiated() {
        JoshSkillExecutors.BOUNDED.submit {
            //Facebook event
            val context = AppObjectController.joshApplication
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
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }

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

            BranchEvent(BRANCH_STANDARD_EVENT.COMPLETE_REGISTRATION)
                .setDescription(BranchEventName.SPEAKING_COMPLETED.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.LEVEL_UP,params)
        }
    }

    fun logSpeakingSectionCompletedForFreeTrial() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }
            BranchEvent(BRANCH_STANDARD_EVENT.SUBSCRIBE)
                .setDescription(BranchEventName.SUBSCRIBE.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.VIEW_PROMOTION,params)
        }
    }

    fun openPreCheckoutPage() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }

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

            BranchEvent(BRANCH_STANDARD_EVENT.START_TRIAL)
                .setDescription(BranchEventName.START_TRIAL.name)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.LEVEL_START,params)
        }
    }

    fun lessonNo2Complete(){
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val params = Bundle().apply {
                putString(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
            }

            BranchEvent(BRANCH_STANDARD_EVENT.SPEND_CREDITS)
                .addCustomDataProperty(ParamKeys.DEVICE_ID.name, Utils.getDeviceId())
                .logEvent(context)

            FirebaseAnalytics.getInstance(AppObjectController.joshApplication).logEvent(FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY,params)
        }
    }
}
