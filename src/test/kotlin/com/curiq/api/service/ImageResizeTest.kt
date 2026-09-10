package com.curiq.api.service

import org.junit.jupiter.api.Test
import java.io.File
import javax.imageio.ImageIO
import java.awt.Image
import java.awt.image.BufferedImage
import java.net.URL

class ImageResizeTest {

    @Test
    fun testWebpResize() {
        // We need twelvemonkeys registered, since it's in build.gradle.kts it should be available when running test task
        val url = URL("https://www.gstatic.com/webp/gallery/1.webp")
        val originalImage = ImageIO.read(url)
        
        println("Original Size: ${originalImage.width}x${originalImage.height}")
        
        val newWidth = 400
        val newHeight = 300
        
        // Method 1: getScaledInstance
        val scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
        val method1Img = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val g1 = method1Img.createGraphics()
        g1.color = java.awt.Color.WHITE
        g1.fillRect(0, 0, newWidth, newHeight)
        g1.drawImage(scaledImage, 0, 0, null)
        g1.dispose()
        ImageIO.write(method1Img, "jpg", File("./test_method1.jpg"))
        
        // Method 2: Graphics2D
        val method2Img = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val g2 = method2Img.createGraphics()
        g2.color = java.awt.Color.WHITE
        g2.fillRect(0, 0, newWidth, newHeight)
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.drawImage(originalImage, 0, 0, newWidth, newHeight, null)
        g2.dispose()
        ImageIO.write(method2Img, "jpg", File("./test_method2.jpg"))
        
        println("Saved test_method1.jpg and test_method2.jpg")
    }
}
