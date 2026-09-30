package com.notes.client.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ultra-compact Markdown Editor Formatting Toolbar.
 * Features 4px item spacing, no individual button backgrounds, and dynamic button ordering.
 */
@Composable
fun EditorToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    activeButtonIds: List<String> = ToolbarRegistry.DEFAULT_BUTTON_IDS
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            activeButtonIds.forEach { buttonId ->
                val actionItem = ToolbarRegistry.ALL_ACTIONS[buttonId]
                if (actionItem != null) {
                    CompactToolbarButton(
                        label = actionItem.label,
                        title = actionItem.title,
                        icon = actionItem.icon,
                        fontWeight = actionItem.fontWeight,
                        fontStyle = actionItem.fontStyle,
                        fontFamily = actionItem.fontFamily,
                        textDecoration = actionItem.textDecoration,
                        fontSize = actionItem.fontSize,
                        onClick = { onValueChange(actionItem.action(value)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactToolbarButton(
    label: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    textDecoration: TextDecoration? = null,
    fontSize: TextUnit = 13.sp
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontFamily = fontFamily,
                textDecoration = textDecoration,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
