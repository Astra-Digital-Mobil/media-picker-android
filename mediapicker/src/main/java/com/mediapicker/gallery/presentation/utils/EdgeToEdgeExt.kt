package com.mediapicker.gallery.presentation.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding

private fun View.installCompatInsetsDispatchSafely() {
    if (isAttachedToWindow) {
        (rootView as? ViewGroup)?.let(ViewGroupCompat::installCompatInsetsDispatch)
    } else {
        doOnAttach { (it.rootView as? ViewGroup)?.let(ViewGroupCompat::installCompatInsetsDispatch) }
    }
}

fun View.applyToolbarInset() {
    installCompatInsetsDispatchSafely()
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            left = systemBars.left,
            right = systemBars.right,
            top = systemBars.top
        )
        WindowInsetsCompat.CONSUMED
    }
}


@JvmOverloads
fun View.applyContentInset(includeTop: Boolean = false, includeBottom: Boolean = true) {
    installCompatInsetsDispatchSafely()
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        v.updatePadding(
            left = systemBars.left,
            top = if (includeTop) systemBars.top else 0,
            right = systemBars.right,
            bottom = if (includeBottom) maxOf(systemBars.bottom, imeBottom) else 0,
        )
        WindowInsetsCompat.CONSUMED
    }
}
