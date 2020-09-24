package com.yass.cameraxcodelab

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.yass.cameraxcodelab.databinding.ActivityPhotoAlbumBinding
import java.io.File

class PhotoAlbumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoAlbumBinding

    private val photoAlbumAdapter: PhotoAlbumAdapter = PhotoAlbumAdapter()

    private var isRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_album)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo_album)

        binding.apply {
            lifecycleOwner = this@PhotoAlbumActivity
        }

        setUpRecyclerView()
    }

    override fun onResume() {
        super.onResume()

        this.isRunning = true

        Thread({ loadPhotoFromStrage() }).start()
    }

    override fun onPause() {
        super.onPause()

        this.isRunning = false
    }

    private fun loadPhotoFromStrage() {

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"
        )

        cursor ?: return

        try {

            val columnIndexId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext() && this.isRunning) {

                val imageId = cursor.getLong(columnIndexId)

                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    imageId.toString()
                )

                val file = getFileFromUri(this, uri) ?: return

                photoAlbumAdapter.addPhoto(file)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }
    }

    private fun setUpRecyclerView() {

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@PhotoAlbumActivity, 3)
            adapter = photoAlbumAdapter
        }
    }

    fun getFileFromUri(context: Context, uri: Uri): File? {

        val filePath = when {
            isContentScheme(uri) -> getDataColumn(context, uri, null, null)
            isFileScheme(uri) -> uri.path
            else -> null
        }

        return File(filePath ?: return null)
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {

        uri ?: return null

        val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            .use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                    if (columnIndex != -1) {
                        return cursor.getString(columnIndex)
                    }
                }
            }
        return null
    }

    private fun isFileScheme(uri: Uri): Boolean = (ContentResolver.SCHEME_FILE == uri.scheme)

    private fun isContentScheme(uri: Uri): Boolean = (ContentResolver.SCHEME_CONTENT == uri.scheme)
}