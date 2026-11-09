package com.pulihgo.app.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R

private val Teal = Color(0xFF0E7C7B)
private val SafetyRed = Color(0xFFD64545)
private val PrimaryText = Color(0xFF1A1D1A)
private val SecondaryText = Color(0xFF5B5F58)
private val NeutralBorder = Color(0xFFE2E4DE)

private data class OnboardingSlide(
    val kicker: String,
    val title: String,
    val body: String,
    @DrawableRes val backgroundRes: Int,
    @DrawableRes val mascotRes: Int,
    val accent: Color,
    val ringBackground: Color
)

/**
 * Visual reference: Figma "PulihGo / Patient Hi-Fi Final", frames 76:1473,
 * 76:1503, 76:1535, 76:1567. Copy and image mapping are based on the
 * original PulihGo onboarding source. No clinical claims are added.
 */
private val slides = listOf(
    OnboardingSlide(
        kicker = "WELCOME",
        title = "PulihGo",
        body = "PulihGo helps you practice your arm exercises at home, safely.",
        backgroundRes = R.drawable.onboarding_bg_welcome,
        mascotRes = R.drawable.onboarding_mascot_hi,
        accent = Teal,
        ringBackground = Color(0xFFE1F4F7)
    ),
    OnboardingSlide(
        kicker = "DAILY TRACKING",
        title = "See Your Day",
        body = "See if you've practiced today and start your exercise here.",
        backgroundRes = R.drawable.onboarding_bg_daily,
        mascotRes = R.drawable.onboarding_mascot_daily,
        accent = Teal,
        ringBackground = Color(0xFFE1F4F7)
    ),
    OnboardingSlide(
        kicker = "EASY PRACTICE",
        title = "Exercise Library",
        body = "Pick an exercise, follow the steps, and the app counts your reps automatically.",
        backgroundRes = R.drawable.onboarding_bg_exercise,
        mascotRes = R.drawable.onboarding_mascot_exercise,
        accent = Teal,
        ringBackground = Color(0xFFE1F4F7)
    ),
    OnboardingSlide(
        kicker = "ALWAYS SAFE",
        title = "Safety First",
        body = "If anything hurts, tap 'Stop — it hurts' any time. Stopping is never a bad thing.",
        backgroundRes = R.drawable.onboarding_bg_welcome,
        mascotRes = R.drawable.onboarding_mascot_hi,
        accent = SafetyRed,
        ringBackground = Color(0xFFFBE6E4)
    )
)

/**
 * The host owns the destination after the final slide or Skip.
 * This screen intentionally has no dependency on Dinda's navigation/role flow.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val slide = slides[currentStep]

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        Image(
            painter = painterResource(slide.backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.75f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = painterResource(R.drawable.onboarding_logo),
                    contentDescription = "PulihGo logo",
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text("Skip", color = SecondaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val line = 0.8.dp.toPx()
                            drawCircle(
                                color = slide.accent.copy(alpha = 0.35f),
                                radius = size.minDimension / 2 - line / 2,
                                style = Stroke(
                                    width = line,
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(4.dp.toPx(), 6.dp.toPx())
                                    )
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .shadow(5.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.05f))
                                .clip(CircleShape)
                                .background(slide.ringBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(slide.mascotRes),
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(Modifier.height(36.dp))
                    Text(
                        text = slide.kicker,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = slide.accent,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = slide.title,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = slide.body,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = SecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Onboarding slide ${currentStep + 1} of ${slides.size}"
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.forEachIndexed { index, _ ->
                    if (index > 0) Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(if (index == currentStep) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (index == currentStep) slide.accent else NeutralBorder)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f).height(52.dp),
                        border = BorderStroke(1.dp, NeutralBorder),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText)
                    ) {
                        Text("Back", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Button(
                    onClick = { if (currentStep == slides.lastIndex) onDone() else currentStep++ },
                    modifier = Modifier
                        .weight(if (currentStep == 0) 1f else 2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = slide.accent)
                ) {
                    Text(
                        if (currentStep == slides.lastIndex) "Get Started" else "Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 924)
@Composable
private fun OnboardingPreview() {
    OnboardingScreen(onDone = {})
}
