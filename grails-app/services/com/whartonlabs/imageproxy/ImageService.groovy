package com.whartonlabs.imageproxy

import org.apache.commons.io.IOUtils
import org.codehaus.groovy.grails.commons.GrailsApplication

import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream
import java.awt.image.BufferedImage

class ImageService {

    private String originalBase = "http://cdn.deciduouspress.com.au.s3-website-ap-southeast-1.amazonaws.com/image"

    GrailsApplication grailsApplication

    /**
     * Scales the image using the given width and height, scales proportionally if only one is given
     *
     * @param imageBytes
     * @param params
     * @return
     */
    byte[] scale(byte[] imageBytes, Map params) {

        InputStream stream = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(stream);
        BufferedImage processedImage

        if (params.width && params.height){
            processedImage = Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, params.width as int, params.height as int)
        } else if(params.width || params.height) {
            processedImage = Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, params.width ? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT, params.width ? params.width as int: params.height as int)
        } else {
            processedImage = imageBytes
        }

        return writeToByteArray(processedImage, 0.95f)

    }

    /**
     * Trims the edges from the corresponding edge by the given number of pixels
     *
     * @param params.top Number of pixels to trim from the top edge
     * @param params.bottom Number of pixels to trim from the bottom edge
     * @param params.left Number of pixels to trim from the left edge
     * @param params.right Number of pixels to trim from the right edge
     * @return the cropped JPEG image bytes
     */
    byte[] crop(byte[] imageBytes, Map params) {

        InputStream stream = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(stream);

        Map cropWindow = [
            x: params.left ? Integer.parseInt(params.left) : 0,
            y: params.top ? Integer.parseInt(params.top) : 0,
        ]
        cropWindow.width = (originalImage.width - cropWindow.x - (params.right ? Integer.parseInt(params.right) : 0))
        cropWindow.height = (originalImage.height - cropWindow.y - (params.bottom ? Integer.parseInt(params.bottom) : 0))

        BufferedImage processedImage = Scalr.crop(originalImage, cropWindow.x, cropWindow.y, cropWindow.width, cropWindow.height)

        return  writeToByteArray(processedImage, 0.95f)
    }

    /**
     * Fetches the images original bytes using java.net.URL
     *
     * @param path
     * @return
     */
    @Cacheable('originalImages')
    byte[] getOriginalImageData(String path) {
        URL originalURL = new URL("${originalBase}/${path}")
        InputStream inputStream = originalURL.newInputStream()
        byte[] originalImageBytes
        try {
            originalImageBytes = IOUtils.toByteArray(inputStream)
        } catch(java.io.IOException ex) {
            println(ex.message)
            originalImageBytes = new byte[1]
        } finally {
            IOUtils.closeQuietly(inputStream)
        }
        return originalImageBytes
    }

    /**
     * Encapsulates the more complex method of encoding an image to a JPEG encoded byte array.
     *
     * @param bufferedImage
     * @param quality
     * @return
     */
    byte[] writeToByteArray(BufferedImage bufferedImage, float quality = 0.8f) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        writer.setOutput(ios);
        writer.write(null, new IIOImage(bufferedImage, null, null), param);

        byte[] data = baos.toByteArray();

        writer.dispose();

        return data;
    }
}
