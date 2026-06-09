package com.sumit.paymentalert.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.sumit.paymentalert.ui.viewmodel.PaymentViewModel
import java.io.OutputStream
import java.util.Hashtable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScreen(
    viewModel: PaymentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val upiId by viewModel.upiId.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var upiInputText by remember { mutableStateOf(upiId) }
    var isEditingUpId by remember { mutableStateOf(upiId.isEmpty()) }

    // Generate QR Bitmap when UPI changes
    val qrBitmap = remember(upiId, userName) {
        if (upiId.isNotEmpty()) {
            val nameEscaped = userName.replace(" ", "%20")
            val upiUri = "upi://pay?pa=$upiId&pn=$nameEscaped&cu=INR"
            generateQrCode(upiUri)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Header Screen
            Text(
                text = "My Business QR",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, top = 4.dp),
                textAlign = TextAlign.Start
            )
            
            Text(
                text = "Scan to receive instantaneous payments.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Start
            )

            if (isEditingUpId) {
                // Settings/Setup Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .testTag("upi_setup_box"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Setup Primary UPI ID",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Please enter your active UPI address (e.g. 7488625014@ybl) so customers can scan your QR code and credit you directly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        OutlinedTextField(
                            value = upiInputText,
                            onValueChange = { upiInputText = it },
                            placeholder = { Text("e.g. 7488625014@ybl") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upi_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (upiInputText.trim().isNotEmpty()) {
                                        viewModel.setUpiId(upiInputText.trim())
                                        isEditingUpId = false
                                    } else {
                                        Toast.makeText(context, "Please enter a valid UPI address", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        )

                        Button(
                            onClick = {
                                if (upiInputText.trim().isNotEmpty()) {
                                    viewModel.setUpiId(upiInputText.trim())
                                    isEditingUpId = false
                                } else {
                                    Toast.makeText(context, "Please enter a valid UPI address", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_upi_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save & Generate QR Code", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // QR Display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Scan and Pay with Any UPI App",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF146C2E), // Money Green
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Render QR
                        qrBitmap?.let { bmp ->
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "UPI Business QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic UPI address tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "UPI ID: $upiId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
                                    Toast.makeText(context, "UPI ID Copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copy UPI address",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Download / share row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            qrBitmap?.let { bmp ->
                                val posterBmp = createDownloadPoster(bmp, userName, upiId)
                                savePosterToGallery(context, posterBmp)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("download_poster_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Poster", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Use this link to pay me securely via UPI:\nUPI ID: $upiId\nName: $userName\n\nGenerated with Payment Alert App.")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share UPI Details"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_upi_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Details", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        upiInputText = upiId
                        isEditingUpId = true
                    },
                    modifier = Modifier.testTag("change_upi_id_button")
                ) {
                    Text("Change UPI ID", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand trust card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF146C2E), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Supported on all UPI apps GPay, PhonePe, Paytm, BHIM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// 🚀 Highly Standard ZXing QR generation block 🚀
private fun generateQrCode(upiUri: String): Bitmap? {
    return try {
        val hints = Hashtable<EncodeHintType, ErrorCorrectionLevel>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H

        val bitMatrix = MultiFormatWriter().encode(upiUri, BarcodeFormat.QR_CODE, 512, 512, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }

        val baseQr = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        baseQr.setPixels(pixels, 0, width, 0, 0, width, height)
        baseQr
    } catch (e: Exception) {
        null
    }
}

// 🎨 Canvas Graphics Poster creator 🎨
private fun createDownloadPoster(qrBitmap: Bitmap, merchantName: String, upiId: String): Bitmap {
    val width = 1080
    val height = 1920
    val poster = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(poster)
    
    // Draw solid white background poster card
    canvas.drawColor(AndroidColor.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. Draw elegant top purple gradients
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, 0f, 500f,
        AndroidColor.parseColor("#3145F5"),
        AndroidColor.parseColor("#8B61F6"),
        android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), 480f, paint)
    paint.shader = null // Clear shader

    // 2. Draw Top Heading Text
    paint.color = AndroidColor.WHITE
    paint.textSize = 64f
    paint.isFakeBoldText = true
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("Payment Alert Merchant", width / 2f, 200f, paint)

    paint.textSize = 40f
    paint.isFakeBoldText = false
    canvas.drawText("BHIM UPI - INSTANT AUDIO NOTIFICATION ENABLED", width / 2f, 280f, paint)

    // 3. Draw Subtext
    paint.color = AndroidColor.parseColor("#424242")
    paint.textSize = 48f
    paint.isFakeBoldText = true
    canvas.drawText("ACCEPTED HERE", width / 2f, 620f, paint)

    paint.color = AndroidColor.parseColor("#757575")
    paint.textSize = 34f
    paint.isFakeBoldText = false
    canvas.drawText("Scan & Pay with Any Banking App", width / 2f, 680f, paint)

    // 4. Draw Qr Code card and shadow
    paint.color = AndroidColor.parseColor("#EDEFF8")
    canvas.drawRoundRect(
        (width - 760) / 2f - 20f,
        760f - 20f,
        (width + 760) / 2f + 20f,
        1520f + 20f,
        32f, 32f, paint
    )

    val scaledQr = Bitmap.createScaledBitmap(qrBitmap, 760, 760, true)
    canvas.drawBitmap(scaledQr, (width - 760) / 2f, 760f, null)

    // 5. Merchant Name
    paint.color = AndroidColor.BLACK
    paint.textSize = 56f
    paint.isFakeBoldText = true
    canvas.drawText(merchantName, width / 2f, 1640f, paint)

    paint.color = AndroidColor.parseColor("#3145F5")
    paint.textSize = 36f
    paint.isFakeBoldText = false
    canvas.drawText("UPI ID: $upiId", width / 2f, 1710f, paint)

    // 6. Footer Brand note
    paint.color = AndroidColor.parseColor("#9E9E9E")
    paint.textSize = 30f
    canvas.drawText("© 2026 Payment Alert • Smart Voice Assistant", width / 2f, 1850f, paint)

    return poster
}

// Save image utility
private fun savePosterToGallery(context: Context, bitmap: Bitmap) {
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "UPI_Payment_Poster_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PaymentAlert")
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            val outStream: OutputStream? = context.contentResolver.openOutputStream(uri)
            if (outStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
                outStream.close()
                Toast.makeText(context, "Business QR Poster saved to Gallery!", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
