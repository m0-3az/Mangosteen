package ai.opencode.mangosteen.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

/** Copies picked images into app storage (downscaled) for later upload. */
object ImageStore {

    private const val MAX_DIM = 1440

    fun save(context: Context, uri: Uri): Pair<String, String>? = runCatching {
        val resolver = context.contentResolver
        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        val file = File(dir, "img-${System.currentTimeMillis()}.jpg")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        val sample = if (longest > MAX_DIM) longest / MAX_DIM else 1

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        file.absolutePath to "image/jpeg"
    }.getOrNull()

    fun base64(path: String): String? = runCatching {
        Base64.encodeToString(File(path).readBytes(), Base64.NO_WRAP)
    }.getOrNull()
}
