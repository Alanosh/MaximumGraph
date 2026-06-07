package com.example.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class LayerType {
    VIDEO, AUDIO, IMAGE, TEXT
}

enum class PropertyType {
    POSITION_X,
    POSITION_Y,
    SCALE,
    ROTATION,
    OPACITY,
    ANCHOR_X,
    ANCHOR_Y,
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    BLUR,
    DISTORT
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "project_id")
    val projectId: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "output_width")
    val outputWidth: Int = 1280,
    @ColumnInfo(name = "output_height")
    val outputHeight: Int = 720
)

@Entity(
    tableName = "layers",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["project_id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LayerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "layer_id")
    val layerId: Long = 0,
    @ColumnInfo(name = "project_id", index = true)
    val projectId: Long,
    val type: LayerType,
    @ColumnInfo(name = "source_path")
    val sourcePath: String,
    @ColumnInfo(name = "timeline_start_us")
    val timelineStartUs: Long,
    @ColumnInfo(name = "duration_us")
    val durationUs: Long,
    @ColumnInfo(name = "trim_in_us")
    val trimInUs: Long = 0,
    // Store simple serialized representation of waveform (approx 100 peaks per second of duration)
    // Audio decimation requirement: decimated byte array containing peaks.
    @ColumnInfo(name = "waveform_peaks")
    val waveformPeaks: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerEntity) return false
        if (layerId != other.layerId) return false
        return true
    }

    override fun hashCode(): Int {
        return layerId.hashCode()
    }
}

@Entity(
    tableName = "keyframes",
    foreignKeys = [
        ForeignKey(
            entity = LayerEntity::class,
            parentColumns = ["layer_id"],
            childColumns = ["layer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KeyframeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "keyframe_id")
    val keyframeId: Long = 0,
    @ColumnInfo(name = "layer_id", index = true)
    val layerId: Long,
    @ColumnInfo(name = "timestamp_us")
    val timestampUs: Long,
    @ColumnInfo(name = "property_type")
    val propertyType: PropertyType,
    val value: Float
)

@Entity(
    tableName = "custom_curves",
    foreignKeys = [
        ForeignKey(
            entity = KeyframeEntity::class,
            parentColumns = ["keyframe_id"],
            childColumns = ["keyframe_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomCurveEntity(
    @PrimaryKey
    @ColumnInfo(name = "keyframe_id")
    val keyframeId: Long,
    // Flat float coordinates stored as CSV (e.g. "x1,y1,x2,y2,...")
    @ColumnInfo(name = "coordinates_blob")
    val coordinatesBlob: String
)

class DBTypeConverters {
    @TypeConverter
    fun fromLayerType(value: LayerType): String = value.name

    @TypeConverter
    fun toLayerType(value: String): LayerType = LayerType.valueOf(value)

    @TypeConverter
    fun fromPropertyType(value: PropertyType): String = value.name

    @TypeConverter
    fun toPropertyType(value: String): PropertyType = PropertyType.valueOf(value)
}
