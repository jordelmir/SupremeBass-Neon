package com.supremecorp.bass.ui.neural

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supremecorp.bass.ui.components.NeonScreenTitle
import com.supremecorp.bass.ui.components.NeonCard
import com.supremecorp.bass.ui.components.NeonSectionHeader
import com.supremecorp.bass.ui.components.MatrixRain
import com.supremecorp.bass.ui.theme.TitanColors

@Composable
fun NeuralLabScreen() {
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
                title = "NEURAL LAB",
                subtitle = "AI-powered audio analysis — coming soon"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Placeholder content
            NeonCard(glowColor = Color(0xFFFF00FF)) {
                NeonSectionHeader(text = "COMING SOON")
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Neural Lab will feature:",
                    style = TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                FeatureItem("AI-powered sound classification", "Identify instruments, vocals, and audio elements")
                FeatureItem("Neural network-based enhancement", "Smart audio processing using machine learning")
                FeatureItem("Real-time audio transcription", "Convert speech to text with AI models")
                FeatureItem("Sound scene detection", "Automatically detect and categorize audio environments")
            }

            Spacer(modifier = Modifier.height(16.dp))

            NeonCard(glowColor = TitanColors.NeonCyan) {
                NeonSectionHeader(text = "PLANNED FEATURES")
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Phase 1: Sound Classification",
                    style = TextStyle(fontSize = 11.sp, color = TitanColors.NeonCyan)
                )
                Text(
                    text = "Phase 2: AI Enhancement",
                    style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                )
                Text(
                    text = "Phase 3: Real-time Transcription",
                    style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureItem(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = TitanColors.RadioactiveGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            )
            Text(
                text = description,
                style = TextStyle(fontSize = 10.sp, color = Color.Gray)
            )
        }
    }
}
