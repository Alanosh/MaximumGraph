package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.LayerEntity
import com.example.db.LayerType
import com.example.db.ProjectEntity
import com.example.db.PropertyType
import com.example.utils.CurveUtils
import com.example.utils.Point
import com.example.editor.VideoEditorViewModel
import kotlinx.coroutines.launch

@Composable
fun DrawAnimateApp(viewModel: VideoEditorViewModel) {
    val currentProject by viewModel.currentProject.collectAsState()

    Surface(
        color = Color(0xFF121214), // Midnight Velvet Dark Workstation
        modifier = Modifier.fillMaxSize()
    ) {
        Crossfade(targetState = currentProject, label = "ScreenTransition") { project ->
            if (project == null) {
                WelcomeScreen(viewModel)
            } else {
                MainEditorWorkspaceScreen(viewModel, project)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(viewModel: VideoEditorViewModel) {
    val projects by viewModel.recentProjects.collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DrawAnimate",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD0BCFF),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Low-RAM Professional Film Workstation",
            fontSize = 14.sp,
            color = Color(0xFF938F99),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            onClick = { showCreateDialog = true },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .widthIn(max = 450.dp)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("create_project_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Project",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("New Empty Timeline", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Initialize 10s custom parameters", fontSize = 12.sp, color = Color(0xFF938F99))
                }
            }
        }

        Card(
            onClick = {
                scope.launch {
                    Toast.makeText(context, "Populating custom vector scene...", Toast.LENGTH_SHORT).show()
                    viewModel.loadTemplateScenario()
                }
            },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .widthIn(max = 450.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .testTag("load_template_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Template Project",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Load Cinematic Sunsets Demo", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Includes PCM Waveforms & S-Curve Zoom", fontSize = 12.sp, color = Color(0xFF938F99))
                }
            }
        }

        if (projects.isNotEmpty()) {
            Text(
                text = "Recent Timelines",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .heightIn(max = 250.dp)
                    .fillMaxWidth()
            ) {
                items(projects) { project ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFF16161A), RoundedCornerShape(8.dp))
                            .clickable { viewModel.openProject(project) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Project icon",
                                tint = Color(0xFFCBC4CF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = project.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteProject(project) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Project",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Project", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Project Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.createProject(newProjectName.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                    modifier = Modifier.testTag("confirm_create_button")
                ) {
                    Text("Create", color = Color(0xFF381E72))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color(0xFFD0BCFF))
                }
            },
            containerColor = Color(0xFF1E1E22),
            titleContentColor = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEditorWorkspaceScreen(viewModel: VideoEditorViewModel, project: ProjectEntity) {
    val fpsVal by viewModel.simulatedFps.collectAsState()
    val ramVal by viewModel.simulatedRamUsageMb.collectAsState()
    val undoStack by viewModel.undoStack.collectAsState()
    val redoStack by viewModel.redoStack.collectAsState()

    var showAddLayerSheet by remember { mutableStateOf(false) }
    var activeCurveOverlayKeyframeId by remember { mutableStateOf<Long?>(null) }
    var showNumericalDrawer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF1A1A1E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.closeProject() }) {
                            Icon(Icons.Default.Close, "Exit timeline", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(project.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${project.outputWidth}x${project.outputHeight} MP4 timeline", fontSize = 11.sp, color = Color(0xFF938F99))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2E7D32).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "FPS: ${"%.1f".format(fpsVal)}",
                                color = Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (ramVal > 2.0f * 1024f) Color(0xFFD32F2F).copy(alpha = 0.15f)
                                    else Color(0xFF1976D2).copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Heap: ${"%.1f".format(ramVal)}MB",
                                color = if (ramVal > 2000f) Color(0xFFE57373) else Color(0xFF64B5F6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showNumericalDrawer = true }) {
                            Icon(Icons.Default.Settings, "Direct Parameters", tint = Color(0xFFD0BCFF))
                        }
                    }
                }
            }
        },
        bottomBar = {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.stepUndo() },
                                enabled = undoStack.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Undo Action",
                                    tint = if (undoStack.isNotEmpty()) Color.White else Color(0xFF49454F)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.stepRedo() },
                                enabled = redoStack.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Redo Action",
                                    tint = if (redoStack.isNotEmpty()) Color.White else Color(0xFF49454F)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = { viewModel.splitActiveLayerAtPlayhead() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282830)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Crop, "Split action", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Virtual Split", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { showAddLayerSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, "add media layer", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Media Track", color = Color.White)
                        }
                    }

                    TimelineWorkspaceWidget(viewModel = viewModel) { kfId ->
                        activeCurveOverlayKeyframeId = kfId
                    }
                }
            }
        },
        containerColor = Color(0xFF101012)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF282830), RoundedCornerShape(12.dp))
                ) {
                    VideoPreviewCanvas(viewModel = viewModel)
                }
            }

            activeCurveOverlayKeyframeId?.let { kfId ->
                SpeedCurveDrawingCanvasWidget(
                    viewModel = viewModel,
                    keyframeId = kfId,
                    onDismiss = { activeCurveOverlayKeyframeId = null }
                )
            }

            if (showNumericalDrawer) {
                ModalBottomSheet(
                    onDismissRequest = { showNumericalDrawer = false },
                    containerColor = Color(0xFF1A1A1E),
                    scrimColor = Color.Black.copy(alpha = 0.5f)
                ) {
                    NumericalValuesDrawerContent(viewModel = viewModel)
                }
            }
        }
    }

    if (showAddLayerSheet) {
        AddLayerOptionDialog(
            onDismiss = { showAddLayerSheet = false },
            onConfirm = { type, path, durationSec ->
                viewModel.insertLayer(type, path, 0L, durationSec * 1_000_000L)
                showAddLayerSheet = false
            }
        )
    }
}

@Composable
fun VideoPreviewCanvas(viewModel: VideoEditorViewModel) {
    val pheadUs by viewModel.playheadUs.collectAsState()
    val props by viewModel.interpolatedProperties.collectAsState()

    var dragTranslateX by remember { mutableStateOf(0f) }
    var dragTranslateY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragTranslateX += dragAmount.x
                    dragTranslateY += dragAmount.y
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerWidth = size.width / 2f
            val centerHeight = size.height / 2f

            val baseColor = Color(0xFFF97316)
            val topColor = Color(0xFF701A75)
            
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, baseColor),
                    startY = 0f,
                    endY = size.height
                )
            )

            val scaleVal = props[PropertyType.SCALE] ?: 1.0f
            val posX = centerWidth + (props[PropertyType.POSITION_X] ?: 0f) + dragTranslateX
            val posY = centerHeight + (props[PropertyType.POSITION_Y] ?: 0f) + dragTranslateY
            val alphaVal = (props[PropertyType.OPACITY] ?: 1.0f).coerceIn(0f, 1f)
            val brightnessVal = props[PropertyType.BRIGHTNESS] ?: 1.0f

            val sunRadius = 90f * scaleVal
            drawCircle(
                color = Color(0xFFFDE047).copy(alpha = alphaVal),
                radius = sunRadius,
                center = Offset(posX, posY - 40f)
            )

            drawOval(
                color = Color(0xFFFDE047).copy(alpha = alphaVal * 0.4f),
                topLeft = Offset(posX - sunRadius, posY + 20f),
                size = Size(sunRadius * 2, 40f * scaleVal)
            )

            val lineCount = 6
            for (i in 1..lineCount) {
                val stepY = posY + 60f + (i * 24f)
                val widthWave = (sunRadius * (1.5f + (i * 0.2f))) * scaleVal
                val startX = posX - widthWave
                val endX = posX + widthWave
                
                drawLine(
                    color = Color(0xFFFACC15).copy(alpha = alphaVal * (1f - (i.toFloat() / lineCount))),
                    start = Offset(startX, stepY),
                    end = Offset(endX, stepY),
                    strokeWidth = 4f
                )
            }

            val mPath = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, size.height - 180f)
                quadraticTo(centerWidth * 0.4f, size.height - 300f, centerWidth * 0.8f, size.height - 140f)
                quadraticTo(centerWidth * 1.4f, size.height - 290f, size.width, size.height - 120f)
                lineTo(size.width, size.height)
                close()
            }

            drawPath(
                path = mPath,
                color = Color(0xFF1E1B4B).copy(alpha = alphaVal * brightnessVal)
            )

            drawCircle(
                color = Color(0xFF8B5CF6),
                radius = 12f,
                center = Offset(posX, posY)
            )
            
            drawLine(
                color = Color(0xFF8B5CF6),
                start = Offset(posX, posY),
                end = Offset(centerWidth, centerHeight),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            Text("Preview Frame Vector Stats", color = Color(0xFFD0BCFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Playhead: ${formattedDuration(pheadUs / 1000L)}", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text("Position: (${"%.1f".format(props[PropertyType.POSITION_X] ?: 0f)}, ${"%.1f".format(props[PropertyType.POSITION_Y] ?: 0f)})", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("Scale/Rot: ${"%.2f".format(props[PropertyType.SCALE] ?: 1.0f)} / ${"%.1f".format(props[PropertyType.ROTATION] ?: 0f)}°", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun TimelineWorkspaceWidget(
    viewModel: VideoEditorViewModel,
    onDoubleTapKeyframe: (Long) -> Unit
) {
    val playheadUs by viewModel.playheadUs.collectAsState()
    val totalProgressUs by viewModel.totalDurationUs.collectAsState()
    val isSnapping by viewModel.isSnappingEnabled.collectAsState()
    
    val layersList by viewModel.layers.collectAsState()
    val keyframesList by viewModel.keyframes.collectAsState()
    
    val selectedId by viewModel.selectedLayerId.collectAsState()
    val selectedKfId by viewModel.selectedKeyframeId.collectAsState()

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .background(Color(0xFF141416))
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                TextButton(
                    onClick = { viewModel.isSnappingEnabled.value = !isSnapping },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSnapping) "SNAP ON" else "SNAP OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSnapping) Color(0xFFD0BCFF) else Color(0xFF938F99)
                    )
                }
            }

            layersList.forEach { layer ->
                val isSelected = selectedId == layer.layerId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E1E22))
                        .then(
                            if (isSelected) Modifier.border(2.dp, Color(0xFFD0BCFF), RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .clickable { viewModel.selectedLayerId.value = layer.layerId }
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val trackIcon = when (layer.type) {
                        LayerType.VIDEO -> Icons.Default.Videocam
                        LayerType.AUDIO -> Icons.Default.VolumeUp
                        LayerType.TEXT -> Icons.Default.TextFields
                        LayerType.IMAGE -> Icons.Default.Image
                    }
                    Icon(trackIcon, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = layer.type.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val remScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(remScrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1500.dp)
                        .pointerInput(layersList, keyframesList, totalProgressUs, isSnapping) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val scaleWidth = size.width
                                    var matchedId: Long? = null
                                    for (layer in layersList) {
                                        val idx = layersList.indexOf(layer)
                                        val topY = 40f + (idx * 42f)
                                        val trackKeys = keyframesList.filter { it.layerId == layer.layerId }
                                        for (kf in trackKeys) {
                                            val kfFrac = kf.timestampUs.toFloat() / totalProgressUs
                                            val kfx = kfFrac * scaleWidth
                                            val kfy = topY + 17f * 2.75f
                                            val dist = Math.hypot((offset.x - kfx).toDouble(), (offset.y - kfy).toDouble())
                                            if (dist < 40.0) {
                                                matchedId = kf.keyframeId
                                                break
                                            }
                                        }
                                        if (matchedId != null) break
                                    }
                                    if (matchedId != null) {
                                        onDoubleTapKeyframe(matchedId)
                                    } else {
                                        Toast.makeText(context, "Double-tap exact keyframe to edit curve", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onTap = { offset ->
                                    val scaleWidth = size.width
                                    var matchedId: Long? = null
                                    for (layer in layersList) {
                                        val idx = layersList.indexOf(layer)
                                        val topY = 40f + (idx * 42f)
                                        val trackKeys = keyframesList.filter { it.layerId == layer.layerId }
                                        for (kf in trackKeys) {
                                            val kfFrac = kf.timestampUs.toFloat() / totalProgressUs
                                            val kfx = kfFrac * scaleWidth
                                            val kfy = topY + 17f * 2.75f
                                            val dist = Math.hypot((offset.x - kfx).toDouble(), (offset.y - kfy).toDouble())
                                            if (dist < 40.0) {
                                                matchedId = kf.keyframeId
                                                break
                                            }
                                        }
                                        if (matchedId != null) break
                                    }

                                    if (matchedId != null) {
                                        viewModel.selectedKeyframeId.value = matchedId
                                    } else {
                                        val proportion = offset.x / scaleWidth
                                        val rawTarget = (proportion * totalProgressUs).toLong()
                                        viewModel.playheadUs.value = viewModel.handleMagnetPlayheadSnap(rawTarget)
                                    }
                                }
                            )
                        }
                ) {
                    val scaleWidth = size.width
                    
                    for (i in 0..100) {
                        val frac = i / 100f
                        val screenX = frac * scaleWidth
                        val markHeight = if (i % 10 == 0) 24f else 12f
                        
                        drawLine(
                            color = Color(0xFF49454F),
                            start = Offset(screenX, 0f),
                            end = Offset(screenX, markHeight),
                            strokeWidth = 2f
                        )
                    }

                    layersList.forEachIndexed { idx, layer ->
                        val topY = 40f + (idx * 42f)
                        val startPct = layer.timelineStartUs.toFloat() / totalProgressUs
                        val durPct = layer.durationUs.toFloat() / totalProgressUs

                        val startX = startPct * scaleWidth
                        val barWidth = durPct * scaleWidth

                        drawRoundRect(
                            color = when (layer.type) {
                                LayerType.VIDEO -> Color(0xFF283593)
                                LayerType.AUDIO -> Color(0xFF311B92)
                                LayerType.TEXT -> Color(0xFF006064)
                                LayerType.IMAGE -> Color(0xFF4A148C)
                            },
                            topLeft = Offset(startX, topY),
                            size = Size(barWidth, 34.dp.toPx()),
                            cornerRadius = CornerRadius(12f, 12f)
                        )

                        if (layer.type == LayerType.AUDIO && layer.waveformPeaks != null) {
                            val peaks = layer.waveformPeaks ?: byteArrayOf()
                            val binCount = peaks.size
                            val barStep = barWidth / binCount
                            for (pIdx in 0 until binCount) {
                                val peakVal = peaks[pIdx].toInt() / 127f
                                val px = startX + (pIdx * barStep)
                                val waveHeight = (30.dp.toPx()) * peakVal
                                drawLine(
                                    color = Color(0xFFD0BCFF).copy(alpha = 0.6f),
                                    start = Offset(px, topY + (17.dp.toPx()) - (waveHeight / 2f)),
                                    end = Offset(px, topY + (17.dp.toPx()) + (waveHeight / 2f)),
                                    strokeWidth = 2f
                                )
                            }
                        }

                        val trackKeys = keyframesList.filter { it.layerId == layer.layerId }
                        trackKeys.forEach { kf ->
                            val kfFrac = kf.timestampUs.toFloat() / totalProgressUs
                            val kfx = kfFrac * scaleWidth
                            
                            val diamondCenter = Offset(kfx, topY + (17.dp.toPx()))
                            val kfSize = 12f
                            val dPath = Path().apply {
                                moveTo(diamondCenter.x, diamondCenter.y - kfSize)
                                lineTo(diamondCenter.x + kfSize, diamondCenter.y)
                                lineTo(diamondCenter.x, diamondCenter.y + kfSize)
                                lineTo(diamondCenter.x - kfSize, diamondCenter.y)
                                close()
                            }

                            val isKfSelected = selectedKfId == kf.keyframeId
                            drawPath(
                                path = dPath,
                                color = if (isKfSelected) Color(0xFFFFEB3B) else Color.White
                            )
                        }
                    }

                    val progressRatio = playheadUs.toFloat() / totalProgressUs
                    val px = progressRatio * scaleWidth
                    
                    drawLine(
                        color = Color(0xFFD0BCFF),
                        start = Offset(px, 0f),
                        end = Offset(px, size.height),
                        strokeWidth = 4f
                    )
                    
                    drawCircle(
                        color = Color(0xFFD0BCFF),
                        radius = 16f,
                        center = Offset(px, 8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedCurveDrawingCanvasWidget(
    viewModel: VideoEditorViewModel,
    keyframeId: Long,
    onDismiss: () -> Unit
) {
    var rawInputPoints by remember { mutableStateOf(listOf<Point>()) }
    var resultSplinePoints by remember { mutableStateOf(listOf<Point>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        rawInputPoints = listOf(Point(offset.x, offset.y))
                        resultSplinePoints = emptyList()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currentList = rawInputPoints.toMutableList()
                        val last = currentList.last()
                        val newPt = Point(last.x + dragAmount.x, last.y + dragAmount.y)
                        currentList.add(newPt)
                        rawInputPoints = currentList
                    },
                    onDragEnd = {
                        val simplified = CurveUtils.ramerDouglasPeucker(rawInputPoints, 6f)
                        
                        val boundingBoxLeft = rawInputPoints.minOfOrNull { it.x } ?: 0f
                        val boundingBoxRight = rawInputPoints.maxOfOrNull { it.x } ?: 1f
                        val boundingBoxTop = rawInputPoints.minOfOrNull { it.y } ?: 0f
                        val boundingBoxBottom = rawInputPoints.maxOfOrNull { it.y } ?: 1f
                        val rangeX = (boundingBoxRight - boundingBoxLeft).coerceAtLeast(1f)
                        val rangeY = (boundingBoxBottom - boundingBoxTop).coerceAtLeast(1f)

                        val normalisedPoints = simplified.map {
                            Point(
                                x = ((it.x - boundingBoxLeft) / rangeX).coerceIn(0f, 1f),
                                y = (1.0f - ((it.y - boundingBoxTop) / rangeY)).coerceIn(0f, 1f)
                            )
                        }.sortedBy { it.x }

                        if (normalisedPoints.size >= 2) {
                            resultSplinePoints = normalisedPoints
                            viewModel.saveSpeedCurveForSelectedKeyframe(resultSplinePoints)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Draw Custom speed curves", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Drag to sketch a line. Simplifies with S-Curve peaks.", color = Color(0xFF938F99), fontSize = 12.sp)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Done, "Accept Spline curve", tint = Color(0xFFD0BCFF))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .background(Color(0xFF16161A), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridSteps = 4
                    for (step in 1 until gridSteps) {
                        val ratio = step.toFloat() / gridSteps
                        drawLine(
                            color = Color(0xFF282830),
                            start = Offset(0f, size.height * ratio),
                            end = Offset(size.width, size.height * ratio),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0xFF282830),
                            start = Offset(size.width * ratio, 0f),
                            end = Offset(size.width * ratio, size.height),
                            strokeWidth = 2f
                        )
                    }

                    if (rawInputPoints.size >= 2) {
                        val pathTrace = Path().apply {
                            moveTo(rawInputPoints[0].x, rawInputPoints[0].y)
                            for (i in 1 until rawInputPoints.size) {
                                lineTo(rawInputPoints[i].x, rawInputPoints[i].y)
                            }
                        }
                        drawPath(
                            path = pathTrace,
                            color = Color(0xFFD0BCFF).copy(alpha = 0.4f),
                            style = Stroke(width = 4f)
                        )
                    }

                    if (resultSplinePoints.size >= 2) {
                        val spline = CurveUtils.generateSpline(resultSplinePoints)
                        val splinePath = Path().apply {
                            val startX = spline[0].x * size.width
                            val startY = (1f - spline[0].y) * size.height
                            moveTo(startX, startY)
                            for (i in 1 until spline.size) {
                                lineTo(spline[i].x * size.width, (1f - spline[i].y) * size.height)
                            }
                        }
                        drawPath(
                            path = splinePath,
                            color = Color(0xFFD0BCFF),
                            style = Stroke(width = 8f, cap = StrokeCap.Round)
                        )

                        resultSplinePoints.forEach { pt ->
                            drawCircle(
                                color = Color(0xFFFFEB3B),
                                radius = 10f,
                                center = Offset(pt.x * size.width, (1f - pt.y) * size.height)
                            )
                        }
                    }
                }
            }

            Text("Sleek spline fitting engine: active", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumericalValuesDrawerContent(viewModel: VideoEditorViewModel) {
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val selectedProp by viewModel.selectedProperty.collectAsState()
    val props by viewModel.interpolatedProperties.collectAsState()
    val playheadUs by viewModel.playheadUs.collectAsState()

    if (selectedLayerId == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No active media track selected", color = Color(0xFF938F99), fontSize = 14.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text("Manual Frame Parameters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PropertyType.values().forEach { type ->
                val isSelected = selectedProp == type
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedProperty.value = type },
                    label = { Text(type.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF381E72),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF282830),
                        labelColor = Color(0xFFCBC4CF)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val currentVal = props[selectedProp] ?: 0f
        val rangeMin = when (selectedProp) {
            PropertyType.SCALE -> 0.1f
            PropertyType.ROTATION -> -360f
            PropertyType.OPACITY -> 0f
            PropertyType.POSITION_X -> -800f
            PropertyType.POSITION_Y -> -800f
            else -> 0f
        }
        val rangeMax = when (selectedProp) {
            PropertyType.SCALE -> 5.0f
            PropertyType.ROTATION -> 360f
            PropertyType.OPACITY -> 1f
            PropertyType.POSITION_X -> 800f
            PropertyType.POSITION_Y -> 800f
            else -> 2f
        }

        Text("${selectedProp.name}: ${"%.2f".format(currentVal)}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Slider(
            value = currentVal.coerceIn(rangeMin, rangeMax),
            onValueChange = { scale ->
                viewModel.createOrUpdateKeyframe(playheadUs, selectedProp, scale)
            },
            valueRange = rangeMin..rangeMax,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFD0BCFF),
                activeTrackColor = Color(0xFFD0BCFF),
                inactiveTrackColor = Color(0xFF49454F)
            ),
            modifier = Modifier.testTag("numerical_parameter_slider")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.createOrUpdateKeyframe(playheadUs, selectedProp, currentVal)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF))
        ) {
            Text("Pin Dynamic Keyframe", color = Color(0xFF381E72))
        }
    }
}

@Composable
fun AddLayerOptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (LayerType, String, Long) -> Unit
) {
    var selectedType by remember { mutableStateOf(LayerType.VIDEO) }
    var filepath by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Media Track", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Track Format:", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LayerType.values().forEach { type ->
                        val isSelected = selectedType == type
                        Button(
                            onClick = { selectedType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF381E72) else Color(0xFF222226)
                            )
                        ) {
                            Text(type.name, color = Color.White)
                        }
                    }
                }

                OutlinedTextField(
                    value = filepath,
                    onValueChange = { filepath = it },
                    label = { Text("Source file path / Label text") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD0BCFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Timeline Duration (Seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD0BCFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationText.toLongOrNull() ?: 10L
                    val finalPath = if (filepath.isNotBlank()) filepath else "imported_${selectedType.name.lowercase()}"
                    onConfirm(selectedType, finalPath, duration)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF))
            ) {
                Text("Insert Track", color = Color(0xFF381E72))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back", color = Color(0xFFD0BCFF))
            }
        },
        containerColor = Color(0xFF1E1E22)
    )
}

private fun formattedDuration(millis: Long): String {
    val totSec = millis / 1000
    val min = totSec / 60
    val sec = totSec % 60
    val tenths = (millis % 1000) / 100
    return "%02d:%02d.%01d".format(min, sec, tenths)
}
