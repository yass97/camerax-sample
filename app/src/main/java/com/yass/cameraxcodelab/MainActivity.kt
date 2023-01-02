package com.yass.cameraxcodelab

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.exifinterface.media.ExifInterface
import com.jakewharton.threetenabp.AndroidThreeTen
import com.yass.cameraxcodelab.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var outputDirectory: File

    private var cameraExecutor: ExecutorService? = null

    private var imageCapture: ImageCapture? = null

    private var orientationEventListener: OrientationEventListener? = null

    private var rotation: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        AndroidThreeTen.init(this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        setClickListeners()

        this.outputDirectory = getOutputDirectory()
    }

    private fun setClickListeners() {

        binding.apply {
            shutterButton.setOnClickListener { takePhoto() }
            importFromGallery.setOnClickListener { clickGallary() }
        }
    }

    private fun clickGallary() {

        startActivity(Intent(this, PhotoAlbumActivity::class.java))
    }

    override fun onResume() {
        super.onResume()

        this.orientationEventListener = object : OrientationEventListener(this) {

            override fun onOrientationChanged(orientation: Int) {

                val rotation = getRotationFromOrientation(orientation)

                this@MainActivity.rotation = rotation
            }
        }

        this.orientationEventListener?.enable()
    }

    private fun getRotationFromOrientation(orientation: Int): Int {

        return when {
            (orientation >= 315 || orientation < 45) -> Surface.ROTATION_0
            (orientation in 225..314) -> Surface.ROTATION_90
            (orientation in 135..224) -> Surface.ROTATION_180
            (orientation in 45..134) -> Surface.ROTATION_270
            else -> Surface.ROTATION_0
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_CODE_PERMISSIONS) return

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            showToast("Permissions not granted by the user.")
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        adjustPreviewView()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.cameraExecutor?.shutdown()
        this.orientationEventListener?.disable()
    }

    /**
     * 画面の縦のサイズを取得する
     * 画面下部のボタン領域の高さを取得する
     * 画面の縦のサイズから画面下部の領域の高さを引く
     * 引いた高さをPreviewViewに設定する
     */
    private fun adjustPreviewView() {

        // ツールバー領域を含む画面のサイズを取得
        val displaySize = getDisplaySize()

        // ボタン領域のサイズを取得する
        val takenPhotoLayoutSize = getViewSize(binding.takenPhotoLayout)

        // PreviewViewのHeightのサイズを取得する
        val previewViewHeight = (displaySize.y - takenPhotoLayoutSize.y)

        binding.viewFinder.layoutParams.height = previewViewHeight
    }

    private fun getViewSize(view: View): Point {

        val point = Point(0, 0)

        point.set(view.width, view.height)

        return point
    }

    private fun getDisplaySize(): Point {

        val display = windowManager.defaultDisplay

        val point = Point()

        display.getSize(point)

        return point
    }

    private fun getOutputDirectory(): File {

        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, getString(R.string.app_name)).apply { mkdirs() }
        }

        return if ((mediaDir != null) && mediaDir.exists()) mediaDir else filesDir
    }

    private fun takePhoto() {

        /**
         * 変更可能な画像キャプチャのユースケースの安定したリファレンスを取得します
         * まず、ImageCaptureユースケースへの参照を取得します。
         * ユースケースがnullの場合、関数を終了します。
         * 画像キャプチャが設定される前に写真ボタンをタップすると、これはnullになります。
         * returnステートメントがないと、nullの場合にアプリがクラッシュします。
         */
        val imageCapture = this.imageCapture ?: run {
            showToast("ImageCaptureの参照がnullです")
            return
        }

        imageCapture.targetRotation = this.rotation

        /**
         * ファイル+メタデータを含む出力オプションオブジェクトを作成します
         * OutputFileOptionsオブジェクトを作成します。
         * このオブジェクトは、出力をどのようにするかについて指定できる場所です。
         * 先ほど作成したファイルに出力を保存したいので、photoFileを追加します。
         */

//        val options = createOutputFileOptionsMediaStoreVer()

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val options = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val time = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                    )

                    val exif = ExifInterface(photoFile)

                    exif.setAttribute(ExifInterface.TAG_DATETIME, time)

                    exif.saveAttributes()

                    val date = exif.getAttribute(ExifInterface.TAG_DATETIME)

                    Log.d("MainActivity", "date-time: $date")
                }

                override fun onError(exception: ImageCaptureException) {
                    showToast("Photo capture failed.")
                }
            }
        )
    }

    private fun saveExifInterface(uri: Uri?) {

        uri ?: run {
            showToast("savedUriがnullやで")
            return
        }

        contentResolver.openFileDescriptor(uri, "rw").use {

            val fileDescriptor = it?.fileDescriptor ?: return

            val exif = ExifInterface(fileDescriptor)

            val orientation = getOrientationFromRotate(this.rotation)

            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())

            exif.saveAttributes()
        }
    }

    private fun createOutputFileOptionsMediaStoreVer(): ImageCapture.OutputFileOptions {

        val fileName = "${SimpleDateFormat(
            BitmapUtils.DATE_FORMAT,
            Locale.getDefault()
        ).format(Date())}${BitmapUtils.EXTENSION_JPEG}"

        val folderName = getString(R.string.app_name)

        val relativePath = "${Environment.DIRECTORY_PICTURES}/$folderName"

        val orientation = getRotationFromOrientation(this.rotation)

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
//            put(MediaStore.Images.Media.ORIENTATION, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            } else {

                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    folderName
                )

                if (!dir.exists()) {
                    dir.mkdir()
                }

                val file = File(dir, fileName)

                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
        }

        return ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
    }

    private fun getOrientationFromRotate(rotate: Int): Int {

        return when (rotate) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    private fun startCamera() {

        // これによりカメラを開閉するタスクが不要になる
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {

            // カメラのライフサイクルをライフサイクル所有者にバインドするために使用されます
            // アプリケーションのプロセス内で、カメラをライフサイクルにバインドさせるために使用される
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            this.imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {

                // 再バインドする前にユースケースのバインドを解除
                cameraProvider.unbindAll()

                // ユースケースをカメラにバインド
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Use case binding failed")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted(): Boolean {

        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showToast(message: String?) {

        val m = message ?: "メッセージの取得に失敗しました"

        Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}