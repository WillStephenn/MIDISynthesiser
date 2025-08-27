package synth;

import synth.core.Synthesiser;
import synth.utils.AudioConstants;
import javax.sound.sampled.*;

public class Main {
    private static double masterVolume = 1;

    public static SourceDataLine getOutputLine(String name, AudioFormat format) throws LineUnavailableException {
        SourceDataLine line = null;
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(name)) {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.isLineSupported(new DataLine.Info(SourceDataLine.class, format))) {
                    line = (SourceDataLine) mixer.getLine(new DataLine.Info(SourceDataLine.class, format));
                    break;
                }
            }
        }
        if (line == null) {
            System.err.println("Output device '" + name + "' not found. Using default device.");
            line = AudioSystem.getSourceDataLine(format);
        }
        return line;
    }

    public static void main(String[] args) {
        AudioFormat audioFormat = new AudioFormat(
                (float) AudioConstants.SAMPLE_RATE,
                16, 2, true, true
        );

        String outputDeviceName = "BlackHole 2ch";

        try (SourceDataLine line = getOutputLine(outputDeviceName, audioFormat)) {
            line.open(audioFormat, 8192);
            line.start();

            Synthesiser synth = new Synthesiser(9);

            synth.loadPatch(
                    Synthesiser.Waveform.SAW,
                    500, 7, 3000,
                    0.6, 0.8, 0.1, 0.01,
                    0.01, 0.2, 0.5, 1,
                    -10, 0,
                    Synthesiser.Waveform.SINE, 0.5,
                    1.0
            );

            byte[] arpeggioMIDINotes = {
                    45, 48, 52, 55, // A2, C3, E3, G3
                    57, 60, 55, // A3, C4, E4
                    69, 72, 67, // A4, C5, E5
                    //81, 84, 88, // A5, C6, E6
                    //93, 96, 100 // A6, C7, E7
            };

            double noteDurationInSeconds = 0.25;
            int samplesPerNote = (int) (noteDurationInSeconds * AudioConstants.SAMPLE_RATE);

            long samplesElapsed = 0;
            int currentNoteIndex = 0;
            byte[] buffer = new byte[1024];
            System.out.println("Playing arpeggio to '" + outputDeviceName + "'... Stop the program to exit.");

            // Trigger the first note
            synth.noteOn(arpeggioMIDINotes[0], 1.0);

            while (true) {
                for (int i = 0; i < buffer.length; i += 4) {
                    // Check if it's time to switch notes
                    if (samplesElapsed > 0 && samplesElapsed % samplesPerNote == 0) {
                        byte lastNote = arpeggioMIDINotes[currentNoteIndex];
                        synth.noteOff(lastNote); // Stop the old note

                        currentNoteIndex = (currentNoteIndex + 1) % arpeggioMIDINotes.length;
                        byte newNote = arpeggioMIDINotes[currentNoteIndex];
                        synth.noteOn(newNote, 1.0); // Start the new note
                    }
                    // Get the stereo sample pair from the synth
                    double[] stereoSample = synth.processSample();
                    double leftSample = stereoSample[0];
                    double rightSample = stereoSample[1];

                    // Convert left sample to bytes
                    short pcmLeft = (short) (leftSample * Short.MAX_VALUE);
                    buffer[i] = (byte) (pcmLeft >> 8);
                    buffer[i + 1] = (byte) (pcmLeft & 0xFF);

                    // Convert right sample to bytes
                    short pcmRight = (short) (rightSample * Short.MAX_VALUE);
                    buffer[i + 2] = (byte) (pcmRight >> 8);
                    buffer[i + 3] = (byte) (pcmRight & 0xFF);

                    samplesElapsed++;
                }
                line.write(buffer, 0, buffer.length);
            }

        } catch (LineUnavailableException e) {
            System.err.println("Audio line is unavailable.");
            e.printStackTrace();
        }
    }
}