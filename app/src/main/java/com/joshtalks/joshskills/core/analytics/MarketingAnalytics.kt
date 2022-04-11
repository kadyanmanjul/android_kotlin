package com.joshtalks.joshskills.core.analytics

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.RegistrationMethods
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.*
import java.math.BigDecimal
import java.util.*


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
                    putString(
                        AppEventsConstants.EVENT_PARAM_REGISTRATION_METHOD,
                        registrationMethod.type
                    )
                    putString(
                        AppEventsConstants.EVENT_PARAM_SUCCESS,
                        AppEventsConstants.EVENT_PARAM_VALUE_YES
                    )
                    putString(AppEventsConstants.EVENT_PARAM_CURRENCY, CurrencyType.INR.name)
                }
                AppObjectController.facebookEventLogger.logEvent(
                    AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
                    params
                )

                BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.COMPLETE_REGISTRATION)
            }
        }
    }

    fun viewContentEvent(context: Context, courseExploreModel: CourseExploreModel) {
        JoshSkillExecutors.BOUNDED.submit {
            //Fb view event
        /*    val params = Bundle()
            params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "E-learning")
            params.putString(AppEventsConstants.EVENT_PARAM_CONTENT, courseExploreModel.toString())
            params.putString(
                AppEventsConstants.EVENT_PARAM_CONTENT_ID,
                courseExploreModel.id?.toString()
            )
            params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, CurrencyType.INR.name)
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_VIEWED_CONTENT, params)
*/
            //Branch view event
            val buo = BranchUniversalObject()
                .setCanonicalIdentifier(courseExploreModel.id?.toString() ?: "")
                .setTitle(courseExploreModel.courseName)
                .setContentDescription(courseExploreModel.testName)
                .setContentImageUrl(courseExploreModel.imageUrl)
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
            BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM).addContentItems(buo).logEvent(context)
        }
    }

    fun initPurchaseEvent(
        data: MutableMap<String, String>,
        mPaymentDetailsResponse: OrderDetailResponse
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
                mPaymentDetailsResponse.currency
            )
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(
                AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT,
                mPaymentDetailsResponse.amount,
                params
            )

            // branch init event
            val buo = BranchUniversalObject()
                .setCanonicalIdentifier(testId.toString())
                .setTitle("joshskills")
                .setContentMetadata(
                    ContentMetadata()
                        .setPrice(mPaymentDetailsResponse.amount, CurrencyType.INR)
                        .setQuantity(1.0)
                        .setContentSchema(BranchContentSchema.COMMERCE_PRODUCT)
                )

            BranchEvent(BRANCH_STANDARD_EVENT.INITIATE_PURCHASE)
                .setCurrency(CurrencyType.INR)
                .setDescription("Customer init purchase ")
                .addContentItems(buo)
                .logEvent(context)
        }
    }

    fun sevenDayFreeTrialStart(testId: String) {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication

            val params = Bundle().apply {
                putString("test_id", testId)
            }
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent("fb_seven_day_free_trial", params)

            BranchEvent("fb_seven_day_free_trial")
                .setDescription("7 day free trial")
                .addCustomDataProperty("test_id", testId)
                .logEvent(context)
        }
    }

    fun startFreeTrail() {
        val context = AppObjectController.joshApplication
        val params = Bundle().apply {
            putString(AppEventsConstants.EVENT_PARAM_CURRENCY, CurrencyType.INR.name)
            putFloat(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM, 0f)
        }

        // Facebook Event
        AppEventsLogger.activateApp(context)
        val facebookEventLogger = AppEventsLogger.newLogger(context)
        facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_START_TRIAL, params)

        // Branch Events
        BranchEvent(BRANCH_STANDARD_EVENT.START_TRIAL)
            .setCustomerEventAlias("start_free_trail")
            .logEvent(context)

        // Firebase Events
        AppAnalytics.create(BRANCH_STANDARD_EVENT.START_TRIAL.name)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    fun coursePurchased(amount : BigDecimal) {
        val context = AppObjectController.joshApplication
        val params = Bundle().apply {
            putString(AppEventsConstants.EVENT_PARAM_CURRENCY, CurrencyType.INR.name)
            putString(AppEventsConstants.EVENT_PARAM_CONTENT, "Course")
            putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "E-learning")
            putInt(AppEventsConstants.EVENT_PARAM_NUM_ITEMS, 1)
            putString(
                AppEventsConstants.EVENT_PARAM_SUCCESS,
                AppEventsConstants.EVENT_PARAM_VALUE_YES
            )
        }

        // Facebook Event
        AppEventsLogger.activateApp(context)
        val facebookEventLogger = AppEventsLogger.newLogger(context)

        facebookEventLogger.logPurchase(
            amount,
            Currency.getInstance(CurrencyType.INR.name),
            params
        )

        // Branch Events
        BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
            .setCustomerEventAlias("purchase")
            .logEvent(context)

        // Firebase Events
        AppAnalytics.create(BRANCH_STANDARD_EVENT.PURCHASE.name)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    fun logAchievementLevelEvent(achievementLevel: Int) {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication

            val params = Bundle()
            params.putString(AppEventsConstants.EVENT_PARAM_LEVEL, achievementLevel.toString())
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_ACHIEVED_LEVEL, params)

            BranchEvent(BRANCH_STANDARD_EVENT.ACHIEVE_LEVEL)
                .setCustomerEventAlias("achieve_level")
                .addCustomDataProperty("level", achievementLevel.toString())
                .logEvent(context)

            AppAnalytics.create(BRANCH_STANDARD_EVENT.ACHIEVE_LEVEL.name)
                .addBasicParam()
                .addUserDetails()
                .addParam("achieve_level", achievementLevel)
                .push()
        }
    }

    fun logLessonCompletedEvent(lessonNumber: Int) {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_SUBMIT_APPLICATION)

            BranchEvent("lesson_complete_event")
                .setDescription("User has completed his lesson")
                .addCustomDataProperty("lesson_number",lessonNumber.toString() )
                .logEvent(context)

            AppAnalytics.create(AnalyticsEvent.LESSON_COMPLETED.name)
                .addBasicParam()
                .addUserDetails()
                .addParam("lesson_number", lessonNumber)
                .push()
        }
    }

    fun logGrammarSectionCompleted() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_SCHEDULE)

            BranchEvent(AppEventsConstants.EVENT_NAME_SCHEDULE)
                .setDescription("User has completed his grammar section")
                .logEvent(context)

            AppAnalytics.create(AppEventsConstants.EVENT_NAME_SCHEDULE)
                .addBasicParam()
                .addUserDetails()
                .push()
        }
    }

    fun logWhatsappRemarketing() {
        JoshSkillExecutors.BOUNDED.submit {
            val context = AppObjectController.joshApplication
            val facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.logEvent(AppEventsConstants.EVENT_PARAM_SEARCH_STRING)

            BranchEvent(AppEventsConstants.EVENT_PARAM_SEARCH_STRING)
                .setDescription("Exclude this user from add")
                .logEvent(context)

            AppAnalytics.create(AppEventsConstants.EVENT_PARAM_SEARCH_STRING)
                .addBasicParam()
                .addUserDetails()
                .push()
        }
    }

        fun logCallInitiated() {
            JoshSkillExecutors.BOUNDED.submit {
                val context = AppObjectController.joshApplication
                val facebookEventLogger = AppEventsLogger.newLogger(context)
                facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_CONTACT)

                BranchEvent(AppEventsConstants.EVENT_NAME_CONTACT)
                    .setDescription("User has initiated his call")
                    .logEvent(context)

                AppAnalytics.create(AppEventsConstants.EVENT_NAME_CONTACT)
                    .addBasicParam()
                    .addUserDetails()
                    .push()
            }
        }

        fun logNewPaymentPageOpened() {
            JoshSkillExecutors.BOUNDED.submit {
                val context = AppObjectController.joshApplication
                val facebookEventLogger = AppEventsLogger.newLogger(context)
                facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_DONATE)

                BranchEvent(AppEventsConstants.EVENT_NAME_DONATE)
                    .setDescription("User has opened te new payment page")
                    .logEvent(context)

                AppAnalytics.create(AppEventsConstants.EVENT_NAME_DONATE)
                    .addBasicParam()
                    .addUserDetails()
                    .push()
            }
        }

        fun logSpeakingSectionCompleted() {
            JoshSkillExecutors.BOUNDED.submit {
                val context = AppObjectController.joshApplication
                val facebookEventLogger = AppEventsLogger.newLogger(context)
                facebookEventLogger.logEvent(AppEventsConstants.EVENT_NAME_CUSTOMIZE_PRODUCT)

                BranchEvent(AppEventsConstants.EVENT_NAME_CUSTOMIZE_PRODUCT)
                    .setDescription("User has completed his speaking section")
                    .logEvent(context)

                AppAnalytics.create(AppEventsConstants.EVENT_NAME_CUSTOMIZE_PRODUCT)
                    .addBasicParam()
                    .addUserDetails()
                    .push()
            }
        }

    }
