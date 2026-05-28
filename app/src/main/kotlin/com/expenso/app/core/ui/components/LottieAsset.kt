package com.expenso.app.core.ui.components

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun LottieLoop(
    @RawRes res: Int,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(res))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = speed,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
}

@Composable
fun LottieOneShot(
    @RawRes res: Int,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    onFinished: () -> Unit = {},
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(res))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = speed,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
    LaunchedEffect(progress) {
        if (progress >= 1f && composition != null) onFinished()
    }
}
