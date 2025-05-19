/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.material3.macrobenchmark.common

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

val TransformingLazyColumnNotificationsBenchmark =
    object : MacrobenchmarkScreen {
        override val content: @Composable (BoxScope.() -> Unit)
            get() = {
                val state = rememberTransformingLazyColumnState()
                val transformationSpec = rememberTransformationSpec()
                AppScaffold {
                    ScreenScaffold(state) { contentPadding ->
                        TransformingLazyColumn(
                            state = state,
                            contentPadding = contentPadding,
                            modifier =
                                Modifier.semantics { contentDescription = CONTENT_DESCRIPTION },
                        ) {
                            item {
                                ListHeader(
                                    transformation = SurfaceTransformation(transformationSpec),
                                    modifier = Modifier.transformedHeight(this, transformationSpec),
                                ) {
                                    Text("Notifications")
                                }
                            }
                            items(50_000) { index ->
                                val notification = notificationList[index % notificationList.size]
                                TitleCard(
                                    onClick = {},
                                    title = {
                                        Text(
                                            notification.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    },
                                    subtitle = { Text(notification.body) },
                                    transformation = SurfaceTransformation(transformationSpec),
                                    modifier =
                                        Modifier.transformedHeight(this@items, transformationSpec),
                                )
                            }
                        }
                    }
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                val swipeStartY = device.displayHeight * 9 / 10 // scroll up
                val swipeEndY = device.displayHeight / 10
                val midX = device.displayWidth / 2
                repeat(20) {
                    device.swipe(midX, swipeStartY, midX, swipeEndY, 2)
                    device.waitForIdle()
                }
            }
    }

private data class NotificationItem(val title: String, val body: String)

private val notificationList =
    listOf(
        NotificationItem(
            "☕ Coffee Break?",
            "Step away from the screen and grab a pick-me-up. Step away from the screen and grab a pick-me-up.",
        ),
        NotificationItem("🌟 You're Awesome!", "Just a little reminder in case you forgot 😊"),
        NotificationItem("👀 Did you know?", "Check out [app name]'s latest feature update."),
        NotificationItem("📅 Appointment Time", "Your meeting with [name] is in 15 minutes."),
        NotificationItem("📦 Package On the Way", "Your order is expected to arrive today!"),
        NotificationItem("🤔 Trivia Time!", "Test your knowledge with a quick quiz on [app name]."),
        NotificationItem(
            "🌤️ Weather Update",
            "Don't forget your umbrella - rain is likely this afternoon.",
        ),
        NotificationItem("🤝 Connect with [name]", "They sent you a message on [social platform]."),
        NotificationItem("🧘‍♀️ Time to Breathe", "Take a 5-minute mindfulness break."),
        NotificationItem("🌟 Goal Achieved!", "You completed your daily step goal. Way to go!"),
        NotificationItem("💡 New Idea!", "Got a spare moment? Jot down a quick note."),
        NotificationItem("👀 Photo Memories", "Rediscover photos from this day last year."),
        NotificationItem("🚗 Parking Reminder", "Your parking meter expires in 1 hour."),
        NotificationItem("🎧 Playlist Time", "Your daily mix on [music app] is ready."),
        NotificationItem(
            "🎬 Movie Night?",
            "New releases are out on your favorite streaming service. New releases are out on your favorite streaming service.",
        ),
        NotificationItem("📚 Reading Time", "Pick up where you left off in your current book."),
        NotificationItem("🤔 Something to Ponder", "Here's a thought-provoking quote for today..."),
        NotificationItem("⏰ Time for [task]", "Remember to [brief description]."),
        NotificationItem("💧 Stay Hydrated!", "Have you had a glass of water recently?"),
        NotificationItem("👀 Game Update Available", "Your favorite game has new content!"),
        NotificationItem("🌎 Learn Something New", "Fact of the day: [Insert a fun fact]."),
        NotificationItem(
            "☀️ Step Outside",
            "Get some fresh air and sunshine for a quick energy boost",
        ),
        NotificationItem("🎉 It's [friend's name]'s Birthday!", "Don't forget to send a message."),
        NotificationItem("✈️ Travel Inspiration", "Where's your dream travel destination?"),
        NotificationItem("😋 Recipe Time", "Find a new recipe to try on [recipe website]."),
        NotificationItem("👀 Explore!", "[App name] has a hidden feature - can you find it?"),
        NotificationItem("💰 Savings Update", "You're [percent] closer to your savings goal!"),
        NotificationItem("🌟 Daily Challenge", "Try today's mini-challenge on [app name]."),
        NotificationItem("💤 Bedtime Approaching", "Start winding down for a good night's sleep."),
        NotificationItem("🤝 Team Update", "[Team member] posted on your project board."),
        NotificationItem("🌿 Plant Care", "Time to water your [plant type]."),
        NotificationItem("🎮 Game Break?", "Take a 10-minute break with your favorite game."),
        NotificationItem("🗣️  Your Voice Matters", "New poll available on [topic/app]."),
        NotificationItem("🎨 Get Creative", "Doodle, draw, or paint for a few minutes."),
        NotificationItem("❓Ask a Question", "What's something that's been on your mind?"),
        NotificationItem("🔍 Search Time", "Research a topic that interests you."),
        NotificationItem(
            "🤝 Help Someone Out",
            "Is there a small way you can assist someone today?",
        ),
        NotificationItem("🐾 Pet Appreciation", "Give your furry friend some extra love."),
        NotificationItem("📝 Journal Time", "Take 5 minutes to jot down your thoughts."),
    )
