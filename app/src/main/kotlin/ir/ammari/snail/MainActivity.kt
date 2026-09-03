package ir.ammari.snail

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ir.ammari.snail.ui.theme.SnailTheme
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SnailTheme {
                TimerScreen(
                    onAddToCalendar = { startTime, endTime ->
                        addCalendarEvent(
                            startTime = startTime,
                            endTime = endTime
                        )
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
            startTime = startTime,
            endTime = endTime
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
                "Focus"
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
    /*
     * rememberSaveable survives configuration changes,
     * including screen rotation.
     */
    var isRunning by rememberSaveable {
        mutableStateOf(false)
    }

    var elapsedSeconds by rememberSaveable {
        mutableLongStateOf(0L)
    }

    var startTime by rememberSaveable {
        mutableLongStateOf(0L)
    }

    /*
     * Calculate elapsed time from the actual clock.
     *
     * We don't increment elapsedSeconds by one every second.
     * Instead, we calculate:
     *
     * current time - start time
     *
     * This prevents timer drift.
     *
     * When the screen rotates, this effect is recreated,
     * but isRunning and startTime are restored by
     * rememberSaveable, so the timer continues running.
     */
    LaunchedEffect(isRunning, startTime) {

        while (isRunning) {

            val currentTime =
                System.currentTimeMillis()

            elapsedSeconds =
                (currentTime - startTime) / 1000L

            delay(1000)
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
                    startTime =
                        System.currentTimeMillis()

                    elapsedSeconds = 0L

                    isRunning = true

                } else {

                    // Stop timer
                    val endTime =
                        System.currentTimeMillis()

                    // Calculate final elapsed time
                    elapsedSeconds =
                        (endTime - startTime) / 1000L

                    isRunning = false

                    // Add timer session to calendar
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

    val hours =
        seconds / 3600

    val minutes =
        (seconds % 3600) / 60

    val remainingSeconds =
        seconds % 60

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