package com.supremecorp.bass.infrastructure.export

import com.supremecorp.bass.domain.model.SignalTelemetry
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TelemetryExporter {

    fun exportSession(telemetry: SignalTelemetry): String {
        val json = JSONObject()
        json.put("schemaVersion", 1)
        json.put("sessionId", telemetry.sessionId)
        json.put("timestamp", telemetry.timestamp)
        json.put("timestampISO", formatTimestamp(telemetry.timestamp))

        val device = JSONObject()
        device.put("manufacturer", telemetry.manufacturer)
        device.put("model", telemetry.model)
        device.put("androidVersion", telemetry.androidVersion)
        json.put("device", device)

        val signal = JSONObject()
        signal.put("waveform", telemetry.waveform.name)
        signal.put("frequencyHz", telemetry.frequencyHz)
        signal.put("amplitude", telemetry.amplitude.toDouble())
        signal.put("sampleRate", telemetry.sampleRate)
        signal.put("encoding", telemetry.encoding)
        signal.put("bufferFrames", telemetry.bufferFrames)
        json.put("signal", signal)

        val metrics = JSONObject()
        metrics.put("peak", telemetry.peak.toDouble())
        metrics.put("rms", telemetry.rms.toDouble())
        metrics.put("durationMs", telemetry.durationMs)
        metrics.put("underruns", telemetry.underruns)
        json.put("metrics", metrics)

        json.put("audioRoute", telemetry.audioRoute.name)
        json.put("terminationReason", telemetry.terminationReason)

        return json.toString(2)
    }

    fun exportSessions(telemetryList: List<SignalTelemetry>): String {
        val wrapper = JSONObject()
        wrapper.put("schemaVersion", 1)
        wrapper.put("exportTimestamp", formatTimestamp(System.currentTimeMillis()))
        wrapper.put("sessionCount", telemetryList.size)

        val sessions = JSONArray()
        for (t in telemetryList) {
            sessions.put(JSONObject(exportSession(t)))
        }
        wrapper.put("sessions", sessions)

        return wrapper.toString(2)
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }
}
