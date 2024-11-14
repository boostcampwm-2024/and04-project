package com.and04.naturealbum.ui.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import coil3.load
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.and04.naturealbum.R
import java.io.File
import java.io.IOException
import java.io.InputStream


class ImageMarkerCoil @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView

    init {
        Log.d("ImageMarkerCoil", "ImageMarkerCoil initialized")
        LayoutInflater.from(context).inflate(R.layout.image_marker, this, true)
        imageView = findViewById(R.id.iv_marker_image)
    }

    fun loadImage(uri: String, onImageLoaded: () -> Unit) {
        imageView.load(Uri.parse(uri)) {
            crossfade(true)
            placeholder(R.drawable.cat_dummy)
            error(R.drawable.ic_launcher_background)
            allowHardware(false)
            listener(
                onStart = {
                    Log.d("ImageMarkerCoil", "Coil image loading started.")
                },
                onSuccess = { _, _ ->
                    Log.d("ImageMarkerCoil", "Coil image loading success.")
                    onImageLoaded()  // 이미지 로딩 완료 후 콜백 호출
                },
                onError = { _, throwable ->
                    Log.e("ImageMarkerCoil", "Coil image loading failed: ${throwable.throwable}")
                }
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d("ImageMarkerCoil", "ImageMarkerCoil attached to window.")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d("ImageMarkerCoil", "ImageMarkerCoil detached from window.")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d("ImageMarkerCoil", "ImageMarkerCoil onMeasure called.")
    }
}


class ImageMarkerFromLocalFileBitmap @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.image_marker, this, true)
        imageView = findViewById(R.id.iv_marker_image)
    }

    fun loadImage(uri: String) {
        // 파일 경로 처리
        val bitmap: Bitmap? = if (uri.startsWith("file://")) {
            // 파일 경로에서 비트맵 로드
            loadImageFromFile(Uri.parse(uri), context)
        } else {
            // URI로부터 비트맵 로드
            loadImageFromUri(Uri.parse(uri), context)
        }

        bitmap?.let {
            imageView.setImageBitmap(it)
        } ?: run {
            // 에러 처리: 이미지가 없으면 기본 이미지 사용
            imageView.setImageResource(R.drawable.cat_dummy)
        }
    }

    private fun loadImageFromFile(uri: Uri, context: Context): Bitmap? {
        return try {
            val file = File(uri.path)
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: IOException) {
            Log.e("ImageMarker", "Error loading image from file: ${e.message}")
            null
        }
    }

    private fun loadImageFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            Log.e("ImageMarker", "Error loading image from URI: ${e.message}")
            null
        }
    }
}
