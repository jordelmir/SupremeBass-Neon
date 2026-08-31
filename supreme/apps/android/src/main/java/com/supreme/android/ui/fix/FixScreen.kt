package com.supreme.android.ui.fix

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supreme.core.Diagnosis
import com.supreme.core.Cause
import com.supreme.core.CheckStatus

/**
 * Fix AI Screen — diagnose problems with camera + audio + vibration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixScreen(
    onBack: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var diagnosis by remember { mutableStateOf<Diagnosis?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Fix AI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Point camera at anything — diagnose what's wrong",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Camera Preview Placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "Camera",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Record Button
        Button(
            onClick = { isRecording = !isRecording },
            modifier = Modifier.fillMaxWidth(),
            colors = if (isRecording) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnosis Results
        diagnosis?.let { diag ->
            DiagnosisCard(diag)
        } ?: run {
            // Placeholder state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Record audio/video to diagnose",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosisCard(diagnosis: Diagnosis) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Diagnosis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Confidence: ${(diagnosis.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Most Likely Causes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            diagnosis.mostLikelyCauses.forEach { cause ->
                CauseItem(cause)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Checks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            diagnosis.checks.forEach { check ->
                CheckItem(check.name, check.status, check.detail)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Next Tests",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            diagnosis.nextTests.forEach { test ->
                Text(
                    text = "• $test",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun CauseItem(cause: Cause) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = { cause.probability.toFloat() },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${(cause.probability * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = cause.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
    Text(
        text = cause.explanation,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun CheckItem(name: String, status: CheckStatus, detail: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (status) {
                CheckStatus.PASSED -> Icons.Default.CheckCircle
                CheckStatus.WARNING -> Icons.Default.Warning
                CheckStatus.FAILED -> Icons.Default.Error
                CheckStatus.UNKNOWN -> Icons.Default.Help
            },
            contentDescription = status.name,
            tint = when (status) {
                CheckStatus.PASSED -> MaterialTheme.colorScheme.primary
                CheckStatus.WARNING -> MaterialTheme.colorScheme.error
                CheckStatus.FAILED -> MaterialTheme.colorScheme.error
                CheckStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
