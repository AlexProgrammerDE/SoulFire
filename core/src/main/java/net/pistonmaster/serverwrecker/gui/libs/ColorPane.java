package net.pistonmaster.serverwrecker.gui.libs;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class ColorPane extends JTextPane {
    static final Color D_Black = Color.getHSBColor(0.000f, 0.000f, 0.000f);
    static final Color D_Red = Color.getHSBColor(0.000f, 1.000f, 0.502f);
    static final Color D_Blue = Color.getHSBColor(0.667f, 1.000f, 0.502f);
    static final Color D_Magenta = Color.getHSBColor(0.833f, 1.000f, 0.502f);
    static final Color D_Green = Color.getHSBColor(0.333f, 1.000f, 0.502f);
    static final Color D_Yellow = Color.getHSBColor(0.167f, 1.000f, 0.502f);
    static final Color D_Cyan = Color.getHSBColor(0.500f, 1.000f, 0.502f);
    static final Color D_White = Color.getHSBColor(0.000f, 0.000f, 0.753f);
    static final Color B_Black = Color.getHSBColor(0.000f, 0.000f, 0.502f);
    static final Color B_Red = Color.getHSBColor(0.000f, 1.000f, 1.000f);
    static final Color B_Blue = Color.getHSBColor(0.667f, 1.000f, 1.000f);
    static final Color B_Magenta = Color.getHSBColor(0.833f, 1.000f, 1.000f);
    static final Color B_Green = Color.getHSBColor(0.333f, 1.000f, 1.000f);
    static final Color B_Yellow = Color.getHSBColor(0.167f, 1.000f, 1.000f);
    static final Color B_Cyan = Color.getHSBColor(0.500f, 1.000f, 1.000f);
    static final Color B_White = Color.getHSBColor(0.000f, 0.000f, 1.000f);
    static final Color cReset = Color.getHSBColor(0.000f, 0.000f, 1.000f);
    static Color colorCurrent = cReset;
    String remaining = "";

    public void append(Color c, String s) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
        int len = getDocument().getLength();
        try {
            getDocument().insertString(len, s, aset);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void appendANSI(String s) { // convert ANSI color codes first
        int aPos = 0;   // current char position in addString
        int aIndex = 0; // index of next Escape sequence
        int mIndex = 0; // index of "m" terminating Escape sequence
        String tmpString = "";
        boolean stillSearching = true; // true until no more Escape sequences
        String addString = remaining + s;
        remaining = "";

        if (addString.length() > 0) {
            aIndex = addString.indexOf("\u001B"); // find first escape
            if (aIndex == -1) { // no escape/color change in this string, so just send it with current color
                append(colorCurrent, addString);
                return;
            }
// otherwise There is an escape character in the string, so we must process it

            if (aIndex > 0) { // Escape is not first char, so send text up to first escape
                tmpString = addString.substring(0, aIndex);
                append(colorCurrent, tmpString);
                aPos = aIndex;
            }
// aPos is now at the beginning of the first escape sequence

            stillSearching = true;
            while (stillSearching) {
                mIndex = addString.indexOf("m", aPos); // find the end of the escape sequence
                if (mIndex < 0) { // the buffer ends halfway through the ansi string!
                    remaining = addString.substring(aPos, addString.length());
                    stillSearching = false;
                    continue;
                } else {
                    tmpString = addString.substring(aPos, mIndex + 1);
                    colorCurrent = getANSIColor(tmpString);
                }
                aPos = mIndex + 1;
// now we have the color, send text that is in that color (up to next escape)

                aIndex = addString.indexOf("\u001B", aPos);

                if (aIndex == -1) { // if that was the last sequence of the input, send remaining text
                    tmpString = addString.substring(aPos, addString.length());
                    append(colorCurrent, tmpString);
                    stillSearching = false;
                    continue; // jump out of loop early, as the whole string has been sent now
                }

                // there is another escape sequence, so send part of the string and prepare for the next
                tmpString = addString.substring(aPos, aIndex);
                aPos = aIndex;
                append(colorCurrent, tmpString);

            } // while there's text in the input buffer
        }
    }

    public Color getANSIColor(String ANSIColor) {
        return switch (ANSIColor) {
            case "\u001B[30m", "\u001B[0;30m" -> D_Black;
            case "\u001B[31m", "\u001B[0;31m" -> D_Red;
            case "\u001B[32m", "\u001B[0;32m" -> D_Green;
            case "\u001B[33m", "\u001B[0;33m" -> D_Yellow;
            case "\u001B[34m", "\u001B[0;34m" -> D_Blue;
            case "\u001B[35m", "\u001B[0;35m" -> D_Magenta;
            case "\u001B[36m", "\u001B[0;36m" -> D_Cyan;
            case "\u001B[37m" -> D_White;
            case "\u001B[0;37m" -> D_White;
            case "\u001B[1;30m" -> B_Black;
            case "\u001B[1;31m" -> B_Red;
            case "\u001B[1;32m" -> B_Green;
            case "\u001B[1;33m" -> B_Yellow;
            case "\u001B[1;34m" -> B_Blue;
            case "\u001B[1;35m" -> B_Magenta;
            case "\u001B[1;36m" -> B_Cyan;
            case "\u001B[1;37m" -> B_White;
            case "\u001B[0m" -> cReset;
            default -> B_White;
        };
    }
}