package de.openhabvoice.voice

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * Manages the on-device storage of the offline Vosk models:
 * downloading, unzipping, checking and deleting.
 *
 * Models live in `filesDir/models/<modelName>/` (app-private storage, no
 * storage permission needed). The download is a plain HTTPS GET of the
 * official model zip from alphacephei.com (~40-50 MB per language).
 */
class VoskModelStore(context: Context) {

    private val appContext = context.applicationContext
    private val modelsDir = File(appContext.filesDir, "models")

    /** Reuse OkHttp with generous timeouts (a 45 MB download can be slow). */
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
        .build()

    /** Directory of the installed model, null if it is not downloaded yet. */
    fun installedDir(modelName: String): File? =
        modelDir(modelName).takeIf { it.resolve("am/final.mdl").exists() }

    /** True if the model is fully downloaded and extracted. */
    fun isDownloaded(modelName: String): Boolean = installedDir(modelName) != null

    /** Size of the installed model in bytes (0 if not installed). */
    fun installedSizeBytes(modelName: String): Long = dirSize(modelDir(modelName))

    /**
     * Downloads and extracts the model. Blocking - must be called from a
     * background thread. [onProgress] receives the downloaded bytes and the
     * total size (total may be -1 if the server does not send Content-Length).
     */
    @Throws(IOException::class)
    fun download(modelName: String, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        val url = VoskModels.downloadUrl(modelName)
        val targetDir = modelDir(modelName)
        val tmpFile = File.createTempFile("$modelName-", ".zip", appContext.cacheDir)

        try {
            val response = client.newCall(
                Request.Builder().url(url).build()
            ).execute()

            response.use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("Download failed: HTTP ${resp.code}")
                }
                val body = resp.body ?: throw IOException("Download failed: empty body")
                val total = body.contentLength()
                var downloaded = 0L
                body.byteStream().use { input ->
                    tmpFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                    }
                }
            }

            // Extract into the models directory (zip-slip protected).
            // Model zips contain a single top-level folder (named like the model),
            // which is stripped so the model files land directly in targetDir.
            targetDir.deleteRecursively()
            targetDir.mkdirs()
            ZipFile(tmpFile).use { zip ->
                val prefix = commonRootPrefix(zip)
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.removePrefix(prefix)
                    if (name.isBlank()) continue
                    val outFile = File(targetDir, name)
                    if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath + File.separator)) {
                        throw IOException("Blocked zip entry outside target dir: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            outFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }

            if (!isDownloaded(modelName)) {
                throw IOException("Model archive did not contain the expected files")
            }
            Log.i(TAG, "Vosk model $modelName installed (${installedSizeBytes(modelName)} bytes)")
        } finally {
            tmpFile.delete()
        }
    }

    /** Deletes the installed model (frees storage). */
    fun delete(modelName: String) {
        modelDir(modelName).deleteRecursively()
    }

    private fun modelDir(modelName: String): File = File(modelsDir, modelName)

    /**
     * Returns the common top-level directory prefix of all zip entries
     * (e.g. "vosk-model-small-de-0.15/"), or "" if the zip has no single
     * root folder. Only strips the prefix when EVERY entry lives inside it.
     */
    private fun commonRootPrefix(zip: ZipFile): String {
        val names = zip.entries().asSequence().map { it.name }.toList()
        if (names.isEmpty()) return ""
        val firstSlash = names[0].indexOf('/')
        if (firstSlash == -1) return ""
        val root = names[0].substring(0, firstSlash + 1)
        return if (names.all { it.startsWith(root) }) root else ""
    }

    private fun dirSize(dir: File): Long =
        if (!dir.exists()) 0L else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private companion object {
        private const val TAG = "VoskModelStore"
    }
}
