package com.janreins.nutrisnap.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.janreins.nutrisnap.ui.theme.EmeraldContainer
import com.janreins.nutrisnap.ui.theme.EmeraldDark
import com.janreins.nutrisnap.ui.theme.EmeraldPrimary
import com.janreins.nutrisnap.ui.theme.MacroCaloriesColor
import com.janreins.nutrisnap.ui.theme.MacroCarbsColor
import com.janreins.nutrisnap.ui.theme.MacroFatsColor
import com.janreins.nutrisnap.ui.theme.MacroProteinColor
import com.janreins.nutrisnap.ui.theme.Slate200
import com.janreins.nutrisnap.ui.theme.Slate400
import com.janreins.nutrisnap.ui.theme.Slate500
import com.janreins.nutrisnap.ui.theme.Slate700
import java.io.File
import kotlin.math.roundToInt

@Composable
fun MealInputSection(
    isAnalyzing: Boolean,
    analysisStatusText: String,
    onAnalyzeText: (String) -> Unit,
    onAnalyzePhoto: (Bitmap, String?) -> Unit,
    onLogManually: (name: String, calories: Int, protein: Float, carbs: Float, fats: Float, fiber: Float, sugar: Float, mealType: String, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var textInput by remember { mutableStateOf("") }
    var selectedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoNoteInput by remember { mutableStateOf("") }

    // Manual Log state
    var manualName by remember { mutableStateOf("") }
    var manualMealType by remember { mutableStateOf("Meal") }
    var manualCalories by remember { mutableStateOf("") }
    var manualProtein by remember { mutableStateOf("") }
    var manualCarbs by remember { mutableStateOf("") }
    var manualFats by remember { mutableStateOf("") }

    // Camera Uri creation helper
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(context, it)
            if (bitmap != null) {
                selectedPhotoBitmap = bitmap
            } else {
                Toast.makeText(context, "Could not load selected photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // High resolution Camera Launcher with FileProvider
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            val bitmap = uriToBitmap(context, tempCameraUri!!)
            if (bitmap != null) {
                selectedPhotoBitmap = bitmap
            }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val photoFile = createTempImageFile(context)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to snap food photos", Toast.LENGTH_SHORT).show()
        }
    }

    val quickSuggestions = listOf(
        "Chicken adobo with 1 cup rice",
        "2 fried eggs, 2 slices toast & butter",
        "Grilled salmon with roasted vegetables",
        "1 cup oatmeal with banana and honey",
        "Protein shake with almond milk",
        "Tapsilog with sunny side up egg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("meal_input_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Log Your Meal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Input Mode Tabs (Describe / Snap Photo / Manual)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = EmeraldPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = EmeraldPrimary,
                            height = 3.dp
                        )
                    }
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Describe", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_describe")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Photo Snap", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_photo")
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Manual", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_manual")
                )
            }

            // Tab 0: Describe via Natural Text
            if (selectedTabIndex == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("e.g. 200g chicken adobo with 1 cup jasmine rice and steamed broccoli") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("meal_text_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (textInput.isNotBlank() && !isAnalyzing) {
                                focusManager.clearFocus()
                                onAnalyzeText(textInput)
                            }
                        })
                    )

                    // Quick suggestion pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickSuggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { textInput = suggestion }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Estimate button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onAnalyzeText(textInput)
                        },
                        enabled = textInput.isNotBlank() && !isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("estimate_macros_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(analysisStatusText, style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Estimate Macros with Gemini",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Tab 1: Snap or Upload Photo
            if (selectedTabIndex == 1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedPhotoBitmap == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Camera Button
                            Button(
                                onClick = {
                                    val permissionCheck = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    )
                                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                        val photoFile = createTempImageFile(context)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            photoFile
                                        )
                                        tempCameraUri = uri
                                        takePictureLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .testTag("camera_snap_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldContainer,
                                    contentColor = EmeraldDark
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Snap Camera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Gallery Button
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .testTag("gallery_pick_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Pick Gallery", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // Photo Preview & Retake
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            Image(
                                bitmap = selectedPhotoBitmap!!.asImageBitmap(),
                                contentDescription = "Captured meal",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedPhotoBitmap = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Optional context prompt
                        OutlinedTextField(
                            value = photoNoteInput,
                            onValueChange = { photoNoteInput = it },
                            placeholder = { Text("Optional note (e.g. used olive oil, ate half the portion)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Analyze Photo Button
                        Button(
                            onClick = {
                                selectedPhotoBitmap?.let { bmp ->
                                    onAnalyzePhoto(bmp, photoNoteInput)
                                }
                            },
                            enabled = !isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("analyze_photo_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(analysisStatusText, style = MaterialTheme.typography.labelMedium)
                            } else {
                                Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyze Photo with Gemini", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Tab 2: Manual Log Entry
            if (selectedTabIndex == 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Meal Name") },
                        placeholder = { Text("e.g. Greek Yogurt with Walnuts") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_meal_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Calories
                    OutlinedTextField(
                        value = manualCalories,
                        onValueChange = { manualCalories = it.filter { c -> c.isDigit() } },
                        label = { Text("Calories (kcal)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = MacroCaloriesColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_calories_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 3 Macros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualProtein,
                            onValueChange = { manualProtein = it.filter { c -> c.isDigit() } },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_protein_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = manualCarbs,
                            onValueChange = { manualCarbs = it.filter { c -> c.isDigit() } },
                            label = { Text("Carbs (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_carbs_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = manualFats,
                            onValueChange = { manualFats = it.filter { c -> c.isDigit() } },
                            label = { Text("Fats (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_fats_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Save Manual Meal
                    Button(
                        onClick = {
                            val cals = manualCalories.toIntOrNull() ?: 0
                            val p = manualProtein.toFloatOrNull() ?: 0f
                            val c = manualCarbs.toFloatOrNull() ?: 0f
                            val f = manualFats.toFloatOrNull() ?: 0f

                            onLogManually(
                                manualName.ifBlank { "Logged Meal" },
                                cals,
                                p,
                                c,
                                f,
                                0f,
                                0f,
                                manualMealType,
                                "Manually logged"
                            )

                            // Clear inputs
                            manualName = ""
                            manualCalories = ""
                            manualProtein = ""
                            manualCarbs = ""
                            manualFats = ""
                        },
                        enabled = manualName.isNotBlank() || manualCalories.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_manual_meal_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Meal Directly", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun createTempImageFile(context: Context): File {
    val storageDir = File(context.cacheDir, "camera")
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }
    return File.createTempFile("SNAP_", ".jpg", storageDir)
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        null
    }
}
