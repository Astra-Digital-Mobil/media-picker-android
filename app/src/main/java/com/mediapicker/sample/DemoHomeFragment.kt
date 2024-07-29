package com.mediapicker.sample

import android.os.Bundle
import android.view.View
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.fragments.HomeFragment
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.viewmodels.VideoFile
import com.mediapicker.sample.databinding.DemoFragmentMainBinding
import java.io.Serializable

class DemoHomeFragment : HomeFragment() {

    override fun getChildView(): View {
        val binding = DemoFragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun setHomeAsUp(): Boolean = false

    override fun shouldHideToolBar(): Boolean = true

    companion object {
        fun getInstance(
            listOfSelectedPhotos: List<PhotoFile> = emptyList(),
            listOfSelectedVideos: List<VideoFile> = emptyList(),
            defaultPageType: DefaultPage = DefaultPage.PhotoPage
        ): DemoHomeFragment {
            return DemoHomeFragment().apply {
                this.arguments = Bundle().apply {
                    putSerializable(EXTRA_SELECTED_PHOTOS, listOfSelectedPhotos as Serializable)
                    putSerializable(EXTRA_SELECTED_VIDEOS, listOfSelectedVideos as Serializable)
                    putSerializable(EXTRA_DEFAULT_PAGE, defaultPageType)
                }
            }
        }
    }
}