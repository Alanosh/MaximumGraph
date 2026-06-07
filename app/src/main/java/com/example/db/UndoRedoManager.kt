package com.example.db

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UndoableAction {
    val description: String
    suspend fun execute()
    suspend fun undo()
}

class UndoRedoManager {
    private val undoStack = ArrayDeque<UndoableAction>()
    private val redoStack = ArrayDeque<UndoableAction>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _stackUpdateTrigger = MutableStateFlow(0)
    val stackUpdateTrigger: StateFlow<Int> = _stackUpdateTrigger.asStateFlow()

    suspend fun executeAction(action: UndoableAction) {
        action.execute()
        undoStack.addLast(action)
        if (undoStack.size > 20) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        updateStates()
    }

    suspend fun undo() {
        if (undoStack.isNotEmpty()) {
            val action = undoStack.removeLast()
            action.undo()
            redoStack.addLast(action)
            updateStates()
        }
    }

    suspend fun redo() {
        if (redoStack.isNotEmpty()) {
            val action = redoStack.removeLast()
            action.execute()
            undoStack.addLast(action)
            if (undoStack.size > 20) {
                undoStack.removeFirst()
            }
            updateStates()
        }
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateStates()
    }

    private fun updateStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _stackUpdateTrigger.value += 1
    }
}

// -------------------------------------------------------------
// Concrete Undoable Actions for Room Database
// -------------------------------------------------------------

class AddLayerAction(
    private val dao: EditorDao,
    private val layer: LayerEntity,
    private val defaultKeyframes: List<KeyframeEntity> = emptyList()
) : UndoableAction {
    override val description = "Add ${layer.type.name} Layer"
    private var insertedLayerId: Long = layer.layerId
    private val insertedKeyframeIds = mutableListOf<Long>()

    override suspend fun execute() {
        // Safe check to avoid duplicate inserting
        val toInsert = if (insertedLayerId > 0) layer.copy(layerId = insertedLayerId) else layer
        val newId = dao.insertLayer(toInsert)
        insertedLayerId = newId

        insertedKeyframeIds.clear()
        for (kf in defaultKeyframes) {
            val kfToInsert = kf.copy(layerId = newId)
            val newKfId = dao.insertKeyframe(kfToInsert)
            insertedKeyframeIds.add(newKfId)
        }
    }

    override suspend fun undo() {
        if (insertedLayerId > 0) {
            dao.deleteLayerById(insertedLayerId)
        }
    }
}

class DeleteLayerAction(
    private val dao: EditorDao,
    private val layer: LayerEntity,
    private val keyframes: List<KeyframeEntity>,
    private val curves: List<CustomCurveEntity>
) : UndoableAction {
    override val description = "Delete ${layer.type.name} Layer"

    override suspend fun execute() {
        dao.deleteLayer(layer)
    }

    override suspend fun undo() {
        val newLayerId = dao.insertLayer(layer)
        for (kf in keyframes) {
            // Reinsert keyframes with correct mapped ID if layer ID changed, though cascade delete takes care
            dao.insertKeyframe(kf)
        }
        for (c in curves) {
            dao.insertCustomCurve(c)
        }
    }
}

class AddKeyframeAction(
    private val dao: EditorDao,
    private val keyframe: KeyframeEntity,
    private val customCurve: CustomCurveEntity? = null
) : UndoableAction {
    override val description = "Add Keyframe"
    private var insertedKeyframeId: Long = keyframe.keyframeId

    override suspend fun execute() {
        val targetKf = if (insertedKeyframeId > 0) keyframe.copy(keyframeId = insertedKeyframeId) else keyframe
        insertedKeyframeId = dao.insertKeyframe(targetKf)
        customCurve?.let {
            dao.insertCustomCurve(it.copy(keyframeId = insertedKeyframeId))
        }
    }

    override suspend fun undo() {
        if (insertedKeyframeId > 0) {
            dao.deleteKeyframeById(insertedKeyframeId)
        }
    }
}

class DeleteKeyframeAction(
    private val dao: EditorDao,
    private val keyframe: KeyframeEntity,
    private val customCurve: CustomCurveEntity? = null
) : UndoableAction {
    override val description = "Delete Keyframe"

    override suspend fun execute() {
        dao.deleteKeyframe(keyframe)
    }

    override suspend fun undo() {
        dao.insertKeyframe(keyframe)
        customCurve?.let {
            dao.insertCustomCurve(it)
        }
    }
}

class UpdateKeyframeValueAction(
    private val dao: EditorDao,
    private val keyframe: KeyframeEntity,
    private val oldValue: Float,
    private val newValue: Float
) : UndoableAction {
    override val description = "Update Property Value"

    override suspend fun execute() {
        dao.updateKeyframe(keyframe.copy(value = newValue))
    }

    override suspend fun undo() {
        dao.updateKeyframe(keyframe.copy(value = oldValue))
    }
}

class UpdateCustomCurveAction(
    private val dao: EditorDao,
    private val keyframeId: Long,
    private val oldCurveBlob: String?,
    private val newCurveBlob: String
) : UndoableAction {
    override val description = "Update Custom Curve"

    override suspend fun execute() {
        dao.insertCustomCurve(CustomCurveEntity(keyframeId, newCurveBlob))
    }

    override suspend fun undo() {
        if (oldCurveBlob != null) {
            dao.insertCustomCurve(CustomCurveEntity(keyframeId, oldCurveBlob))
        } else {
            dao.deleteCustomCurveById(keyframeId)
        }
    }
}

class SplitClipAction(
    private val dao: EditorDao,
    private val originalLayer: LayerEntity,
    private val splitTimeUs: Long,
    private val leftLayer: LayerEntity,
    private val rightLayer: LayerEntity,
    private val originalKeyframes: List<KeyframeEntity>,
    private val newKeyframesLeft: List<KeyframeEntity>,
    private val newKeyframesRight: List<KeyframeEntity>
) : UndoableAction {
    override val description = "Split Clip"
    private var insertedLeftId: Long = 0
    private var insertedRightId: Long = 0

    override suspend fun execute() {
        dao.deleteLayer(originalLayer)
        insertedLeftId = dao.insertLayer(leftLayer)
        insertedRightId = dao.insertLayer(rightLayer)

        for (kf in newKeyframesLeft) {
            dao.insertKeyframe(kf.copy(layerId = insertedLeftId))
        }
        for (kf in newKeyframesRight) {
            dao.insertKeyframe(kf.copy(layerId = insertedRightId))
        }
    }

    override suspend fun undo() {
        if (insertedLeftId > 0) dao.deleteLayerById(insertedLeftId)
        if (insertedRightId > 0) dao.deleteLayerById(insertedRightId)

        val origId = dao.insertLayer(originalLayer)
        for (kf in originalKeyframes) {
            dao.insertKeyframe(kf.copy(layerId = origId))
        }
    }
}
