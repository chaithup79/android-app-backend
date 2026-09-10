package com.curiq.api.service

import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ImageArtifactTest {

    @Test
    fun generateArtifactImage() {
        val sourceUrl = "https://www.gstatic.com/webp/gallery/1.webp"
        // Configure the service to save directly to the artifact scratch folder!
        val service = ImageDownloaderService("C:/Users/chait/.gemini/antigravity-ide/brain/f9d37ff9-52e1-4523-aa5c-b38db6e15a3e/scratch", "http://localhost")
        
        println("Generating image for artifact...")
        val result = service.downloadAndCompressImage(sourceUrl, "fixed_image")
        println("Saved artifact to: $result")
    }
}
