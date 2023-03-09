package io.kudos.base.image

import org.soul.base.image.ImageTool
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URI


/**
 * 图片处理工具类
 *
 * @author K
 * @since 1.0.0
 */
object ImageKit {

    /**
     * 从文件读取图片
     *
     * @param imageFile 图片文件
     * @return BufferedImage对象
     * @author K
     * @since 1.0.0
     */
    fun readImageFromFile(imageFile: File): BufferedImage {
        return ImageTool.readImageFromFile(imageFile)
    }

    /**
     * 从文件读取图片
     *
     * @param imagePath 图片路径
     * @return BufferedImage对象
     * @author K
     * @since 1.0.0
     */
    fun readImageFromFile(imagePath: String): BufferedImage {
        return ImageTool.readImageFromFile(imagePath)
    }

    /**
     * 从URI读取文件
     *
     * @param imageUri 图片uri
     * @return BufferedImage对象
     * @author K
     * @since 1.0.0
     */
    fun readImageFromUri(imageUri: URI): BufferedImage {
        return ImageTool.readImageFromUri(imageUri)
    }

    /**
     * 从URI读取文件
     *
     * @param imageUriStr 图片网络地址
     * @return BufferedImage对象
     * @author K
     * @since 1.0.0
     */
    fun readImageFromUri(imageUriStr: String): BufferedImage {
        return ImageTool.readImageFromUri(imageUriStr)
    }

    /**
     * 写图片到文件
     *
     * @param bufferedImage BufferedImage对象
     * @param imageFormat 图片类型(如png,jpg,gif等)
     * @param imagePath 图片目标文件路径
     * @author K
     * @since 1.0.0
     */
    fun writeImage(bufferedImage: BufferedImage, imageFormat: String, imagePath: String) {
        ImageTool.writeImage(bufferedImage, imageFormat, imagePath)
    }

    /**
     * 将图片转换为字符串表示
     *
     * @param imageFile 图片文件
     * @param imageFormat 图片类型(如png,jpg,gif等)
     * @return 图片的字符串表示
     * @author K
     * @since 1.0.0
     */
    fun imageToString(imageFile: File, imageFormat: String): String {
        return ImageTool.imageToString(imageFile, imageFormat)
    }

    /**
     * 将图片转换为字符串表示
     *
     * @param imageUri 图片uri
     * @param imageFormat 图片类型(如png,jpg,gif等)
     * @return 图片的字符串表示
     * @author K
     * @since 1.0.0
     */
    fun imageToString(imageUri: URI, imageFormat: String): String {
        return ImageTool.imageToString(imageUri, imageFormat)
    }

    /**
     * 将图片转换为字符串表示
     *
     * @param bufferedImage BufferedImage对象
     * @param imageFormat 图片类型(如png,jpg,gif等)
     * @return 图片的字符串表示(Base64编码)
     * @author K
     * @since 1.0.0
     */
    fun imageToString(bufferedImage: BufferedImage, imageFormat: String): String {
        return ImageTool.imageToString(bufferedImage, imageFormat)
    }

    /**
     * 将字符串表示转换为图片对象
     *
     * @param imgStr 图片的字符串表示(Base64编码)
     * @return BufferedImage对象
     * @author K
     * @since 1.0.0
     */
    fun stringToImage(imgStr: String): BufferedImage {
        return ImageTool.stringToImage(imgStr)
    }


    /**
     * 把传入的原始图像按高度和宽度进行缩放，生成符合要求的图标
     *
     * @param srcImageFile 源文件地址
     * @param height       目标高度
     * @param width        目标宽度
     * @param hasFiller    比例不对时是否需要补白：true为补白（缺省值）; false为不补白;
     * @author K
     * @since 1.0.0
     */
    fun scale(srcImageFile: String, height: Int, width: Int, hasFiller: Boolean = true): BufferedImage {
        return ImageTool.scale(srcImageFile, height, width, hasFiller)
    }

    /**
     * 从内存字节数组中读取图像
     *
     * @param imgBytes 未解码的图像数据
     * @return 返回 [BufferedImage]
     * @throws IOException 当读写错误或不识别的格式时抛出
     * @author https://blog.csdn.net/johnwaychan/article/details/79106983
     * @author K
     * @since 1.0.0
     */
    fun readMemoryImage(imgBytes: ByteArray, format: String? = null): BufferedImage {
        return ImageTool.readMemoryImage(imgBytes, format)
    }

    /**
     * 图形化展现图片对象
     *
     * @param bufferedImage BufferedImage对象
     * @author K
     * @since 1.0.0
     */
    fun showImage(bufferedImage: BufferedImage) {
        ImageTool.showImage(bufferedImage)
    }

    /**
     * 将svg格式的xml渲染成图片
     *
     * @param xmlContent svg格式的xml
     * @param width 图片宽度
     * @param height 图片高度
     * @return BufferedImage
     * @author https://blog.csdn.net/do168/article/details/51564492
     * @author K
     * @since 1.0.0
     */
    fun renderSvgToImage(xmlContent: String, width: Int, height: Int): BufferedImage {
        return ImageTool.renderSvgToImage(xmlContent, width, height)
    }

}