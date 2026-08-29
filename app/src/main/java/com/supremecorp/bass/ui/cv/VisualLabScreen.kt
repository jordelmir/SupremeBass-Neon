package com.supremecorp.bass.ui.cv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supremecorp.bass.cv.VisualAcousticData
import com.supremecorp.bass.ui.theme.TitanColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualLabScreen(
    onCapture: () -> Unit = {},
    isCapturing: Boolean = false,
    visualData: VisualAcousticData? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TitanColors.AbsoluteBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "VISUAL LAB",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TitanColors.NeonCyan,
                letterSpacing = 4.sp,
                shadow = Shadow(
                    color = TitanColors.NeonCyan.copy(alpha = 0.6f),
                    offset = Offset.Zero,
                    blurRadius = 12f
                )
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Visual acoustic analysis — camera-based brightness/contrast",
            style = TextStyle(
                fontSize = 9.sp,
                color = TitanColors.NeonCyan.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Capture controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CAPTURE", style = TextStyle(fontSize = 10.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onCapture,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCapturing) Color(0xFFFF1744) else TitanColors.RadioactiveGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isCapturing) Icons.Default.Stop else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isCapturing) "STOP CAPTURE" else "START CAPTURE",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual data display
        if (visualData != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ANALYSIS", style = TextStyle(fontSize = 10.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                    Spacer(modifier = Modifier.height(12.dp))

                    DataRow("Brightness", String.format("%.3f", visualData.brightness))
                    DataRow("Contrast", String.format("%.3f", visualData.contrast))
                    DataRow("Edge Density", String.format("%.3f", visualData.edgeDensity))

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("COLOR DISTRIBUTION", style = TextStyle(fontSize = 9.sp, color = Color.Gray, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ColorBar("R", visualData.colorHistogram[0], Color.Red)
                        ColorBar("G", visualData.colorHistogram[1], Color.Green)
                        ColorBar("B", visualData.colorHistogram[2], Color.Blue)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Last updated: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(visualData.timestamp)}",
                        style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ABOUT", style = TextStyle(fontSize = 10.sp, color = TitanColors.NeonCyan.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Visual Lab analyzes camera feed brightness and contrast patterns. " +
                            "Useful for visual confirmation of speaker cone movement or " +
                            "environmental lighting analysis during acoustic testing.",
                    style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = TextStyle(fontSize = 12.sp, color = Color.Gray))
        Text(value, style = TextStyle(fontSize = 12.sp, color = TitanColors.NeonCyan, fontWeight = FontWeight.Medium))
    }
}

@Composable
fun ColorBar(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height((value * 100).dp.coerceIn(4.dp, 100.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.7f))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = TextStyle(fontSize = 10.sp, color = Color.Gray))
        Text(String.format("%.2f", value), style = TextStyle(fontSize = 9.sp, color = Color.Gray))
    }
}
