package com.notey.app

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasSidebar(
    modifier: Modifier = Modifier,
    isDetailLocked: Boolean,
    onToggleDetailLock: () -> Unit,
    isSnapToGrid: Boolean,
    onToggleSnapToGrid: () -> Unit,
    onQuickAddNode: (CanvasNodeType) -> Unit,
    onPickMediaNode: () -> Unit,
    onLinkNote: () -> Unit,
    onAutoLayout: () -> Unit,
    onExportDiagram: () -> Unit
) {
    var showAddDrawer by remember { mutableStateOf(false) }

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
                // 1. [Plus Button - Triggers Canvas Add Drawer]
                IconButton(
                    onClick = { showAddDrawer = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (showAddDrawer) Color(0xFF38BDF8) else NoteySurfaces.Surface3,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add to Canvas",
                        tint = if (showAddDrawer) Color(0xFF000000) else NoteySurfaces.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                HorizontalDivider(
                    color = NoteySurfaces.HairlineBorder,
                    modifier = Modifier.width(24.dp)
                )

                // 2. [Detail Lock Toggle (Semantic Legibility Zoom)]
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
                        contentDescription = "Detail Lock (Semantic Legibility Zoom)",
                        tint = if (isDetailLocked) Color(0xFF38BDF8) else NoteySurfaces.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3. [Magnetic Snap to Grid (24dp Coordinate Snap)]
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
                        contentDescription = "Magnetic 24dp Snap to Grid",
                        tint = if (isSnapToGrid) Color(0xFF34D399) else NoteySurfaces.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 4. [Auto-Layout / Tidy Board]
                IconButton(
                    onClick = onAutoLayout,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        NoteyIcons.Workflow,
                        contentDescription = "Auto-Layout Board",
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
                        NoteyIcons.Share,
                        contentDescription = "Export Canvas Diagram",
                        tint = NoteySurfaces.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick Add Modal Bottom Sheet (Identical luxury style to Home's plus drawer)
        if (showAddDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showAddDrawer = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = NoteySurfaces.Surface2
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Add to Canvas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NoteySurfaces.TextPrimary
                            )
                            Text(
                                text = "Spawns new elements at canvas center",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NoteySurfaces.TextMuted)
                            )
                        }
                        IconButton(onClick = { showAddDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = NoteySurfaces.TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    CanvasDrawerMenuItem(
                        icon = NoteyIcons.Document,
                        title = "Markdown Note Block",
                        subtitle = "Rich text node with markdown typography",
                        accentColor = Color(0xFF38BDF8),
                        onClick = {
                            showAddDrawer = false
                            onQuickAddNode(CanvasNodeType.NOTE)
                        }
                    )

                    CanvasDrawerMenuItem(
                        icon = NoteyIcons.TaskCheck,
                        title = "Interactive Checklist",
                        subtitle = "Dynamic todo card with toggleable checkboxes",
                        accentColor = Color(0xFF34D399),
                        onClick = {
                            showAddDrawer = false
                            onQuickAddNode(CanvasNodeType.TASK)
                        }
                    )

                    CanvasDrawerMenuItem(
                        icon = NoteyIcons.Camera,
                        title = "Media Embed",
                        subtitle = "Place image or video directly on canvas",
                        accentColor = Color(0xFFA78BFA),
                        onClick = {
                            showAddDrawer = false
                            onPickMediaNode()
                        }
                    )

                    CanvasDrawerMenuItem(
                        icon = NoteyIcons.Workflow,
                        title = "Decision Point / Milestone",
                        subtitle = "Architectural flowchart diamond node",
                        accentColor = Color(0xFFFBBF24),
                        onClick = {
                            showAddDrawer = false
                            onQuickAddNode(CanvasNodeType.DECISION)
                        }
                    )

                    CanvasDrawerMenuItem(
                        icon = NoteyIcons.Folder,
                        title = "Link Note from Vault",
                        subtitle = "Embed real note from your library vault",
                        accentColor = Color(0xFF818CF8),
                        onClick = {
                            showAddDrawer = false
                            onLinkNote()
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CanvasDrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NoteySurfaces.Surface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = NoteySurfaces.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NoteySurfaces.TextSecondary
                )
            }
        }
    }
}

/**
 * Intelligent auto-layout algorithm for canvas nodes:
 * - If nodes have connectors: performs layered topological DAG flow with horizontal centering
 * - If nodes are unconnected: arranges into an elegant, balanced 3-column matrix grid
 */
fun autoLayoutCanvasNodes(nodes: List<CanvasNode>, connectors: List<CanvasConnector>): List<CanvasNode> {
    if (nodes.isEmpty()) return nodes

    val validConnectors = connectors.filter { c ->
        nodes.any { it.id == c.fromNodeId } && nodes.any { it.id == c.toNodeId }
    }

    if (validConnectors.isNotEmpty()) {
        val inDegree = mutableMapOf<String, Int>()
        val adj = mutableMapOf<String, MutableList<String>>()
        nodes.forEach {
            inDegree[it.id] = 0
            adj[it.id] = mutableListOf()
        }
        validConnectors.forEach { c ->
            adj[c.fromNodeId]?.add(c.toNodeId)
            inDegree[c.toNodeId] = (inDegree[c.toNodeId] ?: 0) + 1
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

        val maxLevel = (levels.values.maxOrNull() ?: 0)
        var unassignedLevel = maxLevel + 1
        nodes.forEach { n ->
            if (!levels.containsKey(n.id)) {
                levels[n.id] = unassignedLevel
            }
        }

        val levelGroups = nodes.groupBy { levels[it.id] ?: 0 }
        val newPositions = mutableMapOf<String, Pair<Float, Float>>()

        levelGroups.entries.sortedBy { it.key }.forEach { (level, group) ->
            val x = 120f + level * 260f
            val totalHeight = (group.size - 1) * 160f
            val startY = (240f - (totalHeight / 2f)).coerceAtLeast(80f)
            group.forEachIndexed { idx, node ->
                val y = startY + idx * 160f
                newPositions[node.id] = Pair(x, y)
            }
        }

        return nodes.map { node ->
            val pos = newPositions[node.id]
            if (pos != null) node.copy(x = pos.first, y = pos.second) else node
        }
    } else {
        // Unconnected nodes: Arrange in a balanced 3-column matrix grid
        val columns = 3
        val cardWidth = 220f
        val cardHeight = 140f
        val gapX = 40f
        val gapY = 40f
        val startX = 120f
        val startY = 120f

        return nodes.mapIndexed { index, node ->
            val col = index % columns
            val row = index / columns
            val newX = startX + col * (cardWidth + gapX)
            val newY = startY + row * (cardHeight + gapY)
            node.copy(x = newX, y = newY)
        }
    }
}
