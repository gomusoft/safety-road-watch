package watch.saferoad

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.camera.video.Recorder as CxRecorder

/** Whether the back camera of this phone can stabilize video. Many mid-range and cheap phones cannot. */
object Stabilization {
    fun supported(provider: ProcessCameraProvider): Boolean = try {
        CxRecorder.getVideoCapabilities(provider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)).isStabilizationSupported
    } catch (ex: Exception) {
        false
    }

    /** Asks the camera and calls [done] on the main thread. */
    fun check(c: Context, done: (Boolean) -> Unit) {
        val future = ProcessCameraProvider.getInstance(c)
        future.addListener({
            done(try { supported(future.get()) } catch (ex: Exception) { false })
        }, ContextCompat.getMainExecutor(c))
    }
}
