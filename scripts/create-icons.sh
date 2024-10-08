#!/bin/bash

# Create icons for different platforms

# png
convert -version
inkscape -z -o server/src/main/resources/icons/icon.png -w 512 -h 512 server/src/main/resources/icons/icon.svg

# ico
convert -background transparent -density 1000 \
-define icon:auto-resize -colors 256 \
server/src/main/resources/icons/icon.png \
server/src/main/resources/icons/icon.ico

# icns
convert -background transparent -density 1000 -resize 16x16 \
server/src/main/resources/icons/icon.png output_16x16.png
convert -background transparent -density 1000 -resize 32x32 \
server/src/main/resources/icons/icon.png output_32x32.png
convert -background transparent -density 1000 -resize 128x128 \
server/src/main/resources/icons/icon.png output_128x128.png
convert -background transparent -density 1000 -resize 256x256 \
server/src/main/resources/icons/icon.png output_256x256.png
convert -background transparent -density 1000 -resize 512x512 \
server/src/main/resources/icons/icon.png output_512x512.png
png2icns server/src/main/resources/icons/icon.icns output_*.png
rm output_*.png
