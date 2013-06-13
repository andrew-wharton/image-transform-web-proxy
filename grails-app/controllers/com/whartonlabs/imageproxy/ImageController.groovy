package com.whartonlabs.imageproxy

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class ImageController {

    ImageService imageService

    static final List TRIM_PARAMS = [
        "trim_top", "trim_right", "trim_bottom", "trim_left"
    ]

    static final List CROP_PARAMS = [
        "crop_ratio", "crop_x_offset", "crop_y_offset", "crop_shrink"
    ]

    static final List SCALE_PARAMS = [
        "width", "height"
    ]

    def index() {

        byte[] imageBytes = imageService.getOriginalImageData(params.pathToFile)

        InputStream stream = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(stream);

        if(params.any{ it.key in TRIM_PARAMS }) {
            image = imageService.trim(image, params)
        }

        if(params.any{ it.key in CROP_PARAMS }) {
            image = imageService.crop(image, params)
        }

        if(params.any{ it.key in SCALE_PARAMS }) {
            image = imageService.scale(image, params)
        }

        byte[] outputBytes = image ? imageService.writeToByteArray(image, 0.95f) : imageBytes
        response.contentType = "image/jpeg"
        response.contentLength = outputBytes.length
        response.outputStream << outputBytes
        response.outputStream.flush()
    }
}
