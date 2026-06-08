# JPEG Diff Android App

An Android application built with Jetpack Compose to compare two JPEG images pixel-by-pixel with zero tolerance.

## Features

- **Pixel-by-Pixel Diff**: Compares two JPEGs and highlights mismatching pixels in red, while keeping matching pixels in their original colors.
- **Crop Alignment Match**:
  - When images of different sizes are compared, the app automatically determines if one fits inside the other.
  - It searches all possible positions aligned on **JPEG 8x8 block boundaries** (multiples of 8 pixels) using a fast, robust pixel sampling metric.
  - Highlights mismatching pixels inside the crop, and marks all areas outside the crop entirely in red.
- **Interactive UI**:
  - Sleek dark-mode theme utilizing Material 3 design guidelines.
  - Interactive output viewer supporting pinch-to-zoom and drag-to-pan gestures.
  - Detailed statistics panel showing total pixels, mismatch count, and exact match rate percentage.

## Use cases

Useful to test lossless or semi-lossless JPEG operations such as rotating/cropping/blurring. See https://github.com/lossless-jpg/data for context.

## License

Licensed under the Apache License, Version 2.0 (the same license as the Commons Android app). See the `LICENSE` file for details.
