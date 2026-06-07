package com.example.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.*
import com.example.utils.CurveUtils
import com.example.utils.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = EditorDatabase.getInstance(application)
    private val dao = database.editorDao

    val recentProjects: Flow<List<ProjectEntity>> = dao.getAllProjectsFlow()

    private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
    val currentProject: StateFlow<ProjectEntity?> = _currentProject.asStateFlow()

    val layers = MutableStateFlow<List<LayerEntity>>(emptyList())
    val keyframes = MutableStateFlow<List<KeyframeEntity>>(emptyList())
    val customCurves = MutableStateFlow<Map<Long, CustomCurveEntity>>(emptyMap())

    val playheadUs = MutableStateFlow(0L)
    val totalDurationUs = MutableStateFlow(10_000_000L) // 10s default

    val selectedLayerId = MutableStateFlow<Long?>(null)
    val selectedKeyframeId = MutableStateFlow<Long?>(null)
    val selectedProperty = MutableStateFlow(PropertyType.SCALE)

    val isSnappingEnabled = MutableStateFlow(true)

    private val _interpolatedProperties = MutableStateFlow<Map<PropertyType, Float>>(emptyMap())
    val interpolatedProperties: StateFlow<Map<PropertyType, Float>> = _interpolatedProperties.asStateFlow()

    // Undo/Redo Snapshots Stack
    private val historyUndoStack = MutableStateFlow<List<HistorySnapshot>>(emptyList())
    val undoStack: StateFlow<List<HistorySnapshot>> = historyUndoStack.asStateFlow()

    private val historyRedoStack = MutableStateFlow<List<HistorySnapshot>>(emptyList())
    val redoStack: StateFlow<List<HistorySnapshot>> = historyRedoStack.asStateFlow()

    // RAM usage & Performance stats
    val simulatedFps = MutableStateFlow(60.0f)
    val simulatedRamUsageMb = MutableStateFlow(138.5f)

    data class HistorySnapshot(
        val layers: List<LayerEntity>,
        val keyframes: List<KeyframeEntity>,
        val customCurves: Map<Long, CustomCurveEntity>,
        val playheadUs: Long,
        val selectedLayerId: Long?,
        val selectedKeyframeId: Long?
    )

    init {
        // Ticker to simulate rendering and monitor memory usage
        viewModelScope.launch {
            while (true) {
                delay(1000)
                simulatedFps.value = 59.4f + Random.nextFloat() * 1.2f
                simulatedRamUsageMb.value = 120f + (keyframes.value.size * 0.4f) + Random.nextFloat() * 5.0f
            }
        }

        // Real-time microsecond property interpolation
        viewModelScope.launch {
            combine(playheadUs, selectedLayerId, keyframes, layers, customCurves) { phead, layerId, kfs, listLayers, curvesMap ->
                if (layerId == null || listLayers.none { it.layerId == layerId }) {
                    return@combine mapOf(
                        PropertyType.SCALE to 1.0f,
                        PropertyType.ROTATION to 0.0f,
                        PropertyType.OPACITY to 1.0f,
                        PropertyType.POSITION_X to 0.0f,
                        PropertyType.POSITION_Y to 0.0f,
                        PropertyType.BRIGHTNESS to 1.0f
                    )
                }

                val result = mutableMapOf<PropertyType, Float>()
                val matchedKfs = kfs.filter { it.layerId == layerId }

                PropertyType.values().forEach { prop ->
                    val propKfs = matchedKfs.filter { it.propertyType == prop }.sortedBy { it.timestampUs }
                    if (propKfs.isEmpty()) {
                        result[prop] = when (prop) {
                            PropertyType.SCALE -> 1.0f
                            PropertyType.OPACITY -> 1.0f
                            PropertyType.BRIGHTNESS -> 1.0f
                            else -> 0.0f
                        }
                    } else if (phead <= propKfs.first().timestampUs) {
                        result[prop] = propKfs.first().value
                    } else if (phead >= propKfs.last().timestampUs) {
                        result[prop] = propKfs.last().value
                    } else {
                        var prev = propKfs.first()
                        var next = propKfs.last()
                        for (i in 0 until propKfs.size - 1) {
                            if (phead >= propKfs[i].timestampUs && phead <= propKfs[i + 1].timestampUs) {
                                prev = propKfs[i]
                                next = propKfs[i + 1]
                                break
                            }
                        }

                        val duration = (next.timestampUs - prev.timestampUs).toFloat()
                        val elapsed = (phead - prev.timestampUs).toFloat()
                        val progress = if (duration > 0) elapsed / duration else 1f

                        val curve = curvesMap[next.keyframeId]
                        val interpolatedProgress = if (curve != null) {
                            val parsedPoints = curve.coordinatesBlob.split(";").mapNotNull {
                                val parts = it.split(",")
                                if (parts.size == 2) {
                                    val x = parts[0].toFloatOrNull() ?: 0f
                                    val y = parts[1].toFloatOrNull() ?: 0f
                                    Point(x, y)
                                } else null
                            }
                            if (parsedPoints.isNotEmpty()) {
                                val spline = CurveUtils.generateSpline(parsedPoints)
                                CurveUtils.evaluateCurveValue(spline, progress)
                            } else progress
                        } else {
                            progress
                        }

                        val range = next.value - prev.value
                        result[prop] = prev.value + (range * interpolatedProgress)
                    }
                }
                result
            }.collect { values ->
                _interpolatedProperties.value = values
            }
        }
    }

    private fun captureSnapshot() {
        val snapshot = HistorySnapshot(
            layers = layers.value.map { it.copy() },
            keyframes = keyframes.value.map { it.copy() },
            customCurves = customCurves.value.toMap(),
            playheadUs = playheadUs.value,
            selectedLayerId = selectedLayerId.value,
            selectedKeyframeId = selectedKeyframeId.value
        )
        val currentUndo = historyUndoStack.value.toMutableList()
        currentUndo.add(snapshot)
        if (currentUndo.size > 20) {
            currentUndo.removeAt(0)
        }
        historyUndoStack.value = currentUndo
        historyRedoStack.value = emptyList()
    }

    fun stepUndo() {
        val undoList = historyUndoStack.value.toMutableList()
        if (undoList.isEmpty()) return

        val snapshot = undoList.removeAt(undoList.size - 1)
        historyUndoStack.value = undoList

        val redoList = historyRedoStack.value.toMutableList()
        redoList.add(
            HistorySnapshot(
                layers = layers.value.map { it.copy() },
                keyframes = keyframes.value.map { it.copy() },
                customCurves = customCurves.value.toMap(),
                playheadUs = playheadUs.value,
                selectedLayerId = selectedLayerId.value,
                selectedKeyframeId = selectedKeyframeId.value
            )
        )
        historyRedoStack.value = redoList

        layers.value = snapshot.layers
        keyframes.value = snapshot.keyframes
        customCurves.value = snapshot.customCurves
        playheadUs.value = snapshot.playheadUs
        selectedLayerId.value = snapshot.selectedLayerId
        selectedKeyframeId.value = snapshot.selectedKeyframeId

        persistSnapshotsToDatabase()
    }

    fun stepRedo() {
        val redoList = historyRedoStack.value.toMutableList()
        if (redoList.isEmpty()) return

        val snapshot = redoList.removeAt(redoList.size - 1)
        historyRedoStack.value = redoList

        val undoList = historyUndoStack.value.toMutableList()
        undoList.add(
            HistorySnapshot(
                layers = layers.value.map { it.copy() },
                keyframes = keyframes.value.map { it.copy() },
                customCurves = customCurves.value.toMap(),
                playheadUs = playheadUs.value,
                selectedLayerId = selectedLayerId.value,
                selectedKeyframeId = selectedKeyframeId.value
            )
        )
        historyUndoStack.value = undoList

        layers.value = snapshot.layers
        keyframes.value = snapshot.keyframes
        customCurves.value = snapshot.customCurves
        playheadUs.value = snapshot.playheadUs
        selectedLayerId.value = snapshot.selectedLayerId
        selectedKeyframeId.value = snapshot.selectedKeyframeId

        persistSnapshotsToDatabase()
    }

    private fun persistSnapshotsToDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val proj = _currentProject.value ?: return@launch
            dao.clearLayersForProject(proj.projectId)
            dao.clearKeyframesForProject(proj.projectId)

            layers.value.forEach { dao.insertLayer(it) }
            keyframes.value.forEach { dao.insertKeyframe(it) }
            customCurves.value.values.forEach { dao.insertCustomCurve(it) }
        }
    }

    // Helper functions for Virtual split state cleanup
    private suspend fun EditorDao.clearLayersForProject(projectId: Long) {
        getLayersForProject(projectId).forEach { deleteLayer(it) }
    }

    private suspend fun EditorDao.clearKeyframesForProject(projectId: Long) {
        getAllKeyframesForProject(projectId).forEach { deleteKeyframe(it) }
    }

    fun createProject(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val project = ProjectEntity(
                name = name,
                createdAt = System.currentTimeMillis(),
                outputWidth = 1280,
                outputHeight = 720
            )
            val projId = dao.insertProject(project)
            val savedProject = project.copy(projectId = projId)
            _currentProject.value = savedProject
            loadProjectData(projId)
        }
    }

    fun openProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentProject.value = project
            loadProjectData(project.projectId)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteProjectById(project.projectId)
            if (_currentProject.value?.projectId == project.projectId) {
                _currentProject.value = null
            }
        }
    }

    fun closeProject() {
        _currentProject.value = null
        layers.value = emptyList()
        keyframes.value = emptyList()
        customCurves.value = emptyMap()
        playheadUs.value = 0L
    }

    private suspend fun loadProjectData(projectId: Long) {
        val savedLayers = dao.getLayersForProject(projectId)
        val savedKeyframes = dao.getAllKeyframesForProject(projectId)
        val savedCurves = dao.getAllCustomCurvesForProject(projectId)

        layers.value = savedLayers
        keyframes.value = savedKeyframes
        customCurves.value = savedCurves.associateBy { it.keyframeId }
        playheadUs.value = 0L
        selectedLayerId.value = savedLayers.firstOrNull()?.layerId
        selectedKeyframeId.value = null
    }

    fun insertLayer(type: LayerType, path: String, startUs: Long, durationUs: Long) {
        val currentProj = _currentProject.value ?: return
        captureSnapshot()

        viewModelScope.launch(Dispatchers.IO) {
            val peaksBytes = if (type == LayerType.AUDIO) {
                ByteArray(60) { (20 + Random.nextInt(85)).toByte() }
            } else null

            val layer = LayerEntity(
                projectId = currentProj.projectId,
                type = type,
                sourcePath = path,
                timelineStartUs = startUs,
                durationUs = durationUs,
                waveformPeaks = peaksBytes
            )
            val layerId = dao.insertLayer(layer)
            val updatedLayer = layer.copy(layerId = layerId)

            layers.value = layers.value + updatedLayer
            selectedLayerId.value = layerId

            createOrUpdateKeyframeDirect(0L, PropertyType.SCALE, 1.0f, layerId)
            createOrUpdateKeyframeDirect(0L, PropertyType.OPACITY, 1.0f, layerId)
            createOrUpdateKeyframeDirect(0L, PropertyType.POSITION_X, 0.0f, layerId)
            createOrUpdateKeyframeDirect(0L, PropertyType.POSITION_Y, 0.0f, layerId)
        }
    }

    fun splitActiveLayerAtPlayhead() {
        val targetId = selectedLayerId.value ?: return
        val phead = playheadUs.value
        val layer = layers.value.find { it.layerId == targetId } ?: return

        if (phead <= layer.timelineStartUs || phead >= (layer.timelineStartUs + layer.durationUs)) return

        captureSnapshot()
        viewModelScope.launch(Dispatchers.IO) {
            val leftDuration = phead - layer.timelineStartUs
            val rightDuration = (layer.timelineStartUs + layer.durationUs) - phead

            // Shrink the original layer
            val leftLayer = layer.copy(durationUs = leftDuration)
            dao.updateLayer(leftLayer)

            // Create subsequent splitted right track section
            val rightLayer = LayerEntity(
                projectId = layer.projectId,
                type = layer.type,
                sourcePath = "${layer.sourcePath} (Part 2)",
                timelineStartUs = phead,
                durationUs = rightDuration,
                waveformPeaks = layer.waveformPeaks?.let { peaks ->
                    val frac = leftDuration.toFloat() / layer.durationUs
                    val splitIdx = (peaks.size * frac).toInt().coerceIn(1 until peaks.size)
                    peaks.copyOfRange(splitIdx, peaks.size)
                }
            )
            val rightId = dao.insertLayer(rightLayer)

            // Split dynamic keyframes cleanly onto their relative parts
            val projKfs = dao.getAllKeyframesForProject(layer.projectId)
            val originalKfs = projKfs.filter { it.layerId == targetId }
            originalKfs.forEach { kf ->
                if (kf.timestampUs >= phead) {
                    dao.deleteKeyframe(kf)
                    val migratedKf = kf.copy(keyframeId = 0, layerId = rightId)
                    dao.insertKeyframe(migratedKf)
                }
            }

            // Sync States
            loadProjectData(layer.projectId)
        }
    }

    private suspend fun createOrUpdateKeyframeDirect(timestampUs: Long, property: PropertyType, value: Float, layerId: Long) {
        val projId = _currentProject.value!!.projectId
        val currentKfs = dao.getAllKeyframesForProject(projId)
        val existing = currentKfs.find { it.layerId == layerId && it.timestampUs == timestampUs && it.propertyType == property }

        if (existing != null) {
            val updated = existing.copy(value = value)
            dao.updateKeyframe(updated)
        } else {
            val kf = KeyframeEntity(
                layerId = layerId,
                timestampUs = timestampUs,
                propertyType = property,
                value = value
            )
            dao.insertKeyframe(kf)
        }

        keyframes.value = dao.getAllKeyframesForProject(projId)
    }

    fun createOrUpdateKeyframe(timestampUs: Long, property: PropertyType, value: Float) {
        val activeLayerId = selectedLayerId.value ?: return
        captureSnapshot()

        viewModelScope.launch(Dispatchers.IO) {
            createOrUpdateKeyframeDirect(timestampUs, property, value, activeLayerId)
        }
    }

    fun saveSpeedCurveForSelectedKeyframe(points: List<Point>) {
        val kfId = selectedKeyframeId.value ?: return
        val currentProj = _currentProject.value ?: return

        captureSnapshot()
        viewModelScope.launch(Dispatchers.IO) {
            val pointsStr = points.joinToString(";") { "${it.x},${it.y}" }
            
            val curve = CustomCurveEntity(
                keyframeId = kfId,
                coordinatesBlob = pointsStr
            )
            dao.insertCustomCurve(curve)

            val savedCurves = dao.getAllCustomCurvesForProject(currentProj.projectId)
            customCurves.value = savedCurves.associateBy { it.keyframeId }
        }
    }

    fun handleMagnetPlayheadSnap(targetUs: Long): Long {
        if (!isSnappingEnabled.value) return targetUs.coerceIn(0L, totalDurationUs.value)

        val thresholdUs = 180_000L // Approx 180ms snap radius
        var nearestSnap = targetUs

        // Snap to start of timelines
        if (Math.abs(targetUs) < thresholdUs) {
            return 0L
        }
        // Snap to end of timelines
        if (Math.abs(targetUs - totalDurationUs.value) < thresholdUs) {
            return totalDurationUs.value
        }

        // Snap to layers starts/ends
        layers.value.forEach { layer ->
            if (Math.abs(targetUs - layer.timelineStartUs) < thresholdUs) {
                nearestSnap = layer.timelineStartUs
            }
            val layerEnd = layer.timelineStartUs + layer.durationUs
            if (Math.abs(targetUs - layerEnd) < thresholdUs) {
                nearestSnap = layerEnd
            }
        }

        // Snap to keyframe markers
        keyframes.value.forEach { kf ->
            if (Math.abs(targetUs - kf.timestampUs) < thresholdUs) {
                nearestSnap = kf.timestampUs
            }
        }

        return nearestSnap.coerceIn(0L, totalDurationUs.value)
    }

    fun loadTemplateScenario() {
        viewModelScope.launch(Dispatchers.IO) {
            val project = ProjectEntity(
                name = "Golden Gate Sunset Pan",
                createdAt = System.currentTimeMillis(),
                outputWidth = 1920,
                outputHeight = 1080
            )
            val projId = dao.insertProject(project)
            val savedProject = project.copy(projectId = projId)
            _currentProject.value = savedProject

            // 1. Procedural Sunset Video Track
            val videoTrack = LayerEntity(
                projectId = projId,
                type = LayerType.VIDEO,
                sourcePath = "sunsets_gradient_vector.mp4",
                timelineStartUs = 0L,
                durationUs = 10_000_000L
            )
            val vId = dao.insertLayer(videoTrack)

            // Keyframe pin transitions details
            createOrUpdateKeyframeDirect(0L, PropertyType.SCALE, 1.0f, vId)
            createOrUpdateKeyframeDirect(4_000_000L, PropertyType.SCALE, 1.75f, vId)
            createOrUpdateKeyframeDirect(10_000_000L, PropertyType.SCALE, 1.0f, vId)

            createOrUpdateKeyframeDirect(0L, PropertyType.POSITION_Y, 0.0f, vId)
            createOrUpdateKeyframeDirect(5_000_000L, PropertyType.POSITION_Y, -250.0f, vId)
            createOrUpdateKeyframeDirect(10_000_000L, PropertyType.POSITION_Y, 0.0f, vId)

            // 2. Procedural Audio Tracks
            val audioTrack = LayerEntity(
                projectId = projId,
                type = LayerType.AUDIO,
                sourcePath = "ambient_synthesizer.wav",
                timelineStartUs = 0L,
                durationUs = 10_000_000L,
                waveformPeaks = ByteArray(100) { (15..120).random().toByte() }
            )
            val aId = dao.insertLayer(audioTrack)
            createOrUpdateKeyframeDirect(0L, PropertyType.OPACITY, 0.2f, aId)
            createOrUpdateKeyframeDirect(10_000_000L, PropertyType.OPACITY, 0.9f, aId)

            loadProjectData(projId)
        }
    }
}
