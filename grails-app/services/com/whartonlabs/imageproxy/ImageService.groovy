package com.whartonlabs.imageproxy

import grails.plugin.cache.Cacheable
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.imgscalr.Scalr

import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream
import java.awt.image.BufferedImage

class ImageService {

    private String originalBase = "http://cdn.deciduouspress.com.au.s3-website-ap-southeast-1.amazonaws.com"

    GrailsApplication grailsApplication

    /**
     * Trims the edges from the corresponding edge by the given number of pixels
     *
     * @param params.top Number of pixels to trim from the top edge
     * @param params.bottom Number of pixels to trim from the bottom edge
     * @param params.left Number of pixels to trim from the left edge
     * @param params.right Number of pixels to trim from the right edge
     * @return the cropped JPEG image bytes
     */
    BufferedImage trim(BufferedImage originalImage, Map params) {
        Map cropWindow = calculateCropWindowForTrimParams(originalImage, params)
        return Scalr.crop(originalImage, cropWindow.x, cropWindow.y, cropWindow.width, cropWindow.height)
    }

    private Map calculateCropWindowForTrimParams(BufferedImage image, Map params) {
        Map cropWindow = [
            x: params.trim_left ? Integer.parseInt(params.trim_left) : 0,
            y: params.trim_top ? Integer.parseInt(params.trim_top) : 0,
        ]
        cropWindow.width = (image.width - cropWindow.x - (params.trim_right ? Integer.parseInt(params.trim_right) : 0))
        cropWindow.height = (image.height - cropWindow.y - (params.trim_bottom ? Integer.parseInt(params.trim_bottom) : 0))

        return cropWindow
    }

    /**
     * Crops the images based on the desired aspect ratio, x and y offset and the scaling down of the crop window
     *
     * By default, the crop window will be centered vertically and horizontally,
     * scaled to the maximum size that the specified aspect ration allows, given the aspect ratio
     * of the input image.
     *
     * @param params.crop_ratio The desired aspect ratio of the output image eg. 4:3 -> 1.33, 16:9 -> 1.78, square -> 1
     * @param params.crop_x_offset the
     * @param params.crop_y_offset Number of pixels to trim from the left edge
     * @param params.crop_shrink Number of pixels to trim from the right edge
     * @return the cropped BufferedImage
     */
    BufferedImage crop(BufferedImage originalImage, Map params) {
        if(!params.crop_ratio) {
            return originalImage
        } else {
            Map cropWindow = calculateCropWindowForCropParams(originalImage, params)
            return Scalr.crop(originalImage, cropWindow.x as int, cropWindow.y as int, cropWindow.width as int, cropWindow.height as int)
        }
    }

    private static Map calculateCropWindowForCropParams(originalImage, params) {
        // parse input values and add defaults where not specified
        double desiredRatio = params.crop_ratio as double
        int xOffset = params.crop_x_offset ? params.crop_x_offset as int : 0
        int yOffset = params.crop_y_offset ? params.crop_y_offset as int : 0
        double shrink = params.crop_shrink ? params.crop_shrink as double : 1.0

        Map cropWindow = [
            x: 0,
            y: 0,
            width: originalImage.width,
            height: originalImage.height
        ]
        println cropWindow
        Map ratioCropWindow = calculateCropWindowForRatio(originalImage, cropWindow, desiredRatio)
        println ratioCropWindow
        Map shrinkCropWindow = (shrink < 1.0) ? calculateCropWindowForShrink(ratioCropWindow, shrink) : ratioCropWindow
        println shrinkCropWindow
        Map xOffsetCropWindow = xOffset ? calculateCropWindowForXOffset(originalImage, shrinkCropWindow, xOffset) : shrinkCropWindow
        println xOffsetCropWindow
        Map yOffsetCropWindow = yOffset ? calculateCropWindowForYOffset(originalImage, xOffsetCropWindow, yOffset) : xOffsetCropWindow
        println yOffsetCropWindow
        return yOffsetCropWindow
    }

    private static Map calculateCropWindowForRatio(BufferedImage originalImage, Map existingCropWindow, double desiredRatio) {

        Map newCropWindow = existingCropWindow.clone()

        double originalImageRatio = (originalImage.width / originalImage.height) as double

        if(originalImageRatio > desiredRatio) {
            newCropWindow.width = (desiredRatio / originalImageRatio) * originalImage.width
            int numberOfXPixelsToRemove = originalImage.width - newCropWindow.width
            newCropWindow.x = numberOfXPixelsToRemove / 2 // horizontally center the crop
        } else {
            newCropWindow.height = (originalImageRatio / desiredRatio) * originalImage.height
            int numberOfYPixelsToRemove = originalImage.height - newCropWindow.height
            newCropWindow.y = numberOfYPixelsToRemove / 2 // horizontally center the crop
        }

        return newCropWindow
    }

    private static Map calculateCropWindowForShrink(final Map cropWindow, final double shrinkRatio) {

        int shrunkWidth = (cropWindow.width * shrinkRatio) as int
        int shrunkHeight = (cropWindow.height * shrinkRatio) as int
        int xOffsetDelta = ((cropWindow.width - shrunkWidth) / 2) as int
        int yOffsetDelta = ((cropWindow.height - shrunkHeight) / 2) as int

        return [
            x: cropWindow.x + xOffsetDelta,
            y: cropWindow.y + yOffsetDelta,
            width: shrunkWidth,
            height: shrunkHeight
        ]
    }

    private static Map calculateCropWindowForXOffset(BufferedImage originalImage, Map cropWindow, int xOffset) {

        Map newCropWindow = cropWindow.clone()

        if(cropWindow.x + xOffset < 0) {
            newCropWindow.x = 0
        } else if(cropWindow.x + xOffset + cropWindow.width > originalImage.width) {
            newCropWindow.x = originalImage.width - cropWindow.width
        } else {
            newCropWindow.x = cropWindow.x + xOffset
        }
        println newCropWindow.width
        return newCropWindow
    }

    private static Map calculateCropWindowForYOffset(BufferedImage originalImage, Map cropWindow, int yOffset) {

        Map newCropWindow = cropWindow.clone()

        if(cropWindow.y + yOffset < 0) {
            newCropWindow.y = 0
        } else if(cropWindow.y + yOffset + cropWindow.height > originalImage.height) {
            newCropWindow.y = originalImage.height - cropWindow.height
        } else {
            newCropWindow.y = cropWindow.y + yOffset
        }

        return newCropWindow

    }

    /**
     * Scales the image using the given width and height, scales proportionally if only one is given
     *
     * @param image
     * @param params
     * @return
     */
    BufferedImage scale(BufferedImage image, Map params) {

        if (params.width && params.height){
            return Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, params.width as int, params.height as int)
        } else if(params.width || params.height) {
            return Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, params.width ? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT, params.width ? params.width as int: params.height as int)
        } else {
            return image
        }

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
