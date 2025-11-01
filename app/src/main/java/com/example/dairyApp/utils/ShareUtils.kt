package com.example.dairyApp.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utility to share one or more image Uris together with a caption.
 * Behavior:
 * - Copies source URI streams into app cache under cacheDir/shared_images/
 * - Exposes them via FileProvider.getUriForFile(...) using authority "${applicationId}.provider"
 * - Puts EXTRA_TEXT with caption and also copies caption to clipboard
 * - Launches chooser with FLAG_GRANT_READ_URI_PERMISSION
 */
object ShareUtils {
    private const val SHARE_DIR = "shared_images"

    fun shareImagesWithCaption(context: Context, sourceUris: List<Uri>, caption: String) {
        val cacheDir = File(context.cacheDir, SHARE_DIR)
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val contentUris = ArrayList<Uri>()
        val resolver = context.contentResolver
        try {
            for ((index, src) in sourceUris.withIndex()) {
                // create a stable file name
                val outFile = File(cacheDir, "share_${System.currentTimeMillis()}_${index}.jpg")
                resolver.openInputStream(src)?.use { input ->
                    FileOutputStream(outFile).use { out ->
                        copyStream(input, out)
                    }
                }
                val authority = context.packageName + ".provider"
                val contentUri = FileProvider.getUriForFile(context, authority, outFile)
                contentUris.add(contentUri)
            }

            // Copy caption to clipboard so user can paste if target app ignores EXTRA_TEXT
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("caption", caption)
                clipboard.setPrimaryClip(clip)
            } catch (_: Exception) {
                // non-fatal
            }

            val shareIntent: Intent = if (contentUris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, contentUris.first())
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, contentUris)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            val chooser = Intent.createChooser(shareIntent, "分享到")
            // Grant temporary read permission to all resolved targets
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // best-effort fallback to share text only
            try {
                context.startActivity(Intent.createChooser(Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, caption)
                }, "分享到"))
            } catch (_: Exception) {
                // give up silently
            }
        }
    }

    private fun copyStream(input: InputStream, out: FileOutputStream) {
        val buffer = ByteArray(8 * 1024)
        var read: Int
        while (true) {
            read = input.read(buffer)
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
        out.flush()
    }
}
