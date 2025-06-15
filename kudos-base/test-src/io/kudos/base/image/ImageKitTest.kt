package io.kudos.base.image

import io.kudos.base.io.PathKit
import org.junit.jupiter.api.Disabled
import java.io.File
import kotlin.test.Test

/**
 * test for ImageKit
 *
 * @author K
 * @since 1.0.0
 */
internal class ImageKitTest {

    @Test
    @Disabled("无GUI的环境下跑不了")
    fun imageToString() {
//        val url = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png"
//        val image = ImageKit.readImageFromUri(url)

        val image = File("${PathKit.getProjectRootPath()}/resources/logo.png")
        val imageStr = ImageKit.imageToString(image, "png")
        println(imageStr)
        ImageKit.showImage(ImageKit.stringToImage(imageStr))
        Thread.sleep(3000)
    }

}