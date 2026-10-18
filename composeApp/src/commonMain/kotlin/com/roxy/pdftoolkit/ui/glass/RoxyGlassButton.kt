package com.roxy.pdftoolkit.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roxy.pdftoolkit.ui.theme.RoxyColor

private val RoxyButtonPillShape = RoundedCornerShape(percent = 50)

/**
 * High-emphasis interactive Liquid Glass pill button.
 */
@Composable
fun RoxyGlassButton(
    text: String,
    backdrop: RoxyBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = RoxyColor.Primary,
    enabled: Boolean = true
) {
    val interaction = rememberRoxyGlassInteraction()

    Row(
        modifier = modifier
            .height(52.dp)
            .roxyLiquidGlass(
                backdrop = backdrop,
                shape = RoxyButtonPillShape,
                interaction = if (enabled) interaction else null,
                tintColor = if (enabled) accentColor else Color.Gray,
                scrimAlpha = 0.35f
            )
            .clip(RoxyButtonPillShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (enabled) Color.White else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = text,
            color = if (enabled) Color.White else Color.Gray,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}
