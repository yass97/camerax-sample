package com.yass.cameraxcodelab

import android.content.Context
import android.util.AttributeSet
import androidx.camera.view.PreviewView

class AutoFitPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PreviewView(context, attrs, defStyleAttr) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    fun setAspectRatio(width: Int?, height: Int?) {

        width ?: return
        height ?: return

        if ((width < 0) || (height < 0)) return

        this.ratioWidth = width
        this.ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        when ((0 == ratioWidth) || (0 == ratioHeight)) {
            true -> setMeasuredDimension(width, height)
            false -> {
                when (width < height * ratioWidth / ratioHeight) {
                    true -> setMeasuredDimension(width, width * ratioHeight / ratioWidth)
                    false -> setMeasuredDimension(height * ratioWidth / ratioHeight, height)
                }
            }
        }
    }
}