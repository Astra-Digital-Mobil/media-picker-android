package com.mediapicker.gallery.presentation.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.GalleryConfig
import com.mediapicker.gallery.R
import com.mediapicker.gallery.domain.contract.GalleryPagerCommunicator
import com.mediapicker.gallery.domain.entity.GalleryViewMediaType
import com.mediapicker.gallery.domain.entity.MediaGalleryEntity
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.activity.GalleryActivity
import com.mediapicker.gallery.presentation.activity.MediaGalleryActivity
import com.mediapicker.gallery.presentation.adapters.PagerAdapter
import com.mediapicker.gallery.presentation.carousalview.CarousalActionListener
import com.mediapicker.gallery.presentation.carousalview.MediaGalleryView
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.utils.PermissionRequestWrapper
import com.mediapicker.gallery.presentation.utils.getActivityScopedViewModel
import com.mediapicker.gallery.presentation.utils.getFragmentScopedViewModel
import com.mediapicker.gallery.presentation.viewmodels.BridgeViewModel
import com.mediapicker.gallery.presentation.viewmodels.HomeViewModel
import com.mediapicker.gallery.presentation.viewmodels.VideoFile
import com.mediapicker.gallery.utils.SnackbarUtils
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructPermissionsRequest
import java.io.Serializable


open class PhotoCarousalFragment : BaseFragment(), GalleryPagerCommunicator,
    MediaGalleryView.OnGalleryItemClickListener {

    private val PHOTO_PREVIEW = 43475

    private lateinit var mediaGalleryView: MediaGalleryView
    private lateinit var actionButton: AppCompatButton
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout

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

    private lateinit var permissionsRequester: PermissionsRequester
    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionsRequester = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            constructPermissionsRequest(
                permissions = arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                ),
                onPermissionDenied = ::onPermissionDenied,
                onNeverAskAgain = ::showNeverAskAgainPermission,
                requiresPermission = ::checkPermissions,
                onShowRationale = ::onShowRationale
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            constructPermissionsRequest(
                permissions = arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                onPermissionDenied = ::onPermissionDenied,
                onNeverAskAgain = ::showNeverAskAgainPermission,
                requiresPermission = ::checkPermissions,
                onShowRationale = ::onShowRationale
            )
        } else {
            constructPermissionsRequest(
                permissions = arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                onPermissionDenied = ::onPermissionDenied,
                onNeverAskAgain = ::showNeverAskAgainPermission,
                requiresPermission = ::checkPermissions,
                onShowRationale = ::onShowRationale
            )
        }
    }

    private fun onShowRationale(permissionRequest: PermissionRequest) {
        Gallery.galleryConfig.galleryCommunicator?.onShowPermissionRationale(
            PermissionRequestWrapper(permissionRequest)
        )
    }

    override fun getLayoutId() = R.layout.oss_fragment_carousal

    override fun getScreenTitle() = if (Gallery.galleryConfig.galleryLabels.homeTitle.isNotBlank())
        Gallery.galleryConfig.galleryLabels.homeTitle
    else
        getString(com.mediapicker.gallery.R.string.oss_title_home_screen)


    override fun setUpViews() {
        Gallery.pagerCommunicator = this

        mediaGalleryView = childView.findViewById(R.id.mediaGalleryView)
        actionButton = childView.findViewById(R.id.action_button)
        tabLayout = childView.findViewById(R.id.tabLayout)
        viewPager = childView.findViewById(R.id.viewPager)

        val mediaGalleryViewContainer: AppBarLayout =
            childView.findViewById(R.id.mediaGalleryViewContainer)
        if (Gallery.galleryConfig.showPreviewCarousal.showCarousal) {
            mediaGalleryViewContainer.visibility = View.VISIBLE
            mediaGalleryView.setOnGalleryClickListener(this@PhotoCarousalFragment)
            if (Gallery.galleryConfig.showPreviewCarousal.imageId != 0) {
                mediaGalleryView.updateDefaultPhoto(Gallery.galleryConfig.showPreviewCarousal.imageId)
            }
            if (Gallery.galleryConfig.showPreviewCarousal.previewText != 0) {
                mediaGalleryView.updateDefaultText(Gallery.galleryConfig.showPreviewCarousal.previewText)
            }
        }

        actionButton.text = if (Gallery.galleryConfig.galleryLabels.homeAction.isNotBlank())
            Gallery.galleryConfig.galleryLabels.homeAction
        else
            getString(com.mediapicker.gallery.R.string.oss_posting_next)


        baseBinding.customToolbar.apply {
            toolbarTitle.isAllCaps = Gallery.galleryConfig.textAllCaps
            toolbarTitle.gravity = Gallery.galleryConfig.galleryLabels.titleAlignment
            toolbarBackButton.setImageResource(Gallery.galleryConfig.galleryUiConfig.backIcon)
        }

        permissionsRequester.launch()
    }


    fun checkPermissions() {
        if (!isRemoving && isAdded) {
            when (homeViewModel.getMediaType()) {
                GalleryConfig.MediaType.PhotoOnly -> {
                    setUpWithOutTabLayout()
                }

                GalleryConfig.MediaType.PhotoWithFolderOnly -> {
                    setUpWithOutTabLayout()
                }

                GalleryConfig.MediaType.PhotoWithoutCameraFolderOnly -> {
                    setUpWithOutTabLayout()
                }

                else -> {
                    setUpWithOutTabLayout()
                }
            }
            openPage()
            childView.findViewById<AppCompatButton>(R.id.action_button).apply {
                isSelected = false
                setOnClickListener { onActionButtonClicked() }
            }
        }
    }

    fun onPermissionDenied() {
        // activity?.supportFragmentManager?.popBackStack()
        Gallery.galleryConfig.galleryCommunicator?.onPermissionDenied()
    }

    fun addMediaForPager(mediaGalleryEntity: MediaGalleryEntity) {
        mediaGalleryView.addMediaForPager(mediaGalleryEntity)
    }

    fun removeMediaFromPager(mediaGalleryEntity: MediaGalleryEntity) {
        mediaGalleryView.removeMediaFromPager(mediaGalleryEntity)
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

    fun setActionButtonLabel(label: String) {
        actionButton.text = label
    }

    fun setCarousalActionListener(carousalActionListener: CarousalActionListener?) {
        Gallery.carousalActionListener = carousalActionListener
    }

    override fun onBackPressed() {
        closeIfHostingOnActivity()
        bridgeViewModel.onBackPressed()
    }

    private fun changeActionButtonState(state: Boolean) {
        Gallery.galleryConfig.galleryCommunicator?.onStepValidate(state)
        actionButton.isSelected = state
    }

    private fun showError(error: String) {
        view?.let { SnackbarUtils.show(it, error, Snackbar.LENGTH_LONG) }
    }

    private fun setUpWithOutTabLayout() {
        tabLayout.visibility = View.GONE
        mediaGalleryView.setImagesForPager(
            convertPhotoFileToMediaGallery(
                getPhotosFromArguments()
            )
        )
        PagerAdapter(
            childFragmentManager,
            listOf(
                PhotoGridFragment.getInstance(
                    getString(com.mediapicker.gallery.R.string.oss_title_tab_photo),
                    getPhotosFromArguments()
                )
            )
        ).apply {
            viewPager.adapter = this
        }
    }

    private fun openPage() {
        if (defaultPageToOpen is DefaultPage.PhotoPage) {
            viewPager.currentItem = 0
        } else {
            viewPager.currentItem = 1
        }
    }

    private fun onActionButtonClicked() {
        bridgeViewModel.complyRules()
    }

    private fun setUpWithTabLayout() {
        childView.findViewById<ViewPager>(R.id.viewPager).apply {
            PagerAdapter(
                childFragmentManager, listOf(
                    PhotoGridFragment.getInstance(
                        getString(com.mediapicker.gallery.R.string.oss_title_tab_photo),
                        getPhotosFromArguments()
                    ),
                    VideoGridFragment.getInstance(
                        getString(com.mediapicker.gallery.R.string.oss_title_tab_video),
                        getVideosFromArguments()
                    )
                )
            ).apply {
                childView.findViewById<ViewPager>(R.id.viewPager).adapter = this
            }
            childView.findViewById<TabLayout>(R.id.tabLayout).setupWithViewPager(this)
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

    companion object {
        fun getInstance(
            listOfSelectedPhotos: List<PhotoFile> = emptyList(),
            listOfSelectedVideos: List<VideoFile> = emptyList(),
            defaultPageType: DefaultPage = DefaultPage.PhotoPage
        ): PhotoCarousalFragment {
            return PhotoCarousalFragment().apply {
                this.arguments = Bundle().apply {
                    putSerializable(EXTRA_SELECTED_PHOTOS, listOfSelectedPhotos as Serializable)
                    putSerializable(EXTRA_SELECTED_VIDEOS, listOfSelectedVideos as Serializable)
                    putSerializable(EXTRA_DEFAULT_PAGE, defaultPageType)
                }
            }
        }
    }

    override fun onItemClicked(photoFile: PhotoFile, isSelected: Boolean) {
        if (isSelected) {
            if (Gallery.galleryConfig.showPreviewCarousal.addImage) {
                addMediaForPager(getMediaEntity(photoFile))
            }
        } else {
            if (Gallery.galleryConfig.showPreviewCarousal.addImage) {
                removeMediaFromPager(getMediaEntity(photoFile))
            }
        }
    }

    private fun getMediaEntity(photo: PhotoFile): MediaGalleryEntity {
        var path: String? = photo.fullPhotoUrl
        var isLocalImage = false
        if (!TextUtils.isEmpty(photo.path) && photo.path?.contains("/")!!) {
            path = photo.path
            isLocalImage = true
        }
        return MediaGalleryEntity(
            photo.path,
            photo.imageId,
            path,
            isLocalImage,
            GalleryViewMediaType.IMAGE
        )
    }

    private fun convertPhotoFileToMediaGallery(photoList: List<PhotoFile>): ArrayList<MediaGalleryEntity> {
        val mediaList = ArrayList<MediaGalleryEntity>()
        for (photo in photoList) {
            mediaList.add(getMediaEntity(photo))
        }
        return mediaList
    }

    override fun onPreviewItemsUpdated(listOfSelectedPhotos: List<PhotoFile>) {
        if (Gallery.galleryConfig.showPreviewCarousal.addImage) {
            mediaGalleryView.setImagesForPager(
                convertPhotoFileToMediaGallery(
                    listOfSelectedPhotos
                )
            )
        }
    }

    override fun onGalleryItemClick(mediaIndex: Int) {
        Gallery.carousalActionListener?.onGalleryImagePreview(
            mediaIndex,
            bridgeViewModel.getSelectedPhotos().size
        )
        MediaGalleryActivity.startActivityForResult(
            this, convertPhotoFileToMediaGallery(
                bridgeViewModel.getSelectedPhotos()
            ), mediaIndex, "", PHOTO_PREVIEW
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTO_PREVIEW && view != null) {
            var index = 0
            if (data != null) {
                val bundle = data.extras
                index = bundle!!.getInt("gallery_media_index", 0)
            }
            Gallery.carousalActionListener?.onGalleryImagePreviewClosed(
                index,
                bridgeViewModel.getSelectedPhotos().size
            )
            mediaGalleryView.setSelectedPhoto(index)
        }
    }
}
