package com.example.prototyx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.theme.BorderLight
import com.example.prototyx.theme.SurfaceWhite
import com.example.prototyx.theme.TextCharcoal
import com.example.prototyx.theme.TextMuted

@Composable
fun DoubleBezelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Outer Shell - Optimized by using simple Box and Column instead of Card
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.03f), shape = RoundedCornerShape(24.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.05f), shape = RoundedCornerShape(24.dp))
            .padding(6.dp)
    ) {
        // Inner Core - Use Column directly with background for performance
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite, shape = RoundedCornerShape(18.dp))
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
fun EditorialHeading(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextCharcoal
) {
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 26.sp,
        letterSpacing = (-0.5).sp,
        color = color,
        modifier = modifier
    )
}

@Composable
fun MetadataLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextMuted
) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = color,
        modifier = modifier
    )
}

@Composable
fun BentoMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        MetadataLabel(text = label)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextCharcoal
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TextCharcoal,
            contentColor = SurfaceWhite
        ),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = SurfaceWhite,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
