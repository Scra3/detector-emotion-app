package com.affectiva.cameradetectordemo;

import android.os.Vibrator;

import java.util.HashMap;

/**
 * Created by scra on 22/11/17.
 */

class Morse {

    private final int dot_length;
    private final int dash_length;
    private final int pause_between_element;
    private final int pause_between_char;
    private final int pause_between_word;

    private String morse;
    private Vibrator vibrator;

    Morse(String text, Vibrator vibrator, int speed) {
        this.vibrator = vibrator;
        this.morse = alphaToMorse(text);
        dot_length = speed;
        dash_length = dot_length * 3;
        pause_between_element = dot_length;
        pause_between_char = dot_length * 3;
        pause_between_word = dot_length * 7;
    }

    private static String[] ALPHA = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
            "s", "t", "u", "v", "w", "x", "y", "z", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "!", ",", "?",
            ".", "'"};

    private static String[] MORSE = {".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..",
            "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..", ".----",
            "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----.", "-----", "-.-.--", "--..--",
            "..--..", ".-.-.-", ".----."};

    private static HashMap<String, String> ALPHA_TO_MORSE = new HashMap<>();

    static {
        for (int i = 0; i < ALPHA.length && i < MORSE.length; i++) {
            ALPHA_TO_MORSE.put(ALPHA[i], MORSE[i]);
        }
    }

    private String alphaToMorse(String code) {
        StringBuilder builder = new StringBuilder();
        String[] words = code.trim().split(" ");

        for (String word : words) {
            for (int i = 0; i < word.length(); i++) {
                String morse = ALPHA_TO_MORSE.get(word.substring(i, i + 1).toLowerCase());
                builder.append(morse).append("  ");
            }

            builder.append(" ");
        }

        String morse = builder.toString();
        return morse.substring(0, morse.length() - 3);
    }

    private void delay(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ie) {

        }
    }

    void morseToImpulses() {
        String[] morseCodesWords = morse.trim().split("  ");

        for (String morseElement : morseCodesWords) {
            for (int i = 0; i < morseElement.length(); i++) {
                if (morseElement.charAt(i) == ' ') {
                    delay(pause_between_char);
                } else {
                    if (morseElement.charAt(i) == '.') {
                        vibrator.vibrate(dot_length);
                        delay(dot_length);
                    } else if (morseElement.charAt(i) == '-') {
                        vibrator.vibrate(dash_length);
                        delay(dash_length);
                    }

                    delay(pause_between_element);
                }
            }

            delay(pause_between_word);
        }
    }

    String getMorse() {
        return morse;
    }
}
