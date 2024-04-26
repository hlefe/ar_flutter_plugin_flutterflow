import android.media.Image
import java.nio.ShortBuffer

class DepthImgUtil {
    inner class DepthImgArrays(size: Int) {
        var xBuffer: ShortArray = ShortArray(size)
        var yBuffer: ShortArray = ShortArray(size)
        var dBuffer: FloatArray = FloatArray(size)
        var percentageBuffer: FloatArray = FloatArray(size)
        var length: Int = size
    }

    fun parseImg(depthImg: Image): DepthImgArrays {
        // Buffers for storing TOF output
        val arrays = DepthImgArrays(depthImg.width * depthImg.height)
        val plane = depthImg.planes[0]
        val shortDepthBuffer: ShortBuffer = plane.buffer.asShortBuffer()

        val stride = plane.rowStride
        var offset = 0
        var i = 0
        for (y in 0 until depthImg.height) {
            for (x in 0 until depthImg.width) {
                // Parse the data. Format is [depth|confidence]
                var depthSample =
                    shortDepthBuffer[y / 2 * stride + x].toInt() and 0xFFFF
                depthSample =
                    (depthSample and 0xFF shl 8 and 0xFF00) or ((depthSample and 0xFF00 shr 8) and 0xFF)
                val depthSampleShort = depthSample.toShort()
                val depthRange = (depthSampleShort.toInt() and 0x1FFF).toShort()
                val depthConfidence = ((depthSampleShort.toInt() shr 13) and 0x7).toShort()
                val depthPercentage = if (depthConfidence.toInt() == 0) 1f else (depthConfidence - 1) / 7f

                // Store data in buffer
                arrays.xBuffer[i] = x.toShort()
                arrays.yBuffer[i] = y.toShort()
                arrays.dBuffer[i] = depthRange / 1000.0f
                arrays.percentageBuffer[i] = depthPercentage
                i++
            }
            offset += depthImg.width
        }
        return arrays
    }
}
