package com.mediapicker.gallery.presentation.utils

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import permissions.dispatcher.PermissionRequest

import permissions.dispatcher.ktx.PermissionsRequester

import permissions.dispatcher.ktx.constructPermissionsRequest


fun Fragment.galleryPermissions(): Array<String> {
    val permissions = mutableListOf(CAMERA) //Camera Permission
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            if (isPermissionGranted(READ_MEDIA_IMAGES) && isPermissionGranted(READ_MEDIA_VIDEO)) {
                //app has full access of media, we dont need to ask for media permission
            } else if (isPermissionGranted(READ_MEDIA_VISUAL_USER_SELECTED)) {
                //app has partial access of media, we dont need to ask for media permission
            } else {
                permissions.add(READ_MEDIA_IMAGES)
                permissions.add(READ_MEDIA_VIDEO)
                permissions.add(READ_MEDIA_VISUAL_USER_SELECTED)
            }
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            permissions.add(READ_MEDIA_IMAGES)
            permissions.add(READ_MEDIA_VIDEO)
        }

        Build.VERSION.SDK_INT > Build.VERSION_CODES.Q -> {
            permissions.add(READ_EXTERNAL_STORAGE)
        }

        else -> {
            permissions.add(READ_EXTERNAL_STORAGE)
            permissions.add(WRITE_EXTERNAL_STORAGE)
        }
    }
    return permissions.toTypedArray()
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun Fragment.mediaPermission() =
    arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED)

fun isAtLeast34Api() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

fun Fragment.isPermissionGranted(name: String) = ContextCompat.checkSelfPermission(
    requireContext(), name
) == PackageManager.PERMISSION_GRANTED

fun Fragment.constructGalleryPermissionsRequest(
    vararg permissions: String,
    onShowRationale: ShowRationaleFun? = null,
    onPermissionDenied: Fun? = null,
    onNeverAskAgain: Fun? = null,
    requiresPermission: Fun
): PermissionsRequester = constructPermissionsRequest(
    permissions = permissions,
    onShowRationale = ShowRationaleFunWrapper(onShowRationale, requiresPermission),
    onPermissionDenied = FunWrapper(onPermissionDenied, requiresPermission),
    onNeverAskAgain = FunWrapper(onNeverAskAgain, requiresPermission),
    requiresPermission = requiresPermission,
)

private fun Fragment.FunWrapper(action: Fun?, requiresPermission: Fun): Fun = {
    if (isAtLeast34Api() && hasMediaPermission()) {
        requiresPermission.invoke()
    } else {
        action?.invoke()
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun Fragment.hasMediaPermission() =
    ((isPermissionGranted(READ_MEDIA_IMAGES)
            && isPermissionGranted(READ_MEDIA_VIDEO))
            || isPermissionGranted(READ_MEDIA_VISUAL_USER_SELECTED))

private fun Fragment.ShowRationaleFunWrapper(
    action: ShowRationaleFun?,
    requiresPermission: Fun
): ShowRationaleFun {
    return { permissions ->
        if (isAtLeast34Api() && hasMediaPermission()) {
            requiresPermission.invoke()
        } else {
            action?.invoke(permissions)
        }
    }
}


interface MediaPermissionRequest{
    fun launch()
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class AtLeastApi34MediaPermissionRequest (
    private val fragment: Fragment,
    private val permissions: Array<String>,
    private val onPermissionDenied: Fun? = null,
    private val requiresPermission: Fun
) : MediaPermissionRequest{
    private val requestPermissionLauncher =
        fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (fragment.hasMediaPermission()) {
                requiresPermission()
            } else {
                onPermissionDenied?.invoke()
            }

        }

    override fun launch() {
        requestPermissionLauncher.launch(permissions)
    }
}

class BelowApi34MediaPermissionRequest : MediaPermissionRequest {
    override fun launch() {}
}

fun Fragment.constructMediaPermissionsRequest(
    onPermissionDenied: Fun? = null,
    onPermissionGranted: Fun
): MediaPermissionRequest {
    return if (isAtLeast34Api()) {
        AtLeastApi34MediaPermissionRequest(
            this,
            permissions = mediaPermission(),
            onPermissionDenied,
            onPermissionGranted
        )
    } else {
        BelowApi34MediaPermissionRequest()
    }
}

internal typealias Fun = () -> Unit
internal typealias ShowRationaleFun = (PermissionRequest) -> Unit



