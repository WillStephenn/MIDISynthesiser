package synth;


import synth.core.Voice;
import synth.utils.AudioConstants;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Main {
    public static void main(String[] args) {
        // 1. Set up the audio format and line
        AudioFormat audioFormat = new AudioFormat(
                (float) AudioConstants.SAMPLE_RATE,
                16, // 16-bit audio
                1,  // Mono
                true, // Signed
                true  // Big-endian
        );

        try (SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat)) {
            line.open(audioFormat, 1024); // Open the line with a buffer size
            line.start(); // Start the line, allowing it to play audio

            Voice voice = new Voice(Voice.Waveform.SINE, 440, AudioConstants.SAMPLE_RATE);
            voice.ampEnvelope.setEnvelope(5, 5, 0.1, 10);
            voice.noteOn();

            // Create the buffer to hold the audio data
            byte[] buffer = new byte[1024];

            System.out.println("Playing a 440 Hz tone. Stop the program to exit.");

            // 3. The main audio loop
            while (true) {
                // Fill the buffer with samples
                for (int i = 0; i < buffer.length; i += 2) {
                    // Generate one sample from the oscillator
                    double sample = voice.processSample(0.0);

                    // Convert the double sample (-1.0 to 1.0) to a 16-bit integer
                    short pcmSample = (short) (sample * Short.MAX_VALUE);

                    // Convert the 16-bit sample into two bytes (big-endian)
                    buffer[i] = (byte) (pcmSample >> 8);     // High byte
                    buffer[i + 1] = (byte) (pcmSample & 0xFF); // Low byte
                }

                // 4. Write the buffer to the audio line
                line.write(buffer, 0, buffer.length);
            }

        } catch (LineUnavailableException e) {
            System.err.println("Audio line is unavailable.");
            e.printStackTrace();
        }
    }
}