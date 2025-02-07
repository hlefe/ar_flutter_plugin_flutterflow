package com.uhg0.ar_flutter_plugin_2

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * Vue contenant l'animation de la main pour guider l'utilisateur
 */
class HandMotionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var animation: HandMotionAnimation? = null

    companion object {
        private const val ANIMATION_SPEED_MS = 2500L
        private const val ANIMATION_START_DELAY = 1000L
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        // Arrêter toute animation précédente
        clearAnimation()

        // Trouver le conteneur parent
        val container = parent as? FrameLayout ?: return

        // Créer et démarrer la nouvelle animation
        animation = HandMotionAnimation(container, this).apply {
            repeatCount = Animation.INFINITE
            duration = ANIMATION_SPEED_MS
            startOffset = ANIMATION_START_DELAY
        }

        // Démarrer l'animation
        startAnimation(animation)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearAnimation()
        animation = null
    }
}
