package ir.ammari.tomato

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ir.ammari.tomato.ui.theme.TomatoTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TomatoTheme {
                TimerScreen(
                    onAddToCalendar = { startTime, endTime ->
                        addCalendarEvent(startTime, endTime)
                    }
                )
            }
        }
    }

    private fun addCalendarEvent(
        startTime: Long,
        endTime: Long
    ) {
        val (roundedStart, roundedEnd) = roundToFiveMinutes(
            startTime,
            endTime
        )

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI

            putExtra(
                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                roundedStart
            )

            putExtra(
                CalendarContract.EXTRA_EVENT_END_TIME,
                roundedEnd
            )

            putExtra(
                CalendarContract.Events.TITLE,
                "Tomato Timer"
            )
        }

        startActivity(intent)
    }
}

/**
 * Rounds the start time down and the end time up
 * to 5-minute intervals.
 *
 * Example:
 * 17:41 -> 17:40
 * 18:19 -> 18:20
 *
 * The resulting event will always be at least 5 minutes long.
 */
private fun roundToFiveMinutes(
    startTime: Long,
    endTime: Long
): Pair<Long, Long> {

    val fiveMinutes = 5 * 60 * 1000L

    // Round start DOWN
    val roundedStart =
        (startTime / fiveMinutes) * fiveMinutes

    // Round end UP
    var roundedEnd =
        ((endTime + fiveMinutes - 1) / fiveMinutes) * fiveMinutes

    // Minimum event duration: 5 minutes
    if (roundedEnd <= roundedStart) {
        roundedEnd = roundedStart + fiveMinutes
    }

    return roundedStart to roundedEnd
}

@Composable
fun TimerScreen(
    onAddToCalendar: (Long, Long) -> Unit
) {
    var isRunning by remember {
        mutableStateOf(false)
    }

    var elapsedSeconds by remember {
        mutableLongStateOf(0L)
    }

    var startTime by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            elapsedSeconds++
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                if (!isRunning) {
                    // Start timer
                    startTime = System.currentTimeMillis()
                    elapsedSeconds = 0L
                    isRunning = true
                } else {
                    // Stop timer
                    val endTime = System.currentTimeMillis()

                    isRunning = false

                    // Open calendar with rounded times
                    onAddToCalendar(
                        startTime,
                        endTime
                    )

                    // Reset timer
                    elapsedSeconds = 0L
                    startTime = 0L
                }
            }
        ) {
            Text(
                text = if (isRunning) {
                    formatTime(elapsedSeconds)
                } else {
                    "Start"
                }
            )
        }
    }
}

private fun formatTime(
    seconds: Long
): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return if (hours > 0) {
        "%02d:%02d:%02d".format(
            hours,
            minutes,
            remainingSeconds
        )
    } else {
        "%02d:%02d".format(
            minutes,
            remainingSeconds
        )
    }
}
