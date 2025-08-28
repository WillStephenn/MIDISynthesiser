package synth;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.*;

import synth.MIDI.MidiDeviceConnector;
import synth.MIDI.MidiFilePlayer;
import synth.core.Synthesiser;
import synth.utils.AudioConstants;

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

        String outputDeviceName = "BlackHole 2ch";

        try (SourceDataLine line = getOutputLine(outputDeviceName, audioFormat)) {
            line.open(audioFormat, 8192 * 2);
            line.start();

            Synthesiser synth = new Synthesiser(16);

            synth.loadPatch(
                    Synthesiser.Waveform.TRIANGLE,
                    1000, 2, 2000,
                    0.01, 0.3, 0.5, 0.1,
                    0.005, 0.1, 0.4, 0.2,
                    -3.0, 0.0,
                    Synthesiser.Waveform.SINE, 0.2,
                    0.4
            );

            // --- MIDI FILE PLAYBACK ---
            //MidiFilePlayer midiPlayer = new MidiFilePlayer(synth);
            //Sequencer sequencer = midiPlayer.playMidiFile("midi/chords.mid");

            MidiDeviceConnector.listMidiDevices();
            String targetDeviceName = "Logic Pro Virtual Out";
            MidiDevice midiDevice = MidiDeviceConnector.connectToDevice(synth, targetDeviceName);

            if (midiDevice == null) {
                System.out.println("Exiting due to MIDI connection failure.");
                return;
            }

            // --- AUDIO PROCESSING LOOP ---
            byte[] buffer = new byte[64];

            //while ((sequencer != null && sequencer.isRunning()) || synth.anyVoicesActive()) {
            while(true){
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

            //if (sequencer != null) {
            //    sequencer.close();
            //}

            //line.drain();
            //line.close();
            //System.out.println("Playback finished. Exiting.");

        } catch (LineUnavailableException e) {
            System.err.println("Audio line is unavailable.");
            e.printStackTrace();
        }
    }
}