package synth;

import javax.sound.midi.MidiDevice;
import javax.sound.sampled.*;

import synth.MIDI.MidiDeviceConnector;
import synth.core.Synthesiser;
import synth.utils.AudioConstants;
import synth.visualisation.AsciiRenderer;

public class Main {
    // NEXT STEP. BRIDGE MIDI CONTROL TO LOGIC. TIE AUTOMATION CHANNELS AND VELOCITY TO FX.
    // Create a seperate UI class that pulls the synth params and note being played and renders it in ascii

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

        try (SourceDataLine line = getOutputLine(AudioConstants.AUDIO_OUTPUT_DEVICE, audioFormat)) {
            line.open(audioFormat, 2048 * 2);
            line.start();

            Synthesiser synth = new Synthesiser(8);

            synth.loadPatch(
                    Synthesiser.Waveform.SAW,
                    1000, 3, 2000,
                    0.01, 0.3, 0.5, 0.1,
                    0.005, 0.1, 0.4, 0.4,
                    -3.0, 0.0,
                    Synthesiser.Waveform.SINE, 0.2,
                    1
            );

            MidiDeviceConnector.listMidiDevices();
            MidiDevice midiDevice = MidiDeviceConnector.connectToDevice(synth, AudioConstants.MIDI_INPUT_SOURCE);

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