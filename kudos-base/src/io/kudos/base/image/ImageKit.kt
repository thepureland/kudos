package io.kudos.base.image

import io.kudos.base.io.PathKit
import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.bridge.BridgeContext
import org.apache.batik.bridge.GVTBuilder
import org.apache.batik.bridge.UserAgentAdapter
import org.apache.batik.gvt.renderer.ConcreteImageRendererFactory
import org.w3c.dom.svg.SVGDocument
import org.w3c.dom.svg.SVGElement
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.*
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import javax.swing.JFrame
import javax.swing.JPanel

/**
 * Image processing utility.
 *
 * @author K
 * @since 1.0.0
 */
object ImageKit {

    /**
     * Reads an image from a file.
     *
     * @param imageFile the image file
     * @return a BufferedImage
     * @author K
     * @since 1.0.0
     */
    fun readImageFromFile(imageFile: File): BufferedImage = readImageFromFile(imageFile.path)

    /**
     * Reads an image from a file.
     *
     * @param imagePath the image file path
     * @return a BufferedImage
     * @author K
     * @since 1.0.0
     */
    fun readImageFromFile(imagePath: String): BufferedImage = ImageIO.read(Files.newInputStream(Paths.get(imagePath)))

    /**
     * Reads an image from a URI.
     *
     * @param imageUri the image URI
     * @return a BufferedImage
     * @author K
     * @since 1.0.0
     */
    fun readImageFromUri(imageUri: URI): BufferedImage = ImageIO.read(imageUri.toURL().openStream())

    /**
     * Reads an image from a URI.
     *
     * @param imageUriStr the image network address
     * @return a BufferedImage
     * @author K
     * @since 1.0.0
     */
    fun readImageFromUri(imageUriStr: String): BufferedImage = readImageFromUri(URI.create(imageUriStr))

    /**
     * Writes an image to a file.
     *
     * @param bufferedImage the BufferedImage
     * @param imageFormat the image format (e.g. png, jpg, gif)
     * @param imagePath the target image file path
     * @author K
     * @since 1.0.0
     */
    fun writeImage(bufferedImage: BufferedImage, imageFormat: String, imagePath: String) =
        ImageIO.write(bufferedImage, imageFormat, Files.newOutputStream(Paths.get(imagePath)))

    /**
     * Converts an image to a String representation.
     *
     * @param imageFile the image file
     * @param imageFormat the image format (e.g. png, jpg, gif)
     * @return the string representation of the image
     * @author K
     * @since 1.0.0
     */
    fun imageToString(imageFile: File, imageFormat: String): String {
        val bufferedImage = readImageFromFile(imageFile)
        return imageToString(bufferedImage, imageFormat)
    }

    /**
     * Converts an image to a String representation.
     *
     * @param imageUri the image URI
     * @param imageFormat the image format (e.g. png, jpg, gif)
     * @return the string representation of the image
     * @author K
     * @since 1.0.0
     */
    fun imageToString(imageUri: URI, imageFormat: String): String {
        val bufferedImage = readImageFromUri(imageUri)
        return imageToString(bufferedImage, imageFormat)
    }

    /**
     * Converts an image to a String representation.
     *
     * @param bufferedImage the BufferedImage
     * @param imageFormat the image format (e.g. png, jpg, gif)
     * @return the Base64-encoded string representation of the image
     * @author K
     * @since 1.0.0
     */
    fun imageToString(bufferedImage: BufferedImage, imageFormat: String): String {
        return ByteArrayOutputStream().use {
            ImageIO.write(bufferedImage, imageFormat, it)
            val byteArray = it.toByteArray()
            String(Base64.getEncoder().encode(byteArray))
        }
    }

    /**
     * Converts a String representation to an image.
     *
     * @param imgStr the Base64-encoded string representation of the image
     * @return a BufferedImage
     * @author K
     * @since 1.0.0
     */
    fun stringToImage(imgStr: String): BufferedImage {
        val decoder = Base64.getDecoder()
        val imageByte = decoder.decode(imgStr)
        return ByteArrayInputStream(imageByte).use {
            ImageIO.read(it)
        }
    }


    /**
     * Scales the given source image to the requested height and width, producing an icon-sized image.
     *
     * @param srcImageFile path to the source file
     * @param height       target height
     * @param width        target width
     * @param hasFiller    whether to pad with whitespace when the aspect ratio does not match: true to pad (default); false otherwise
     * @author K
     * @since 1.0.0
     */
    fun scale(srcImageFile: String, height: Int, width: Int, hasFiller: Boolean = true): BufferedImage {
        val ratio: Double // scale ratio
        val file = File(srcImageFile)
        val srcImage: BufferedImage = ImageIO.read(file)
        var destImage = srcImage.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH)
        // compute the ratio
        if (srcImage.height > height || srcImage.width > width) {
            ratio = if (srcImage.height > srcImage.width) {
                height.toDouble() / srcImage.height
            } else {
                width.toDouble() / srcImage.width
            }
            val op = AffineTransformOp(AffineTransform.getScaleInstance(ratio, ratio), null)
            destImage = op.filter(srcImage, null)
        }
        if (hasFiller) { // pad
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val graphic = image.createGraphics()
            graphic.color = Color.white
            graphic.fillRect(0, 0, width, height)
            if (width == destImage.getWidth(null)) graphic.drawImage(
                destImage, 0, (height - destImage.getHeight(null)) / 2, destImage.getWidth(null),
                destImage.getHeight(null), Color.white, null
            ) else graphic.drawImage(
                destImage, (width - destImage.getWidth(null)) / 2, 0, destImage.getWidth(null),
                destImage.getHeight(null), Color.white, null
            )
            graphic.dispose()
            destImage = image
        }
        return destImage as BufferedImage
    }

    /**
     * Reads an image from an in-memory byte array.
     *
     * @param imgBytes undecoded image data
     * @return a [BufferedImage]
     * @throws IOException when a read/write error occurs or the format is unrecognized
     * @author https://blog.csdn.net/johnwaychan/article/details/79106983
     * @author K
     * @since 1.0.0
     */
    fun readMemoryImage(imgBytes: ByteArray, format: String? = null): BufferedImage {
        // Convert the byte array to an InputStream, and then to a MemoryCacheImageInputStream
        val imageInputStream = MemoryCacheImageInputStream(ByteArrayInputStream(imgBytes))
        // Get all ImageReader objects that recognize this stream format
        val it = if (format == null) {
            val imageReaders = ImageIO.getImageReaders(imageInputStream)
//            if (!imageReaders.hasNext()) {
//                imageReaders = ImageIO.getImageReaders(FileImageInputStream(File(srcFilePath)))
//            }
            imageReaders
        } else {
            ImageIO.getImageReadersByFormatName(format)
        }
        // Iterate and try decoding with each ImageReader
        while (it.hasNext()) {
            val imageReader = it.next()
            // Set the decoder's input stream
            imageReader.setInput(imageInputStream, true, true)
            // Image format suffix
            val suffix: String = imageReader.formatName.trim().lowercase()
            // Image width
            val width: Int = imageReader.getWidth(0)
            // Image height
            val height: Int = imageReader.getHeight(0)
            System.out.printf("format %s,%dx%d\n", suffix, width, height)
            try {
                // On success, return a BufferedImage
                // 0 means decode the first image (gif format may have multiple); the 0 used when getting width/height has the same meaning
                return imageReader.read(0, imageReader.defaultReadParam)
            } catch (_: IOException) {
                imageReader.dispose()
                // If decoding fails, try the next ImageReader
            } catch (_: RuntimeException) {
                imageReader.dispose()
                // If decoding fails, try the next ImageReader
            }
        }
        imageInputStream.close()
        throw IOException("unsupported image format")
    }

    /**
     * Displays the image graphically.
     *
     * @param bufferedImage the BufferedImage
     * @author K
     * @since 1.0.0
     */
    fun showImage(bufferedImage: BufferedImage) {
        class ImagePanel(var image: BufferedImage) : JPanel() {

            override fun paintComponent(g: Graphics) {
                val x: Int = (width - image.width) / 2
                val y: Int = (height - image.height) / 2
                g.drawImage(image, x, y, this)
            }

            override fun getPreferredSize(): Dimension {
                return Dimension(image.width, image.height)
            }
        }

        val panel = ImagePanel(bufferedImage)
        val f = JFrame()
        f.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        f.add(panel)
        f.setLocation(200, 200)
        f.isVisible = true
        f.pack()
    }

    /**
     * Renders an SVG xml string to an image.
     *
     * @param xmlContent the SVG xml
     * @param width the image width
     * @param height the image height
     * @return a BufferedImage
     * @author https://blog.csdn.net/do168/article/details/51564492
     * @author K
     * @since 1.0.0
     */
    fun renderSvgToImage(xmlContent: String, width: Int, height: Int): BufferedImage {
        return renderSvgToImage(xmlContent, width, height, false, null, null)
    }

    /**
     * Internal implementation of SVG rendering, supporting fill-color replacement by id regex prior to rendering.
     * Specifying a temporary URI is required for Batik to resolve SVG fragment references (e.g. `#linearGradient1`);
     * otherwise references with a URI fragment fail to resolve.
     *
     * @param xmlContent the SVG text
     * @param width the target width
     * @param height the target height
     * @param stretch true to scale independently on X/Y to fill, false to preserve aspect ratio
     * @param idRegex regex of element ids whose fill color should be replaced; may be null
     * @param replacementColor the new fill color; may be null
     * @return the rendered bitmap
     * @author K
     * @since 1.0.0
     */
    private fun renderSvgToImage(
        xmlContent: String,
        width: Int,
        height: Int,
        stretch: Boolean,
        idRegex: String?,
        replacementColor: Color?,
    ): BufferedImage {
        // the following is necessary so that batik knows how to resolve URI fragments
        // (#myLinearGradient). Otherwise the resolution fails and you cannot render.
        val uri = "${PathKit.getTempDirectoryPath()}/temp.svg"
        val df = SAXSVGDocumentFactory("org.apache.xerces.parsers.SAXParser")
        val document = df.createSVGDocument(uri, StringReader(xmlContent))
        if (idRegex != null && replacementColor != null) {
            replaceFill(document, idRegex, replacementColor)
        }
        return renderToImage(document, width, height, stretch)
    }

    /**
     * Uses Batik to render the parsed [SVGDocument] to a [BufferedImage].
     * Computes the X/Y scale and, according to [stretch], chooses whether to preserve aspect ratio; when not stretching, output is centered.
     *
     * @param document the parsed SVG document
     * @param width the target bitmap width
     * @param height the target bitmap height
     * @param stretch true to scale independently on X/Y to fill, false to preserve aspect ratio and center
     * @return the rendered bitmap
     * @author K
     * @since 1.0.0
     */
    private fun renderToImage(document: SVGDocument?, width: Int, height: Int, stretch: Boolean): BufferedImage {
        val rendererFactory = ConcreteImageRendererFactory()
        val renderer = rendererFactory.createStaticImageRenderer()
        val builder = GVTBuilder()
        val ctx = BridgeContext(UserAgentAdapter())
        ctx.setDynamicState(BridgeContext.STATIC)
        val rootNode = builder.build(ctx, document)
        renderer.tree = rootNode
        val docWidth = ctx.documentSize.width.toFloat()
        val docHeight = ctx.documentSize.height.toFloat()
        var xScale = width / docWidth
        var yScale = height / docHeight
        if (!stretch) {
            val scale = xScale.coerceAtMost(yScale)
            xScale = scale
            yScale = scale
        }
        val px = AffineTransform.getScaleInstance(xScale.toDouble(), yScale.toDouble())
        val tx = (-0 + (width / xScale - docWidth) / 2).toDouble()
        val ty = (-0 + (height / yScale - docHeight) / 2).toDouble()
        px.translate(tx, ty)
        //cgn.setViewingTransform(px);
        renderer.updateOffScreen(width, height)
        renderer.tree = rootNode
        renderer.transform = px
        //renderer.clearOffScreen();
        renderer.repaint(Rectangle(0, 0, width, height))
        return renderer.offScreen
    }

    /**
     * Iterates over all SVG elements in the document and, for any element whose id matches [idRegex],
     * replaces the `fill:#XXXXXX` value in its `style` attribute with the given color.
     * Only inline style attributes are modified; CSS classes and external stylesheets are not handled.
     *
     * @param document the SVG document
     * @param idRegex regex matching the target element ids
     * @param color the new fill color
     * @author K
     * @since 1.0.0
     */
    private fun replaceFill(document: SVGDocument, idRegex: String, color: Color) {
        val colorCode = String.format("#%02x%02x%02x", color.red, color.green, color.blue)
        val children = document.getElementsByTagName("*")
        for (i in 0 until children.length) {
            if (children.item(i) is SVGElement) {
                val element = children.item(i) as SVGElement
                if (element.id.matches(Regex(idRegex))) {
                    var style = element.getAttributeNS(null, "style")
                    style = style.replaceFirst("fill:#[a-zA-z0-9]+".toRegex(), "fill:$colorCode")
                    element.setAttributeNS(null, "style", style)
                }
            }
        }
    }


}
