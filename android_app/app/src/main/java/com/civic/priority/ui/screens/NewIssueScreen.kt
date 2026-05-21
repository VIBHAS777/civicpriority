package com.civic.priority.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.civic.priority.data.*
import com.civic.priority.ui.theme.CivicColors
import com.civic.priority.viewmodel.AppViewModel
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewIssueScreen(viewModel: AppViewModel, navController: NavController) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(IssueCategory.COMMON_AREA) }
    var severity by remember { mutableFloatStateOf(3f) }
    var affectedPeople by remember { mutableFloatStateOf(100f) }
    var locationType by remember { mutableStateOf(LocationZone.COMMON_AREA) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showError by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showLocationMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            selectedBitmap = bitmap
        }
    }

    val severityLabel = when (severity.toInt()) {
        1 -> "Minor – Cosmetic only"
        2 -> "Low – Some inconvenience"
        3 -> "Moderate – Functional impact"
        4 -> "High – Significant disruption"
        5 -> "Critical – Urgent attention needed"
        else -> ""
    }

    // Live score preview
    val dummyIssue = remember(category, severity, affectedPeople, locationType) {
        Issue(
            title = title, description = description,
            category = category, severity = severity.toDouble(),
            affectedPeople = affectedPeople.toDouble(),
            locationType = locationType,
            dateReported = System.currentTimeMillis(),
            reporterId = UUID.randomUUID().toString(),
            reporterName = ""
        )
    }

    // Smooth animations for live score adjustments
    val animatedBaseScore by animateFloatAsState(
        targetValue = dummyIssue.baseScore.toFloat(),
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "LiveBaseScore"
    )
    val animatedFinalScore by animateFloatAsState(
        targetValue = dummyIssue.finalScore.toFloat(),
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "LiveFinalScore"
    )

    var pulseTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(dummyIssue.finalScore) {
        pulseTrigger = true
        delay(300)
        pulseTrigger = false
    }

    val pulseProgress by animateFloatAsState(
        targetValue = if (pulseTrigger) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "PulseProgress"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Report Issue", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Cancel", color = CivicColors.TextSecondary)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (title.isEmpty() || description.isEmpty()) {
                            showError = true
                        } else {
                            val imgData = selectedBitmap?.let { bmp ->
                                val stream = ByteArrayOutputStream()
                                bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                                stream.toByteArray()
                            }
                            viewModel.addIssue(title, description, category, severity.toDouble(), affectedPeople.toDouble(), locationType, imgData)
                            navController.popBackStack()
                        }
                    }) {
                        Text("Submit", color = CivicColors.Primary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = CivicColors.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Score Preview
            val scale = 1f + pulseProgress * 0.03f
            val cardGlowColor = CivicColors.scoreColor(animatedFinalScore.toDouble()).copy(alpha = 0.4f)
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(
                        elevation = (8 + pulseProgress * 8).dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = cardGlowColor,
                        spotColor = cardGlowColor
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Live Score Preview", fontWeight = FontWeight.Bold, color = CivicColors.Primary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Score:", color = CivicColors.TextPrimary)
                        Text(String.format("%.1f", animatedBaseScore), fontWeight = FontWeight.Bold, color = CivicColors.TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Projected Final Score:", color = CivicColors.TextPrimary)
                        Text(
                            String.format("%.1f", animatedFinalScore),
                            fontWeight = FontWeight.Bold,
                            color = CivicColors.scoreColor(animatedFinalScore.toDouble())
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFinalScore / 100f)
                                .background(
                                    CivicColors.scoreColor(animatedFinalScore.toDouble()),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Priority Level:", color = CivicColors.TextPrimary)
                        Text(
                            dummyIssue.priorityLevel.displayName,
                            fontWeight = FontWeight.Bold,
                            color = dummyIssue.priorityLevel.getColor(),
                            modifier = Modifier
                                .background(
                                    dummyIssue.priorityLevel.getColor().copy(alpha = 0.2f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Issue Details Section
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Issue Details", fontWeight = FontWeight.Bold, color = CivicColors.Primary, fontSize = 14.sp)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CivicColors.Primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = CivicColors.Primary,
                            focusedLabelColor = CivicColors.Primary,
                            unfocusedLabelColor = CivicColors.TextSecondary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe the issue in detail...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CivicColors.Primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = CivicColors.Primary,
                            focusedLabelColor = CivicColors.Primary,
                            unfocusedLabelColor = CivicColors.TextSecondary
                        )
                    )

                    // Category picker
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category.displayName,
                            onValueChange = {},
                            label = { Text("Category") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CivicColors.Primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = CivicColors.Primary,
                                unfocusedLabelColor = CivicColors.TextSecondary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { showCategoryMenu = true }
                        )
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            IssueCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = { category = cat; showCategoryMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            // Photo Evidence
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Photo Evidence", fontWeight = FontWeight.Bold, color = CivicColors.Primary, fontSize = 14.sp)

                    AnimatedVisibility(
                        visible = selectedBitmap != null,
                        enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500)),
                        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                    ) {
                        selectedBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Selected photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, CivicColors.Primary, RoundedCornerShape(12.dp))
                            )
                        }
                    }

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CivicColors.Primary)
                    ) {
                        Icon(
                            if (selectedBitmap == null) Icons.Default.CameraAlt else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedBitmap == null) "Attach Photo" else "Change Photo",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                }
            }

            // Impact & Location Section
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Impact & Location", fontWeight = FontWeight.Bold, color = CivicColors.Primary, fontSize = 14.sp)

                    // Severity slider
                    Column {
                        Text("Severity: ${severity.toInt()}", color = CivicColors.TextPrimary)
                        Text(severityLabel, fontSize = 12.sp, color = CivicColors.TextSecondary)
                        Slider(
                            value = severity,
                            onValueChange = { severity = it },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = CivicColors.Primary,
                                activeTrackColor = CivicColors.Primary
                            )
                        )
                    }

                    // Affected People slider
                    Column {
                        Text("Affected People: ${affectedPeople.toInt()}", color = CivicColors.TextPrimary)
                        Slider(
                            value = affectedPeople,
                            onValueChange = { affectedPeople = it },
                            valueRange = 1f..5000f,
                            colors = SliderDefaults.colors(
                                thumbColor = CivicColors.Primary,
                                activeTrackColor = CivicColors.Primary
                            )
                        )
                    }

                    // Location picker
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = locationType.displayName,
                            onValueChange = {},
                            label = { Text("Location Zone") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLocationMenu) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CivicColors.Primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = CivicColors.Primary,
                                unfocusedLabelColor = CivicColors.TextSecondary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { showLocationMenu = true }
                        )
                        DropdownMenu(
                            expanded = showLocationMenu,
                            onDismissRequest = { showLocationMenu = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            LocationZone.entries.forEach { zone ->
                                DropdownMenuItem(
                                    text = { Text(zone.displayName) },
                                    onClick = { locationType = zone; showLocationMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Error dialog
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Missing Input") },
            text = { Text("Please fill out the Title and Description.") },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK", color = CivicColors.Primary)
                }
            }
        )
    }
}
