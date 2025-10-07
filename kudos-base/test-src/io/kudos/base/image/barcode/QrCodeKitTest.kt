package io.kudos.base.image.barcode

import io.kudos.base.image.ImageKit
import io.kudos.base.io.PathKit
import io.kudos.base.lang.SystemKit
import kotlin.test.Test

/**
 * test for QrCodeKit
 *
 * @author K
 * @since 1.0.0
 */
internal class QrCodeKitTest {

    @Test
    fun genQrCode() {
        val logoImagePath = "${PathKit.getProjectRootPath()}/resources/logo.png"
        val bufferedImage = QrCodeKit.genQrCode("https://www.baidu.com", logoImagePath)
        if (SystemKit.hasGUI()) {
            ImageKit.showImage(bufferedImage)
            Thread.sleep(3000)
        }
    }

}