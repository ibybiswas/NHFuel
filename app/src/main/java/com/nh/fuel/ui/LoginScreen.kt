package com.nh.fuel.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.core.content.ContextCompat
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.nh.fuel.BuildConfig
import com.nh.fuel.data.AppUserSession
import com.nh.fuel.data.KeyStatus
import com.nh.fuel.data.Role
import com.nh.fuel.data.StaffAccessKey
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors

// Read whitelisted owner emails injected securely via Gradle BuildConfig / GitHub Secrets
val WHITELISTED_OWNER_GMAILS: List<String> by lazy {
    BuildConfig.MASTER_OWNER_EMAILS
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (AppUserSession) -> Unit
) {
    var rawInputKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showQrScanner by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val formattedKey = remember(rawInputKey) {
        val clean = rawInputKey.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        if (clean.length > 4) "${clean.take(4)} - ${clean.drop(4).take(4)}" else clean
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )

                Text(
                    text = "NH FUEL STATION",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.1.sp
                )

                Text(
                    text = "Staff & Manager Login",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                OutlinedButton(
                    onClick = { showQrScanner = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Staff QR Code", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Text("— OR ENTER ACCESS KEY —", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = formattedKey,
                    onValueChange = { input ->
                        val clean = input.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
                        if (clean.length <= 8) {
                            rawInputKey = clean
                        }
                    },
                    label = { Text("8-Character Key (e.g. NH78-K92B)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else {
                    Button(
                        onClick = {
                            if (rawInputKey.length == 8) {
                                isLoading = true
                                errorMessage = null
                                verifyAccessCode(
                                    code = rawInputKey,
                                    onSuccess = { session ->
                                        isLoading = false
                                        onLoginSuccess(session)
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            } else {
                                errorMessage = "Please enter a full 8-character access key."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Log In with Access Key", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // --- REAL GOOGLE SIGN-IN VIA CREDENTIAL MANAGER & FIREBASE ---
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                errorMessage = null

                                val credentialManager = CredentialManager.create(context)

                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId("954316338055-hdhb96jl5l7fg1bm2r855bf8skpdjdqv.apps.googleusercontent.com") // Paste Web Client ID here
                                    .setAutoSelectEnabled(false)
                                    .build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(context, request)
                                val credential = result.credential

                                if (credential is androidx.credentials.CustomCredential) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    val googleToken = googleIdTokenCredential.idToken
                                    val googleAuthCredential = GoogleAuthProvider.getCredential(googleToken, null)

                                    FirebaseAuth.getInstance().signInWithCredential(googleAuthCredential)
                                        .addOnSuccessListener { authResult ->
                                            isLoading = false
                                            val user = authResult.user
                                            val email = user?.email?.lowercase(Locale.getDefault()) ?: ""

                                            // Validate against whitelisted owner emails
                                            val isWhitelisted = WHITELISTED_OWNER_GMAILS.any {
                                                it.lowercase(Locale.getDefault()) == email
                                            }

                                            if (isWhitelisted || WHITELISTED_OWNER_GMAILS.isEmpty()) {
                                                val session = AppUserSession(
                                                    emailOrKey = email,
                                                    displayName = user?.displayName ?: "Station Owner",
                                                    role = Role.SUPER_ADMIN,
                                                    canEditPastDates = true,
                                                    isOwnerLogin = true
                                                )
                                                onLoginSuccess(session)
                                            } else {
                                                FirebaseAuth.getInstance().signOut()
                                                errorMessage = "Access Denied: '$email' is not whitelisted."
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            errorMessage = "Firebase Error: ${e.localizedMessage}"
                                        }
                                }
                            } catch (e: GetCredentialCancellationException) {
                                isLoading = false
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Google Sign-In Error: ${e.localizedMessage ?: "Failed to initiate sign-in"}"
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Owner Google Sign-In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showQrScanner) {
        CameraQrScannerDialog(
            onDismiss = { showQrScanner = false },
            onCodeScanned = { scannedCode ->
                showQrScanner = false
                val clean = scannedCode.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
                if (clean.length == 8) {
                    rawInputKey = clean
                    isLoading = true
                    verifyAccessCode(
                        code = clean,
                        onSuccess = { session ->
                            isLoading = false
                            onLoginSuccess(session)
                        },
                        onError = { err ->
                            isLoading = false
                            errorMessage = err
                        }
                    )
                }
            }
        )
    }
}

private fun verifyAccessCode(
    code: String,
    onSuccess: (AppUserSession) -> Unit,
    onError: (String) -> Unit
) {
    // Strip hyphens and non-alphanumeric characters. The clean code is the Firestore document ID.
    val cleanInputCode = code.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    val auth = FirebaseAuth.getInstance()

    // Firestore rules require a signed-in user, so give this device an anonymous identity first.
    if (auth.currentUser != null) {
        lookupAccessKey(cleanInputCode, onSuccess, onError)
    } else {
        auth.signInAnonymously()
            .addOnSuccessListener { lookupAccessKey(cleanInputCode, onSuccess, onError) }
            .addOnFailureListener {
                onError("Device sign-in failed: ${it.localizedMessage ?: "Unknown error"}")
            }
    }
}

private fun lookupAccessKey(
    cleanCode: String,
    onSuccess: (AppUserSession) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid == null) {
        onError("Device sign-in failed. Please try again.")
        return
    }

    // Single-document lookup (no listing of all keys)
    db.collection("access_keys").document(cleanCode)
        .get()
        .addOnSuccessListener { doc ->
            val foundKey = if (doc.exists()) doc.toObject(StaffAccessKey::class.java) else null

            if (foundKey == null) {
                onError("Invalid Key: Access key not found.")
            } else if (foundKey.status != KeyStatus.ACTIVE) {
                onError("Access Denied: This key has been revoked by the Admin.")
            } else {
                // Claim this key for this device so Firestore rules recognise the session
                db.collection("staff_sessions").document(uid)
                    .set(mapOf("code" to cleanCode, "createdAt" to System.currentTimeMillis()))
                    .addOnSuccessListener {
                        onSuccess(
                            AppUserSession(
                                emailOrKey = foundKey.accessCode,
                                displayName = foundKey.nickname,
                                role = foundKey.role,
                                canEditPastDates = foundKey.canEditPastDates,
                                canEditFinancePastDates = foundKey.canEditFinancePastDates,
                                isReadOnly = foundKey.isReadOnly,
                                isOwnerLogin = false
                            )
                        )
                    }
                    .addOnFailureListener {
                        onError("Could not start session: ${it.localizedMessage ?: "Unknown error"}")
                    }
            }
        }
        .addOnFailureListener {
            onError("Network error verifying key: ${it.localizedMessage ?: "Unknown error"}")
        }
}

@Composable
private fun CameraQrScannerDialog(
    onDismiss: () -> Unit,
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .height(420.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scan Staff Login QR Code", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    ) {
                        CameraPreviewView(onCodeScanned = onCodeScanned)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Camera permission is required to scan QR code.", fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewView(onCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val scanner = BarcodeScanning.getClient()
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    if (!rawValue.isNullOrBlank()) {
                                        onCodeScanned(rawValue)
                                        break
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun generateQrCodeBitmap(text: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
    }
    return bmp
}
