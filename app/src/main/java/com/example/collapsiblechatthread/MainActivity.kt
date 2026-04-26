package com.example.collapsiblechatthread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.chatthread.CollapsibleChatThread
import com.example.chatthread.ThreadComment
import com.example.chatthread.ThreadStyle
import com.example.chatthread.ThreadTag
import com.example.chatthread.rememberThreadState
import com.example.collapsiblechatthread.ui.theme.CollapsibleChatThreadTheme

/**
 * Demo host for the `:chatthread` library. Draws a single screen with [DemoScreen] inside the
 * app theme, using edge-to-edge so the thread can extend behind the status/navigation bars.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CollapsibleChatThreadTheme {
                DemoScreen()
            }
        }
    }
}

/**
 * Sample screen wiring the library together:
 *
 * 1. Builds a hard-coded [demoComments] tree from string resources.
 * 2. Creates a [com.example.chatthread.ThreadState] with the root pre-expanded.
 * 3. Overrides the default [ThreadStyle] with concrete icon painters via `painterResource`.
 * 4. Pads the list inside the system bars (`WindowInsets.systemBars`) so scroll content can pass
 *    under them while first/last rows still stay fully tappable.
 * 5. Overlays [StatusBarGradient] on top so content fades into the background color as it scrolls
 *    under the status bar.
 */
@Composable
private fun DemoScreen() {
    val comments = demoComments()
    val state = rememberThreadState(initiallyExpanded = setOf(comments.first().id))
    val style = ThreadStyle.Default.copy(
        expandIcon = { painterResource(R.drawable.plus_circle) },
        collapseIcon = { painterResource(R.drawable.minus_circle) },
        replyIcon = { painterResource(R.drawable.reply) },
    )
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF06060A), Color(0xFF0F1318)),
    )

    Box(modifier = Modifier.fillMaxSize().background(gradientBackground)) {
        CollapsibleChatThread(
            comments = comments,
            state = state,
            style = style,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = systemBarsPadding.calculateTopPadding(),
                bottom = systemBarsPadding.calculateBottomPadding(),
            ),
        )
        StatusBarGradient(color = Color(0xFF06060A))
    }
}

/**
 * Paints a vertical gradient across the status-bar region so scrolled content fades into the
 * background color instead of hard-clipping under the system bar.
 *
 * [fadeEnd] is a bit taller than the bar itself (x1.2) so the fade finishes just *below* the bar,
 * giving the illusion of a translucent bar rather than a fixed opaque strip.
 */
@Composable
private fun StatusBarGradient(color: Color) {
    val statusBarHeightPx = with(LocalDensity.current) {
        WindowInsets.systemBars.asPaddingValues().calculateTopPadding().toPx()
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val fadeEnd = statusBarHeightPx * 1.2f
        val gradient = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 1f), color.copy(alpha = 0.8f), Color.Transparent),
            startY = 0f,
            endY = fadeEnd,
        )
        drawRect(brush = gradient, size = Size(size.width, fadeEnd))
    }
}

/**
 * Builds the demo comment tree (one root post, three top-level replies, nested leaves) from
 * localized strings + tag color palette.
 *
 * Kept as a `@Composable` function so it can resolve `stringResource` — the returned list is plain
 * data and contains no Compose state.
 */
@Composable
private fun demoComments(): List<ThreadComment> {
    val tagBgAmber = Color(0x801F222A)
    val tagFgAmber = Color(0xFFAFB2B9)
    val tagBgGreen = Color(0x1F75FABF)
    val tagFgGreen = Color(0xFF75FABF)
    val tagBgOrange = Color(0x1FFFC368)
    val tagFgOrange = Color(0xFFFFC368)

    return listOf(
        ThreadComment(
            id = "root",
            author = stringResource(R.string.main_post_username),
            avatarBackground = Color(0xFF75FABF),
            timestamp = "1 day ago",
            channel = stringResource(R.string.main_post_category),
            title = stringResource(R.string.main_post_title),
            body = stringResource(R.string.main_post_body),
            tags = listOf(ThreadTag("Top 1% Poster", tagBgAmber, tagFgAmber)),
            replies = listOf(
                ThreadComment(
                    id = "c1",
                    author = stringResource(R.string.comment1_username),
                    avatarBackground = Color(0xFFFFC368),
                    timestamp = stringResource(R.string.comment1_time),
                    body = stringResource(R.string.comment1_body),
                    tags = listOf(
                        ThreadTag(
                            text = stringResource(R.string.comment1_badge),
                            background = tagBgOrange,
                            contentColor = tagFgOrange,
                        )
                    ),
                    replies = listOf(
                        leafReply("c1r1", R.string.comment1_reply1_username, R.string.comment1_reply1_time, R.string.comment1_reply1_body, Color(0xFF68C3FF)),
                        leafReply("c1r2", R.string.comment1_reply2_username, R.string.comment1_reply2_time, R.string.comment1_reply2_body, Color(0xFF75FABF)),
                        leafReply("c1r3", R.string.comment1_reply3_username, R.string.comment1_reply3_time, R.string.comment1_reply3_body, Color(0xFFFFC368)),
                        leafReply("c1r4", R.string.comment1_reply4_username, R.string.comment1_reply4_time, R.string.comment1_reply4_body, Color(0xFF68C3FF)),
                    ),
                ),
                ThreadComment(
                    id = "c2",
                    author = stringResource(R.string.comment2_username),
                    avatarBackground = Color(0xFF75FABF),
                    timestamp = stringResource(R.string.comment2_time),
                    body = stringResource(R.string.comment2_body),
                    tags = listOf(ThreadTag(stringResource(R.string.comment2_badge), tagBgGreen, tagFgGreen)),
                ),
                ThreadComment(
                    id = "c3",
                    author = stringResource(R.string.comment3_username),
                    avatarBackground = Color(0xFFFFC368),
                    timestamp = stringResource(R.string.comment3_time),
                    body = stringResource(R.string.comment3_body),
                    replies = listOf(
                        leafReply("c3r1", R.string.comment3_reply1_username, R.string.comment3_reply1_time, R.string.comment3_reply1_body, Color(0xFF68C3FF)),
                        leafReply("c3r2", R.string.comment3_reply2_username, R.string.comment3_reply2_time, R.string.comment3_reply2_body, Color(0xFF75FABF)),
                    ),
                ),
            ),
        ),
    )
}

/**
 * Tiny helper that builds a leaf [ThreadComment] (no replies, no tags, no title) out of four
 * string resource ids + an avatar color. Cuts the noise in [demoComments] so the tree shape is
 * easier to read.
 */
@Composable
private fun leafReply(
    id: String,
    usernameRes: Int,
    timeRes: Int,
    bodyRes: Int,
    avatarColor: Color,
): ThreadComment = ThreadComment(
    id = id,
    author = stringResource(usernameRes),
    avatarBackground = avatarColor,
    timestamp = stringResource(timeRes),
    body = stringResource(bodyRes),
)
