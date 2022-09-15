package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CouponListModel
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.ListOfCoupon


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
    function: ((ListOfCoupon, Int, Int, String) -> Unit)?
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
    function: ((ListOfCoupon, Int, Int,String) -> Unit)?
) {
    view.setHasFixedSize(false)
    view.adapter = adapter
    adapter.setLayoutManager(view.layoutManager)

    adapter.setListener(function)
}

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