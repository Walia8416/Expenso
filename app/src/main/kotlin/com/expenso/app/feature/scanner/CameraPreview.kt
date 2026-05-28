package com.expenso.app.feature.scanner

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import timber.log.Timber

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onQrDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        QrAnalyzer { raw ->
            onQrDetected(raw)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                try {
                    val provider = providerFuture.get()
                    bindCamera(
                        context = context,
                        provider = provider,
                        preview = previewView,
                        analyzer = analyzer,
                        executor = executor,
                        lifecycleOwner = lifecycleOwner,
                    )
                } catch (t: Throwable) {
                    Timber.e(t, "Failed to bind camera")
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            executor.shutdown()
            val provider = runCatching { providerFuture.get() }.getOrNull()
            provider?.unbindAll()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun bindCamera(
    context: Context,
    provider: ProcessCameraProvider,
    preview: PreviewView,
    analyzer: QrAnalyzer,
    executor: java.util.concurrent.ExecutorService,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
) {
    val previewUseCase = Preview.Builder().build().also {
        it.surfaceProvider = preview.surfaceProvider
    }
    val analysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build().also {
            it.setAnalyzer(executor, analyzer)
        }

    val selector = CameraSelector.DEFAULT_BACK_CAMERA
    provider.unbindAll()
    provider.bindToLifecycle(lifecycleOwner, selector, previewUseCase, analysisUseCase)
}
