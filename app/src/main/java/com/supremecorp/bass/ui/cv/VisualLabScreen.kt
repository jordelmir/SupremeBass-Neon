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
import com.supremecorp.bass.ui.components.NeonScreenTitle
import com.supremecorp.bass.ui.components.NeonCard
import com.supremecorp.bass.ui.components.NeonButton
import com.supremecorp.bass.ui.components.NeonSectionHeader
import com.supremecorp.bass.ui.components.MatrixRain
import com.supremecorp.bass.ui.theme.TitanColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualLabScreen(
    onCapture: () -> Unit = {},
    isCapturing: Boolean = false,
    visualData: VisualAcousticData? = null
) {
    Box(modifier = Modifier.fillMaxSize().background(TitanColors.AbsoluteBlack)) {
        MatrixRain(
            modifier = Modifier.fillMaxSize(),
            color = TitanColors.NeonCyan.copy(alpha = 0.12f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            NeonScreenTitle(
                title = "VISUAL LAB",
                subtitle = "Visual acoustic analysis — camera-based brightness/contrast"
            )

            // Capture controls
            NeonCard(glowColor = TitanColors.RadioactiveGreen) {
                NeonSectionHeader(text = "CAPTURE")
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

            Spacer(modifier = Modifier.height(16.dp))

            // Visual data display
            if (visualData != null) {
                NeonCard(glowColor = TitanColors.NeonCyan) {
                    NeonSectionHeader(text = "ANALYSIS")
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

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            NeonCard(glowColor = TitanColors.NeonCyan) {
                NeonSectionHeader(text = "ABOUT")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Visual Lab analyzes camera feed brightness and contrast patterns. " +
                            "Useful for visual confirmation of speaker cone movement or " +
                            "environmental lighting analysis during acoustic testing.",
                    style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
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
