package com.princesanitary.grahak.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Lightweight, direct Cloudinary uploader using standard Android HttpURLConnection.
 * Does not require external SDKs or billing accounts.
 */
object CloudinaryUploader {

    private const val BASE_URL = "https://api.cloudinary.com/v1_1"

    suspend fun uploadImage(
        imageBytes: ByteArray,
        cloudName: String = CloudinaryConfig.CLOUD_NAME,
        uploadPreset: String = CloudinaryConfig.UPLOAD_PRESET,
        folder: String = CloudinaryConfig.PRODUCT_FOLDER,
        enableAiBackgroundRemoval: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        if (cloudName.isBlank() || cloudName == "YOUR_CLOUD_NAME" ||
            uploadPreset.isBlank() || uploadPreset == "YOUR_UPLOAD_PRESET"
        ) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Cloudinary is not configured. Please set your Cloud Name and Unsigned Upload Preset in CloudinaryConfig.kt"
                )
            )
        }

        var connection: HttpURLConnection? = null
        try {
            val endpoint = URL("$BASE_URL/$cloudName/image/upload")
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                connectTimeout = 30000
                readTimeout = 30000
            }

            val boundary = "Boundary-${UUID.randomUUID()}"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            DataOutputStream(connection.outputStream).use { output ->
                writeFormField(output, boundary, "upload_preset", uploadPreset)

                if (folder.isNotBlank()) {
                    writeFormField(output, boundary, "folder", folder)
                }

                // Write file field
                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"item_${System.currentTimeMillis()}.png\"\r\n")
                output.writeBytes("Content-Type: image/png\r\n\r\n")
                output.write(imageBytes)
                output.writeBytes("\r\n")

                // Closing boundary
                output.writeBytes("--$boundary--\r\n")
                output.flush()
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else (connection.errorStream ?: connection.inputStream)
            val responseBody = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            val json = JSONObject(responseBody)
            if (statusCode in 200..299) {
                val secureUrl = json.getString("secure_url")
                Result.success(secureUrl)
            } else {
                val errorMsg = json.optJSONObject("error")?.optString("message")
                    ?: "Cloudinary upload failed (HTTP $statusCode): $responseBody"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun uploadUri(
        context: android.content.Context,
        uri: android.net.Uri,
        cloudName: String = CloudinaryConfig.CLOUD_NAME,
        uploadPreset: String = CloudinaryConfig.UPLOAD_PRESET,
        folder: String = CloudinaryConfig.PRODUCT_FOLDER
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("Cannot read image from Uri"))
            uploadImage(bytes, cloudName, uploadPreset, folder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeFormField(output: DataOutputStream, boundary: String, name: String, value: String) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        output.write(value.toByteArray(Charsets.UTF_8))
        output.writeBytes("\r\n")
    }
}
