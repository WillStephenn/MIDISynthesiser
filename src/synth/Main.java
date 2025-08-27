package synth;

import synth.core.Voice;
import synth.utils.AudioConstants;

import javax.sound.sampled.*;

public class Main {

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
                16, 1, true, true
        );

        String outputDeviceName = "BlackHole 2ch";

        try (SourceDataLine line = getOutputLine(outputDeviceName, audioFormat)) {
            line.open(audioFormat, 1024);
            line.start();

            double[] arpeggioFrequencies = {
                    // Octave 1
                    110.00, 130.81, 164.81,
                    // Octave 2
                    220.00, 261.63, 329.63,
                    // Octave 3
                    440.00, 523.25, 659.26,
                    // Octave 4
                    880.00, 1046.50, 1318.51,
                    // Octave 5
                    1760.00, 2093.00, 2637.02
            };

            double noteDurationInSeconds = 0.2;
            int samplesPerNote = (int) (noteDurationInSeconds * AudioConstants.SAMPLE_RATE);

            Voice voice = new Voice(Voice.Waveform.SAW, arpeggioFrequencies[0], AudioConstants.SAMPLE_RATE);
            voice.ampEnvelope.setEnvelope(0.01, 0.01, 0.6, 0);
            voice.filterEnvelope.setEnvelope(0.1, 0.1, 0.01, 0.5);
            voice.setFilterParameters(200, 10);

            long samplesElapsed = 0;
            int currentNoteIndex = 0;

            byte[] buffer = new byte[128];
            System.out.println("Playing 5-octave A minor arpeggio to '" + outputDeviceName + "'... Stop the program to exit.");

            voice.noteOn();

            while (true) {
                for (int i = 0; i < buffer.length; i += 2) {

                    if (samplesElapsed > 0 && samplesElapsed % samplesPerNote == 0) {
                        voice.noteOff();
                        currentNoteIndex = (currentNoteIndex + 1) % arpeggioFrequencies.length;
                        double newFrequency = arpeggioFrequencies[currentNoteIndex];
                        voice.setOscillatorFrequency(newFrequency);
                        voice.noteOn();
                    }

                    double volume = 0.1;

                    double sample = voice.processSample(0.0);
                    sample = sample * volume;

                    short pcmSample = (short) (sample * Short.MAX_VALUE);
                    buffer[i] = (byte) (pcmSample >> 8);
                    buffer[i + 1] = (byte) (pcmSample & 0xFF);

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