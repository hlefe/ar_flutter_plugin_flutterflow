package com.uhg0.ar_flutter_plugin_2

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animation de la main pour guider l'utilisateur dans la détection des plans
 */
class HandMotionAnimation(
    private val containerView: View,
    private val handImageView: View
) : Animation() {

    companion object {
        private const val TWO_PI = PI.toFloat() * 2.0f
        private const val HALF_PI = PI.toFloat() / 2.0f
        private const val RADIUS_FACTOR = 20.0f
        private const val MARGIN_FACTOR = 1.2f
    }

    override fun applyTransformation(interpolatedTime: Float, transformation: Transformation) {
        val startAngle = HALF_PI
        val progressAngle = TWO_PI * interpolatedTime
        val currentAngle = startAngle + progressAngle

        val handWidth = handImageView.width
        val handHeight = handImageView.height
        
        val radius = (Math.min(containerView.width, containerView.height) / RADIUS_FACTOR) * MARGIN_FACTOR

        var xPos = radius * cos(currentAngle)
        var yPos = radius * sin(currentAngle)

        xPos += (containerView.width - handWidth) / 2.0f
        yPos += (containerView.height - handHeight) / 2.0f

        handImageView.x = xPos
        handImageView.y = yPos
        handImageView.invalidate()
    }
}
