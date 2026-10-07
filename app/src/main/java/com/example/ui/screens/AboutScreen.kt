package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.ui.components.NotebookBackground
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val context = LocalContext.current

    NotebookBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.ink)
                }
                Text("About", style = ProstutiTypography.h2, color = colors.ink, modifier = Modifier.padding(start = 8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title block (engineering-drawing title block)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.ink, RoundedCornerShape(4.dp))
                            .background(colors.card, RoundedCornerShape(4.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TitleBlockRow(label = "APP", value = "Prostuti v1.0")
                            TitleBlockRow(label = "DRAWN BY", value = "Mahfuz Alam Tonmoy")
                            TitleBlockRow(label = "DEPT", value = "Electrical & Electronic Engineering")
                            TitleBlockRow(label = "INSTITUTION", value = "Bangladesh University of Engineering and Technology (BUET)")
                            TitleBlockRow(label = "DATA", value = "Stored on this device only")
                            TitleBlockRow(label = "CONTACT", value = "tonmoy.eee.buet@gmail.com")
                        }

                        // Stamp REV 1.0 in vermilion, rotated -4 degrees, 1.5dp border
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .rotate(-4f)
                                .border(1.5.dp, colors.vermilion, RoundedCornerShape(2.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "REV 1.0",
                                style = ProstutiTypography.monoSmall,
                                color = colors.vermilion
                            )
                        }
                    }
                }

                // Author Intro
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Mahfuz Alam Tonmoy",
                            style = ProstutiTypography.h3,
                            color = colors.ink
                        )
                        Text(
                            text = "Electrical & Electronic Engineering Student | Researcher | Developer",
                            style = ProstutiTypography.bodyMedium,
                            color = colors.ink2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "I am Mahfuz Alam Tonmoy, an undergraduate student of Electrical & Electronic Engineering (EEE) at Bangladesh University of Engineering and Technology (BUET).\nMy interests lie at the intersection of Electrical Engineering, Artificial Intelligence, Machine Learning, Signal Processing, Computer Vision, and Research. I enjoy building practical systems that connect theoretical engineering concepts with real-world applications.",
                            style = ProstutiTypography.bodyLarge,
                            color = colors.ink
                        )
                    }
                }

                // Research & Technical Interests Tags
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Research & Technical Interests",
                            style = ProstutiTypography.h3,
                            color = colors.ink
                        )
                        val interests = listOf(
                            "Machine Learning & Deep Learning",
                            "Computer Vision & Visual Speech Recognition",
                            "Digital Signal Processing",
                            "Power Systems",
                            "Optical & Photonic Biosensors",
                            "Embedded & Engineering Systems",
                            "Scientific Computing and Simulation"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            interests.forEach { itemText ->
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, colors.rule, RoundedCornerShape(4.dp))
                                        .background(colors.card, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = itemText,
                                        style = ProstutiTypography.caption,
                                        color = colors.ink
                                    )
                                }
                            }
                        }
                    }
                }

                // Projects & Research
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Projects & Research",
                            style = ProstutiTypography.h3,
                            color = colors.ink
                        )
                        Text(
                            text = "I have worked on projects involving visual speech recognition, temporal deep-learning models, digital signal processing, power systems, and photonic biosensing. My research work includes comparative studies of deep-learning architectures and the design and simulation of PCF-SPR-based biosensors.\nI am particularly interested in understanding not only how a system works, but also the mathematical and engineering principles behind it.",
                            style = ProstutiTypography.bodyLarge,
                            color = colors.ink
                        )
                    }
                }

                // Development
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Development",
                            style = ProstutiTypography.h3,
                            color = colors.ink
                        )
                        Text(
                            text = "I develop software and engineering tools primarily using Python, PyTorch, MATLAB, and modern web technologies, with a focus on creating practical, efficient, and user-friendly applications.",
                            style = ProstutiTypography.bodyLarge,
                            color = colors.ink
                        )
                    }
                }

                // Education
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Education",
                            style = ProstutiTypography.h3,
                            color = colors.ink
                        )
                        Text(
                            text = "Bangladesh University of Engineering and Technology (BUET), Department of Electrical & Electronic Engineering, Undergraduate Student",
                            style = ProstutiTypography.bodyLarge,
                            color = colors.ink
                        )
                    }
                }

                // Connect links
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Connect",
                            style = ProstutiTypography.h3,
                            color = colors.ink
                        )
                        ConnectLinkRow(
                            label = "GitHub",
                            link = "github.com/parthotonmoy",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/parthotonmoy"))
                                context.startActivity(intent)
                            }
                        )
                        ConnectLinkRow(
                            label = "LinkedIn",
                            link = "linkedin.com/in/mahfuz-alam-tonmoy-916b82331",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://linkedin.com/in/mahfuz-alam-tonmoy-916b82331"))
                                context.startActivity(intent)
                            }
                        )
                        ConnectLinkRow(
                            label = "Email",
                            link = "tonmoy.eee.buet@gmail.com",
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:tonmoy.eee.buet@gmail.com"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                // Footer quote
                item {
                    Text(
                        text = "Building, learning, researching, and turning engineering ideas into practical solutions...",
                        style = ProstutiTypography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = colors.ink2,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleBlockRow(label: String, value: String) {
    val colors = ProstutiTheme.colors
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = ProstutiTypography.monoMicro,
            color = colors.ink3,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            style = ProstutiTypography.monoSmall,
            color = colors.ink
        )
    }
}

@Composable
private fun ConnectLinkRow(label: String, link: String, onClick: () -> Unit) {
    val colors = ProstutiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: $link",
            style = ProstutiTypography.bodyLarge,
            color = colors.blue
        )
    }
}
