package com.curiq.api.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import javax.imageio.ImageIO

@Service
class ImageDownloaderService(
    @Value("\${curiq.images.storage-path:/app/images}") private val storagePath: String,
    @Value("\${curiq.images.base-url:https://curiq.in/images}") private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(ImageDownloaderService::class.java)

    private val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    private val MAX_PIXELS = 16_000_000 // e.g. 4000x4000
    private val MAX_DIMENSION = 800

    init {
        val dir = File(storagePath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    fun downloadAndCompressImage(sourceUrl: String, filename: String): String? {
        try {
            val url = URI(sourceUrl).toURL()

            // 1. SSRF Protection
            if (!isSafeHost(url.host)) {
                logger.warn("SSRF Protection: Blocked download from internal host: ${url.host}")
                return null
            }

            // 2. Setup Connection with Strict Timeouts
            var connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            
            var status = connection.responseCode
            
            // Follow up to 1 redirect
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                val newUrl = connection.getHeaderField("Location")
                if (!newUrl.isNullOrBlank()) {
                    val redirectUrl = URI(newUrl).toURL()
                    if (!isSafeHost(redirectUrl.host)) return null
                    
                    connection = redirectUrl.openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 15000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    status = connection.responseCode
                }
            }

            if (status != HttpURLConnection.HTTP_OK) {
                logger.warn("Image download failed with status $status for URL: $sourceUrl")
                return null
            }

            // Check Content Length
            val contentLength = connection.contentLength
            if (contentLength > MAX_FILE_SIZE) {
                logger.warn("Image rejected due to size: ${contentLength / 1024 / 1024}MB (Max 10MB)")
                return null
            }

            // Check Content Type
            val contentType = connection.contentType?.lowercase() ?: ""
            if (!contentType.startsWith("image/")) {
                logger.warn("Image rejected due to invalid content type: $contentType")
                return null
            }

            val isPng = contentType.contains("png")

            // 3. Read image and check pixel count
            connection.inputStream.use { inputStream ->
                val originalImage = ImageIO.read(inputStream) ?: run {
                    logger.warn("Failed to decode image from URL: $sourceUrl")
                    return null
                }

                val totalPixels = originalImage.width.toLong() * originalImage.height.toLong()
                if (totalPixels > MAX_PIXELS) {
                    logger.warn("Image rejected due to massive dimensions: ${originalImage.width}x${originalImage.height}")
                    return null
                }

                // 4. Resize if necessary
                val resizedImage = if (originalImage.width > MAX_DIMENSION || originalImage.height > MAX_DIMENSION) {
                    val ratio = minOf(MAX_DIMENSION.toDouble() / originalImage.width, MAX_DIMENSION.toDouble() / originalImage.height)
                    val newWidth = (originalImage.width * ratio).toInt()
                    val newHeight = (originalImage.height * ratio).toInt()

                    val bufferedScaledImage = if (isPng) {
                        BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
                    } else {
                        BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
                    }
                    
                    val g2d = bufferedScaledImage.createGraphics()
                    if (!isPng) {
                        g2d.color = java.awt.Color.WHITE
                        g2d.fillRect(0, 0, newWidth, newHeight)
                    }
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                    g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null)
                    g2d.dispose()
                    bufferedScaledImage
                } else {
                    if (!isPng) {
                         // ALWAYS strip alpha channel for JPEGs to prevent crashes!
                         val newImg = BufferedImage(originalImage.width, originalImage.height, BufferedImage.TYPE_INT_RGB)
                         val g = newImg.createGraphics()
                         g.color = java.awt.Color.WHITE
                         g.fillRect(0, 0, originalImage.width, originalImage.height)
                         g.drawImage(originalImage, 0, 0, null)
                         g.dispose()
                         newImg
                    } else {
                        originalImage
                    }
                }

                // 5. Save the final image
                val extension = if (isPng) "png" else "jpg"
                val finalFilename = "$filename.$extension"
                val dir = File(storagePath)
                val outputFile = File(dir, finalFilename)
                
                val writerFormat = if (isPng) "png" else "jpg"
                val success = ImageIO.write(resizedImage, writerFormat, outputFile)

                if (success) {
                    logger.info("Successfully downloaded and saved image: $finalFilename")
                    return "$baseUrl/$finalFilename"
                } else {
                    logger.warn("ImageIO failed to write format: $writerFormat")
                    return null
                }
            }
        } catch (e: Exception) {
            logger.warn("Exception while downloading image $sourceUrl: ${e.message}")
            return null
        }
    }

    private fun isSafeHost(host: String): Boolean {
        try {
            if (host.equals("localhost", ignoreCase = true) || host.equals("127.0.0.1") || host.equals("0.0.0.0") || host.equals("::1")) {
                return false
            }
            val inetAddress = InetAddress.getByName(host)
            if (inetAddress.isAnyLocalAddress || 
                inetAddress.isLoopbackAddress || 
                inetAddress.isLinkLocalAddress || 
                inetAddress.isSiteLocalAddress) {
                return false
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }
}


