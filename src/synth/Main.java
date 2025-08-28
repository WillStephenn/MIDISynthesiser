package synth;

import synth.MIDI.MidiFilePlayer;
import synth.core.Synthesiser;
import synth.utils.AudioConstants;
import javax.sound.sampled.*;
import javax.sound.midi.Sequencer;

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
                16, 2, true, true
        );

        String outputDeviceName = "BlackHole 2ch";

        try (SourceDataLine line = getOutputLine(outputDeviceName, audioFormat)) {
            line.open(audioFormat, 8192 * 2);
            line.start();

            Synthesiser synth = new Synthesiser(16);

            synth.loadPatch(
                    Synthesiser.Waveform.SAW,
                    1000, 2, 2000,
                    0.01, 0.3, 0.5, 0.1,
                    0.005, 0.1, 0.8, 0.3,
                    -3.0, 0.0,
                    Synthesiser.Waveform.SINE, 0.2,
                    1
            );

            // --- MIDI FILE PLAYBACK ---
            MidiFilePlayer midiPlayer = new MidiFilePlayer(synth);
            Sequencer sequencer = midiPlayer.playMidiFile("midi/Silhuette.mid");

            // --- AUDIO PROCESSING LOOP ---
            byte[] buffer = new byte[1024];

            while ((sequencer != null && sequencer.isRunning()) || synth.anyVoicesActive()) {
                for (int i = 0; i < buffer.length; i += 4) {
                    double[] stereoSample = synth.processSample();
                    double leftSample = stereoSample[0];
                    double rightSample = stereoSample[1];

                    short pcmLeft = (short) (leftSample * Short.MAX_VALUE);
                    buffer[i] = (byte) (pcmLeft >> 8);
                    buffer[i + 1] = (byte) (pcmLeft & 0xFF);

                    short pcmRight = (short) (rightSample * Short.MAX_VALUE);
                    buffer[i + 2] = (byte) (pcmRight >> 8);
                    buffer[i + 3] = (byte) (pcmRight & 0xFF);
                }
                line.write(buffer, 0, buffer.length);
            }

            if (sequencer != null) {
                sequencer.close();
            }

            line.drain();
            line.close();
            System.out.println("Playback finished. Exiting.");

        } catch (LineUnavailableException e) {
            System.err.println("Audio line is unavailable.");
            e.printStackTrace();
        }
    }
}