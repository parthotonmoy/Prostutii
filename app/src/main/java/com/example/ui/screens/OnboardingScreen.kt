package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.model.UserAvailability
import com.example.model.UserSettings
import com.example.ui.components.DraftingCard
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.DraftingPrimaryButton
import com.example.ui.components.NotebookBackground
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.util.DateUtil
import java.time.LocalDate

@Composable
fun OnboardingScreen(
    availability: UserAvailability,
    onSaveAvailability: (UserAvailability) -> Unit,
    onCompleteOnboarding: () -> Unit,
    onLoadSampleData: () -> Unit,
    onAddFirstSubject: (name: String, examDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 2 local hours
    var weekdayMinutes by remember(availability) {
        mutableStateOf(availability.weekdayMinutes.toMutableList())
    }

    // Step 3 inputs
    var subjectNameInput by remember { mutableStateOf("") }
    var examDaysInput by remember { mutableStateOf("14") }

    NotebookBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Progress (three ruler ticks, current tick blue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Three ruler ticks
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..3).forEach { stepIndex ->
                        val isCurrent = stepIndex == currentStep
                        val isPassed = stepIndex < currentStep
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(if (isCurrent) 24.dp else 16.dp)
                                .background(
                                    when {
                                        isCurrent -> colors.blue
                                        isPassed -> colors.ink
                                        else -> colors.rule
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                TextButton(onClick = onCompleteOnboarding) {
                    Text("Skip", style = ProstutiTypography.bodyMedium, color = colors.ink2)
                }
            }

            // Step Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentStep) {
                    1 -> {
                        // Step 1: Meaning & Privacy
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Prostuti (প্রস্তুতি)",
                                style = ProstutiTypography.h1,
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Prostuti (প্রস্তুতি): preparation",
                                style = ProstutiTypography.bodyMedium,
                                color = colors.ink2
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Prostuti takes your exam dates, topics, difficulty and study hours, and produces a daily timetable that rebuilds itself when life gets in the way.",
                                style = ProstutiTypography.bodyLarge,
                                color = colors.ink
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Everything stays on this phone. There is no account and no server.",
                                style = ProstutiTypography.caption,
                                color = colors.ink3
                            )
                        }
                    }
                    2 -> {
                        // Step 2: Study Hours Stepper Rows
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Your daily study hours",
                                    style = ProstutiTypography.h2,
                                    color = colors.ink
                                )
                                Text(
                                    text = "How much can you actually study each day?",
                                    style = ProstutiTypography.bodyMedium,
                                    color = colors.ink2
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            val dayNames = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
                            items(dayNames.size) { index ->
                                val mins = weekdayMinutes.getOrElse(index) { 180 }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.card, RoundedCornerShape(4.dp))
                                        .border(1.dp, colors.rule, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dayNames[index], style = ProstutiTypography.bodyMedium, color = colors.ink)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DraftingOutlinedButton(
                                            text = "-",
                                            onClick = {
                                                val updated = weekdayMinutes.toMutableList()
                                                updated[index] = (mins - 30).coerceAtLeast(0)
                                                weekdayMinutes = updated
                                            },
                                            modifier = Modifier.size(width = 40.dp, height = 36.dp)
                                        )
                                        Text(
                                            text = DateUtil.formatDuration(mins),
                                            style = ProstutiTypography.monoSmall,
                                            color = colors.ink,
                                            modifier = Modifier.width(60.dp)
                                        )
                                        DraftingOutlinedButton(
                                            text = "+",
                                            onClick = {
                                                val updated = weekdayMinutes.toMutableList()
                                                updated[index] = mins + 30
                                                weekdayMinutes = updated
                                            },
                                            modifier = Modifier.size(width = 40.dp, height = 36.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Step 3: Add First Subject or Sample Data
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Add your first subject",
                                style = ProstutiTypography.h2,
                                color = colors.ink
                            )
                            Text(
                                text = "Enter an exam to start planning, or load realistic sample engineering courses.",
                                style = ProstutiTypography.bodyMedium,
                                color = colors.ink2
                            )

                            DraftingCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = subjectNameInput,
                                        onValueChange = { subjectNameInput = it },
                                        label = { Text("Subject name (e.g. Signals & Systems)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = examDaysInput,
                                        onValueChange = { examDaysInput = it },
                                        label = { Text("Exam in how many days? (e.g. 14)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            DraftingOutlinedButton(
                                text = "Load sample data instead",
                                onClick = {
                                    onLoadSampleData()
                                    onCompleteOnboarding()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    DraftingOutlinedButton(
                        text = "Back",
                        onClick = { currentStep-- }
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                DraftingPrimaryButton(
                    text = if (currentStep == 3) "Finish" else "Next",
                    onClick = {
                        if (currentStep == 2) {
                            onSaveAvailability(availability.copy(weekdayMinutes = weekdayMinutes))
                            currentStep++
                        } else if (currentStep == 3) {
                            if (subjectNameInput.isNotBlank()) {
                                val days = examDaysInput.toIntOrNull() ?: 14
                                val examDateIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay() + days)
                                onAddFirstSubject(subjectNameInput.trim(), examDateIso)
                            }
                            onCompleteOnboarding()
                        } else {
                            currentStep++
                        }
                    }
                )
            }
        }
    }
}
