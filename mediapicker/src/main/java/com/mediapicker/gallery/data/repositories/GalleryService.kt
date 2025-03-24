package com.mediapicker.gallery.data.repositories

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.domain.repositories.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class GalleryService(private val applicationContext: Context) : GalleryRepository {

    companion object {
        fun getInstance(context: Application) = GalleryService(context)
        const val COL_FULL_PHOTO_URL = "fullPhotoUrl"
    }


    @Throws(IllegalArgumentException::class)
    override suspend fun getAlbums(): HashSet<PhotoAlbum> = withContext(Dispatchers.IO) {
        queryMedia()
    }


    private fun queryMedia(): HashSet<PhotoAlbum> {
        val mutableListOfFolders = hashSetOf<PhotoAlbum>()
        val selection = MediaStore.Images.Media.MIME_TYPE + "!=?"
        val mimeTypeGif = MimeTypeMap.getSingleton().getMimeTypeFromExtension("gif")
        val selectionTypeGifArgs = arrayOf(mimeTypeGif)

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val cursor = applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionTypeGifArgs,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val album = getAlbumEntry(it)
                    album?.let { item ->
                        val photo = getPhoto(it)
                        if (mutableListOfFolders.contains(item)) {
                            mutableListOfFolders.forEach { folder ->
                                if (folder == item) {
                                    folder.addEntryToAlbum(photo)
                                }
                            }
                        } else {
                            item.addEntryToAlbum(photo)
                            mutableListOfFolders.add(item)
                        }
                    }
                } while (it.moveToNext())
            }
        }

        return mutableListOfFolders
    }

    private fun getAlbumEntry(cursor: Cursor): PhotoAlbum? {
        val albumIdIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)

        val albumNameIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        if (albumIdIndex != -1 && albumNameIndex != -1) {
            val id = cursor.getInt(albumIdIndex)
            val name = cursor.getString(albumNameIndex)
            return PhotoAlbum(id.toString(), name)
        } else {
            return null
        }

    }

    private fun getPhoto(cursor: Cursor): PhotoFile {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
        val col = cursor.getColumnIndex(COL_FULL_PHOTO_URL)
        var fullPhotoUrl = ""
        if (col != -1) {
            fullPhotoUrl = cursor.getString(col)
        }
        return PhotoFile.Builder()
            .imageId(id)
            .path(path)
            .smallPhotoUrl("")
            .fullPhotoUrl(fullPhotoUrl)
            .photoBackendId(0L)
            .mimeType(mimeType)
            .build()
    }

}
