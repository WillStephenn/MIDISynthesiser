package synth;

import javax.sound.midi.MidiDevice;
import javax.sound.sampled.*;

import synth.MIDI.MidiDeviceConnector;
import synth.core.Synthesiser;
import synth.utils.AudioConstants;
import synth.utils.AudioDeviceConnector;
import synth.visualisation.AsciiRenderer;

public class Main {

    public static void main(String[] args) {
        // --- PROMPT USER INPUT TO SET MIDI AND AUDIO DEVICES ---
        String midiInputDevice = MidiDeviceConnector.promptUser();
        String audioOutputDevice = AudioDeviceConnector.promptUser();

        AudioFormat audioFormat = new AudioFormat(
                (float) AudioConstants.SAMPLE_RATE,
                16, 2, true, true
        );

        try (SourceDataLine line = AudioDeviceConnector.getOutputLine(audioOutputDevice, audioFormat)) {

            // --- INITIALISE AND VALIDATE THE SYNTH, AUDIO AND MIDI DEVICES
            line.open(audioFormat, AudioConstants.BUFFER_SIZE);
            line.start();
            Synthesiser synth = new Synthesiser(AudioConstants.NUMBER_OF_VOICES);
            MidiDevice midiDevice = MidiDeviceConnector.connectToDevice(synth, midiInputDevice);

            if (midiDevice == null) {
                System.out.println("Exiting due to MIDI connection failure.");
                return;
            }

            // --- AUDIO PROCESSING LOOP ---
            byte[] buffer = new byte[64];
            double renderCounter = 0;

            while(true){
                if (renderCounter == 0){
                    AsciiRenderer.render(synth);
                }
                renderCounter = (renderCounter + 1)% AudioConstants.RENDER_RATE;

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

        } catch (LineUnavailableException e) {
            System.err.println("Audio line is unavailable.");
            e.printStackTrace();
        }
    }
}