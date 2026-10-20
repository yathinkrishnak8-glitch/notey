package com.notey.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object NoteyIcons {
    val Folder: ImageVector by lazy {
        ImageVector.Builder(
            name = "Folder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10f, 4f)
                lineTo(4f, 4f)
                curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
                lineTo(2f, 18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                lineTo(20f, 20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                lineTo(22f, 8f)
                curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
                lineTo(12f, 6f)
                lineTo(10f, 4f)
                close()
            }
        }.build()
    }

    val GridView: ImageVector by lazy {
        ImageVector.Builder(
            name = "GridView",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(3f, 3f); lineTo(11f, 3f); lineTo(11f, 11f); lineTo(3f, 11f); close()
                moveTo(13f, 3f); lineTo(21f, 3f); lineTo(21f, 11f); lineTo(13f, 11f); close()
                moveTo(3f, 13f); lineTo(11f, 13f); lineTo(11f, 21f); lineTo(3f, 21f); close()
                moveTo(13f, 13f); lineTo(21f, 13f); lineTo(21f, 21f); lineTo(13f, 21f); close()
            }
        }.build()
    }

    val ViewAgenda: ImageVector by lazy {
        ImageVector.Builder(
            name = "ViewAgenda",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 3f); lineTo(4f, 3f); curveTo(2.9f, 3f, 2f, 3.9f, 2f, 5f)
                lineTo(2f, 10f); curveTo(2f, 11.1f, 2.9f, 12f, 4f, 12f)
                lineTo(20f, 12f); curveTo(21.1f, 12f, 22f, 11.1f, 22f, 10f)
                lineTo(22f, 5f); curveTo(22f, 3.9f, 21.1f, 3f, 20f, 3f); close()
                moveTo(20f, 14f); lineTo(4f, 14f); curveTo(2.9f, 14f, 2f, 14.9f, 2f, 16f)
                lineTo(2f, 19f); curveTo(2f, 20.1f, 2.9f, 21f, 4f, 21f)
                lineTo(20f, 21f); curveTo(21.1f, 21f, 22f, 20.1f, 22f, 19f)
                lineTo(22f, 16f); curveTo(22f, 14.9f, 21.1f, 14f, 20f, 14f); close()
            }
        }.build()
    }

    val Palette: ImageVector by lazy {
        ImageVector.Builder(
            name = "Palette",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 3f)
                curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
                curveTo(12.83f, 21f, 13.5f, 20.33f, 13.5f, 19.5f)
                curveTo(13.5f, 19.11f, 13.35f, 18.76f, 13.11f, 18.49f)
                curveTo(12.88f, 18.23f, 12.73f, 17.88f, 12.73f, 17.5f)
                curveTo(12.73f, 16.67f, 13.4f, 16f, 14.23f, 16f)
                lineTo(16.5f, 16f)
                curveTo(18.99f, 16f, 21f, 13.99f, 21f, 11.5f)
                curveTo(21f, 6.81f, 16.97f, 3f, 12f, 3f)
                close()
                moveTo(6.5f, 12f); curveTo(5.67f, 12f, 5f, 11.33f, 5f, 10.5f); curveTo(5f, 9.67f, 5.67f, 9f, 6.5f, 9f); curveTo(7.33f, 9f, 8f, 9.67f, 8f, 10.5f); curveTo(8f, 11.33f, 7.33f, 12f, 6.5f, 12f); close()
                moveTo(9.5f, 8f); curveTo(8.67f, 8f, 8f, 7.33f, 8f, 6.5f); curveTo(8f, 5.67f, 8.67f, 5f, 9.5f, 5f); curveTo(10.33f, 5f, 11f, 5.67f, 11f, 6.5f); curveTo(11f, 7.33f, 10.33f, 8f, 9.5f, 8f); close()
                moveTo(14.5f, 8f); curveTo(13.67f, 8f, 13f, 7.33f, 13f, 6.5f); curveTo(13f, 5.67f, 13.67f, 5f, 14.5f, 5f); curveTo(15.33f, 5f, 16f, 5.67f, 16f, 6.5f); curveTo(16f, 7.33f, 15.33f, 8f, 14.5f, 8f); close()
                moveTo(17.5f, 12f); curveTo(16.67f, 12f, 16f, 11.33f, 16f, 10.5f); curveTo(16f, 9.67f, 16.67f, 9f, 17.5f, 9f); curveTo(18.33f, 9f, 19f, 9.67f, 19f, 10.5f); curveTo(19f, 11.33f, 18.33f, 12f, 17.5f, 12f); close()
            }
        }.build()
    }

    val Pencil: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pencil",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(3f, 17.25f)
                lineTo(3f, 21f)
                lineTo(6.75f, 21f)
                lineTo(17.81f, 9.94f)
                lineTo(14.06f, 6.19f)
                lineTo(3f, 17.25f)
                close()
                moveTo(20.71f, 7.04f)
                curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
                lineTo(18.37f, 3.29f)
                curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
                lineTo(15.13f, 5.12f)
                lineTo(18.88f, 8.87f)
                lineTo(20.71f, 7.04f)
                close()
            }
        }.build()
    }

    val Paperclip: ImageVector by lazy {
        ImageVector.Builder(
            name = "Paperclip",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(16.5f, 6f)
                lineTo(8.5f, 14f)
                curveTo(7.12f, 15.38f, 7.12f, 17.62f, 8.5f, 19f)
                curveTo(9.88f, 20.38f, 12.12f, 20.38f, 13.5f, 19f)
                lineTo(20f, 12.5f)
                curveTo(22.21f, 10.29f, 22.21f, 6.71f, 20f, 4.5f)
                curveTo(17.79f, 2.29f, 14.21f, 2.29f, 12f, 4.5f)
                lineTo(4.5f, 12f)
                curveTo(1.46f, 15.04f, 1.46f, 19.96f, 4.5f, 23f)
                curveTo(7.54f, 26.04f, 12.46f, 26.04f, 15.5f, 23f)
                lineTo(21f, 17.5f)
                lineTo(19.5f, 16f)
                lineTo(14f, 21.5f)
                curveTo(11.79f, 23.71f, 8.21f, 23.71f, 6f, 21.5f)
                curveTo(3.79f, 19.29f, 3.79f, 15.71f, 6f, 13.5f)
                lineTo(13.5f, 6f)
                curveTo(15.02f, 4.48f, 17.48f, 4.48f, 19f, 6f)
                curveTo(20.52f, 7.52f, 20.52f, 9.98f, 19f, 11.5f)
                lineTo(12.5f, 18f)
                curveTo(11.67f, 18.83f, 10.33f, 18.83f, 9.5f, 18f)
                curveTo(8.67f, 17.17f, 8.67f, 15.83f, 9.5f, 15f)
                lineTo(17.5f, 7f)
                close()
            }
        }.build()
    }

    val Pin: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(16f, 12f)
                lineTo(16f, 4f)
                lineTo(17f, 4f)
                lineTo(17f, 2f)
                lineTo(7f, 2f)
                lineTo(7f, 4f)
                lineTo(8f, 4f)
                lineTo(8f, 12f)
                lineTo(5f, 15f)
                lineTo(5f, 17f)
                lineTo(11f, 17f)
                lineTo(11f, 22f)
                lineTo(13f, 22f)
                lineTo(13f, 17f)
                lineTo(19f, 17f)
                lineTo(19f, 15f)
                lineTo(16f, 12f)
                close()
            }
        }.build()
    }

    val PinOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "PinOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(14f, 4f)
                lineTo(14f, 9.17f)
                lineTo(16f, 11.17f)
                lineTo(16f, 4f)
                lineTo(17f, 4f)
                lineTo(17f, 2f)
                lineTo(7.83f, 2f)
                lineTo(9.83f, 4f)
                lineTo(14f, 4f)
                close()
                moveTo(19.78f, 22.61f)
                lineTo(18.49f, 23.9f)
                lineTo(13f, 18.41f)
                lineTo(13f, 22f)
                lineTo(11f, 22f)
                lineTo(11f, 16.41f)
                lineTo(5.86f, 11.27f)
                lineTo(5.86f, 15f)
                lineTo(5f, 15f)
                lineTo(5f, 17f)
                lineTo(10.59f, 17f)
                lineTo(1.39f, 4.22f)
                lineTo(2.8f, 2.81f)
                lineTo(19.78f, 22.61f)
                close()
            }
        }.build()
    }

    val Document: ImageVector by lazy {
        ImageVector.Builder(
            name = "Document",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(14f, 2f)
                lineTo(6f, 2f)
                curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
                lineTo(4f, 20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                lineTo(18f, 22f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                lineTo(20f, 8f)
                lineTo(14f, 2f)
                close()
                moveTo(13f, 9f)
                lineTo(13f, 3.5f)
                lineTo(18.5f, 9f)
                lineTo(13f, 9f)
                close()
                moveTo(8f, 13f); lineTo(16f, 13f); lineTo(16f, 15f); lineTo(8f, 15f); close()
                moveTo(8f, 17f); lineTo(13f, 17f); lineTo(13f, 19f); lineTo(8f, 19f); close()
            }
        }.build()
    }

    val Workflow: ImageVector by lazy {
        ImageVector.Builder(
            name = "Workflow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(22f, 11f); lineTo(17f, 11f); curveTo(15.9f, 11f, 15f, 11.9f, 15f, 13f); lineTo(15f, 14f)
                lineTo(9f, 14f); lineTo(9f, 9f); curveTo(9f, 7.9f, 8.1f, 7f, 7f, 7f); lineTo(6f, 7f)
                curveTo(6f, 5.34f, 4.66f, 4f, 3f, 4f); curveTo(1.34f, 4f, 0f, 5.34f, 0f, 7f)
                curveTo(0f, 8.66f, 1.34f, 10f, 3f, 10f); curveTo(4.24f, 10f, 5.29f, 9.24f, 5.71f, 8.15f)
                lineTo(7f, 8.15f); curveTo(7.47f, 8.15f, 7.85f, 8.53f, 7.85f, 9f); lineTo(7.85f, 15f)
                curveTo(7.85f, 15.55f, 8.3f, 16f, 8.85f, 16f); lineTo(15f, 16f); lineTo(15f, 17f)
                curveTo(15f, 18.1f, 15.9f, 19f, 17f, 19f); lineTo(22f, 19f); lineTo(22f, 11f); close()
            }
        }.build()
    }

    val Vault: ImageVector by lazy {
        ImageVector.Builder(
            name = "Vault",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 3.79f, 2f, 6f)
                lineTo(2f, 18f)
                curveTo(2f, 20.21f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 20.21f, 22f, 18f)
                lineTo(22f, 6f)
                curveTo(22f, 3.79f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 4f)
                curveTo(16.42f, 4f, 20f, 5.34f, 20f, 6f)
                curveTo(20f, 6.66f, 16.42f, 8f, 12f, 8f)
                curveTo(7.58f, 8f, 4f, 6.66f, 4f, 6f)
                curveTo(4f, 5.34f, 7.58f, 4f, 12f, 4f)
                close()
                moveTo(4f, 9.68f)
                curveTo(6.05f, 10.98f, 8.87f, 11.75f, 12f, 11.75f)
                curveTo(15.13f, 11.75f, 17.95f, 10.98f, 20f, 9.68f)
                lineTo(20f, 12f)
                curveTo(20f, 12.66f, 16.42f, 14f, 12f, 14f)
                curveTo(7.58f, 14f, 4f, 12.66f, 4f, 12f)
                lineTo(4f, 9.68f)
                close()
                moveTo(4f, 15.68f)
                curveTo(6.05f, 16.98f, 8.87f, 17.75f, 12f, 17.75f)
                curveTo(15.13f, 17.75f, 17.95f, 16.98f, 20f, 15.68f)
                lineTo(20f, 18f)
                curveTo(20f, 18.66f, 16.42f, 20f, 12f, 20f)
                curveTo(7.58f, 20f, 4f, 18.66f, 4f, 18f)
                lineTo(4f, 15.68f)
                close()
            }
        }.build()
    }

    val TaskCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "TaskCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19f, 3f); lineTo(5f, 3f); curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                lineTo(3f, 19f); curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                lineTo(19f, 21f); curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                lineTo(21f, 5f); curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f); close()
                moveTo(10f, 17f); lineTo(5f, 12f); lineTo(6.41f, 10.59f); lineTo(10f, 14.17f)
                lineTo(17.59f, 6.58f); lineTo(19f, 8f); lineTo(10f, 17f); close()
            }
        }.build()
    }

    val Bold: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bold",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(15.6f, 10.79f)
                curveTo(16.57f, 10.11f, 17.25f, 9.02f, 17.25f, 8f)
                curveTo(17.25f, 5.74f, 15.51f, 4f, 13.25f, 4f)
                lineTo(7f, 4f)
                lineTo(7f, 20f)
                lineTo(14.04f, 20f)
                curveTo(16.14f, 20f, 17.75f, 18.34f, 17.75f, 16.25f)
                curveTo(17.75f, 13.88f, 16.81f, 11.5f, 15.6f, 10.79f)
                close()
                moveTo(10f, 6.5f)
                lineTo(13f, 6.5f)
                curveTo(13.83f, 6.5f, 14.5f, 7.17f, 14.5f, 8f)
                curveTo(14.5f, 8.83f, 13.83f, 9.5f, 13f, 9.5f)
                lineTo(10f, 9.5f)
                lineTo(10f, 6.5f)
                close()
                moveTo(13.5f, 17.5f)
                lineTo(10f, 17.5f)
                lineTo(10f, 12.5f)
                lineTo(13.5f, 12.5f)
                curveTo(14.33f, 12.5f, 15f, 13.17f, 15f, 14f)
                curveTo(15f, 14.83f, 14.33f, 15.5f, 13.5f, 17.5f)
                close()
            }
        }.build()
    }

    val Italic: ImageVector by lazy {
        ImageVector.Builder(
            name = "Italic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10f, 4f); lineTo(19f, 4f); lineTo(17.5f, 7f); lineTo(14.8f, 7f)
                lineTo(11.2f, 17f); lineTo(14f, 17f); lineTo(12.5f, 20f); lineTo(5f, 20f)
                lineTo(6.5f, 17f); lineTo(9.2f, 17f); lineTo(12.8f, 7f); lineTo(10f, 7f); close()
            }
        }.build()
    }

    val Heading: ImageVector by lazy {
        ImageVector.Builder(
            name = "Heading",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(5f, 4f); lineTo(8f, 4f); lineTo(8f, 11f); lineTo(16f, 11f); lineTo(16f, 4f)
                lineTo(19f, 4f); lineTo(19f, 20f); lineTo(16f, 20f); lineTo(16f, 13f); lineTo(8f, 13f)
                lineTo(8f, 20f); lineTo(5f, 20f); close()
            }
        }.build()
    }

    val Code: ImageVector by lazy {
        ImageVector.Builder(
            name = "Code",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9.4f, 16.6f); lineTo(4.8f, 12f); lineTo(9.4f, 7.4f); lineTo(8f, 6f)
                lineTo(2f, 12f); lineTo(8f, 18f); close()
                moveTo(14.6f, 16.6f); lineTo(19.2f, 12f); lineTo(14.6f, 7.4f); lineTo(16f, 6f)
                lineTo(22f, 12f); lineTo(16f, 18f); close()
            }
        }.build()
    }

    val BulletList: ImageVector by lazy {
        ImageVector.Builder(
            name = "BulletList",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 6f); curveTo(2.9f, 6f, 2f, 6.9f, 2f, 8f); curveTo(2f, 9.1f, 2.9f, 10f, 4f, 10f); curveTo(5.1f, 10f, 6f, 9.1f, 6f, 8f); curveTo(6f, 6.9f, 5.1f, 6f, 4f, 6f); close()
                moveTo(4f, 11f); curveTo(2.9f, 11f, 2f, 11.9f, 2f, 13f); curveTo(2f, 14.1f, 2.9f, 15f, 4f, 15f); curveTo(5.1f, 15f, 6f, 14.1f, 6f, 13f); curveTo(6f, 11.9f, 5.1f, 11f, 4f, 11f); close()
                moveTo(4f, 16f); curveTo(2.9f, 16f, 2f, 16.9f, 2f, 18f); curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f); curveTo(5.1f, 20f, 6f, 19.1f, 6f, 18f); curveTo(6f, 16.9f, 5.1f, 16f, 4f, 16f); close()
                moveTo(9f, 7f); lineTo(21f, 7f); lineTo(21f, 9f); lineTo(9f, 9f); close()
                moveTo(9f, 12f); lineTo(21f, 12f); lineTo(21f, 14f); lineTo(9f, 14f); close()
                moveTo(9f, 17f); lineTo(21f, 17f); lineTo(21f, 19f); lineTo(9f, 19f); close()
            }
        }.build()
    }

    val Recenter: ImageVector by lazy {
        ImageVector.Builder(
            name = "Recenter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(5f, 15f); lineTo(3f, 15f); lineTo(3f, 19f); curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                lineTo(9f, 21f); lineTo(9f, 19f); lineTo(5f, 19f); close()
                moveTo(5f, 5f); lineTo(9f, 5f); lineTo(9f, 3f); lineTo(5f, 3f); curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                lineTo(3f, 9f); lineTo(5f, 9f); close()
                moveTo(19f, 3f); lineTo(15f, 3f); lineTo(15f, 5f); lineTo(19f, 5f); lineTo(19f, 9f)
                lineTo(21f, 9f); lineTo(21f, 5f); curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f); close()
                moveTo(19f, 19f); lineTo(15f, 19f); lineTo(15f, 21f); lineTo(19f, 21f); curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                lineTo(21f, 15f); lineTo(19f, 15f); close()
                moveTo(12f, 8f); curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f); curveTo(8f, 14.21f, 9.79f, 16f, 12f, 16f)
                curveTo(14.21f, 16f, 16f, 14.21f, 16f, 12f); curveTo(16f, 9.79f, 14.21f, 8f, 12f, 8f); close()
            }
        }.build()
    }

    val Sun: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sun",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 7f); curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f); curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
                curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f); curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f); close()
                moveTo(11f, 1f); lineTo(13f, 1f); lineTo(13f, 4f); lineTo(11f, 4f); close()
                moveTo(11f, 20f); lineTo(13f, 20f); lineTo(13f, 23f); lineTo(11f, 23f); close()
                moveTo(4.22f, 5.64f); lineTo(5.64f, 4.22f); lineTo(7.76f, 6.34f); lineTo(6.34f, 7.76f); close()
                moveTo(16.24f, 17.66f); lineTo(17.66f, 16.24f); lineTo(19.78f, 18.36f); lineTo(18.36f, 19.78f); close()
                moveTo(1f, 11f); lineTo(4f, 11f); lineTo(4f, 13f); lineTo(1f, 13f); close()
                moveTo(20f, 11f); lineTo(23f, 11f); lineTo(23f, 13f); lineTo(20f, 13f); close()
                moveTo(6.34f, 16.24f); lineTo(7.76f, 17.66f); lineTo(5.64f, 19.78f); lineTo(4.22f, 18.36f); close()
                moveTo(18.36f, 4.22f); lineTo(19.78f, 5.64f); lineTo(17.66f, 7.76f); lineTo(16.24f, 6.34f); close()
            }
        }.build()
    }

    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12.3f, 2f)
                curveTo(6.9f, 2.3f, 2.6f, 6.7f, 2.6f, 12.1f)
                curveTo(2.6f, 17.6f, 7.1f, 22.1f, 12.6f, 22.1f)
                curveTo(16.2f, 22.1f, 19.3f, 20.2f, 21.1f, 17.2f)
                curveTo(20.4f, 17.3f, 19.7f, 17.4f, 19f, 17.4f)
                curveTo(13.5f, 17.4f, 9f, 12.9f, 9f, 7.4f)
                curveTo(9f, 5.2f, 9.7f, 3.2f, 10.9f, 1.6f)
                curveTo(11.4f, 1.7f, 11.9f, 1.8f, 12.3f, 2f)
                close()
            }
        }.build()
    }

    val OledBlack: ImageVector by lazy {
        ImageVector.Builder(
            name = "OledBlack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
                curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
                close()
            }
        }.build()
    }

    val Camera: ImageVector by lazy {
        ImageVector.Builder(
            name = "Camera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 9f); curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f); curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
                curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f); curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f); close()
                moveTo(20f, 4f); lineTo(16.83f, 4f); lineTo(15f, 2f); lineTo(9f, 2f); lineTo(7.17f, 4f); lineTo(4f, 4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f); lineTo(2f, 18f); curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                lineTo(20f, 20f); curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f); lineTo(22f, 6f)
                curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f); close()
                moveTo(12f, 17f); curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f); curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
                curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f); curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f); close()
            }
        }.build()
    }

    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "Download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19f, 9f)
                lineTo(15f, 9f)
                lineTo(15f, 3f)
                lineTo(9f, 3f)
                lineTo(9f, 9f)
                lineTo(5f, 9f)
                lineTo(12f, 16f)
                lineTo(19f, 9f)
                close()
                moveTo(5f, 18f)
                lineTo(19f, 18f)
                lineTo(19f, 20f)
                lineTo(5f, 20f)
                close()
            }
        }.build()
    }

    val Share: ImageVector by lazy {
        ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(18f, 16.08f)
                curveTo(17.24f, 16.08f, 16.56f, 16.38f, 16.04f, 16.85f)
                lineTo(8.91f, 12.7f)
                curveTo(8.96f, 12.47f, 9f, 12.24f, 9f, 12f)
                curveTo(9f, 11.76f, 8.96f, 11.53f, 8.91f, 11.3f)
                lineTo(15.96f, 7.19f)
                curveTo(16.5f, 7.69f, 17.21f, 8f, 18f, 8f)
                curveTo(19.66f, 8f, 21f, 6.66f, 21f, 5f)
                curveTo(21f, 3.34f, 19.66f, 2f, 18f, 2f)
                curveTo(16.34f, 2f, 15f, 3.34f, 15f, 5f)
                curveTo(15f, 5.24f, 15.04f, 5.47f, 15.09f, 5.7f)
                lineTo(8.04f, 9.81f)
                curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
                curveTo(4.34f, 9f, 3f, 10.34f, 3f, 12f)
                curveTo(3f, 13.66f, 4.34f, 15f, 6f, 15f)
                curveTo(6.79f, 15f, 7.5f, 14.69f, 8.04f, 14.19f)
                lineTo(15.16f, 18.35f)
                curveTo(15.11f, 18.56f, 15.08f, 18.78f, 15.08f, 19f)
                curveTo(15.08f, 20.61f, 16.39f, 21.92f, 18f, 21.92f)
                curveTo(19.61f, 21.92f, 20.92f, 20.61f, 20.92f, 19f)
                curveTo(20.92f, 17.39f, 19.61f, 16.08f, 18f, 16.08f)
                close()
            }
        }.build()
    }
}
