package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.Session
import com.example.model.Topic
import com.example.ui.components.DifficultyBars
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.DraftingPrimaryButton
import com.example.ui.components.SegmentedControl
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.ui.theme.SUBJECT_PIGMENTS
import com.example.util.DateUtil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, examDateIso: String, examTime: String?, weight: Int, colorIndex: Int) -> Unit
) {
    val colors = ProstutiTheme.colors
    var nameInput by remember { mutableStateOf("") }
    var daysUntilExamInput by remember { mutableStateOf("14") }
    var examTimeInput by remember { mutableStateOf("") }
    var selectedWeight by remember { mutableIntStateOf(2) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Subject", style = ProstutiTypography.h2, color = colors.ink)

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Subject name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = daysUntilExamInput,
                onValueChange = { daysUntilExamInput = it },
                label = { Text("Days until exam (e.g. 14)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = examTimeInput,
                onValueChange = { examTimeInput = it },
                label = { Text("Exam time (optional, e.g. 10:00)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Importance (Weight 1-3)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Importance weight (1 to 3)", style = ProstutiTypography.caption, color = colors.ink2)
                SegmentedControl(
                    options = listOf("1 (Low)", "2 (Standard)", "3 (High)"),
                    selectedIndex = selectedWeight - 1,
                    onOptionSelected = { selectedWeight = it + 1 },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Colour Swatches
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Subject colour pigment", style = ProstutiTypography.caption, color = colors.ink2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SUBJECT_PIGMENTS.forEachIndexed { idx, pigment ->
                        val isSelected = selectedColorIndex == idx
                        val col = if (colors.isDark) pigment.dark else pigment.light
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(col, RoundedCornerShape(4.dp))
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) colors.ink else colors.rule,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { selectedColorIndex = idx }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DraftingPrimaryButton(
                text = "Save Subject",
                enabled = nameInput.isNotBlank() && (daysUntilExamInput.toIntOrNull() ?: 0) > 0,
                onClick = {
                    val days = daysUntilExamInput.toIntOrNull() ?: 14
                    val examIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay() + days)
                    onConfirm(
                        nameInput.trim(),
                        examIso,
                        examTimeInput.trim().ifBlank { null },
                        selectedWeight,
                        selectedColorIndex
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

data class ParsedTopicRow(
    val title: String,
    var difficulty: Int = 3,
    var isChecked: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTopicsSheet(
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<String, Int>>) -> Unit
) {
    val colors = ProstutiTheme.colors
    var activeTab by remember { mutableIntStateOf(0) } // 0: One by one, 1: Paste syllabus

    // One by one inputs
    var singleTitle by remember { mutableStateOf("") }
    var singleDifficulty by remember { mutableIntStateOf(3) }

    // Paste syllabus inputs
    var rawSyllabusText by remember { mutableStateOf("") }
    val parsedRows = remember { mutableStateListOf<ParsedTopicRow>() }

    fun splitSyllabus(raw: String) {
        val lines = raw.split(Regex("[\r\n;]+"))
        val results = mutableListOf<String>()
        val bulletPattern = Regex("""^(\s*[-*•]|\s*\d+[\.)]|\s*[a-zA-Z][\.)])\s*""")

        for (line in lines) {
            val cleaned = bulletPattern.replace(line, "").trim()
            if (cleaned.isNotBlank() && cleaned.length >= 2) {
                val capped = cleaned.take(80)
                if (results.none { it.equals(capped, ignoreCase = true) }) {
                    results.add(capped)
                }
            }
        }

        parsedRows.clear()
        parsedRows.addAll(results.map { ParsedTopicRow(title = it, difficulty = 3, isChecked = true) })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Topics", style = ProstutiTypography.h2, color = colors.ink)

            SegmentedControl(
                options = listOf("One by one", "Paste syllabus"),
                selectedIndex = activeTab,
                onOptionSelected = { activeTab = it },
                modifier = Modifier.fillMaxWidth()
            )

            if (activeTab == 0) {
                // One by one
                OutlinedTextField(
                    value = singleTitle,
                    onValueChange = { singleTitle = it },
                    label = { Text("Topic title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Difficulty", style = ProstutiTypography.caption, color = colors.ink2)
                        DifficultyBars(difficulty = singleDifficulty, modifier = Modifier.size(24.dp, 12.dp))
                    }
                    Slider(
                        value = singleDifficulty.toFloat(),
                        onValueChange = { singleDifficulty = it.roundToInt() },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }

                DraftingPrimaryButton(
                    text = "Add Topic",
                    enabled = singleTitle.isNotBlank(),
                    onClick = {
                        onConfirm(listOf(Pair(singleTitle.trim(), singleDifficulty)))
                        singleTitle = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Paste syllabus
                if (parsedRows.isEmpty()) {
                    OutlinedTextField(
                        value = rawSyllabusText,
                        onValueChange = { rawSyllabusText = it },
                        label = { Text("Paste syllabus or outline text here...") },
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                    DraftingPrimaryButton(
                        text = "Split into topics",
                        enabled = rawSyllabusText.isNotBlank(),
                        onClick = { splitSyllabus(rawSyllabusText) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Bulk setter buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Set all diff:", style = ProstutiTypography.caption, color = colors.ink2)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { diff ->
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, colors.rule, RoundedCornerShape(4.dp))
                                        .clickable {
                                            parsedRows.forEachIndexed { i, r ->
                                                parsedRows[i] = r.copy(difficulty = diff)
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("$diff", style = ProstutiTypography.caption, color = colors.ink)
                                }
                            }
                        }
                    }

                    // Parsed checklist
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(parsedRows) { idx, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.paper, RoundedCornerShape(4.dp))
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = row.isChecked,
                                    onCheckedChange = { checked ->
                                        parsedRows[idx] = row.copy(isChecked = checked)
                                    }
                                )
                                Text(
                                    text = row.title,
                                    style = ProstutiTypography.bodySmallIfAvailable(),
                                    color = colors.ink,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "D${row.difficulty}",
                                    style = ProstutiTypography.monoMicro,
                                    color = colors.ink2,
                                    modifier = Modifier
                                        .clickable {
                                            val nextDiff = if (row.difficulty >= 5) 1 else row.difficulty + 1
                                            parsedRows[idx] = row.copy(difficulty = nextDiff)
                                        }
                                        .padding(4.dp)
                                )
                                IconButton(
                                    onClick = { parsedRows.removeAt(idx) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = colors.vermilion, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    DraftingPrimaryButton(
                        text = "Add ${parsedRows.count { it.isChecked }} Topics",
                        enabled = parsedRows.any { it.isChecked },
                        onClick = {
                            val itemsToAdd = parsedRows.filter { it.isChecked }.map { Pair(it.title, it.difficulty) }
                            onConfirm(itemsToAdd)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun ProstutiTypography.bodySmallIfAvailable(): androidx.compose.ui.text.TextStyle {
    return this.bodyMedium
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionActionSheet(
    session: Session,
    topic: Topic?,
    onDismiss: () -> Unit,
    onStartFocus: () -> Unit,
    onOpenTopic: () -> Unit,
    onOpenUnstick: () -> Unit,
    onMoveSession: (daysAhead: Int) -> Unit,
    onPinSession: () -> Unit,
    onPartialDay: () -> Unit,
    onSkipSession: () -> Unit,
    onStartPractice: () -> Unit
) {
    val colors = ProstutiTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = topic?.title ?: "Session Details",
                style = ProstutiTypography.h3,
                color = colors.ink
            )
            Text(
                text = "${session.blocks} blocks • ${session.kind.replaceFirstChar { it.uppercase() }}",
                style = ProstutiTypography.bodyMedium,
                color = colors.ink2
            )

            Spacer(modifier = Modifier.height(4.dp))

            DraftingPrimaryButton(
                text = "Start focus",
                onClick = {
                    onStartFocus()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (session.kind == "practice") {
                DraftingPrimaryButton(
                    text = "Start practice",
                    onClick = {
                        onStartPractice()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DraftingOutlinedButton(
                text = "Open topic workspace",
                onClick = {
                    onOpenTopic()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            DraftingOutlinedButton(
                text = "Stuck? Open guide",
                onClick = {
                    onOpenUnstick()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            DraftingOutlinedButton(
                text = "Move to tomorrow",
                onClick = {
                    onMoveSession(1)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            DraftingOutlinedButton(
                text = if (session.pinned) "Unpin" else "Pin to this day",
                onClick = {
                    onPinSession()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            DraftingOutlinedButton(
                text = "I did part of it",
                onClick = {
                    onPartialDay()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            DraftingOutlinedButton(
                text = "Skip session",
                onClick = {
                    onSkipSession()
                    onDismiss()
                },
                borderColor = colors.ink3,
                textColor = colors.ink3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnstickSheet(
    session: Session,
    topic: Topic?,
    onDismiss: () -> Unit,
    onAddCardClick: () -> Unit,
    onMarkNeedsHelp: () -> Unit,
    onAddBlockAndRegenerate: () -> Unit
) {
    val colors = ProstutiTheme.colors
    val checklistItems = when (session.kind.lowercase()) {
        "revise" -> listOf(
            "Close the notes and write the key formulas from memory.",
            "Check against your formula sheet and circle what you missed.",
            "Add one card for each miss."
        )
        "practice" -> listOf(
            "Write the given and the unknown.",
            "Name the formula you would use before touching numbers.",
            "Try for 10 minutes, then read the solution one line at a time.",
            "Redo it with the solution hidden."
        )
        else -> listOf(
            "Write what you already know about this topic in two lines.",
            "Find the single step you can't explain.",
            "Read only that part of your book or slides.",
            "Copy one worked example, then redo it with the page covered.",
            "Say the idea out loud in under a minute."
        )
    }

    val checkedStates = remember { mutableStateListOf(*Array(checklistItems.size) { false }) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Unstick: ${session.kind.replaceFirstChar { it.uppercase() }}", style = ProstutiTypography.h3, color = colors.ink)
            Text("Follow these steps on paper:", style = ProstutiTypography.caption, color = colors.ink3)

            checklistItems.forEachIndexed { idx, text ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { checkedStates[idx] = !checkedStates[idx] },
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = checkedStates[idx],
                        onCheckedChange = { checkedStates[idx] = it }
                    )
                    Text(
                        text = "${idx + 1}. $text",
                        style = ProstutiTypography.bodyMedium,
                        color = colors.ink,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DraftingOutlinedButton(
                    text = "Add a card",
                    onClick = {
                        onAddCardClick()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                DraftingOutlinedButton(
                    text = "Teacher list",
                    onClick = {
                        onMarkNeedsHelp()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                DraftingOutlinedButton(
                    text = "+1 block",
                    onClick = {
                        onAddBlockAndRegenerate()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
