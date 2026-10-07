package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.Card
import com.example.model.Problem
import com.example.model.Topic
import com.example.ui.components.DifficultyBars
import com.example.ui.components.DraftingCard
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.DraftingPrimaryButton
import com.example.ui.components.MathRenderer
import com.example.ui.components.MathText
import com.example.ui.components.NotebookBackground
import com.example.ui.components.SegmentedControl
import com.example.ui.components.TopicStatusIndicator
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import java.util.UUID

@Composable
fun TopicDetailScreen(
    topic: Topic,
    cards: List<Card>,
    problems: List<Problem>,
    onUpdateTopic: (Topic) -> Unit,
    onDeleteTopic: (Topic) -> Unit,
    onAddCard: (front: String, back: String) -> Unit,
    onDeleteCard: (Card) -> Unit,
    onAddProblem: (prompt: String, solution: String, marks: Int?, source: String?) -> Unit,
    onDeleteProblem: (Problem) -> Unit,
    onReviewTopicCards: () -> Unit,
    onPracticeTopicProblems: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    var activeTab by remember { mutableIntStateOf(0) } // 0: Notes, 1: Formula Sheet, 2: Cards, 3: Problems

    var notesText by remember(topic.notes) { mutableStateOf(topic.notes) }
    var formulaText by remember(topic.formulaSheet) { mutableStateOf(topic.formulaSheet) }
    var notesPreviewMode by remember { mutableStateOf(false) }

    var showAddCardDialog by remember { mutableStateOf(false) }
    var showAddProblemDialog by remember { mutableStateOf(false) }

    NotebookBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink
                    )
                }
                Text(
                    text = topic.title,
                    style = ProstutiTypography.h3,
                    color = colors.ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                IconButton(onClick = { onDeleteTopic(topic) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete topic",
                        tint = colors.vermilion
                    )
                }
            }

            // Status, Difficulty & Needs Help controls card
            DraftingCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status", style = ProstutiTypography.caption, color = colors.ink2)
                        val statuses = listOf("new", "learning", "revised", "solid")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            statuses.forEach { st ->
                                val isSelected = topic.status.lowercase() == st
                                Box(
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            if (isSelected) colors.blue else colors.rule,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .background(
                                            if (isSelected) colors.blueSoft else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onUpdateTopic(topic.copy(status = st)) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    TopicStatusIndicator(status = st)
                                }
                            }
                        }
                    }

                    // Difficulty & Est blocks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Difficulty", style = ProstutiTypography.caption, color = colors.ink2)
                            DifficultyBars(difficulty = topic.difficulty, modifier = Modifier.size(24.dp, 12.dp))
                            Text("${topic.difficulty}/5", style = ProstutiTypography.monoSmall, color = colors.ink)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Blocks", style = ProstutiTypography.caption, color = colors.ink2)
                            Text("${topic.estBlocks} est (${topic.blocksDone} done)", style = ProstutiTypography.monoSmall, color = colors.ink)
                        }
                    }

                    // "I need help with this" toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("I need help with this", style = ProstutiTypography.bodyMedium, color = colors.ink)
                        Switch(
                            checked = topic.needsHelp,
                            onCheckedChange = { onUpdateTopic(topic.copy(needsHelp = it)) }
                        )
                    }
                }
            }

            // Tab Selector
            SegmentedControl(
                options = listOf("Notes", "Formula", "Cards (${cards.size})", "Problems (${problems.size})"),
                selectedIndex = activeTab,
                onOptionSelected = { activeTab = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Tab Content
            when (activeTab) {
                0 -> {
                    // Notes Tab
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (notesPreviewMode) "Preview" else "Markdown / Math Editor",
                                style = ProstutiTypography.caption,
                                color = colors.ink2
                            )
                            DraftingOutlinedButton(
                                text = if (notesPreviewMode) "Edit" else "Preview",
                                onClick = {
                                    if (!notesPreviewMode) {
                                        onUpdateTopic(topic.copy(notes = notesText))
                                    }
                                    notesPreviewMode = !notesPreviewMode
                                }
                            )
                        }

                        if (notesPreviewMode) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .border(1.dp, colors.rule, RoundedCornerShape(6.dp))
                                    .background(colors.card, RoundedCornerShape(6.dp))
                                    .padding(12.dp)
                            ) {
                                MathText(text = notesText.ifBlank { "No notes written." })
                            }
                        } else {
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = {
                                    notesText = it
                                    onUpdateTopic(topic.copy(notes = it))
                                },
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                placeholder = { Text("Write notes with # heading, - list, **bold**, and math \$x^2\$...") }
                            )
                        }
                    }
                }
                1 -> {
                    // Formula Sheet Tab
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "High-contrast Formula Sheet (rendered with MathRenderer)",
                            style = ProstutiTypography.caption,
                            color = colors.ink2
                        )
                        OutlinedTextField(
                            value = formulaText,
                            onValueChange = {
                                formulaText = it
                                onUpdateTopic(topic.copy(formulaSheet = it))
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            placeholder = { Text("Formulas in LaTeX, e.g. Z_0 = \\sqrt{L/C} or \\frac{a}{b}...") }
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, colors.rule, RoundedCornerShape(6.dp))
                                .background(colors.card, RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        ) {
                            MathText(
                                text = formulaText.ifBlank { "Formula preview will appear here." },
                                style = ProstutiTypography.h3
                            )
                        }
                    }
                }
                2 -> {
                    // Cards Tab
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (cards.isNotEmpty()) {
                                DraftingPrimaryButton(
                                    text = "Review these",
                                    onClick = onReviewTopicCards
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            DraftingOutlinedButton(
                                text = "Add card",
                                onClick = { showAddCardDialog = true }
                            )
                        }

                        if (cards.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No cards for this topic yet. Write one question you got wrong today.",
                                    style = ProstutiTypography.bodyMedium,
                                    color = colors.ink2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(cards, key = { it.id }) { card ->
                                    DraftingCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    MathText(text = "Q: ${card.front}", style = ProstutiTypography.bodyLarge, color = colors.ink)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    MathText(text = "A: ${card.back}", style = ProstutiTypography.bodyMedium, color = colors.ink2)
                                                }
                                                IconButton(onClick = { onDeleteCard(card) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete card", tint = colors.vermilion)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Problems Tab
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (problems.isNotEmpty()) {
                                DraftingPrimaryButton(
                                    text = "Practice these",
                                    onClick = onPracticeTopicProblems
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            DraftingOutlinedButton(
                                text = "Add problem",
                                onClick = { showAddProblemDialog = true }
                            )
                        }

                        if (problems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No problems saved for this topic. Add one from your textbook or a past paper.",
                                    style = ProstutiTypography.bodyMedium,
                                    color = colors.ink2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(problems, key = { it.id }) { prob ->
                                    DraftingCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    MathText(text = prob.prompt, style = ProstutiTypography.bodyLarge, color = colors.ink)
                                                    prob.source?.let {
                                                        Text("Source: $it", style = ProstutiTypography.caption, color = colors.ink3)
                                                    }
                                                }
                                                IconButton(onClick = { onDeleteProblem(prob) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete problem", tint = colors.vermilion)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Card Dialog with Live Math Preview
        if (showAddCardDialog) {
            var frontInput by remember { mutableStateOf("") }
            var backInput by remember { mutableStateOf("") }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAddCardDialog = false },
                title = { Text("Add Flashcard", style = ProstutiTypography.h3, color = colors.ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = frontInput,
                            onValueChange = { frontInput = it },
                            label = { Text("Question (supports $...$)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (frontInput.contains("$")) {
                            Text("Preview:", style = ProstutiTypography.caption, color = colors.ink3)
                            MathText(text = frontInput)
                        }

                        OutlinedTextField(
                            value = backInput,
                            onValueChange = { backInput = it },
                            label = { Text("Answer (supports $...$)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (backInput.contains("$")) {
                            Text("Preview:", style = ProstutiTypography.caption, color = colors.ink3)
                            MathText(text = backInput)
                        }
                    }
                },
                confirmButton = {
                    DraftingPrimaryButton(
                        text = "Save",
                        enabled = frontInput.isNotBlank() && backInput.isNotBlank(),
                        onClick = {
                            onAddCard(frontInput.trim(), backInput.trim())
                            showAddCardDialog = false
                        }
                    )
                },
                dismissButton = {
                    DraftingOutlinedButton(
                        text = "Cancel",
                        onClick = { showAddCardDialog = false }
                    )
                },
                containerColor = colors.card
            )
        }

        // Add Problem Dialog
        if (showAddProblemDialog) {
            var promptInput by remember { mutableStateOf("") }
            var solInput by remember { mutableStateOf("") }
            var marksInput by remember { mutableStateOf("") }
            var sourceInput by remember { mutableStateOf("") }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAddProblemDialog = false },
                title = { Text("Add Problem", style = ProstutiTypography.h3, color = colors.ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            label = { Text("Problem statement") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = solInput,
                            onValueChange = { solInput = it },
                            label = { Text("Worked solution") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = marksInput,
                                onValueChange = { marksInput = it },
                                label = { Text("Marks") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sourceInput,
                                onValueChange = { sourceInput = it },
                                label = { Text("Source") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    DraftingPrimaryButton(
                        text = "Save",
                        enabled = promptInput.isNotBlank(),
                        onClick = {
                            val marks = marksInput.toIntOrNull()
                            onAddProblem(promptInput.trim(), solInput.trim(), marks, sourceInput.trim().ifBlank { null })
                            showAddProblemDialog = false
                        }
                    )
                },
                dismissButton = {
                    DraftingOutlinedButton(
                        text = "Cancel",
                        onClick = { showAddProblemDialog = false }
                    )
                },
                containerColor = colors.card
            )
        }
    }
}
