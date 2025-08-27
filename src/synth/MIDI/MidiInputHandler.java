package synth.MIDI;

import synth.core.Synthesiser;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class MidiInputHandler implements Receiver{
    private Synthesiser synth;

    public MidiInputHandler(Synthesiser synth){
        this.synth = synth;
    }

    public void send(MidiMessage message, long timeStamp){
        if(message instanceof ShortMessage){
            ShortMessage sm = (ShortMessage) message; // If the message is short, type cast it as a shortmessage object

            byte pitch = (byte) sm.getData1();  // Grabs the pitch byte from the midi message
            double velocity = sm.getData2() /127.00; // Grabs the velocity from the midi message and converts it to a scalar
        }
    }

    @Override
    public void close() {

    }
}
