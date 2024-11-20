package io.carius.lars.ar_flutter_plugin_flutterflow

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import com.google.ar.core.Config
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import io.github.sceneview.ar.ARSceneView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import com.google.ar.core.Anchor
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.node.HitResultNode
import android.graphics.Bitmap
import android.view.PixelCopy
import com.google.ar.core.Pose
import kotlinx.coroutines.withContext
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import kotlinx.coroutines.Job
import android.os.Handler
import android.os.Looper
import com.google.ar.core.Anchor.CloudAnchorState
import io.github.sceneview.ar.node.CloudAnchorNode
import io.github.sceneview.ar.arcore.canHostCloudAnchor
import com.google.ar.core.TrackingState
import com.google.ar.core.Plane
import io.github.sceneview.node.Node
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.RotateGestureDetector
import io.github.sceneview.gesture.ScaleGestureDetector
import android.view.MotionEvent
import io.github.sceneview.Scene
import com.google.ar.core.HitResult
import android.view.GestureDetector
import kotlin.math.sqrt
import io.github.sceneview.math.Position as ScenePosition
import io.github.sceneview.math.Rotation as SceneRotation
import io.github.sceneview.math.Scale as SceneScale
import io.github.sceneview.math.Transform
import io.carius.lars.ar_flutter_plugin_flutterflow.Serialization.serializeHitResult
import io.carius.lars.ar_flutter_plugin_flutterflow.Serialization.deserializeMatrix4
import io.github.sceneview.math.Position
import io.github.sceneview.texture.ImageTexture
import io.github.sceneview.material.setTexture
import io.github.sceneview.ar.scene.PlaneRenderer
import io.flutter.FlutterInjector

class ArView(
    context: Context,
    private val activity: Activity,
    lifecycle: Lifecycle,
    messenger: BinaryMessenger,
    id: Int,
) : PlatformView, MethodChannel.MethodCallHandler {
    private val TAG = "ArView"
    private var sceneView: ARSceneView
    private val mainScope = CoroutineScope(Dispatchers.Main)
    
    private val sessionChannel = MethodChannel(messenger, "arsession_$id")
    private val objectChannel = MethodChannel(messenger, "arobjects_$id")
    private val anchorChannel = MethodChannel(messenger, "aranchors_$id")
    private val nodesMap = mutableMapOf<String, ModelNode>()
    private var planeCount = 0
    private var selectedNode: Node? = null
    private val detectedPlanes = mutableSetOf<Plane>()
    private val anchorNodesMap = mutableMapOf<String, AnchorNode>()

    init {
        sceneView = ARSceneView(context, sharedLifecycle = lifecycle).apply {
            configureSession { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.focusMode = Config.FocusMode.AUTO
            }
             environment = environmentLoader.createHDREnvironment(
                    assetFileLocation = "environments/evening_meadow_2k.hdr"
                )!!
            onTrackingFailureChanged = { reason ->
                mainScope.launch {
                    sessionChannel.invokeMethod("onTrackingFailure", reason?.name)
                }
            }
            onFrame = { frameTime ->
                session?.update()?.let { frame ->
                    frame.getUpdatedTrackables(Plane::class.java).forEach { plane ->
                        if (plane.trackingState == TrackingState.TRACKING && 
                            !detectedPlanes.contains(plane)) {
                            detectedPlanes.add(plane)
                            mainScope.launch {
                                sessionChannel.invokeMethod("onPlaneDetected", detectedPlanes.size)
                            }
                        }
                    }
                }
            }
            setOnGestureListener(
                onSingleTapConfirmed = { motionEvent: MotionEvent, node: Node? ->
                    if (node != null) {
                        // Tap sur un node
                        //objectChannel.invokeMethod("onNodeTap", listOf(node.name))
                        true
                    } else {
                        // Tap sur un plan
                        session?.update()?.let { frame ->
                            val hitResults = frame.hitTest(motionEvent)
                            
                            // Debug des hit results
                            Log.d("ArView", "Hit Results count: ${hitResults.size}")
                            
                            val planeHits = hitResults.filter { hit ->
                                val trackable = hit.trackable
                                trackable is Plane && trackable.trackingState == TrackingState.TRACKING
                            }.map { hit ->
                                mapOf(
                                    "type" to 1,
                                    "distance" to hit.distance.toDouble(),
                                    "position" to mapOf(
                                        "x" to hit.hitPose.tx().toDouble(),
                                        "y" to hit.hitPose.ty().toDouble(),
                                        "z" to hit.hitPose.tz().toDouble()
                                    )
                                )
                            }
                            notifyPlaneOrPointTap(planeHits)
                        }
                        true
                    }
                }
            )
        }

        sceneView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        sceneView.keepScreenOn = true
        
        sessionChannel.setMethodCallHandler(this)
        objectChannel.setMethodCallHandler(this)
        anchorChannel.setMethodCallHandler(this)
    }

    private suspend fun buildModelNode(nodeData: Map<String, Any>): ModelNode? {
        val fileLocation = nodeData["uri"] as? String
        if (fileLocation == null) {
            return null
        }
        val transformation = nodeData["transformation"] as? ArrayList<Double>
        if (transformation == null) {
            return null
        }
        //val transform = deserializeMatrix4(transformation)
        
        return try {
            sceneView.modelLoader.loadModelInstance(fileLocation)?.let { modelInstance ->
                return ModelNode(
                    modelInstance = modelInstance,
                    
                    scaleToUnits = transformation.first().toFloat(),
                    
                ).apply {
                    isEditable = true
                     isPositionEditable = true
                isRotationEditable = true
                isScaleEditable = true
                }
            } ?: run {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun handleAddNodeToPlaneAnchor(call: MethodCall, result: MethodChannel.Result) {
        try {
            val nodeData = call.arguments as? Map<String, Any>
            val dict_node = nodeData?.get("node") as? Map<String, Any>
            val dict_anchor = nodeData?.get("anchor") as? Map<String, Any>
            if (dict_node == null || dict_anchor == null) {
                result.error("INVALID_ARGUMENT", "Node or anchor data is null", null)
                return
            }

            val anchorName = dict_anchor["name"] as? String
            val anchorNode = anchorNodesMap[anchorName]
            if (anchorNode != null) {
                sceneView.addChildNode(
                    anchorNode.apply {
                        isEditable = true
                        mainScope.launch {
                            buildModelNode(dict_node)?.let { 
                                addChildNode(it)
                            } 
                        }
                    } 
                )
            } else {
                Log.e(TAG, "❌ Erreur: AnchorNode non trouvé pour le nom: $anchorName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception dans handleAddNodeToPlaneAnchor: ${e.message}")
            e.printStackTrace()
            result.error("ADD_NODE_TO_PLANE_ERROR", e.message, null)
        }
    }

    private fun handleAddNodeToScreenPosition(call: MethodCall, result: MethodChannel.Result) {
        try {
            val nodeData = call.arguments as? Map<String, Any>
            val screenPosition = call.argument<Map<String, Double>>("screenPosition")
            
            if (nodeData == null || screenPosition == null) {
                result.error("INVALID_ARGUMENT", "Node data or screen position is null", null)
                return
            }

            mainScope.launch {
                val node = buildModelNode(nodeData) ?: return@launch
                val hitResultNode = HitResultNode(
                    engine = sceneView.engine,
                    xPx = screenPosition["x"]?.toFloat() ?: 0f,
                    yPx = screenPosition["y"]?.toFloat() ?: 0f
                ).apply {
                    addChildNode(node)
                }
                
                sceneView.addChildNode(hitResultNode)
                result.success(null)
            }
        } catch (e: Exception) {
            result.error("ADD_NODE_TO_SCREEN_ERROR", e.message, null)
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "init" -> {
                try {
                    Log.i(TAG, "init c'est vraiment bon")
                    val showAnimatedGuide = call.argument<Boolean>("showAnimatedGuide") ?: true
                    val showFeaturePoints = call.argument<Boolean>("showFeaturePoints") ?: false
                    val showPlanes = call.argument<Boolean>("showPlanes") ?: true
                    val customPlaneTexturePath = call.argument<String>("customPlaneTexturePath")
                    val showWorldOrigin = call.argument<Boolean>("showWorldOrigin") ?: false
                    val handleTaps = call.argument<Boolean>("handleTaps") ?: true
                    val handlePans = call.argument<Boolean>("handlePans") ?: false
                    val handleRotation = call.argument<Boolean>("handleRotation") ?: false
                    sceneView.apply {
                                    planeRenderer.isEnabled = true
            planeRenderer.isVisible = true

            if (customPlaneTexturePath != null) {
                try {
                    val loader = FlutterInjector.instance().flutterLoader()
                    val assetKey = loader.getLookupKeyForAsset(customPlaneTexturePath)
                    val customPlaneTexture = ImageTexture.Builder()
                        .bitmap(materialLoader.assets, assetKey)
                        .build(engine)
                    planeRenderer.planeMaterial.defaultInstance.apply {
                        setTexture(PlaneRenderer.MATERIAL_TEXTURE, customPlaneTexture)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erreur lors de l'application de la texture personnalisée: ${e.message}")
                    Log.e(TAG, "Stack trace:", e)
                }
            } else {
                Log.i(TAG, "ℹ️ Utilisation de la texture par défaut")
            }
                        
                    }
                    result.success(null)
                } catch (e: Exception) {
                    result.error("AR_VIEW_ERROR", e.message, null)
                }
            }
            "addNode" -> {
                try {
                    val nodeData = call.arguments as? Map<String, Any>
                    if (nodeData == null) {
                        result.error("INVALID_ARGUMENT", "Node data is null", null)
                        return
                    }

                    mainScope.launch {
                        val node = buildModelNode(nodeData)
                        if (node != null) {
                            sceneView.addChildNode(node)
                            result.success(null)
                        } else {
                            result.error("NODE_CREATION_FAILED", "Failed to create node", null)
                        }
                    }
                } catch (e: Exception) {
                    result.error("ADD_NODE_ERROR", e.message, null)
                }
            }
            "removeNode" -> {
                try {
                    val nodeId = call.argument<String>("nodeId")
                    if (nodeId == null) {
                        result.error("INVALID_ARGUMENT", "Node ID is required", null)
                        return
                    }

                    nodesMap[nodeId]?.let { node ->
                        sceneView.removeChildNode(node)
                        nodesMap.remove(nodeId)
                        result.success(null)
                    } ?: result.error("NODE_NOT_FOUND", "Node with ID $nodeId not found", null)
                } catch (e: Exception) {
                    result.error("REMOVE_NODE_ERROR", e.message, null)
                }
            }
            "transformNode" -> {
                try {
                    val nodeId = call.argument<String>("nodeId")
                    val position = call.argument<Map<String, Double>>("position")
                    val rotation = call.argument<Map<String, Double>>("rotation")
                    val scale = call.argument<Double>("scale")

                    if (nodeId == null) {
                        result.error("INVALID_ARGUMENT", "Node ID is required", null)
                        return
                    }

                    nodesMap[nodeId]?.let { node ->
                        if (node is ModelNode) {
                            val newPosition = position?.let {
                                ScenePosition(
                                    x = (it["x"] ?: 0.0).toFloat(),
                                    y = (it["y"] ?: 0.0).toFloat(),
                                    z = (it["z"] ?: 0.0).toFloat()
                                )
                            }

                            val newRotation = rotation?.let {
                                SceneRotation(
                                    x = (it["x"] ?: 0.0).toFloat(),
                                    y = (it["y"] ?: 0.0).toFloat(),
                                    z = (it["z"] ?: 0.0).toFloat()
                                )
                            }

                            val newScale = scale?.let {
                                SceneScale(it.toFloat(), it.toFloat(), it.toFloat())
                            }

                            node.transform(
                                position = newPosition ?: node.position,
                                rotation = newRotation ?: node.rotation,
                                scale = newScale ?: node.scale
                            )
                            result.success(null)
                        } else {
                            result.error("INVALID_NODE_TYPE", "Node is not a ModelNode", null)
                        }
                    } ?: result.error("NODE_NOT_FOUND", "Node with ID $nodeId not found", null)
                } catch (e: Exception) {
                    result.error("TRANSFORM_NODE_ERROR", e.message, null)
                }
            }
            "hostCloudAnchor" -> handleHostCloudAnchor(call, result)
            "resolveCloudAnchor" -> handleResolveCloudAnchor(call, result)
            "removeAnchor" -> handleRemoveAnchor(call, result)
            "dispose" -> {
                dispose()
                result.success(null)
            }
            "addNodeToPlaneAnchor" -> handleAddNodeToPlaneAnchor(call, result)
            "addNodeToScreenPosition" -> handleAddNodeToScreenPosition(call, result)
            "getCameraPose" -> handleGetCameraPose(result)
            "getAnchorPose" -> handleGetAnchorPose(call, result)
            "snapshot" -> handleSnapshot(result)
            "showPlanes" -> handleShowPlanes(call, result)
            "addAnchor" -> handleAddAnchor(call, result)
            "initGoogleCloudAnchorMode" -> handleInitGoogleCloudAnchorMode(result)
            else -> result.notImplemented()
        }
    }

    private fun handleHostCloudAnchor(call: MethodCall, result: MethodChannel.Result) {
        try {
            val anchorId = call.argument<String>("anchorId")
            if (anchorId == null) {
                result.error("INVALID_ARGUMENT", "Anchor ID is required", null)
                return
            }

            val session = sceneView.session
            if (session == null) {
                result.error("SESSION_ERROR", "AR Session is not available", null)
                return
            }

            if (!session.canHostCloudAnchor(sceneView.cameraNode)) {
                result.error("HOSTING_ERROR", "Insufficient visual data to host", null)
                return
            }

            val anchor = session.allAnchors.find { it.cloudAnchorId == anchorId }
            if (anchor == null) {
                result.error("ANCHOR_NOT_FOUND", "Anchor with ID $anchorId not found", null)
                return
            }

            val cloudAnchorNode = CloudAnchorNode(sceneView.engine, anchor)
            cloudAnchorNode.host(session) { cloudAnchorId, state ->
                if (state == CloudAnchorState.SUCCESS && cloudAnchorId != null) {
                    result.success(cloudAnchorId)
                } else {
                    result.error("HOSTING_ERROR", "Failed to host cloud anchor: $state", null)
                }
            }
            sceneView.addChildNode(cloudAnchorNode)

        } catch (e: Exception) {
            result.error("HOST_CLOUD_ANCHOR_ERROR", e.message, null)
        }
    }

    private fun handleResolveCloudAnchor(call: MethodCall, result: MethodChannel.Result) {
        try {
            val cloudAnchorId = call.argument<String>("cloudAnchorId")
            if (cloudAnchorId == null) {
                result.error("INVALID_ARGUMENT", "Cloud Anchor ID is required", null)
                return
            }

            val session = sceneView.session
            if (session == null) {
                result.error("SESSION_ERROR", "AR Session is not available", null)
                return
            }

            CloudAnchorNode.resolve(
                sceneView.engine,
                session,
                cloudAnchorId
            ) { state, node ->
                if (!state.isError && node != null) {
                    sceneView.addChildNode(node)
                    result.success(null)
                } else {
                    result.error("RESOLVE_ERROR", "Failed to resolve cloud anchor: $state", null)
                }
            }
        } catch (e: Exception) {
            result.error("RESOLVE_CLOUD_ANCHOR_ERROR", e.message, null)
        }
    }

    private fun handleRemoveAnchor(call: MethodCall, result: MethodChannel.Result) {
        try {
            val anchorId = call.argument<String>("anchorId")
            if (anchorId == null) {
                result.error("INVALID_ARGUMENT", "Anchor ID is required", null)
                return
            }

            // Trouver et supprimer l'ancre
            sceneView.childNodes
                .filterIsInstance<AnchorNode>()
                .find { node -> node.anchor?.cloudAnchorId == anchorId }
                ?.let { anchorNode ->
                    sceneView.removeChildNode(anchorNode)
                    anchorNode.anchor?.detach()
                    result.success(null)
                } ?: result.error("ANCHOR_NOT_FOUND", "Anchor with ID $anchorId not found", null)
        } catch (e: Exception) {
            result.error("REMOVE_ANCHOR_ERROR", e.message, null)
        }
    }

    private fun handleGetCameraPose(result: MethodChannel.Result) {
        try {
            val frame = sceneView.session?.update()
            val cameraPose = frame?.camera?.pose
            if (cameraPose != null) {
                val poseData = mapOf(
                    "position" to mapOf(
                        "x" to cameraPose.tx(),
                        "y" to cameraPose.ty(),
                        "z" to cameraPose.tz()
                    ),
                    "rotation" to mapOf(
                        "x" to cameraPose.rotationQuaternion[0],
                        "y" to cameraPose.rotationQuaternion[1],
                        "z" to cameraPose.rotationQuaternion[2],
                        "w" to cameraPose.rotationQuaternion[3]
                    )
                )
                result.success(poseData)
            } else {
                result.error("NO_CAMERA_POSE", "Camera pose is not available", null)
            }
        } catch (e: Exception) {
            result.error("CAMERA_POSE_ERROR", e.message, null)
        }
    }

    private fun handleGetAnchorPose(call: MethodCall, result: MethodChannel.Result) {
        try {
            val anchorId = call.argument<String>("anchorId")
            if (anchorId == null) {
                result.error("INVALID_ARGUMENT", "Anchor ID is required", null)
                return
            }

            val anchor = sceneView.session?.allAnchors?.find { it.cloudAnchorId == anchorId }
            if (anchor != null) {
                val anchorPose = anchor.pose
                val poseData = mapOf(
                    "position" to mapOf(
                        "x" to anchorPose.tx(),
                        "y" to anchorPose.ty(),
                        "z" to anchorPose.tz()
                    ),
                    "rotation" to mapOf(
                        "x" to anchorPose.rotationQuaternion[0],
                        "y" to anchorPose.rotationQuaternion[1],
                        "z" to anchorPose.rotationQuaternion[2],
                        "w" to anchorPose.rotationQuaternion[3]
                    )
                )
                result.success(poseData)
            } else {
                result.error("ANCHOR_NOT_FOUND", "Anchor with ID $anchorId not found", null)
            }
        } catch (e: Exception) {
            result.error("ANCHOR_POSE_ERROR", e.message, null)
        }
    }

    private fun handleSnapshot(result: MethodChannel.Result) {
        try {
            mainScope.launch {
                val bitmap = withContext(Dispatchers.Main) {
                    val bitmap = Bitmap.createBitmap(
                        sceneView.width, sceneView.height,
                        Bitmap.Config.ARGB_8888
                    )
                    
                    try {
                        val listener = PixelCopy.OnPixelCopyFinishedListener { copyResult ->
                            if (copyResult == PixelCopy.SUCCESS) {
                                val byteStream = java.io.ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
                                val byteArray = byteStream.toByteArray()
                                result.success(byteArray)
                            } else {
                                result.error("SNAPSHOT_ERROR", "Failed to capture snapshot", null)
                            }
                        }
                        
                        PixelCopy.request(
                            sceneView, bitmap,
                            listener, Handler(Looper.getMainLooper())
                        )
                    } catch (e: Exception) {
                        result.error("SNAPSHOT_ERROR", e.message, null)
                    }
                }
            }
        } catch (e: Exception) {
            result.error("SNAPSHOT_ERROR", e.message, null)
        }
    }

    private fun handleShowPlanes(call: MethodCall, result: MethodChannel.Result) {
        try {
            val showPlanes = call.argument<Boolean>("showPlanes") ?: true
            sceneView.planeRenderer.isEnabled = showPlanes
            result.success(null)
        } catch (e: Exception) {
            result.error("SHOW_PLANES_ERROR", e.message, null)
        }
    }

    private fun handleAddAnchor(call: MethodCall, result: MethodChannel.Result) {
        try {
            val anchorType = call.argument<Int>("type")
            if (anchorType == 0) { // Plane Anchor
                val transform = call.argument<ArrayList<Double>>("transformation")
                val name = call.argument<String>("name")
                
                if (name != null && transform != null) {
                    try {
                        // Décomposer la matrice de transformation
                        val (position, rotation) = deserializeMatrix4(transform)
                        
                        val pose = Pose(
                            floatArrayOf(position.x, position.y, position.z),
                            floatArrayOf(rotation.x, rotation.y, rotation.z, 1f)
                        )
                        
                        val anchor = sceneView.session?.createAnchor(pose)
                        if (anchor != null) {
                            val anchorNode = AnchorNode(sceneView.engine, anchor)
                            try {
                                anchorNode.transform = Transform(
                                    position = position,
                                    rotation = rotation
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "Transform warning suppressed: ${e.message}")
                            }
                            
                            sceneView.addChildNode(anchorNode)
                            anchorNodesMap[name] = anchorNode
                            result.success(true)
                        } else {
                            result.success(false)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in transform calculation: ${e.message}")
                        result.success(false)
                    }
                } else {
                    result.success(false)
                }
            } else {
                result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleAddAnchor: ${e.message}")
            e.printStackTrace()
            result.success(false)
        }
    }

    private fun handleInitGoogleCloudAnchorMode(result: MethodChannel.Result) {
        try {
            sceneView.configureSession { session, config ->
                config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
            }
            result.success(null)
        } catch (e: Exception) {
            result.error("CLOUD_ANCHOR_INIT_ERROR", e.message, null)
        }
    }

    override fun getView(): View {
        Log.i(TAG, "getView")
        return sceneView
    }

    override fun dispose() {
        Log.i(TAG, "dispose")
        sessionChannel.setMethodCallHandler(null)
        objectChannel.setMethodCallHandler(null)
        anchorChannel.setMethodCallHandler(null)
        nodesMap.clear()
        sceneView.destroy()
    }

    private fun notifyNodeTap(nodeName: String?) {
        mainScope.launch {
            objectChannel.invokeMethod("onNodeTap", listOf(nodeName))
        }
    }

    private fun notifyError(error: String) {
        mainScope.launch {
            sessionChannel.invokeMethod("onError", listOf(error))
        }
    }

    private fun notifyCloudAnchorUploaded(args: Map<String, Any>) {
        mainScope.launch {
            anchorChannel.invokeMethod("onCloudAnchorUploaded", args)
        }
    }

    private fun notifyAnchorDownloadSuccess(anchorData: Map<String, Any>, result: MethodChannel.Result) {
        mainScope.launch {
            anchorChannel.invokeMethod("onAnchorDownloadSuccess", anchorData, object : MethodChannel.Result {
                override fun success(result: Any?) {
                    val anchorName = result.toString()
                    // Mettre à jour l'ancre avec le nom reçu
                }

                override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                    notifyError("Error while registering downloaded anchor: $errorMessage")
                }

                override fun notImplemented() {
                    notifyError("Error while registering downloaded anchor")
                }
            })
        }
    }

    private fun notifyPlaneOrPointTap(hitResults: List<Map<String, Any>>) {
        mainScope.launch {
            try {
                val serializedResults = ArrayList<HashMap<String, Any>>()
                hitResults.forEach { hit ->
                    serializedResults.add(serializeHitResult(hit))
                }
                sessionChannel.invokeMethod("onPlaneOrPointTap", serializedResults)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

