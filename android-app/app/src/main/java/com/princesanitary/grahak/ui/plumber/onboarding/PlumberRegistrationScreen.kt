package com.princesanitary.grahak.ui.plumber.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.ServiceSpeciality
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.ErrorBanner
import com.princesanitary.grahak.ui.components.PrimaryButton
import com.princesanitary.grahak.ui.components.SecondaryButton
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.BrandTeal
import com.princesanitary.grahak.ui.theme.BrandTealSoft
import com.princesanitary.grahak.ui.theme.FixitYellow
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import com.princesanitary.grahak.ui.theme.TextWhite

private val STEPS = listOf("You", "Work & rates", "Documents")

/**
 * Plumber onboarding.
 *
 * Modelled on driver sign-up in ride-hailing apps: a short stepped form that
 * collects identity, then what the plumber does and what they charge, then the
 * paperwork the store needs to approve them. Progress is visible at all times
 * and "Continue" explains itself when it is disabled, because a dead button with
 * no reason is the fastest way to lose an applicant.
 */
@Composable
fun PlumberRegistrationScreen(
    viewModel: PlumberRegistrationViewModel = appViewModel(),
    onRegistrationComplete: () -> Unit
) {
    val app by viewModel.application.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uploadsAvailable by viewModel.uploadsAvailable.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var step by remember { mutableIntStateOf(0) }

    if (state is RegistrationState.Submitted) {
        ApplicationSubmitted(onDone = onRegistrationComplete)
        return
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { picked -> viewModel.update { it.copy(photoUri = picked) } } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "plumber_live_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    Uri.fromFile(file)
                }
                viewModel.update { it.copy(photoUri = uri) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraSafely() {
        val hasCam = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCam) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris -> if (uris.isNotEmpty()) viewModel.update { it.copy(documents = uris) } }

    val problem = viewModel.problemWith(step)
    val submitting = state is RegistrationState.Submitting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
    ) {
        // ---------------- header + progress ----------------
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) {
                    Surface(
                        onClick = { step-- },
                        shape = CircleShape,
                        color = SurfaceWhite,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, "Back", Modifier.size(18.dp), tint = Ink)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = "Become a FixIt plumber",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink
                    )
                    Text(
                        text = "Step ${step + 1} of ${STEPS.size} · ${STEPS[step]}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            StepBar(current = step)
            Spacer(Modifier.height(18.dp))
        }

        // ---------------- step body ----------------
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            (state as? RegistrationState.Error)?.let {
                ErrorBanner(it.message)
                Spacer(Modifier.height(14.dp))
            }

            when (step) {
                0 -> IdentityStep(
                    app = app,
                    uploadsAvailable = uploadsAvailable,
                    onTakePhoto = { launchCameraSafely() },
                    onPickPhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onChange = viewModel::update
                )

                1 -> WorkAndRatesStep(
                    app = app,
                    onToggle = viewModel::toggleSpeciality,
                    onRate = viewModel::setRate,
                    onChange = viewModel::update
                )

                2 -> DocumentsStep(
                    app = app,
                    uploadsAvailable = uploadsAvailable,
                    onPickDocs = {
                        docPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onChange = viewModel::update
                )
            }

            Spacer(Modifier.height(20.dp))
        }

        // ---------------- footer ----------------
        Surface(
            color = SurfaceWhite,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(18.dp)
            ) {
                AnimatedVisibility(visible = problem != null) {
                    Column {
                        Text(
                            text = problem.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                PrimaryButton(
                    text = if (step == STEPS.lastIndex) "Submit application" else "Continue",
                    enabled = problem == null && !submitting,
                    loading = submitting,
                    onClick = {
                        viewModel.clearError()
                        if (step == STEPS.lastIndex) viewModel.submit() else step++
                    }
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ progress */

@Composable
private fun StepBar(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        STEPS.indices.forEach { index ->
            val color by animateColorAsState(
                targetValue = when {
                    index < current -> StatusSuccess
                    index == current -> Ink
                    else -> Line
                },
                label = "step$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/* ------------------------------------------------------------- step 1: you */

@Composable
private fun IdentityStep(
    app: PlumberApplication,
    uploadsAvailable: Boolean,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onChange: ((PlumberApplication) -> PlumberApplication) -> Unit
) {
    Column {
        // Photo Card with Live Camera & Gallery options
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(PaperDim)
                            .clickable(enabled = uploadsAvailable, onClick = onTakePhoto),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.photoUri != null) {
                            AsyncImage(
                                model = app.photoUri,
                                contentDescription = "Your photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Rounded.AddAPhoto,
                                contentDescription = "Add photo",
                                modifier = Modifier.size(28.dp),
                                tint = TextTertiary
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Live Plumber Photo", style = MaterialTheme.typography.titleSmall, color = Ink)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (uploadsAvailable) {
                                "Take a live selfie or pick from gallery. Displayed to customers when you accept their job."
                            } else {
                                "Photo uploads are switched off on this project for now — you can add one later."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                if (uploadsAvailable) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = onTakePhoto,
                            shape = RoundedCornerShape(10.dp),
                            color = FixitYellow.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FixitYellow),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.AddAPhoto, null, tint = Ink, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Live Camera", style = MaterialTheme.typography.labelMedium, color = Ink)
                            }
                        }

                        Surface(
                            onClick = onPickPhoto,
                            shape = RoundedCornerShape(10.dp),
                            color = PaperDim,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Line),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Description, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Gallery", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Field("Full name", app.name, "As printed on your Aadhaar") { v ->
            onChange { it.copy(name = v) }
        }

        Spacer(Modifier.height(16.dp))

        Field(
            label = "Mobile number",
            value = app.phone,
            placeholder = "10-digit number",
            keyboardType = KeyboardType.Phone
        ) { v -> onChange { it.copy(phone = v.filter(Char::isDigit).take(10)) } }

        Spacer(Modifier.height(16.dp))

        Field(
            label = "Years of experience",
            value = app.experienceYears,
            placeholder = "e.g. 6",
            keyboardType = KeyboardType.Number
        ) { v -> onChange { it.copy(experienceYears = v.filter(Char::isDigit).take(2)) } }

        Spacer(Modifier.height(16.dp))

        Field(
            label = "About you (optional)",
            value = app.about,
            placeholder = "Bathroom fittings specialist, 6 years in Saharanpur…",
            singleLine = false
        ) { v -> onChange { it.copy(about = v) } }
    }
}

/* ------------------------------------------------- step 2: work and rates */

@Composable
private fun WorkAndRatesStep(
    app: PlumberApplication,
    onToggle: (ServiceSpeciality) -> Unit,
    onRate: (ServiceSpeciality, String) -> Unit,
    onChange: ((PlumberApplication) -> PlumberApplication) -> Unit
) {
    Column {
        Text(
            text = "What work do you take?",
            style = MaterialTheme.typography.titleMedium,
            color = Ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Pick your services and set your own rate for each. Customers " +
                "compare these rates when choosing a plumber.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(Modifier.height(16.dp))

        ServiceSpeciality.entries.forEach { speciality ->
            val selected = speciality in app.specialities
            SpecialityRow(
                speciality = speciality,
                selected = selected,
                rate = app.rates[speciality.id].orEmpty(),
                onToggle = { onToggle(speciality) },
                onRate = { onRate(speciality, it) }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(10.dp))

        Text("Visit charge", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "A flat fee for showing up and inspecting, before any repair is agreed.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(10.dp))

        Field(
            label = "",
            value = app.visitCharge,
            placeholder = "e.g. 150",
            keyboardType = KeyboardType.Number,
            prefix = "₹"
        ) { v -> onChange { it.copy(visitCharge = v.filter(Char::isDigit).take(5)) } }
    }
}

@Composable
private fun SpecialityRow(
    speciality: ServiceSpeciality,
    selected: Boolean,
    rate: String,
    onToggle: () -> Unit,
    onRate: (String) -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) BrandTealSoft else SurfaceWhite,
        label = "specRow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .clickable(onClick = onToggle)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) BrandTeal else PaperDim),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = TextWhite)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    speciality.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink
                )
                Text(
                    speciality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (!selected) {
                Text(
                    text = "from ${formatRupees(speciality.suggestedRate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        // Rate entry only appears once the service is selected.
        AnimatedVisibility(visible = selected) {
            Column {
                Spacer(Modifier.height(12.dp))
                Field(
                    label = "Your rate",
                    value = rate,
                    placeholder = speciality.suggestedRate.toInt().toString(),
                    keyboardType = KeyboardType.Number,
                    prefix = "₹",
                    onValueChange = onRate
                )
            }
        }
    }
}

/* -------------------------------------------------- step 3: documents */

@Composable
private fun DocumentsStep(
    app: PlumberApplication,
    uploadsAvailable: Boolean,
    onPickDocs: () -> Unit,
    onChange: ((PlumberApplication) -> PlumberApplication) -> Unit
) {
    Column {
        Text("Where do you work?", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(12.dp))

        Field(
            label = "Service pincode",
            value = app.servicePincode,
            placeholder = "247001",
            keyboardType = KeyboardType.Number
        ) { v -> onChange { it.copy(servicePincode = v.filter(Char::isDigit).take(6)) } }

        Spacer(Modifier.height(16.dp))

        Field(
            label = "Area you work from",
            value = app.baseAddress,
            placeholder = "e.g. Jwala Nagar, Saharanpur"
        ) { v -> onChange { it.copy(baseAddress = v) } }

        Spacer(Modifier.height(24.dp))

        Text("Verification", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Prince Sanitary Store checks every plumber before they can accept work.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))

        Field(
            label = "Aadhaar number",
            value = app.aadhaarNo,
            placeholder = "12 digits",
            keyboardType = KeyboardType.Number
        ) { v -> onChange { it.copy(aadhaarNo = v.filter(Char::isDigit).take(12)) } }

        Spacer(Modifier.height(16.dp))

        if (uploadsAvailable) {
            SecondaryButton(
                text = if (app.documents.isEmpty()) {
                    "Attach Aadhaar photo"
                } else {
                    "${app.documents.size} document(s) attached"
                },
                leadingIcon = Icons.Rounded.Description,
                onClick = onPickDocs
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PaperDim)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Document upload needs Firebase Storage enabled on this project. " +
                        "You can submit now and add documents once it's switched on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/* ------------------------------------------------------------------- shared */

@Composable
private fun Field(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    prefix: String? = null,
    onValueChange: (String) -> Unit
) {
    Column {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
            },
            prefix = prefix?.let { { Text(it, color = Ink) } },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                focusedBorderColor = Ink,
                unfocusedBorderColor = Line,
                cursorColor = Ink
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ---------------------------------------------------------------- submitted */

@Composable
private fun ApplicationSubmitted(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(FixitYellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(42.dp), tint = Ink)
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Application submitted",
            style = MaterialTheme.typography.headlineSmall,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Prince Sanitary Store will review your details. Once approved " +
                "you'll start receiving job requests and can go online.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        PrimaryButton(text = "View Application Status", onClick = onDone, leadingIcon = Icons.Rounded.Verified)

        Spacer(Modifier.height(12.dp))

        androidx.compose.material3.TextButton(
            onClick = {
                com.princesanitary.grahak.di.ServiceLocator.auth.signOut()
            }
        ) {
            Text("Switch Account / Log Out", color = TextSecondary)
        }
    }
}
