package com.mediapicker.gallery.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.mediapicker.gallery.R
import com.mediapicker.gallery.databinding.OssFragmentBaseBinding
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.viewmodels.VideoFile

open abstract class BaseFragment : Fragment() {

    companion object {
        const val EXTRA_SELECTED_PHOTOS = "selected_photos"
        const val EXTRA_SELECTED_VIDEOS = "selected_videos"
        const val EXTRA_DEFAULT_PAGE = "extra_default_page"
    }

    lateinit var baseBinding: OssFragmentBaseBinding

    @Suppress("UNCHECKED_CAST")
    protected fun getPhotosFromArguments(): List<PhotoFile> {
        this.arguments?.let {
            if (it.containsKey(EXTRA_SELECTED_PHOTOS)) {
                return it.getSerializable(EXTRA_SELECTED_PHOTOS) as List<PhotoFile>
            }
        }
        return emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    protected fun getVideosFromArguments(): List<VideoFile> {
        this.arguments?.let {
            if (it.containsKey(EXTRA_SELECTED_VIDEOS)) {
                return it.getSerializable(EXTRA_SELECTED_VIDEOS) as List<VideoFile>
            }
        }
        return emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        baseBinding = OssFragmentBaseBinding.inflate(layoutInflater, container, false)
            .apply {
                baseBinding.baseContainer.addView(getChildView())
            }
        return baseBinding.root
    }

    abstract fun getChildView(): View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar()
        initViewModels()
        setUpViews()
    }

    @CallSuper
    private fun setToolbar() {
        baseBinding.customToolbar.apply {
            toolbarTitle.text = getScreenTitle()
            toolbarTitle.setTextColor(requireContext().resources.getColor(R.color.oss_toolbar_text))
            if (setHomeAsUp()) {
                toolbarBackButton.visibility = View.VISIBLE
                toolbarBackButton.setImageResource(getHomeAsUpIcon())
            } else {
                toolbarBackButton.visibility = View.GONE
            }
            if (shouldHideToolBar()) {
                toolbarView.visibility = View.GONE
            }
            toolbarBackButton.setOnClickListener { onBackPressed() }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else {
            super.onOptionsItemSelected(item)
        }

    }

    protected fun showToolbar() {
        baseBinding.customToolbar.toolbarView.visibility = View.VISIBLE
    }

    protected fun hideToolbar() {
        baseBinding.customToolbar.toolbarView.visibility = View.GONE
    }

    abstract fun onBackPressed()

    open fun getHomeAsUpIcon() = R.drawable.oss_media_ic_back

    open fun setHomeAsUp() = false

    abstract fun getScreenTitle(): String

    open fun shouldHideToolBar() = false

    abstract fun setUpViews()

    @CallSuper
    open fun initViewModels() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        baseBinding.baseToolbar.visibility = View.VISIBLE
    }
}