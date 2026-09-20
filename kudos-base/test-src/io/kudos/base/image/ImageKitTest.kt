package io.kudos.base.image

import io.kudos.base.io.PathKit
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for ImageKit
 *
 * @author K
 * @since 1.0.0
 */
internal class ImageKitTest {

    @Test
    fun imageToString_roundTripsImageWithoutOpeningWindow() {
//        val url = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png"
//        val image = ImageKit.readImageFromUri(url)

        val image = File("${PathKit.getProjectRootPath()}/resources/logo.png")
        val imageStr = ImageKit.imageToString(image, "png")
        val originalImage = ImageIO.read(image)
        val roundTrippedImage = ImageKit.stringToImage(imageStr)

        assertTrue(imageStr.isNotBlank())
        assertEquals(originalImage.width, roundTrippedImage.width)
        assertEquals(originalImage.height, roundTrippedImage.height)
    }

}
