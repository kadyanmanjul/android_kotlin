package com.joshtalks.joshskills.buypage.new_buy_page_layout.adapter

import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.buypage.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.buypage.new_buy_page_layout.model.Coupon
import kotlinx.coroutines.flow.MutableStateFlow

@BindingAdapter("featureListAdapter")
fun featureListAdapter(
    view: RecyclerView,
    adapter: FeatureListAdapter
) {
    view.setHasFixedSize(false)
    view.adapter = adapter
}

@BindingAdapter("couponListAdapter", "onCouponItemClick")
fun couponListAdapter(
    view: RecyclerView,
    adapter: CouponListAdapter,
    function: ((Coupon, Int, Int, String) -> Unit)?
) {
    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setListener(function)
}

@BindingAdapter("onCouponApply")
fun setOnCouponApply(view: AppCompatEditText, query: MutableStateFlow<String>) {
    view.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            query.value = s.toString()
        }
    })
}

@BindingAdapter("ratingAndReviewListAdapter")
fun ratingAndReviewListAdapter(
    view: RecyclerView,
    adapter: RatingAndReviewsAdapter
) {
    view.setHasFixedSize(false)
    view.adapter = adapter
}

@BindingAdapter("offersListAdapter", "onOfferItemClick")
fun offersListAdapter(
    view: RecyclerView,
    adapter: OffersListAdapter,
    function: ((Coupon, Int, Int, String) -> Unit)?
) {
    view.setHasFixedSize(false)
    view.adapter = adapter
    adapter.scroll { view.scrollToFirst() }
    adapter.setListener(function)
}

fun RecyclerView.scrollToFirst() = this.layoutManager?.smoothScrollToPosition(this, RecyclerView.State(), 0)

@BindingAdapter("priceListAdapter", "onPriceItemClick")
fun priceListAdapter(
    view: RecyclerView,
    adapter: PriceListAdapter,
    function: ((CourseDetailsList, Int, Int, String) -> Unit)?
) {
    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setListener(function)
}