package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EditorDao {
    // Projects
    @Query("SELECT * FROM projects ORDER BY created_at DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE project_id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("DELETE FROM projects WHERE project_id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    // Layers
    @Query("SELECT * FROM layers WHERE project_id = :projectId")
    fun getLayersForProjectFlow(projectId: Long): Flow<List<LayerEntity>>

    @Query("SELECT * FROM layers WHERE project_id = :projectId")
    suspend fun getLayersForProject(projectId: Long): List<LayerEntity>

    @Query("SELECT * FROM layers WHERE layer_id = :layerId")
    suspend fun getLayerById(layerId: Long): LayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayer(layer: LayerEntity): Long

    @Update
    suspend fun updateLayer(layer: LayerEntity)

    @Delete
    suspend fun deleteLayer(layer: LayerEntity)

    @Query("DELETE FROM layers WHERE layer_id = :layerId")
    suspend fun deleteLayerById(layerId: Long)

    // Keyframes
    @Query("SELECT * FROM keyframes WHERE layer_id = :layerId ORDER BY timestamp_us ASC")
    fun getKeyframesForLayerFlow(layerId: Long): Flow<List<KeyframeEntity>>

    @Query("SELECT * FROM keyframes WHERE layer_id = :layerId ORDER BY timestamp_us ASC")
    suspend fun getKeyframesForLayer(layerId: Long): List<KeyframeEntity>

    @Query("SELECT * FROM keyframes WHERE layer_id IN (SELECT layer_id FROM layers WHERE project_id = :projectId)")
    suspend fun getAllKeyframesForProject(projectId: Long): List<KeyframeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyframe(keyframe: KeyframeEntity): Long

    @Update
    suspend fun updateKeyframe(keyframe: KeyframeEntity)

    @Delete
    suspend fun deleteKeyframe(keyframe: KeyframeEntity)

    @Query("DELETE FROM keyframes WHERE keyframe_id = :keyframeId")
    suspend fun deleteKeyframeById(keyframeId: Long)

    // Custom Curves
    @Query("SELECT * FROM custom_curves WHERE keyframe_id = :keyframeId")
    suspend fun getCustomCurve(keyframeId: Long): CustomCurveEntity?

    @Query("SELECT * FROM custom_curves WHERE keyframe_id IN (SELECT keyframe_id FROM keyframes WHERE layer_id IN (SELECT layer_id FROM layers WHERE project_id = :projectId))")
    suspend fun getAllCustomCurvesForProject(projectId: Long): List<CustomCurveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCurve(customCurve: CustomCurveEntity)

    @Query("DELETE FROM custom_curves WHERE keyframe_id = :keyframeId")
    suspend fun deleteCustomCurveById(keyframeId: Long)
}
