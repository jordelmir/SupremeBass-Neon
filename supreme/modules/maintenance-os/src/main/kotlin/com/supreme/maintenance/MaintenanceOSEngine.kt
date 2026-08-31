package com.supreme.maintenance

import com.supreme.core.*
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Supreme Maintenance OS — "When should I maintain this?"
 *
 * Tracks all assets, schedules maintenance, sends reminders,
 * records completions, and learns optimal intervals.
 */

class MaintenanceOSEngine {

    private val schedules = mutableMapOf<AssetId, MaintenanceSchedule>()

    /**
     * Add an asset and generate a maintenance schedule.
     */
    fun addAsset(asset: Asset): MaintenanceSchedule {
        val tasks = generateMaintenanceTasks(asset)
        val schedule = MaintenanceSchedule(
            assetId = asset.id,
            tasks = tasks,
            nextDue = tasks.minByOrNull { it.dueDate }?.dueDate
        )
        schedules[asset.id] = schedule
        return schedule
    }

    /**
     * Get maintenance summary for all assets.
     */
    fun getMaintenanceSummary(): MaintenanceSummary {
        val allTasks = schedules.values.flatMap { it.tasks }
        val now = Instant.now()

        val overdue = allTasks.filter {
            it.completedDate == null && it.dueDate.isBefore(now)
        }
        val dueSoon = allTasks.filter {
            it.completedDate == null && it.dueDate.isAfter(now) &&
                    it.dueDate.isBefore(now.plus(7, ChronoUnit.DAYS))
        }
        val upcoming = allTasks.filter {
            it.completedDate == null && it.dueDate.isAfter(now.plus(7, ChronoUnit.DAYS)) &&
                    it.dueDate.isBefore(now.plus(30, ChronoUnit.DAYS))
        }

        return MaintenanceSummary(
            totalAssets = schedules.size,
            totalTasks = allTasks.size,
            overdueTasks = overdue,
            dueSoonTasks = dueSoon,
            upcomingTasks = upcoming,
            completedThisMonth = allTasks.filter {
                val completedDate = it.completedDate
                completedDate != null &&
                        completedDate.isAfter(now.minus(30, ChronoUnit.DAYS))
            },
            estimatedMonthlyCost = allTasks.sumOf { it.estimatedCost ?: 0.0 }
        )
    }

    /**
     * Get tasks for a specific asset.
     */
    fun getAssetTasks(assetId: AssetId): List<MaintenanceTask> {
        return schedules[assetId]?.tasks ?: emptyList()
    }

    /**
     * Complete a maintenance task.
     */
    fun completeTask(
        taskId: MaintenanceId,
        cost: Double? = null,
        notes: String? = null,
        actionIds: List<ActionId> = emptyList()
    ): MaintenanceTask? {
        for ((_, schedule) in schedules) {
            val task = schedule.tasks.find { it.id == taskId }
            if (task != null) {
                val completed = task.copy(
                    completedDate = Instant.now(),
                    completedActions = actionIds,
                    actualCost = cost,
                    notes = notes
                )

                // Update schedule
                val updatedTasks = schedule.tasks.map {
                    if (it.id == taskId) completed else it
                }
                val nextTask = if (task.recurring && task.intervalDays != null) {
                    val intervalDays = task.intervalDays!!
                    val nextDue = Instant.now().plus(intervalDays.toLong(), ChronoUnit.DAYS)
                    MaintenanceTask(
                        id = MaintenanceId.generate(),
                        assetId = task.assetId,
                        title = task.title,
                        description = task.description,
                        type = task.type,
                        priority = Priority.MEDIUM,
                        scheduledDate = nextDue,
                        dueDate = nextDue,
                        intervalDays = task.intervalDays,
                        estimatedCost = task.estimatedCost,
                        currency = task.currency,
                        recurring = true
                    )
                } else null

                val newTasks = if (nextTask != null) updatedTasks + nextTask else updatedTasks
                schedules[task.assetId] = schedule.copy(
                    tasks = newTasks,
                    lastCompleted = Instant.now(),
                    nextDue = newTasks.filter { it.completedDate == null }
                        .minByOrNull { it.dueDate }?.dueDate
                )

                return completed
            }
        }
        return null
    }

    /**
     * Check for overdue tasks.
     */
    fun getOverdueTasks(): List<MaintenanceTask> {
        val now = Instant.now()
        return schedules.values.flatMap { it.tasks }
            .filter { it.completedDate == null && it.dueDate.isBefore(now) }
            .sortedBy { it.dueDate }
    }

    /**
     * Get today's tasks.
     */
    fun getTodayTasks(): List<MaintenanceTask> {
        val now = Instant.now()
        val todayEnd = now.plus(1, ChronoUnit.DAYS)
        return schedules.values.flatMap { it.tasks }
            .filter { it.completedDate == null && it.dueDate.isBefore(todayEnd) }
            .sortedBy { it.dueDate }
    }

    /**
     * Learn from completed tasks and adjust intervals.
     */
    fun learnFromCompletion(taskId: MaintenanceId, actualInterval: Int) {
        // Future: adjust interval based on actual usage patterns
        // For now, just log the learning
    }

    // ─────────────────────────────────────────────────────────────
    // TASK GENERATION
    // ─────────────────────────────────────────────────────────────

    private fun generateMaintenanceTasks(asset: Asset): List<MaintenanceTask> {
        val tasks = mutableListOf<MaintenanceTask>()
        val now = Instant.now()

        when (asset.category) {
            AssetCategory.APPLIANCE -> {
                // Refrigerator
                if (asset.subcategory?.contains("refrigerator", true) == true ||
                    asset.name.contains("refrigerator", true)) {
                    tasks.add(MaintenanceTask(
                        id = MaintenanceId.generate(),
                        assetId = asset.id,
                        title = "Clean condenser coils",
                        description = "Clean the condenser coils at the back/bottom of the refrigerator",
                        type = MaintenanceType.PREVENTIVE,
                        priority = Priority.MEDIUM,
                        scheduledDate = now.plus(180, ChronoUnit.DAYS),
                        dueDate = now.plus(180, ChronoUnit.DAYS),
                        intervalDays = 180,
                        estimatedCost = 5000.0,
                        recurring = true
                    ))
                    tasks.add(MaintenanceTask(
                        id = MaintenanceId.generate(),
                        assetId = asset.id,
                        title = "Check door seals",
                        description = "Inspect door gaskets for wear and proper sealing",
                        type = MaintenanceType.INSPECTION,
                        priority = Priority.LOW,
                        scheduledDate = now.plus(365, ChronoUnit.DAYS),
                        dueDate = now.plus(365, ChronoUnit.DAYS),
                        intervalDays = 365,
                        recurring = true
                    ))
                }

                // Washing Machine
                if (asset.subcategory?.contains("washing", true) == true ||
                    asset.name.contains("washing", true)) {
                    tasks.add(MaintenanceTask(
                        id = MaintenanceId.generate(),
                        assetId = asset.id,
                        title = "Clean lint filter",
                        description = "Clean the lint filter and drain pump filter",
                        type = MaintenanceType.CLEANING,
                        priority = Priority.MEDIUM,
                        scheduledDate = now.plus(30, ChronoUnit.DAYS),
                        dueDate = now.plus(30, ChronoUnit.DAYS),
                        intervalDays = 30,
                        recurring = true
                    ))
                    tasks.add(MaintenanceTask(
                        id = MaintenanceId.generate(),
                        assetId = asset.id,
                        title = "Check hoses",
                        description = "Inspect water hoses for cracks or leaks",
                        type = MaintenanceType.INSPECTION,
                        priority = Priority.HIGH,
                        scheduledDate = now.plus(180, ChronoUnit.DAYS),
                        dueDate = now.plus(180, ChronoUnit.DAYS),
                        intervalDays = 180,
                        recurring = true
                    ))
                }

                // AC
                if (asset.subcategory?.contains("air", true) == true ||
                    asset.name.contains("AC", true) ||
                    asset.name.contains("air conditioner", true)) {
                    tasks.add(MaintenanceTask(
                        id = MaintenanceId.generate(),
                        assetId = asset.id,
                        title = "Clean AC filters",
                        description = "Clean or replace air filters",
                        type = MaintenanceType.CLEANING,
                        priority = Priority.HIGH,
                        scheduledDate = now.plus(30, ChronoUnit.DAYS),
                        dueDate = now.plus(30, ChronoUnit.DAYS),
                        intervalDays = 30,
                        estimatedCost = 3000.0,
                        recurring = true
                    ))
                    tasks.add(MaintenanceTask(
                        id = MaintenanceId.generate(),
                        assetId = asset.id,
                        title = "Professional AC service",
                        description = "Schedule professional cleaning and gas check",
                        type = MaintenanceType.PREVENTIVE,
                        priority = Priority.MEDIUM,
                        scheduledDate = now.plus(365, ChronoUnit.DAYS),
                        dueDate = now.plus(365, ChronoUnit.DAYS),
                        intervalDays = 365,
                        estimatedCost = 25000.0,
                        recurring = true
                    ))
                }
            }

            AssetCategory.VEHICLE -> {
                tasks.add(MaintenanceTask(
                    id = MaintenanceId.generate(),
                    assetId = asset.id,
                    title = "Oil change",
                    description = "Change engine oil and filter",
                    type = MaintenanceType.PREVENTIVE,
                    priority = Priority.HIGH,
                    scheduledDate = now.plus(90, ChronoUnit.DAYS),
                    dueDate = now.plus(90, ChronoUnit.DAYS),
                    intervalDays = 90,
                    estimatedCost = 35000.0,
                    recurring = true
                ))
                tasks.add(MaintenanceTask(
                    id = MaintenanceId.generate(),
                    assetId = asset.id,
                    title = "Tire rotation",
                    description = "Rotate tires and check pressure",
                    type = MaintenanceType.PREVENTIVE,
                    priority = Priority.MEDIUM,
                    scheduledDate = now.plus(180, ChronoUnit.DAYS),
                    dueDate = now.plus(180, ChronoUnit.DAYS),
                    intervalDays = 180,
                    estimatedCost = 8000.0,
                    recurring = true
                ))
                tasks.add(MaintenanceTask(
                    id = MaintenanceId.generate(),
                    assetId = asset.id,
                    title = "Brake inspection",
                    description = "Check brake pads and fluid",
                    type = MaintenanceType.INSPECTION,
                    priority = Priority.HIGH,
                    scheduledDate = now.plus(365, ChronoUnit.DAYS),
                    dueDate = now.plus(365, ChronoUnit.DAYS),
                    intervalDays = 365,
                    estimatedCost = 15000.0,
                    recurring = true
                ))
            }

            AssetCategory.ELECTRONICS -> {
                tasks.add(MaintenanceTask(
                    id = MaintenanceId.generate(),
                    assetId = asset.id,
                    title = "Clean dust",
                    description = "Clean dust from vents and surfaces",
                    type = MaintenanceType.CLEANING,
                    priority = Priority.LOW,
                    scheduledDate = now.plus(90, ChronoUnit.DAYS),
                    dueDate = now.plus(90, ChronoUnit.DAYS),
                    intervalDays = 90,
                    recurring = true
                ))
            }

            AssetCategory.STRUCTURE -> {
                tasks.add(MaintenanceTask(
                    id = MaintenanceId.generate(),
                    assetId = asset.id,
                    title = "Inspect gutters",
                    description = "Clean and inspect gutters",
                    type = MaintenanceType.INSPECTION,
                    priority = Priority.MEDIUM,
                    scheduledDate = now.plus(180, ChronoUnit.DAYS),
                    dueDate = now.plus(180, ChronoUnit.DAYS),
                    intervalDays = 180,
                    recurring = true
                ))
            }

            else -> {
                tasks.add(MaintenanceTask(
                    id = MaintenanceId.generate(),
                    assetId = asset.id,
                    title = "General inspection",
                    description = "Visual inspection of condition",
                    type = MaintenanceType.INSPECTION,
                    priority = Priority.LOW,
                    scheduledDate = now.plus(365, ChronoUnit.DAYS),
                    dueDate = now.plus(365, ChronoUnit.DAYS),
                    intervalDays = 365,
                    recurring = true
                ))
            }
        }

        return tasks
    }
}

/**
 * Maintenance summary.
 */
data class MaintenanceSummary(
    val totalAssets: Int,
    val totalTasks: Int,
    val overdueTasks: List<MaintenanceTask>,
    val dueSoonTasks: List<MaintenanceTask>,
    val upcomingTasks: List<MaintenanceTask>,
    val completedThisMonth: List<MaintenanceTask>,
    val estimatedMonthlyCost: Double
) {
    val hasOverdue: Boolean get() = overdueTasks.isNotEmpty()
    val hasDueSoon: Boolean get() = dueSoonTasks.isNotEmpty()
    val nextAction: String get() = when {
        overdueTasks.isNotEmpty() -> "${overdueTasks.size} overdue tasks"
        dueSoonTasks.isNotEmpty() -> "${dueSoonTasks.size} tasks due soon"
        upcomingTasks.isNotEmpty() -> "All caught up! Next: ${upcomingTasks.size} upcoming"
        else -> "No maintenance scheduled"
    }
}
