package com.pocketdev.notey

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun CanvasSidebar(
    modifier: Modifier = Modifier,
    isDetailLocked: Boolean,
    onToggleDetailLock: () -> Unit,
    isSnapToGrid: Boolean,
    onToggleSnapToGrid: () -> Unit,
    onQuickAddNode: (CanvasNodeType) -> Unit,
    onPickMediaNode: () -> Unit,
    onAutoLayout: () -> Unit,
    onExportDiagram: () -> Unit
) {
    var showPalette by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Vertical Sleek Floating Dock along the screen edge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121215),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14FFFFFF)),
            shadowElevation = 8.dp,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. [Quick Add Node]
                IconButton(
                    onClick = { showPalette = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (showPalette) Color(0xFF38BDF8) else NoteySurfaces.Surface3,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Quick Add Node",
                        tint = if (showPalette) Color(0xFF000000) else NoteySurfaces.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                HorizontalDivider(
                    color = NoteySurfaces.HairlineBorder,
                    modifier = Modifier.width(24.dp)
                )

                // 2. [Detail Lock Toggle (Anti-Disappearing Zoom)]
                IconButton(
                    onClick = onToggleDetailLock,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isDetailLocked) Color(0x3338BDF8) else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        NoteyIcons.Heading,
                        contentDescription = "Detail Lock (Anti-Disappearing Zoom)",
                        tint = if (isDetailLocked) Color(0xFF38BDF8) else NoteySurfaces.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3. [Magnetic Snap to Grid]
                IconButton(
                    onClick = onToggleSnapToGrid,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isSnapToGrid) Color(0x3334D399) else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        NoteyIcons.GridView,
                        contentDescription = "Magnetic 16dp Snap to Grid",
                        tint = if (isSnapToGrid) Color(0xFF34D399) else NoteySurfaces.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 4. [Auto-Layout / Tidy]
                IconButton(
                    onClick = onAutoLayout,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        NoteyIcons.Workflow,
                        contentDescription = "Auto-Layout Tree",
                        tint = NoteySurfaces.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 5. [Export Canvas Diagram]
                IconButton(
                    onClick = onExportDiagram,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Export Canvas Diagram",
                        tint = NoteySurfaces.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick Add Node Compact Palette Dialog
        if (showPalette) {
            Dialog(onDismissRequest = { showPalette = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NoteySurfaces.Surface2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                    modifier = Modifier
                        .width(300.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Add Node to Viewport",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NoteySurfaces.TextPrimary
                        )
                        Text(
                            text = "Spawns at current canvas center",
                            style = MonoMetadataStyle,
                            color = NoteySurfaces.TextMuted
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Option 1: Markdown Note Block
                        PaletteItemRow(
                            icon = NoteyIcons.Document,
                            title = "Markdown Note Block",
                            subtitle = "Rich text node with markdown",
                            accentColor = Color(0xFF38BDF8)
                        ) {
                            showPalette = false
                            onQuickAddNode(CanvasNodeType.NOTE)
                        }

                        // Option 2: Interactive Task / Checklist Node
                        PaletteItemRow(
                            icon = NoteyIcons.TaskCheck,
                            title = "Interactive Task Node",
                            subtitle = "Checklist with interactive items",
                            accentColor = Color(0xFF34D399)
                        ) {
                            showPalette = false
                            onQuickAddNode(CanvasNodeType.TASK)
                        }

                        // Option 3: Media Embed Node (Image / Video)
                        PaletteItemRow(
                            icon = NoteyIcons.Camera,
                            title = "Media Embed Node",
                            subtitle = "Upload Image or Video attachment",
                            accentColor = Color(0xFFA78BFA)
                        ) {
                            showPalette = false
                            onPickMediaNode()
                        }

                        // Option 4: Decision / Milestone Node
                        PaletteItemRow(
                            icon = NoteyIcons.Workflow,
                            title = "Decision / Milestone",
                            subtitle = "Diamond decision node or milestone",
                            accentColor = Color(0xFFFBBF24)
                        ) {
                            showPalette = false
                            onQuickAddNode(CanvasNodeType.DECISION)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = NoteySurfaces.Surface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NoteySurfaces.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NoteySurfaces.TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Clean hierarchical tree layout algorithm for canvas nodes.
 */
fun autoLayoutCanvasNodes(nodes: List<CanvasNode>, connectors: List<CanvasConnector>): List<CanvasNode> {
    if (nodes.isEmpty()) return nodes

    val inDegree = mutableMapOf<String, Int>()
    val adj = mutableMapOf<String, MutableList<String>>()
    nodes.forEach {
        inDegree[it.id] = 0
        adj[it.id] = mutableListOf()
    }
    connectors.forEach { c ->
        if (adj.containsKey(c.fromNodeId) && adj.containsKey(c.toNodeId)) {
            adj[c.fromNodeId]?.add(c.toNodeId)
            inDegree[c.toNodeId] = (inDegree[c.toNodeId] ?: 0) + 1
        }
    }

    val levels = mutableMapOf<String, Int>()
    val queue = ArrayDeque<String>()
    nodes.filter { (inDegree[it.id] ?: 0) == 0 }.forEach {
        queue.add(it.id)
        levels[it.id] = 0
    }

    while (queue.isNotEmpty()) {
        val curr = queue.removeFirst()
        val currLevel = levels[curr] ?: 0
        adj[curr]?.forEach { next ->
            if (!levels.containsKey(next) || (levels[next] ?: 0) < currLevel + 1) {
                levels[next] = currLevel + 1
                queue.add(next)
            }
        }
    }

    // Any remaining unvisited nodes get assigned to level 0 or maxLevel + 1
    val maxLevel = (levels.values.maxOrNull() ?: 0)
    var unvisitedRow = 0
    nodes.forEach { n ->
        if (!levels.containsKey(n.id)) {
            levels[n.id] = unvisitedRow % 3
            unvisitedRow++
        }
    }

    // Group nodes by level and compute positions
    val levelGroups = nodes.groupBy { levels[it.id] ?: 0 }
    val newPositions = mutableMapOf<String, Pair<Float, Float>>()

    levelGroups.entries.sortedBy { it.key }.forEach { (level, group) ->
        val x = 120f + level * 260f
        group.forEachIndexed { idx, node ->
            val y = 100f + idx * 160f
            newPositions[node.id] = Pair(x, y)
        }
    }

    return nodes.map { node ->
        val pos = newPositions[node.id]
        if (pos != null) {
            node.copy(x = pos.first, y = pos.second)
        } else {
            node
        }
    }
}
