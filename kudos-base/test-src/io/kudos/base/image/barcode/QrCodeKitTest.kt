package io.kudos.base.image.barcode

import io.kudos.base.io.PathKit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for QrCodeKit
 *
 * @author K
 * @since 1.0.0
 */
internal class QrCodeKitTest {

    @Test
    fun genQrCode_returnsPopulatedImageWithoutOpeningWindow() {
        val logoImagePath = "${PathKit.getProjectRootPath()}/resources/logo.png"
        val bufferedImage = QrCodeKit.genQrCode("https://www.baidu.com", logoImagePath)

        assertEquals(300, bufferedImage.width)
        assertEquals(300, bufferedImage.height)
        val pixels = bufferedImage.getRGB(
            0, 0, bufferedImage.width, bufferedImage.height, null, 0, bufferedImage.width
        )
        assertTrue(pixels.distinct().size > 1)
    }

}
