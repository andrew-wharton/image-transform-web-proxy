# image-transform-web-proxy

## Introduction

The Image Transform Web Proxy is designed to provide a transparent layer of image manipulation functionality over an existing store of images available on the web.

The typical use case is that I have images stored and being served from eg. Amazon S3 or Rackspace CloudFiles and I want to be able to dynamically generate differently cropped and scaled versions on the fly.

## API
All GET parameters are optional. If none are given, then the image is served unmodified. 

The processing is broken into 3 stages, which affects the order in which the transformations are applied. The stages are *trim*, *crop* and *scale*, which are applied in this order.

### Trim
Trim parameters provide relatively low-level operations that remove the number of pixels specified from the corresponding side of the image. The parameters are *trim_left*, *trim_right*, *trim_top*, *trim_bottom*. These are default to 0 if not specified.

### Crop
Crop parameters provide higher level operations with sensible defaults.

*crop_ratio*: Allows you to specify the desired aspect ratio of the image as a decimal number representing the image width as a proportion of it height, similar to how aspect ratios are defined for cinema and television. For example, "crop_ratio=1.33" will yield a 4:3 image and "crop_ratio=1.78" will yield a 16:9 image. By default the cropped image will as large as possible, given the aspect ratio of the original image and be both horizontally and vertically centered.

*crop_shrink*: Allows you to specify the amount by which to reduce the dimensions of the crop window as a proportion of the original dimensions.

*crop_x_offset*: Allows you to move the crop window along the x axis. Positive values will move it to the right and negative values will move it to the left. Note that you will only be able to move the window as much as the original image allows.

*crop_y_offset*: Same as *crop_x_offset, except for the y axis. Positive values move the crop window down, negative values move crop window up.

### Scale
Scales the image width and height to the given dimensions. If only one dimension is given, then the image will be scaled proportionally. If both dimensions are given then the image will be stretched to match the specified dimensions, regardless of it's original aspect ratio.

## Architecture Overview




## Implementation Overview
