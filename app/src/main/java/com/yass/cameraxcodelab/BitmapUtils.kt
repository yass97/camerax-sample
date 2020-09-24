package com.yass.cameraxcodelab

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

object BitmapUtils {

    private const val ORIGINAL_SIZE = 1
    private const val LONG_SIDE_INDEX = 0
    private const val SHORT_SIDE_INDEX = 1
    private const val MAX_SIZE_HEIGHT = 1920.0f
    private const val MAX_SIZE_WIDTH = 1080.0f
    private const val FILE_OPEN_MODE_READ = "r"
    const val DATE_FORMAT = "yyyyMMdd_HHmmss"
    const val EXTENSION_JPEG = ".jpg"
    const val MIME_TYPE_IMAGE_JPEG = "image/jpeg"

    fun getFileFromUri(context: Context, byteArray: ByteArray): File? {

        val appName = context.getString(R.string.app_name)

        val relativePath = "${Environment.DIRECTORY_PICTURES}/$appName"

        val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(relativePath)
        } else {
            File("${Environment.getExternalStorageDirectory().path}/${appName}/")
        }

        if (!dir.exists()) {
            dir.mkdir()
        }

        val fileName = "${SimpleDateFormat(
            DATE_FORMAT,
            Locale.getDefault()
        ).format(Date())}$EXTENSION_JPEG"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_IMAGE_JPEG)
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            } else {
                val attachName = "${dir.absolutePath}/${fileName}/$EXTENSION_JPEG"
                put(MediaStore.Images.Media.DATA, attachName)
            }
        }

        val resolver = context.contentResolver

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        resolver.openOutputStream(uri).use {
            it?.write(byteArray)
        }

        return File("")
    }

    fun savePictureTaken(context: Context, data: ByteArray): Uri? {

        val folderName = context.getString(R.string.app_name)

        val file = File("${Environment.getExternalStorageDirectory().path}/${folderName}/")
        if (!file.exists()) {
            file.mkdir()
        }

        val fileName = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
        val attachName = "${file.absolutePath}/${fileName}$EXTENSION_JPEG"

        var os: OutputStream? = null
        try {
            os = FileOutputStream(attachName)
            os.write(data)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_IMAGE_JPEG)
                put(MediaStore.Images.Media.TITLE, fileName)
                put(MediaStore.Images.Media.DATA, attachName)
            }

            return context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: return null

        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                os?.close()
            } catch (ignored: IOException) {
                ignored.printStackTrace()
            }
        }

        return null
    }

    fun createImageTempFile(context: Context, filePath: String, fileName: String): File? {

        val bitmap = getResizedBitmap(filePath) ?: return null
        val tempFile = File(context.cacheDir, fileName)

        FileOutputStream(tempFile).use {
            bitmap.apply {
                compress(Bitmap.CompressFormat.JPEG, 100, it)
                recycle()
            }
        }

        return tempFile
    }

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {

        val percelFileDescriptor =
            context.contentResolver?.openFileDescriptor(uri, FILE_OPEN_MODE_READ)

        val fileDescriptor = percelFileDescriptor?.fileDescriptor

        val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)

        percelFileDescriptor?.close()

        return bitmap
    }

    private fun calculateLoadSizeOfImage(
        longSideOfOriginalImage: Float,
        shortSideOfOriginalImage: Float
    ): Int {

        if (MAX_SIZE_HEIGHT <= longSideOfOriginalImage) {

            return (longSideOfOriginalImage / MAX_SIZE_HEIGHT).roundToInt()

        } else {

            if (MAX_SIZE_WIDTH <= shortSideOfOriginalImage) {
                return (shortSideOfOriginalImage / MAX_SIZE_WIDTH).roundToInt()
            }
        }

        return ORIGINAL_SIZE
    }

    private fun getResizedHeightAndWidth(height: Float, width: Float): FloatArray {

        val imageSizes = floatArrayOf(height, width)

        if (MAX_SIZE_HEIGHT <= height) {

            imageSizes[LONG_SIDE_INDEX] = MAX_SIZE_HEIGHT
            imageSizes[SHORT_SIDE_INDEX] = MAX_SIZE_HEIGHT * (width / height)

        } else {

            if (MAX_SIZE_WIDTH <= width) {

                imageSizes[SHORT_SIDE_INDEX] = MAX_SIZE_WIDTH
                imageSizes[LONG_SIDE_INDEX] = MAX_SIZE_WIDTH * (height / width)
            }
        }

        return imageSizes
    }

    private fun generateBitmapFromPath(filePath: String): Bitmap? {

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(filePath, options)

        val width = options.outWidth
        val height = options.outHeight

        options.inSampleSize = when (width < height) {
            true -> calculateLoadSizeOfImage(height.toFloat(), width.toFloat())
            false -> calculateLoadSizeOfImage(width.toFloat(), height.toFloat())
        }

        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun getResizedBitmap(filePath: String): Bitmap? {

        val bitmap = generateBitmapFromPath(filePath) ?: return null

        val oldWidth = bitmap.width
        val oldHeight = bitmap.height
        val newWidth: Float
        val newHeight: Float
        val newSize: FloatArray

        when (oldWidth < oldHeight) {
            true -> {
                newSize = getResizedHeightAndWidth(oldHeight.toFloat(), oldWidth.toFloat())
                newHeight = newSize[LONG_SIDE_INDEX]
                newWidth = newSize[SHORT_SIDE_INDEX]
            }
            false -> {
                newSize = getResizedHeightAndWidth(oldWidth.toFloat(), oldHeight.toFloat())
                newHeight = newSize[SHORT_SIDE_INDEX]
                newWidth = newSize[LONG_SIDE_INDEX]
            }
        }

        val scaleWidth = (newWidth / oldWidth)
        val scaleHeight = (newHeight / oldHeight)
        val scaleFactor = scaleWidth.coerceAtMost(scaleHeight)

        val exif: ExifInterface
        try {
            exif = ExifInterface(filePath)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        var orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        if (orientation == 0) {
            orientation = ExifInterface.ORIENTATION_NORMAL
        }

        val scale = getMatrixFromExifOrientation(orientation).apply {
            postScale(scaleFactor, scaleFactor)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, oldWidth, oldHeight, scale, false)
    }

    private fun getMatrixFromExifOrientation(orientation: Int): Matrix {

        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1.0f, -1.0f)
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90.0f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.apply {
                    postRotate(-90.0f)
                    postScale(1.0f, -1.0f)
                }
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.apply {
                    postRotate(90.0f)
                    postScale(1.0f, -1.0f)
                }
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90.0f)
        }

        return matrix
    }
}
