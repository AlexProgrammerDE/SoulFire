#!/bin/bash

# png
convert -version
inkscape -z -o server/src/main/resources/icons/icon.png -w 512 -h 512 server/src/main/resources/icons/icon.svg
inkscape -z -o server/src/main/resources/icons/pov_favicon.png -w 64 -h 64 server/src/main/resources/icons/icon.svg
