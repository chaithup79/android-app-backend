package com.curiq.api.service

import org.junit.jupiter.api.Test
import java.io.File

class ImageDownloaderTest {

    @Test
    fun testWebpImage() {
        val sourceUrl = "https://www.gstatic.com/webp/gallery/1.webp"
        val service = ImageDownloaderService("./test_images", "http://localhost/images")
        
        println("Testing WebP Image: $sourceUrl")
        val result = service.downloadAndCompressImage(sourceUrl, "test_webp")
        println("Result: $result")
    }
}
