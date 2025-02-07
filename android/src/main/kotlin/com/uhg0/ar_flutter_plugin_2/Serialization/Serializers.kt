package com.uhg0.ar_flutter_plugin_2.Serialization

import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import io.github.sceneview.math.Position as ScenePosition
import io.github.sceneview.math.Rotation as SceneRotation
import kotlin.math.sqrt

fun serializeHitResult(hit: Map<String, Any>): HashMap<String, Any> {
    val serializedHit = HashMap<String, Any>()
    
    serializedHit["type"] = hit["type"] as Int
    serializedHit["distance"] = hit["distance"] as Double
    
    val position = hit["position"] as Map<String, Double>
    val pose = Pose(
        floatArrayOf(
            position["x"]?.toFloat() ?: 0f,
            position["y"]?.toFloat() ?: 0f,
            position["z"]?.toFloat() ?: 0f
        ),
        floatArrayOf(0f, 0f, 0f, 1f)
    )
    
    serializedHit["worldTransform"] = serializePose(pose)
    return serializedHit
}

fun serializePose(pose: Pose): DoubleArray {
    val serializedPose = FloatArray(16)
    pose.toMatrix(serializedPose, 0)
    return DoubleArray(16) { serializedPose[it].toDouble() }
} 