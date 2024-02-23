#!/bin/bash

# Create icons for different platforms

# png
convert -version
convert -background transparent -density 1000 -resize 512x512 \
src/main/resources/icons/icon.svg \
src/main/resources/icons/icon.png

# ico
convert -background transparent -density 1000 \
-define icon:auto-resize -colors 256 \
src/main/resources/icons/icon.svg \
src/main/resources/icons/icon.ico

# icns
convert -background transparent -density 1000 -resize 16x16 \
src/main/resources/icons/icon.svg output_16x16.png
convert -background transparent -density 1000 -resize 32x32 \
src/main/resources/icons/icon.svg output_32x32.png
convert -background transparent -density 1000 -resize 128x128 \
src/main/resources/icons/icon.svg output_128x128.png
convert -background transparent -density 1000 -resize 256x256 \
src/main/resources/icons/icon.svg output_256x256.png
convert -background transparent -density 1000 -resize 512x512 \
src/main/resources/icons/icon.svg output_512x512.png
png2icns src/main/resources/icons/icon.icns output_*.png
rm output_*.png

# pkg background
convert src/main/resources/icons/icon.png \
-background transparent -density 1000 -resize 103x123 -gravity center -extent 143x222 \
installer/mac/SoulFire-background.png

cp installer/mac/SoulFire-background.png \
installer/mac/SoulFire-background-darkAqua.png
