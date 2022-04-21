package com.joshtalks.badebhaiya.core.base

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.joshtalks.badebhaiya.core.BaseActivity
import androidx.databinding.library.baseAdapters.BR

@Suppress("DEPRECATION")

/**
 * Base fragment to standardize and simplify initialization for this component.
 *
 * @param layoutId Layout resource reference identifier.
 * @see Fragment
 */
abstract class BaseFragment<T : ViewDataBinding, V : ViewModel>(
    @LayoutRes private val layoutId: Int,
) : Fragment(), LifecycleOwner {

    private var mActivity: BaseActivity? = null
    private lateinit var mViewDataBinding: T

    /**
     * Called to Initialize view data binding variables when fragment view is created.
     */
    abstract fun onInitDataBinding(viewBinding: T)

    abstract fun getViewModel(): V

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity) {
            mActivity
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mViewDataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        mViewDataBinding.setVariable(BR.viewModel, getViewModel())
        mViewDataBinding.lifecycleOwner = viewLifecycleOwner
        return mViewDataBinding.root
    }


    override fun onDetach() {
        mActivity = null
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewDataBinding.executePendingBindings()
        initObservers()
        onInitDataBinding(mViewDataBinding)
    }

    private fun initObservers() {
//        getViewModel().errorLiveData.observe(
//            viewLifecycleOwner,
//            { error ->
//                getBaseActivity()?.showSnackbar(error.peekContent())
//            }
//        )
//
//        getViewModel().messageLiveData.observe(
//            viewLifecycleOwner,
//            { message ->
//                getBaseActivity()?.showSnackbar(message.peekContent())
//            }
//        )
//
//        getViewModel().loadingLiveData.observe(
//            viewLifecycleOwner,
//            { visible ->
//                getBaseActivity()?.showProgress(visible.peekContent())
//            }
//        )
    }

    fun showMessage(message: String) {
//        getBaseActivity()?.showSnackbar(message)
    }

    fun onError(appError: String) {
//        getViewModel().onError(appError)
    }

    fun showProgress(visible: Boolean) {
        getBaseActivity()?.showProgressBar()
    }

    fun hideProgressBar() {
        getBaseActivity()?.hideProgressBar()
    }

//    fun showToast(message: String?) {
//
//    }

    open fun getBaseActivity(): BaseActivity? {
        return mActivity
    }

    open fun getViewDataBinding(): T {
        return mViewDataBinding
    }

}