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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.model.Card
import com.example.model.Problem
import com.example.model.Topic
import com.example.srs.ReviewRating
import com.example.srs.Srs
import com.example.srs.SrsCardState
import com.example.ui.components.DraftingCard
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.DraftingPrimaryButton
import com.example.ui.components.MathText
import com.example.ui.components.NotebookBackground
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.util.DateUtil
import kotlinx.coroutines.delay
import java.util.Random
import kotlin.math.roundToInt

@Composable
fun ReviewScreen(
    dueCards: List<Card>,
    topics: List<Topic>,
    onCardReviewed: (card: Card, newDueIso: String, newEase: Double, newInterval: Int, newReps: Int, newLapses: Int) -> Unit,
    onTopicMarkRevised: (Topic) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val todayEpoch = DateUtil.todayEpochDay()

    val queue = remember(dueCards) {
        mutableStateListOf<Card>().apply {
            addAll(dueCards.take(30))
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    // Counts for summary
    var goodCount by remember { mutableIntStateOf(0) }
    var hardCount by remember { mutableIntStateOf(0) }
    var againCount by remember { mutableIntStateOf(0) }
    var easyCount by remember { mutableIntStateOf(0) }
    val touchedTopicIds = remember { mutableSetOf<String>() }

    val topicMap = remember(topics) { topics.associateBy { it.id } }

    NotebookBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review",
                    style = ProstutiTypography.h2,
                    color = colors.ink
                )
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.ink)
                }
            }

            if (queue.isEmpty() || currentIndex >= queue.size) {
                // End of Run Summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val total = goodCount + hardCount + againCount + easyCount
                    Text(
                        text = if (total > 0) "$total cards." else "No cards due. Come back tomorrow.",
                        style = ProstutiTypography.h2,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (total > 0) {
                        Text(
                            text = "$goodCount Good, $hardCount Hard, $againCount Again, $easyCount Easy.",
                            style = ProstutiTypography.bodyLarge,
                            color = colors.ink2
                        )
                    }

                    // For each touched topic in 'learning' status, suggest 'Mark as Revised?'
                    val suggestTopics = touchedTopicIds.mapNotNull { topicMap[it] }.filter { it.status == "learning" }
                    if (suggestTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Topics reviewed today:",
                            style = ProstutiTypography.caption,
                            color = colors.ink3
                        )
                        suggestTopics.forEach { top ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(top.title, style = ProstutiTypography.bodyMedium, color = colors.ink)
                                DraftingOutlinedButton(
                                    text = "Mark Revised",
                                    onClick = { onTopicMarkRevised(top) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    DraftingPrimaryButton(
                        text = "Done",
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                val card = queue[currentIndex]
                val srsState = SrsCardState(
                    ease = card.ease,
                    intervalDays = card.intervalDays,
                    reps = card.reps,
                    lapses = card.lapses,
                    due = card.dueEpochDay
                )

                touchedTopicIds.add(card.topicId)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress Indicator
                    Text(
                        text = "${currentIndex + 1} of ${queue.size}",
                        style = ProstutiTypography.monoSmall,
                        color = colors.ink2
                    )

                    // Card Display
                    DraftingCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "QUESTION",
                                style = ProstutiTypography.caption,
                                color = colors.ink3
                            )
                            MathText(
                                text = card.front,
                                style = ProstutiTypography.h3,
                                color = colors.ink
                            )

                            if (showAnswer) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.rule)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ANSWER",
                                    style = ProstutiTypography.caption,
                                    color = colors.ink3
                                )
                                MathText(
                                    text = card.back,
                                    style = ProstutiTypography.bodyLarge,
                                    color = colors.ink
                                )
                            }
                        }
                    }
                }

                // Buttons Section
                if (!showAnswer) {
                    DraftingPrimaryButton(
                        text = "Show answer",
                        onClick = { showAnswer = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Again
                        val againInterval = Srs.previewIntervalText(srsState, ReviewRating.AGAIN, todayEpoch)
                        DraftingOutlinedButton(
                            text = "Again ($againInterval)",
                            onClick = {
                                val res = Srs.review(srsState, ReviewRating.AGAIN, todayEpoch)
                                onCardReviewed(card, DateUtil.epochDayToIso(res.due), res.ease, res.intervalDays, res.reps, res.lapses)
                                againCount++
                                if (res.shouldRequeue) {
                                    queue.add(card)
                                }
                                showAnswer = false
                                currentIndex++
                            },
                            borderColor = colors.vermilion,
                            textColor = colors.vermilion,
                            modifier = Modifier.weight(1f)
                        )

                        // Hard
                        val hardInterval = Srs.previewIntervalText(srsState, ReviewRating.HARD, todayEpoch)
                        DraftingOutlinedButton(
                            text = "Hard ($hardInterval)",
                            onClick = {
                                val res = Srs.review(srsState, ReviewRating.HARD, todayEpoch)
                                onCardReviewed(card, DateUtil.epochDayToIso(res.due), res.ease, res.intervalDays, res.reps, res.lapses)
                                hardCount++
                                showAnswer = false
                                currentIndex++
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Good
                        val goodInterval = Srs.previewIntervalText(srsState, ReviewRating.GOOD, todayEpoch)
                        DraftingOutlinedButton(
                            text = "Good ($goodInterval)",
                            onClick = {
                                val res = Srs.review(srsState, ReviewRating.GOOD, todayEpoch)
                                onCardReviewed(card, DateUtil.epochDayToIso(res.due), res.ease, res.intervalDays, res.reps, res.lapses)
                                goodCount++
                                showAnswer = false
                                currentIndex++
                            },
                            borderColor = colors.blue,
                            textColor = colors.blue,
                            modifier = Modifier.weight(1f)
                        )

                        // Easy
                        val easyInterval = Srs.previewIntervalText(srsState, ReviewRating.EASY, todayEpoch)
                        DraftingOutlinedButton(
                            text = "Easy ($easyInterval)",
                            onClick = {
                                val res = Srs.review(srsState, ReviewRating.EASY, todayEpoch)
                                onCardReviewed(card, DateUtil.epochDayToIso(res.due), res.ease, res.intervalDays, res.reps, res.lapses)
                                easyCount++
                                showAnswer = false
                                currentIndex++
                            },
                            borderColor = colors.moss,
                            textColor = colors.moss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeScreen(
    topic: Topic?,
    problems: List<Problem>,
    onProblemAttempted: (problem: Problem, result: String) -> Unit,
    onTopicConfidenceUpdated: (topic: Topic, newConfidence: Int, newStatus: String?) -> Unit,
    onAddProblemClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val todayEpoch = DateUtil.todayEpochDay()

    // Stable seeded shuffle
    val shuffledProblems = remember(problems, topic) {
        val seed = todayEpoch + (topic?.id?.hashCode() ?: 0)
        problems.shuffled(Random(seed))
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var showSolution by remember { mutableStateOf(false) }

    // Optional timer toggle
    var timerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1000)
            timerSeconds++
        }
    }

    NotebookBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Practice",
                        style = ProstutiTypography.h2,
                        color = colors.ink
                    )
                    topic?.let {
                        Text(it.title, style = ProstutiTypography.bodyMedium, color = colors.ink2)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Timer toggle
                    Box(
                        modifier = Modifier
                            .border(1.dp, colors.rule, RoundedCornerShape(4.dp))
                            .clickable { timerRunning = !timerRunning }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val m = timerSeconds / 60
                        val s = timerSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", m, s),
                            style = ProstutiTypography.monoSmall,
                            color = if (timerRunning) colors.blue else colors.ink2
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.ink)
                    }
                }
            }

            if (shuffledProblems.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No problems saved for this topic. Add one from your textbook or a past paper.",
                        style = ProstutiTypography.bodyLarge,
                        color = colors.ink2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The timer still works while you work from your book.",
                        style = ProstutiTypography.caption,
                        color = colors.ink3
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    DraftingPrimaryButton(
                        text = "Add problem",
                        onClick = onAddProblemClick
                    )
                }
            } else if (currentIndex >= shuffledProblems.size) {
                // Done Practice Run
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Practice set complete.", style = ProstutiTypography.h2, color = colors.ink)
                    Spacer(modifier = Modifier.height(16.dp))
                    DraftingPrimaryButton(
                        text = "Done",
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                val problem = shuffledProblems[currentIndex]

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Problem ${currentIndex + 1} of ${shuffledProblems.size}",
                        style = ProstutiTypography.monoSmall,
                        color = colors.ink2
                    )

                    DraftingCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("PROMPT", style = ProstutiTypography.caption, color = colors.ink3)
                            MathText(
                                text = problem.prompt,
                                style = ProstutiTypography.h3,
                                color = colors.ink
                            )
                            problem.source?.let {
                                Text("Source: $it", style = ProstutiTypography.caption, color = colors.ink3)
                            }

                            if (showSolution) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.rule))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("SOLUTION", style = ProstutiTypography.caption, color = colors.ink3)
                                MathText(
                                    text = problem.solution.ifBlank { "No worked solution entered." },
                                    style = ProstutiTypography.bodyLarge,
                                    color = colors.ink
                                )
                            }
                        }
                    }
                }

                // Buttons
                if (!showSolution) {
                    DraftingPrimaryButton(
                        text = "Show solution",
                        onClick = { showSolution = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DraftingOutlinedButton(
                            text = "Missed it",
                            onClick = {
                                onProblemAttempted(problem, "missed")
                                if (topic != null) {
                                    val newConf = (0.7 * topic.confidence + 0.3 * 0.0).roundToInt()
                                    val newStatus = if (newConf < 40) "learning" else null
                                    onTopicConfidenceUpdated(topic, newConf, newStatus)
                                }
                                showSolution = false
                                currentIndex++
                            },
                            borderColor = colors.vermilion,
                            textColor = colors.vermilion,
                            modifier = Modifier.weight(1f)
                        )

                        DraftingOutlinedButton(
                            text = "Partly",
                            onClick = {
                                onProblemAttempted(problem, "partly")
                                if (topic != null) {
                                    val newConf = (0.7 * topic.confidence + 0.3 * 50.0).roundToInt()
                                    onTopicConfidenceUpdated(topic, newConf, null)
                                }
                                showSolution = false
                                currentIndex++
                            },
                            borderColor = colors.amber,
                            textColor = colors.amber,
                            modifier = Modifier.weight(1f)
                        )

                        DraftingOutlinedButton(
                            text = "Got it",
                            onClick = {
                                onProblemAttempted(problem, "got")
                                if (topic != null) {
                                    val newConf = (0.7 * topic.confidence + 0.3 * 100.0).roundToInt()
                                    val newStatus = if (newConf >= 80 && topic.status == "revised") "solid" else null
                                    onTopicConfidenceUpdated(topic, newConf, newStatus)
                                }
                                showSolution = false
                                currentIndex++
                            },
                            borderColor = colors.moss,
                            textColor = colors.moss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
