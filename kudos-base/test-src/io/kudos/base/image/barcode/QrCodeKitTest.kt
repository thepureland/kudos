package io.kudos.base.image.barcode

import io.kudos.base.io.PathKit
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
        val bufferedImage = QrCodeKit.genQrCode("http://www.baidu.com", logoImagePath)
//        ImageKit.showImage(bufferedImage)
//        Thread.sleep(3000)
    }

}