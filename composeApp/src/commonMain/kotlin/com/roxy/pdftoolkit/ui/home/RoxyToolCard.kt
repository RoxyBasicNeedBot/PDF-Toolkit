package com.roxy.pdftoolkit.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roxy.pdftoolkit.ui.glass.RoxyBackdrop
import com.roxy.pdftoolkit.ui.glass.RoxyGlassCard
import com.roxy.pdftoolkit.ui.theme.RoxyColor

@Composable
fun RoxyToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    backdrop: RoxyBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RoxyGlassCard(
        backdrop = backdrop,
        shape = RoundedCornerShape(20.dp),
        tintColor = accentColor,
        scrimAlpha = 0.22f,
        blurRadius = 14.dp,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                RoxyGlassCard(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(12.dp),
                    tintColor = accentColor,
                    scrimAlpha = 0.35f,
                    interactive = false,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    color = RoxyColor.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
