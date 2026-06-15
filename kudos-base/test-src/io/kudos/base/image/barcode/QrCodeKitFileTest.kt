package io.kudos.base.image.barcode

import io.kudos.base.io.PathKit
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/**
 * Tests for the file-writing overload of [QrCodeKit.genQrCode] (content, destImagePath, logoImagePath, ...),
 * which the existing QrCodeKitTest does not exercise.
 *
 * The generated PNG is written to a temp file and read back to verify it is a valid square image of the
 * requested side length.
 *
 * @author K
 * @since 1.0.0
 */
internal class QrCodeKitFileTest {

    private val logoImagePath = "${PathKit.getProjectRootPath()}/resources/logo.png"

    @Test
    fun genQrCode_writesPngFileOfRequestedSize() {
        val dest = File.createTempFile("kudos-qr-", ".png")
        dest.deleteOnExit()
        try {
            QrCodeKit.genQrCode(
                content = "https://kudos.io",
                destImagePath = dest.absolutePath,
                logoImagePath = logoImagePath,
                sideLength = 200
            )
            assertTrue(dest.exists(), "QR code file should be created")
            assertTrue(dest.length() > 0, "QR code file should not be empty")
            val img = ImageIO.read(dest)
            assertNotNull(img, "written file should be a readable image")
            assertEquals(200, img.width)
            assertEquals(200, img.height)
        } finally {
            dest.delete()
        }
    }

    @Test
    fun genQrCode_withExplicitLogoSizeAndMargin() {
        val dest = File.createTempFile("kudos-qr2-", ".png")
        dest.deleteOnExit()
        try {
            QrCodeKit.genQrCode(
                content = "abc",
                destImagePath = dest.absolutePath,
                logoImagePath = logoImagePath,
                sideLength = 300,
                logoSideLength = 60,
                margin = 1
            )
            val img = ImageIO.read(dest)
            assertEquals(300, img.width)
            assertEquals(300, img.height)
        } finally {
            dest.delete()
        }
    }
}
