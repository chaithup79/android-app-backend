package com.curiq.api.service

import org.junit.jupiter.api.Test
import java.io.File
import javax.imageio.ImageIO
import java.net.URL
import java.awt.image.BufferedImage

class ImageWriteTest {

    @Test
    fun testWebpWrite() {
        val url = URL("https://www.gstatic.com/webp/gallery/1.webp")
        val originalImage = ImageIO.read(url)
        
        println("Original Type: ${originalImage.type}")
        
        val success = ImageIO.write(originalImage, "jpg", File("./test_direct_write.jpg"))
        println("Direct write success? $success")
    }
}
