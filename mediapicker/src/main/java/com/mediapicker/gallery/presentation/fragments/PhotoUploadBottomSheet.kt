package com.mediapicker.gallery.presentation.fragments

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.R
import com.mediapicker.gallery.domain.entity.Action
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.domain.entity.Status
import com.mediapicker.gallery.presentation.fragments.BaseFragment.Companion.EXTRA_DEFAULT_PAGE
import com.mediapicker.gallery.presentation.fragments.BaseFragment.Companion.EXTRA_SELECTED_PHOTOS
import com.mediapicker.gallery.presentation.fragments.BaseFragment.Companion.EXTRA_SELECTED_VIDEOS
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.utils.FileUtils
import com.mediapicker.gallery.presentation.utils.constructGalleryPermissionsRequest
import com.mediapicker.gallery.presentation.utils.galleryPermissions
import com.mediapicker.gallery.presentation.utils.getActivityScopedViewModel
import com.mediapicker.gallery.presentation.viewmodels.BridgeViewModel
import com.mediapicker.gallery.presentation.viewmodels.VideoFile
import permissions.dispatcher.ktx.PermissionsRequester
import java.io.Serializable

class PhotoUploadBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "PhotoUploadBottomSheet"

        fun getInstance(
            listOfSelectedPhotos: List<PhotoFile> = emptyList(),
            listOfSelectedVideos: List<VideoFile> = emptyList(),
            defaultPageType: DefaultPage = DefaultPage.PhotoPage
        ): PhotoUploadBottomSheet {
            return PhotoUploadBottomSheet().apply {
                this.arguments = Bundle().apply {
                    putSerializable(EXTRA_SELECTED_PHOTOS, listOfSelectedPhotos as Serializable)
                    putSerializable(EXTRA_SELECTED_VIDEOS, listOfSelectedVideos as Serializable)
                    putSerializable(EXTRA_DEFAULT_PAGE, defaultPageType)
                }
            }
        }
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

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionsRequester: PermissionsRequester
    private var lastRequestFileToSavePath = ""
    private var mediaPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null


    override fun getTheme() = R.style.OSS_BottomSheetDialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLaunchers(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.oss_bottom_sheet_photo_upload, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bridgeViewModel.setCurrentSelectedPhotos(getPhotosFromArguments())
        bridgeViewModel.setCurrentSelectedVideos(getVideosFromArguments())

        view.findViewById<View>(R.id.option_camera).setOnClickListener {
            if (isCameraPermissionGranted()) {
                startTakingPicture()
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        view.findViewById<View>(R.id.option_gallery).setOnClickListener {
            launchGalleryPicker()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        galleryPermissionsRequester = constructGalleryPermissionsRequest(
            permissions = galleryPermissions(),
            onPermissionDenied = ::onPermissionDenied,
            onNeverAskAgain = ::showNeverAskAgainPermission,
            requiresPermission = ::checkPermissions
        )
    }

    private fun onPermissionDenied() {
        Gallery.galleryConfig.galleryCommunicator?.onPermissionDenied()
    }

    private fun showNeverAskAgainPermission() {
        Gallery.galleryConfig.galleryCommunicator?.onNeverAskPermissionAgain()
    }

    private fun checkPermissions() {
        if (!isRemoving && isAdded) {
            startTakingPicture()
        }
    }

    private fun isCameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    @Suppress("UNCHECKED_CAST")
    private fun getPhotosFromArguments(): List<PhotoFile> {
        this.arguments?.let {
            if (it.containsKey(EXTRA_SELECTED_PHOTOS)) {
                return it.getSerializable(EXTRA_SELECTED_PHOTOS) as List<PhotoFile>
            }
        }
        return emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getVideosFromArguments(): List<VideoFile> {
        this.arguments?.let {
            if (it.containsKey(EXTRA_SELECTED_VIDEOS)) {
                return it.getSerializable(EXTRA_SELECTED_VIDEOS) as List<VideoFile>
            }
        }
        return emptyList()
    }

    private fun startTakingPicture() {
        val lastRequestFileToSave = FileUtils.getNewPhotoFileOnPicturesDirectory()
        val fileUri: Uri = if (android.text.TextUtils.isEmpty(Gallery.getClientAuthority())) {
            Uri.fromFile(lastRequestFileToSave)
        } else {
            FileProvider.getUriForFile(
                requireContext(),
                Gallery.getClientAuthority(),
                lastRequestFileToSave
            )
        }
        lastRequestFileToSavePath = lastRequestFileToSave.absolutePath
        takePictureLauncher.launch(fileUri)
    }

    private fun handleCameraResult(success: Boolean) {
        if (success && lastRequestFileToSavePath.isNotEmpty()) {
            insertIntoGallery()

            val newPhotoFile = getPhoto(lastRequestFileToSavePath)
            val allPhotos = bridgeViewModel.getSelectedPhotos() + newPhotoFile

            bridgeViewModel.setCurrentSelectedPhotos(allPhotos)
            bridgeViewModel.complyRules()

            dismiss()
        }
    }

    private fun launchGalleryPicker() {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        mediaPickerLauncher?.launch(request)
    }

    private fun registerLaunchers(fragment: Fragment) {
        val maxItems = Gallery.galleryConfig.validation.getMaxPhotoSelectionRule().maxSelectionLimit

        mediaPickerLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxItems)
        ) { uris -> handlePhotoPickerResult(uris) }

        takePictureLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success -> handleCameraResult(success) }

        cameraPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startTakingPicture()
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun copyUriToLocalFile(uri: Uri): PhotoFile? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = FileUtils.getNewPhotoFileOnPicturesDirectory()
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            PhotoFile.Builder()
                .path(file.absolutePath)
                .action(Action.ADD)
                .status(Status.PENDING)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun handlePhotoPickerResult(uris: List<Uri>) {
        dismiss()
        if (uris.isEmpty()) return

        val newPhotoFiles = uris.mapNotNull { uri -> copyUriToLocalFile(uri) }
        val allPhotos = bridgeViewModel.getSelectedPhotos() + newPhotoFiles

        if (allPhotos.isNotEmpty()) {
            bridgeViewModel.setCurrentSelectedPhotos(allPhotos)
            bridgeViewModel.complyRules()
        }
    }

    private fun getPhoto(path: String): PhotoFile {
        return PhotoFile.Builder()
            .imageId(0)
            .path(path)
            .smallPhotoUrl("")
            .fullPhotoUrl("")
            .photoBackendId(0L)
            .action(Action.ADD)
            .status(Status.PENDING)
            .build()
    }

    private fun insertIntoGallery() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                put(MediaStore.MediaColumns.DATA, lastRequestFileToSavePath)
            }
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            addImageIntoGalleryQAndAboveDevices(values)
        } else {
            addImageIntoGalleryBelowQDevices(values)
        }
    }

    private fun addImageIntoGalleryBelowQDevices(values: ContentValues) {
        requireContext().contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun addImageIntoGalleryQAndAboveDevices(values: ContentValues) {
        val collection =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        requireContext().contentResolver
            .insert(collection, values)
    }
}
