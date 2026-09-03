package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroExceededColor
import com.example.ui.theme.MacroExceededSoft
import com.example.ui.theme.Slate300
import com.example.ui.viewmodel.NutriViewModel
import kotlinx.coroutines.delay

enum class LockMode {
    UNLOCK,
    SETUP,
    CHANGE,
    REMOVE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(
    viewModel: NutriViewModel,
    mode: LockMode = LockMode.UNLOCK,
    onSuccess: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var currentPinInput by remember { mutableStateOf("") }
    var setupStep by remember { mutableIntStateOf(1) } // 1 = enter current/new, 2 = enter new/confirm

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var cooldownRemainingSec by remember { mutableIntStateOf(0) }

    // Cooldown timer effect
    LaunchedEffect(cooldownRemainingSec) {
        if (cooldownRemainingSec > 0) {
            delay(1000L)
            cooldownRemainingSec -= 1
        }
    }

    val title = when (mode) {
        LockMode.UNLOCK -> "Enter PIN"
        LockMode.SETUP -> if (setupStep == 1) "Create App PIN" else "Confirm Your PIN"
        LockMode.CHANGE -> when (setupStep) {
            1 -> "Enter Current PIN"
            2 -> "Enter New PIN"
            else -> "Confirm New PIN"
        }
        LockMode.REMOVE -> "Enter Current PIN"
    }

    val subtitle = when (mode) {
        LockMode.UNLOCK -> "Enter your 4–6 digit PIN to access NutriSnap"
        LockMode.SETUP -> if (setupStep == 1) "Choose a 4–6 digit PIN to protect your app" else "Re-enter your PIN to verify"
        LockMode.CHANGE -> when (setupStep) {
            1 -> "Verify your identity with your current PIN"
            2 -> "Enter a new 4–6 digit PIN"
            else -> "Re-enter your new PIN to confirm"
        }
        LockMode.REMOVE -> "Enter your current PIN to disable PIN lock"
    }

    fun handlePinSubmit() {
        if (cooldownRemainingSec > 0) return

        errorMessage = null

        when (mode) {
            LockMode.UNLOCK -> {
                if (pinInput.length !in 4..6) {
                    errorMessage = "PIN must be 4–6 digits"
                    return
                }
                val isCorrect = viewModel.verifyPin(pinInput)
                if (isCorrect) {
                    failedAttempts = 0
                    onSuccess()
                } else {
                    failedAttempts += 1
                    pinInput = ""
                    if (failedAttempts >= 5) {
                        cooldownRemainingSec = 30
                        errorMessage = "Too many incorrect attempts. Please wait 30 seconds."
                    } else {
                        errorMessage = "Incorrect PIN. Try again."
                    }
                }
            }

            LockMode.SETUP -> {
                if (setupStep == 1) {
                    if (pinInput.length !in 4..6) {
                        errorMessage = "PIN must be 4–6 digits"
                        return
                    }
                    confirmPinInput = pinInput
                    pinInput = ""
                    setupStep = 2
                } else {
                    if (pinInput != confirmPinInput) {
                        errorMessage = "PINs do not match. Try again."
                        pinInput = ""
                        confirmPinInput = ""
                        setupStep = 1
                    } else {
                        viewModel.setPin(pinInput)
                        onSuccess()
                    }
                }
            }

            LockMode.CHANGE -> {
                if (setupStep == 1) {
                    if (!viewModel.verifyPin(pinInput)) {
                        errorMessage = "Incorrect current PIN."
                        pinInput = ""
                        return
                    }
                    currentPinInput = pinInput
                    pinInput = ""
                    setupStep = 2
                } else if (setupStep == 2) {
                    if (pinInput.length !in 4..6) {
                        errorMessage = "New PIN must be 4–6 digits"
                        return
                    }
                    confirmPinInput = pinInput
                    pinInput = ""
                    setupStep = 3
                } else {
                    if (pinInput != confirmPinInput) {
                        errorMessage = "PINs do not match. Try again."
                        pinInput = ""
                        confirmPinInput = ""
                        setupStep = 2
                    } else {
                        if (viewModel.changePin(currentPinInput, pinInput)) {
                            onSuccess()
                        } else {
                            errorMessage = "Failed to update PIN."
                            pinInput = ""
                            setupStep = 1
                        }
                    }
                }
            }

            LockMode.REMOVE -> {
                if (viewModel.removePin(pinInput)) {
                    onSuccess()
                } else {
                    errorMessage = "Incorrect current PIN."
                    pinInput = ""
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (onCancel != null || mode != LockMode.UNLOCK) {
                TopAppBar(
                    title = { Text(text = title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onCancel != null) {
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Security Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (mode == LockMode.UNLOCK) Icons.Rounded.Lock else Icons.Rounded.Security,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("lock_screen_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Visual PIN Dots (Up to 6 dots)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) EmeraldPrimary else Slate300)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cooldown Banner
            AnimatedVisibility(visible = cooldownRemainingSec > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MacroExceededSoft)
                ) {
                    Text(
                        text = "Too many failed attempts. Try again in ${cooldownRemainingSec}s",
                        modifier = Modifier.padding(12.dp),
                        color = MacroExceededColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Error Banner
            AnimatedVisibility(visible = errorMessage != null && cooldownRemainingSec == 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("pin_error_message"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MacroExceededSoft)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MacroExceededColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Text Input Field for Soft Keyboard compatibility and accessibility
            OutlinedTextField(
                value = pinInput,
                onValueChange = { input ->
                    if (input.length <= 6 && input.all { it.isDigit() }) {
                        pinInput = input
                        errorMessage = null
                    }
                },
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .testTag("pin_input_field"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = Slate300
                ),
                enabled = cooldownRemainingSec == 0
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Numeric Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val numRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                for (row in numRows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (num in row) {
                            KeypadButton(
                                text = num,
                                onClick = {
                                    if (cooldownRemainingSec == 0 && pinInput.length < 6) {
                                        pinInput += num
                                        errorMessage = null
                                    }
                                },
                                testTag = "pin_digit_$num"
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Clear / Backspace
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (pinInput.isNotEmpty()) {
                                    pinInput = pinInput.dropLast(1)
                                    errorMessage = null
                                }
                            }
                            .testTag("pin_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Zero Digit
                    KeypadButton(
                        text = "0",
                        onClick = {
                            if (cooldownRemainingSec == 0 && pinInput.length < 6) {
                                pinInput += "0"
                                errorMessage = null
                            }
                        },
                        testTag = "pin_digit_0"
                    )

                    // OK / Submit
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (pinInput.length >= 4 && cooldownRemainingSec == 0) EmeraldPrimary else Slate300)
                            .clickable(enabled = pinInput.length >= 4 && cooldownRemainingSec == 0) {
                                handlePinSubmit()
                            }
                            .testTag("pin_submit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Submit PIN",
                            tint = if (pinInput.length >= 4 && cooldownRemainingSec == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button / Submit text button
            Button(
                onClick = { handlePinSubmit() },
                enabled = pinInput.length >= 4 && cooldownRemainingSec == 0,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
                    .testTag("pin_action_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = when (mode) {
                        LockMode.UNLOCK -> "Unlock App"
                        LockMode.SETUP -> if (setupStep == 1) "Next" else "Confirm & Enable"
                        LockMode.CHANGE -> if (setupStep < 3) "Next" else "Confirm New PIN"
                        LockMode.REMOVE -> "Disable PIN Lock"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onCancel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, Slate300, CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
