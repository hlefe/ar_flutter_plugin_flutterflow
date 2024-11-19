package io.carius.lars.ar_flutter_plugin_flutterflow

import android.util.Log
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.ar.ARSceneView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArModelBuilder(private val sceneView: ARSceneView) {
    suspend fun makeNodeFromGlb(
        modelPath: String
    ): ModelNode? {
        return try {
            withContext(Dispatchers.IO) {
                // Charger le modèle de façon asynchrone avec loadModelInstance
                val modelInstance = sceneView.modelLoader.loadModelInstance(modelPath)
                
                modelInstance?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 0.5f,
                        centerOrigin = Position(y = -0.5f)
                    ).apply {
                        isEditable = true
                        editableScaleRange = 0.2f..0.75f
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ArModelBuilder", "Error creating node: ${e.message}")
            e.printStackTrace() // Ajouter la stack trace pour plus de détails
            null
        }
    }
} 