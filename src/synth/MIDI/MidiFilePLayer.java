package synth.MIDI;

import synth.core.Synthesiser;
import java.io.File;
import javax.sound.midi.*;

public class MidiFilePLayer {

    private Synthesiser synth;

    public MidiFilePLayer(Synthesiser synth){
        this.synth = synth;
    }

    public Sequencer playMidiFile(String filePath){
        try{
            // Load Midi File
            File midiFile = new File(filePath);
            Sequence sequence = MidiSystem.getSequence(midiFile);

            // Create a sequencer and open it. False indicates no connected midi device.
            Sequencer sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.setSequence(sequence);

            // Connected the sequencer to the receiver via a transmitter
            Transmitter transmitter = sequencer.getTransmitter();
            Receiver receiver = new MidiInputHandler(synth);
            transmitter.setReceiver(receiver);

            return sequencer;

        } catch (Exception e) {
            System.err.println("Error playing MIDI file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
