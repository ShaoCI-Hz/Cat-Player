package com.example.smbplayer.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import com.example.smbplayer.MainActivity

class CatPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }
}

@Composable
private fun WidgetContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(ColorProvider(Color(0xFF121212), Color(0xFF121212)))
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(48.dp)
                    .cornerRadius(8.dp)
                    .background(ColorProvider(Color(0xFF1ED760), Color(0xFF1ED760))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "♪",
                    style = TextStyle(
                        color = ColorProvider(Color.White, Color.White),
                        fontSize = 24.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    "Cat Player",
                    style = TextStyle(
                        color = ColorProvider(Color.White, Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    "点击打开播放器",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFB3B3B3), Color(0xFFB3B3B3)),
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }

            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .cornerRadius(20.dp)
                    .background(ColorProvider(Color(0xFF1ED760), Color(0xFF1ED760)))
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "▶",
                    style = TextStyle(
                        color = ColorProvider(Color.Black, Color.Black),
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

class CatPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CatPlayerWidget()
}
