#!/bin/bash

# png
convert -version
inkscape -z -o server/src/main/resources/icons/icon.png -w 512 -h 512 server/src/main/resources/icons/icon.svg
