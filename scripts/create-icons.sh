#!/bin/bash

# Create icons for different platforms
convert -version
convert -background transparent -density 1000 \
-resize 64x64 \
icons/icon.svg \
icons/icon.png
convert -background transparent -density 1000 \
-define icon:auto-resize -colors 256 \
icons/icon.svg \
icons/icon.ico
