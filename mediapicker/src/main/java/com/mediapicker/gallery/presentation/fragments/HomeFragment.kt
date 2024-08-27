package com.mediapicker.gallery.presentation.fragments

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.GalleryConfig
import com.mediapicker.gallery.R
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.activity.GalleryActivity
import com.mediapicker.gallery.presentation.adapters.PagerAdapter
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.utils.MediaPermissionRequest
import com.mediapicker.gallery.presentation.utils.constructGalleryPermissionsRequest
import com.mediapicker.gallery.presentation.utils.constructMediaPermissionsRequest
import com.mediapicker.gallery.presentation.utils.galleryPermissions
import com.mediapicker.gallery.presentation.utils.getActivityScopedViewModel
import com.mediapicker.gallery.presentation.utils.getFragmentScopedViewModel
import com.mediapicker.gallery.presentation.utils.isAtLeast34Api
import com.mediapicker.gallery.presentation.utils.isPermissionGranted
import com.mediapicker.gallery.presentation.viewmodels.BridgeViewModel
import com.mediapicker.gallery.presentation.viewmodels.HomeViewModel
import com.mediapicker.gallery.presentation.viewmodels.VideoFile
import com.mediapicker.gallery.utils.SnackbarUtils
import permissions.dispatcher.ktx.PermissionsRequester
import java.io.Serializable

open class HomeFragment : BaseFragment() {

    private val homeViewModel: HomeViewModel by lazy {
        getFragmentScopedViewModel { HomeViewModel(Gallery.galleryConfig) }
    }

    private val bridgeViewModel: BridgeViewModel by lazy {
        getActivityScopedViewModel {
            BridgeViewModel(
                getPhotosFromArguments(),
                getVideosFromArguments(),
                Gallery.galleryConfig
            )
        }
    }

    private val defaultPageToOpen: DefaultPage by lazy {
        getPageFromArguments()
    }

    private lateinit var galleryPermissionsRequester: PermissionsRequester
    private lateinit var mediaPermissionRequest: MediaPermissionRequest
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var actionButton: AppCompatButton

    override fun onAttach(context: Context) {
        super.onAttach(context)

        galleryPermissionsRequester =  constructGalleryPermissionsRequest(
            permissions = galleryPermissions(),
            onPermissionDenied = ::onPermissionDenied,
            onNeverAskAgain = ::showNeverAskAgainPermission,
            requiresPermission = ::checkPermissions
        )

        mediaPermissionRequest = constructMediaPermissionsRequest(
            onPermissionDenied = ::onPermissionDenied,
            onPermissionGranted = ::reloadMedia
        )
    }

    override fun getLayoutId() = R.layout.oss_fragment_main

    override fun getScreenTitle() = if (Gallery.galleryConfig.galleryLabels.homeTitle.isNotBlank())
        Gallery.galleryConfig.galleryLabels.homeTitle
    else
        getString(R.string.oss_title_home_screen)

    override fun setUpViews() {
        actionButton = childView.findViewById(R.id.action_button)
        tabLayout = childView.findViewById(R.id.tabLayout)
        viewPager = childView.findViewById(R.id.viewPager)

        actionButton.apply {
            isSelected = false
            setOnClickListener { onActionButtonClicked() }
            text = if (Gallery.galleryConfig.galleryLabels.homeAction.isNotBlank())
                Gallery.galleryConfig.galleryLabels.homeAction
            else
                getString(R.string.oss_posting_next)
        }

        galleryPermissionsRequester.launch()

        childView.findViewById<Button>(R.id.action_permission).setOnClickListener {
            onManagePermissionButtonClick()
        }
    }

    private fun checkPermissions() {
        showManagerPermissionUI(true)

        when (homeViewModel.getMediaType()) {
            GalleryConfig.MediaType.PhotoOnly -> {
                setUpWithOutTabLayout()
            }

            GalleryConfig.MediaType.PhotoWithFolderOnly -> {
                setUpWithOutTabLayout()
            }

            GalleryConfig.MediaType.PhotoWithFolderAndVideo -> {
                setUpWithTabLayout()
            }

            GalleryConfig.MediaType.PhotoWithVideo -> {
                setUpWithTabLayout()
            }

            GalleryConfig.MediaType.PhotoWithoutCameraFolderOnly -> {
                setUpWithOutTabLayout()
            }
        }
        openPage()
    }

    fun onPermissionDenied() {
        // activity?.supportFragmentManager?.popBackStack()
        Gallery.galleryConfig.galleryCommunicator?.onPermissionDenied()
    }

    fun showNeverAskAgainPermission() {
        //. Toast.makeText(context, R.string.oss_permissions_denied_attach_image, Toast.LENGTH_LONG).show()
        Gallery.galleryConfig.galleryCommunicator?.onNeverAskPermissionAgain()
    }

    override fun initViewModels() {
        super.initViewModels()
        bridgeViewModel.getActionState().observe(this, Observer { changeActionButtonState(it) })
        bridgeViewModel.getError().observe(this, Observer { showError(it) })
        bridgeViewModel.getClosingSignal().observe(this, Observer { closeIfHostingOnActivity() })
    }

    private fun closeIfHostingOnActivity() {
        if (requireActivity() is GalleryActivity) {
            requireActivity().finish()
        }
    }

    override fun setHomeAsUp() = true

    override fun onBackPressed() {
        closeIfHostingOnActivity()
        bridgeViewModel.onBackPressed()
    }

    private fun changeActionButtonState(state: Boolean) {
        childView.findViewById<AppCompatButton>(R.id.action_button).isSelected = state
    }

    private fun showError(error: String) {
        view?.let { SnackbarUtils.show(it, error, Snackbar.LENGTH_SHORT) }
    }

    private fun setUpWithOutTabLayout() {
        tabLayout.visibility = View.GONE
        PagerAdapter(
            childFragmentManager,
            listOf(
                PhotoGridFragment.getInstance(
                    getString(R.string.oss_title_tab_photo),
                    getPhotosFromArguments()
                )
            )
        ).apply {
            viewPager.adapter = this
        }
    }

    private fun openPage() {
        viewPager.apply {
            currentItem = if (defaultPageToOpen is DefaultPage.PhotoPage) {
                0
            } else {
                1
            }
        }
    }

    private fun onActionButtonClicked() {
        bridgeViewModel.complyRules()
    }

    private fun setUpWithTabLayout() {
        viewPager.apply {
            PagerAdapter(
                childFragmentManager, listOf(
                    PhotoGridFragment.getInstance(
                        getString(R.string.oss_title_tab_photo),
                        getPhotosFromArguments()
                    ),
                    VideoGridFragment.getInstance(
                        getString(R.string.oss_title_tab_video),
                        getVideosFromArguments()
                    )
                )
            ).apply { adapter = this }
            tabLayout.setupWithViewPager(this)
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun getPageFromArguments(): DefaultPage {
        this.arguments?.let {
            if (it.containsKey(EXTRA_DEFAULT_PAGE)) {
                return it.getSerializable(EXTRA_DEFAULT_PAGE) as DefaultPage
            }
        }
        return DefaultPage.PhotoPage
    }

    fun reloadMedia() {
        bridgeViewModel.reloadMedia()
    }

    protected open fun showManagerPermissionUI(visibility: Boolean) {
        val permissionLayout = childView.findViewById<LinearLayout>(R.id.permission_layout)
        if(!visibility){
            permissionLayout.isVisible = visibility
            return
        }
        if (isAtLeast34Api()
            && !(isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES)
                    && isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO))
        ) {
            permissionLayout.isVisible = true
        }else{
            permissionLayout.isVisible = false
        }
    }

    private fun onManagePermissionButtonClick() {
        mediaPermissionRequest.launch()
    }

    companion object {
        fun getInstance(
            listOfSelectedPhotos: List<PhotoFile> = emptyList(),
            listOfSelectedVideos: List<VideoFile> = emptyList(),
            defaultPageType: DefaultPage = DefaultPage.PhotoPage
        ): HomeFragment {
            return HomeFragment().apply {
                this.arguments = Bundle().apply {
                    putSerializable(EXTRA_SELECTED_PHOTOS, listOfSelectedPhotos as Serializable)
                    putSerializable(EXTRA_SELECTED_VIDEOS, listOfSelectedVideos as Serializable)
                    putSerializable(EXTRA_DEFAULT_PAGE, defaultPageType)
                }
            }
        }
    }
}
