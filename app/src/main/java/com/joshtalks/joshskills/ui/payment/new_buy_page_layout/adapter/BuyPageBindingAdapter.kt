package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon


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
    function: ((CourseDetailsList, Int, Int,String) -> Unit)?
) {
    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setListener(function)
}