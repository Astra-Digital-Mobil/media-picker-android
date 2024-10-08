package com.mediapicker.gallery.presentation.fragments

import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.R
import com.mediapicker.gallery.presentation.utils.getActivityScopedViewModel
import com.mediapicker.gallery.presentation.viewmodels.BridgeViewModel
import com.mediapicker.gallery.presentation.viewmodels.StateData
import com.mediapicker.gallery.presentation.viewmodels.factory.BaseLoadMediaViewModel
import com.mediapicker.gallery.util.ItemOffsetDecoration
import com.mediapicker.gallery.utils.SnackbarUtils

abstract class BaseViewPagerItemFragment : BaseFragment() {

    var pageTitle = ""

    protected val bridgeViewModel: BridgeViewModel by lazy {
        getActivityScopedViewModel {
            BridgeViewModel(
                emptyList(),
                emptyList(),
                Gallery.galleryConfig
            )
        }
    }

    override fun initViewModels() {
        super.initViewModels()
        bridgeViewModel.getMediaStateLiveData().observe(this, Observer { reloadMedia() })
        getBaseLoadMediaViewModel().getLoadingState()
            .observe(this, Observer { handleLoadingState(it) })
    }

    override fun setUpViews() {
        childView.findViewById<RecyclerView>(R.id.ossRecycleView).apply {
            val spacing = resources.getDimensionPixelSize(R.dimen.gallery_item_offset)
            val gridLayoutManager = GridLayoutManager(context, 3)
            this.layoutManager = gridLayoutManager
            this.adapter = getMediaAdapter()
            this.clipToPadding = false
            this.addItemDecoration(ItemOffsetDecoration(spacing))
            this.setHasFixedSize(true)
        }
    }

    abstract fun getMediaAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder>

    override fun getLayoutId() = R.layout.oss_fragment_gallery

    abstract fun getBaseLoadMediaViewModel(): BaseLoadMediaViewModel

    protected open fun reloadMedia() {
        getBaseLoadMediaViewModel().loadMedia(this)
    }

    private fun handleLoadingState(stateData: StateData) {
        when (stateData) {
            StateData.SUCCESS -> hideProgressBar()
            StateData.LOADING -> showProgressBar()
            StateData.ERROR -> {
                // no-op
            }
        }
    }

    protected open fun hideProgressBar() {
        childView.findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        childView.findViewById<RecyclerView>(R.id.ossRecycleView).visibility = View.VISIBLE
    }

    protected open fun showProgressBar() {
        childView.findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        childView.findViewById<RecyclerView>(R.id.ossRecycleView).visibility = View.GONE
    }

    protected open fun showMsg(msg: String) {
        SnackbarUtils.show(view, msg, Snackbar.LENGTH_LONG)
    }

}