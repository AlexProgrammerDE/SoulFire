#!/bin/bash

# Create icons for different platforms
convert -version
convert -background transparent -density 1000 \
-resize 120x120 \
src/main/resources/icons/icon.svg \
src/main/resources/icons/icon.png

convert -background transparent -density 1000 \
-define icon:auto-resize -colors 256 \
src/main/resources/icons/icon.svg \
src/main/resources/icons/icon.ico
