#!/bin/bash

# png
convert -version
inkscape -z -o mod/src/main/resources/icons/icon-rounded.png -w 512 -h 512 mod/src/main/resources/icons/icon-rounded.svg
inkscape -z -o mod/src/main/resources/icons/icon-square.png -w 512 -h 512 mod/src/main/resources/icons/icon-square.svg
inkscape -z -o mod/src/main/resources/icons/pov_favicon.png -w 64 -h 64 mod/src/main/resources/icons/icon-rounded.svg
