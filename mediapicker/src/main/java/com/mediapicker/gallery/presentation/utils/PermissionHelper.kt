package com.mediapicker.gallery.presentation.utils

import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


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

