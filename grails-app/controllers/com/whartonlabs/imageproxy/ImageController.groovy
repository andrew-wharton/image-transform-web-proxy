package com.whartonlabs.imageproxy

class ImageController {

    ImageService imageService

    static final List CROP_PARAMS = [
        "top", "right", "bottom", "left"
    ]

    static final List SCALE_PARAMS = [
        "width", "height"
    ]

    /**
     *
     *
     * Example URI: /image/suite/letterpress-invitation.jpg?top=10&bottom=20&left=30&right=50&width=1200
     */
    def index() {

        byte[] image = imageService.getOriginalImageData(params.pathToFile)

        if(params.any{ it.key in CROP_PARAMS }) {
            image = imageService.crop(image, params)
        }

        if(params.any{ it.key in SCALE_PARAMS }) {
            image = imageService.scale(image, params)
        }

        response.contentType = "image/jpeg"
        response.contentLength = image.length
        response.outputStream << image
        response.outputStream.flush()
    }
}
