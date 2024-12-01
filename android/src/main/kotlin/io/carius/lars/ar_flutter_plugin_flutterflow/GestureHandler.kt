package io.carius.lars.ar_flutter_plugin_flutterflow

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import io.flutter.plugin.common.MethodChannel
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import kotlin.math.atan2

class GestureHandler(
    private val sceneView: ARSceneView,
    private val objectChannel: MethodChannel
) : View.OnTouchListener {

    private var selectedNode: ModelNode? = null
    private var isDragging = false
    private var isRotating = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var previousAngle = 0f

    private val gestureDetector = GestureDetector(sceneView.context, GestureListener())

    private fun serializeNodeTransformation(node: ModelNode): HashMap<String, Any> {
        val serializedTransformation = HashMap<String, Any>()
        serializedTransformation["name"] = node.name ?: ""

        val transformArray = DoubleArray(16)
        
        // Quaternion components
        val qx = node.worldQuaternion.x
        val qy = node.worldQuaternion.y
        val qz = node.worldQuaternion.z
        val qw = node.worldQuaternion.w

        // Calculate rotation matrix elements
        val xx = 1 - 2 * (qy * qy + qz * qz)
        val xy = 2 * (qx * qy - qz * qw)
        val xz = 2 * (qx * qz + qy * qw)
        
        val yx = 2 * (qx * qy + qz * qw)
        val yy = 1 - 2 * (qx * qx + qz * qz)
        val yz = 2 * (qy * qz - qx * qw)
        
        val zx = 2 * (qx * qz - qy * qw)
        val zy = 2 * (qy * qz + qx * qw)
        val zz = 1 - 2 * (qx * qx + qy * qy)

        // Combine rotation with scale
        transformArray[0] = (xx * node.worldScale.x).toDouble()
        transformArray[1] = (xy * node.worldScale.x).toDouble()
        transformArray[2] = (xz * node.worldScale.x).toDouble()
        transformArray[3] = 0.0
        
        transformArray[4] = (yx * node.worldScale.y).toDouble()
        transformArray[5] = (yy * node.worldScale.y).toDouble()
        transformArray[6] = (yz * node.worldScale.y).toDouble()
        transformArray[7] = 0.0
        
        transformArray[8] = (zx * node.worldScale.z).toDouble()
        transformArray[9] = (zy * node.worldScale.z).toDouble()
        transformArray[10] = (zz * node.worldScale.z).toDouble()
        transformArray[11] = 0.0

        // Translation
        transformArray[12] = node.worldPosition.x.toDouble()
        transformArray[13] = node.worldPosition.y.toDouble()
        transformArray[14] = node.worldPosition.z.toDouble()
        transformArray[15] = 1.0

        serializedTransformation["transform"] = transformArray
        return serializedTransformation
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        event?.let { motionEvent ->
            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (selectedNode == null) {
                        sceneView.collisionSystem.hitTest(xPx = motionEvent.x, yPx = motionEvent.y)
                            .firstOrNull { it.node is ModelNode }?.let {
                                selectedNode = it.node as ModelNode
                            }
                    }

                    if (motionEvent.pointerCount == 2) {
                        isRotating = true
                        isDragging = false
                        previousAngle = getTwoFingerAngle(motionEvent)
                        selectedNode?.let { node ->
                            objectChannel.invokeMethod("onRotationStart", node.name)
                        }
                    } else {
                        lastTouchX = motionEvent.x
                        lastTouchY = motionEvent.y
                    }
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (motionEvent.pointerCount == 2) {
                        isRotating = true
                        isDragging = false
                        previousAngle = getTwoFingerAngle(motionEvent)
                        selectedNode?.let { node ->
                            objectChannel.invokeMethod("onRotationStart", node.name)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isRotating && motionEvent.pointerCount == 2) {
                        handleRotation(motionEvent)
                    } else if (isDragging && !isRotating) {
                        handleDrag(motionEvent)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    if (isRotating) {
                        selectedNode?.let { node ->
                            val transformData = serializeNodeTransformation(node)
                            objectChannel.invokeMethod("onRotationEnd", transformData)
                        }
                        if (motionEvent.pointerCount <= 2) {
                            isRotating = false
                        }
                    } else if (isDragging) {
                        selectedNode?.let { node ->
                            val transformData = serializeNodeTransformation(node)
                            objectChannel.invokeMethod("onPanEnd", transformData)
                        }
                        isDragging = false
                    }
                    
                    if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                        selectedNode = null
                        isRotating = false
                        isDragging = false
                    }
                    true
                }
                else -> false
            }
        } ?: return false

        return gestureDetector.onTouchEvent(event)
    }

    private fun handleRotation(event: MotionEvent) {
        val currentAngle = getTwoFingerAngle(event)
        val angleDiff = currentAngle - previousAngle
        selectedNode?.let { node ->
            node.rotation *= io.github.sceneview.math.Rotation(y = angleDiff)
            objectChannel.invokeMethod("onRotationChange", node.name)
        }
        previousAngle = currentAngle
    }

    private fun handleDrag(event: MotionEvent) {
        val deltaX = event.x - lastTouchX
        val deltaY = event.y - lastTouchY
        
        selectedNode?.let { node ->
            val hitResult = sceneView.collisionSystem.hitTest(xPx = event.x, yPx = event.y).firstOrNull()
            hitResult?.let { hit ->
                node.worldPosition = hit.worldPosition
            }
            objectChannel.invokeMethod("onPanChange", node.name)
        }
        
        lastTouchX = event.x
        lastTouchY = event.y
    }

    private fun getTwoFingerAngle(event: MotionEvent): Float {
        val (finger1X, finger1Y) = event.getX(0) to event.getY(0)
        val (finger2X, finger2Y) = event.getX(1) to event.getY(1)
        return Math.toDegrees(atan2((finger2Y - finger1Y).toDouble(), (finger2X - finger1X).toDouble())).toFloat()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            if (!isRotating && selectedNode != null) {
                isDragging = true
                objectChannel.invokeMethod("onPanStart", selectedNode?.name)
            }
        }
    }
} 