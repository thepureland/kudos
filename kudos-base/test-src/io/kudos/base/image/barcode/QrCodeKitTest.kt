package io.kudos.base.image.barcode

import io.kudos.base.image.ImageKit
import io.kudos.base.io.PathKit
import org.junit.jupiter.api.Test

internal class QrCodeKitTest {

    @Test
    fun genQrCode() {
        val logoImagePath = "${PathKit.getProjectRootPath()}/resources/logo.png"
        val bufferedImage = QrCodeKit.genQrCode("http://www.baidu.com", logoImagePath)
        ImageKit.showImage(bufferedImage)
        Thread.sleep(3000)
    }

}