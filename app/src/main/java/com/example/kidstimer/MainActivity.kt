package com.example.kidstimer

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kidstimer.ui.theme.KidsTimerTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class TimerState {
    Ready,
    Running,
    Paused,
    Finished
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            KidsTimerTheme {
                KidsTimerApp()
            }
        }
    }
}

@Composable
private fun KidsTimerApp() {
    var selectedTotalSeconds by remember { mutableIntStateOf(5 * 60) }
    var totalSeconds by remember { mutableIntStateOf(selectedTotalSeconds) }
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    var timerState by remember { mutableStateOf(TimerState.Ready) }
    var isMuted by remember { mutableStateOf(false) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }
    val currentMuted by rememberUpdatedState(isMuted)

    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    LaunchedEffect(timerState) {
        while (timerState == TimerState.Running && secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
            if (!currentMuted && secondsLeft > 0) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 55)
            }
            if (secondsLeft == 0) {
                if (!currentMuted) {
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 450)
                }
                timerState = TimerState.Finished
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFF7E8)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF7E8), Color(0xFFE8F8F3))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Header(timerState)

                AnalogCountdownClock(
                    secondsLeft = secondsLeft,
                    totalSeconds = totalSeconds,
                    isFinished = timerState == TimerState.Finished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 18.dp)
                )

                TimerControls(
                    selectedTotalSeconds = selectedTotalSeconds,
                    secondsLeft = secondsLeft,
                    timerState = timerState,
                    onTimeChanged = { newTotalSeconds ->
                        selectedTotalSeconds = newTotalSeconds.coerceIn(5, 60 * 60)
                        totalSeconds = selectedTotalSeconds
                        secondsLeft = totalSeconds
                        timerState = TimerState.Ready
                    },
                    onStartPause = {
                        timerState = when (timerState) {
                            TimerState.Running -> TimerState.Paused
                            TimerState.Finished -> {
                                secondsLeft = totalSeconds
                                TimerState.Running
                            }
                            TimerState.Ready,
                            TimerState.Paused -> TimerState.Running
                        }
                    },
                    onReset = {
                        totalSeconds = selectedTotalSeconds
                        secondsLeft = totalSeconds
                        timerState = TimerState.Ready
                    }
                )
            }

            if (timerState == TimerState.Finished) {
                Celebration(modifier = Modifier.fillMaxSize())
            }

            MuteButton(
                isMuted = isMuted,
                onToggle = { isMuted = !isMuted },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 34.dp, end = 18.dp)
            )
        }
    }
}

@Composable
private fun Header(timerState: TimerState) {
    val message = when (timerState) {
        TimerState.Finished -> "All done!"
        TimerState.Running -> "Time is ticking"
        TimerState.Paused -> "Taking a pause"
        TimerState.Ready -> "Choose your time"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Kids Timer",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF243B3A),
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF59706B)
        )
    }
}

@Composable
private fun AnalogCountdownClock(
    secondsLeft: Int,
    totalSeconds: Int,
    isFinished: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds == 0) 0f else secondsLeft / totalSeconds.toFloat()
    val remainingSweep = -progress * 360f
    val elapsedSweep = -(1f - progress) * 360f
    val handAngle = -90f - progress * 360f
    val bobble by rememberInfiniteTransition(label = "clock bobble").animateFloat(
        initialValue = 0f,
        targetValue = if (isFinished) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobble"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(292.dp)
                .padding(6.dp)
        ) {
            val diameter = min(size.width, size.height)
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val ringStroke = radius * 0.13f
            val handLength = radius * 0.63f

            drawCircle(Color(0xFFFFFEFB), radius = radius * (0.92f + bobble * 0.03f), center = center)
            drawCircle(Color(0xFFB9E7DD), radius = radius * 0.92f, center = center, style = Stroke(ringStroke))
            drawArc(
                color = Color(0xFFEAF4F0),
                startAngle = -90f,
                sweepAngle = elapsedSweep,
                useCenter = false,
                style = Stroke(ringStroke, cap = StrokeCap.Butt),
                topLeft = Offset(center.x - radius * 0.92f, center.y - radius * 0.92f),
                size = androidx.compose.ui.geometry.Size(radius * 1.84f, radius * 1.84f)
            )
            drawArc(
                color = Color(0xFFFFB84D),
                startAngle = -90f,
                sweepAngle = remainingSweep,
                useCenter = false,
                style = Stroke(ringStroke, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius * 0.92f, center.y - radius * 0.92f),
                size = androidx.compose.ui.geometry.Size(radius * 1.84f, radius * 1.84f)
            )

            repeat(12) { tick ->
                val angle = Math.toRadians((tick * 30 - 90).toDouble())
                val outer = radius * 0.74f
                val inner = if (tick % 3 == 0) radius * 0.58f else radius * 0.64f
                val tickColor = if (tick % 3 == 0) Color(0xFF2D5250) else Color(0xFF8AB9B1)
                drawLine(
                    color = tickColor,
                    start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                    end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                    strokeWidth = if (tick % 3 == 0) 8f else 5f,
                    cap = StrokeCap.Round
                )
            }

            rotate(degrees = handAngle, pivot = center) {
                drawLine(
                    color = Color(0xFF2D5250),
                    start = center,
                    end = Offset(center.x + handLength, center.y),
                    strokeWidth = 13f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(Color(0xFFFF6B6B), radius = radius * 0.08f, center = center)
        }

        Text(
            text = formatTime(secondsLeft),
            style = MaterialTheme.typography.displayMedium,
            color = Color(0xFF243B3A),
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun TimerControls(
    selectedTotalSeconds: Int,
    secondsLeft: Int,
    timerState: TimerState,
    onTimeChanged: (Int) -> Unit,
    onStartPause: () -> Unit,
    onReset: () -> Unit
) {
    val selectedMinutes = selectedTotalSeconds / 60
    val selectedSeconds = selectedTotalSeconds % 60
    val canEdit = timerState != TimerState.Running

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.86f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "set countdown",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF718A84)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeAdjuster(
                    label = "min",
                    value = selectedMinutes,
                    enabled = canEdit,
                    onDecrease = { onTimeChanged(selectedTotalSeconds - 60) },
                    onIncrease = { onTimeChanged(selectedTotalSeconds + 60) },
                    modifier = Modifier.weight(1f)
                )
                TimeAdjuster(
                    label = "sec",
                    value = selectedSeconds,
                    enabled = canEdit,
                    onDecrease = { onTimeChanged(selectedTotalSeconds - 5) },
                    onIncrease = { onTimeChanged(selectedTotalSeconds + 5) },
                    modifier = Modifier.weight(1f)
                )
            }

            Slider(
                value = selectedTotalSeconds.toFloat(),
                onValueChange = { onTimeChanged(it.toInt()) },
                valueRange = 5f..3_600f,
                steps = 58,
                enabled = canEdit,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartPause,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F3C))
                ) {
                    Text(
                        text = when (timerState) {
                            TimerState.Running -> "Pause"
                            TimerState.Finished -> "Again"
                            else -> if (secondsLeft == 0) "Start" else "Start"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.width(12.dp))

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimeAdjuster(
    label: String,
    value: Int,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onDecrease,
            enabled = enabled,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFFE8F8F3))
        ) {
            Text("-", fontWeight = FontWeight.Black, color = Color(0xFF243B3A))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF243B3A),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF718A84)
            )
        }

        IconButton(
            onClick = onIncrease,
            enabled = enabled,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFFFFF0D2))
        ) {
            Text("+", fontWeight = FontWeight.Black, color = Color(0xFF243B3A))
        }
    }
}

@Composable
private fun Celebration(modifier: Modifier = Modifier) {
    val party by rememberInfiniteTransition(label = "party").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "party progress"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(Color(0xFFFF6B6B), Color(0xFFFFB84D), Color(0xFF45C4B0), Color(0xFF5B8DEF))
            repeat(32) { index ->
                val angle = Math.toRadians((index * 360f / 32f + party * 35f).toDouble())
                val distance = size.minDimension * (0.08f + party * 0.52f)
                val center = Offset(size.width / 2f, size.height / 2f)
                val position = Offset(
                    center.x + cos(angle).toFloat() * distance,
                    center.y + sin(angle).toFloat() * distance
                )
                drawCircle(
                    color = colors[index % colors.size].copy(alpha = 1f - party * 0.35f),
                    radius = 7f + (index % 4) * 2f,
                    center = position
                )
            }
        }

        Text(
            text = "Yay!",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 28.dp, vertical = 12.dp),
            color = Color(0xFFFF6B6B),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MuteButton(
    isMuted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.84f))
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val bodyColor = if (isMuted) Color(0xFF8A8A8A) else Color(0xFF2D5250)
            val speaker = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * 0.14f, size.height * 0.38f)
                lineTo(size.width * 0.34f, size.height * 0.38f)
                lineTo(size.width * 0.58f, size.height * 0.18f)
                lineTo(size.width * 0.58f, size.height * 0.82f)
                lineTo(size.width * 0.34f, size.height * 0.62f)
                lineTo(size.width * 0.14f, size.height * 0.62f)
                close()
            }
            drawPath(speaker, bodyColor)

            if (isMuted) {
                drawLine(
                    color = Color(0xFFFF6B6B),
                    start = Offset(size.width * 0.70f, size.height * 0.32f),
                    end = Offset(size.width * 0.94f, size.height * 0.68f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFF6B6B),
                    start = Offset(size.width * 0.94f, size.height * 0.32f),
                    end = Offset(size.width * 0.70f, size.height * 0.68f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            } else {
                drawArc(
                    color = bodyColor,
                    startAngle = -38f,
                    sweepAngle = 76f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.52f, size.height * 0.28f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.34f, size.height * 0.44f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = bodyColor,
                    startAngle = -42f,
                    sweepAngle = 84f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.44f, size.height * 0.18f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height * 0.64f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Preview(showBackground = true)
@Composable
private fun KidsTimerPreview() {
    KidsTimerTheme {
        KidsTimerApp()
    }
}
