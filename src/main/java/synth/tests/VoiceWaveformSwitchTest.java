package synth.tests;

import synth.core.Synthesiser;
import synth.core.Voice;
import synth.utils.AudioConstants;

/**
 * Regression test for the bug where Voice.setOscillatorPitch tuned only the
 * current oscillator, so switching waveform during a held note landed on an
 * untuned oscillator (flat DC or silence) or on a stale pitch from an earlier
 * note.
 *
 * The fix tunes all four oscillators, so the held note keeps sounding at its
 * pitch whichever waveform is selected.
 */
public class VoiceWaveformSwitchTest {

    private static final double SAMPLE_RATE = AudioConstants.SAMPLE_RATE;
    private static final int BLOCK_SIZE = AudioConstants.BLOCK_SIZE;
    private static final double SECONDS = 0.25;

    private static int failures = 0;

    public static void main(String[] args) {
        for (Synthesiser.Waveform target : Synthesiser.Waveform.values()) {
            testSwitchDuringHeldNoteKeepsPitch(target);
        }

        if (failures > 0) {
            System.err.println(failures + " test(s) FAILED.");
            System.exit(1);
        }
        System.out.println("All voice waveform switch tests passed.");
    }

    /** Holds A4 on a voice with a stale pitch on its oscillators, switches waveform, and checks the tone. */
    private static void testSwitchDuringHeldNoteKeepsPitch(Synthesiser.Waveform target) {
        String name = "switch-to-" + target;
        Voice voice = new Voice(Synthesiser.Waveform.SQUARE, 0.0, SAMPLE_RATE, BLOCK_SIZE);
        voice.setFilterGainStaging(0.0, 0.0);
        voice.setAmpEnvelope(0.0, 0.0, 1.0, 0.1);

        // An earlier note leaves a stale pitch on the oscillators when only the current one is tuned.
        voice.setOscillatorPitch((byte) 48);
        voice.setOscillatorWaveform(target);
        voice.setOscillatorPitch((byte) 48);
        voice.setOscillatorWaveform(Synthesiser.Waveform.SQUARE);

        voice.setOscillatorPitch((byte) 69); // A4, 440 Hz
        voice.noteOn();
        double[] out = new double[BLOCK_SIZE * 2];
        voice.processBlock(null, out, BLOCK_SIZE);
        voice.setOscillatorWaveform(target);

        int blocks = (int) (SECONDS * SAMPLE_RATE / BLOCK_SIZE);
        double[] left = new double[blocks * BLOCK_SIZE];
        for (int b = 0; b < blocks; b++) {
            voice.processBlock(null, out, BLOCK_SIZE);
            for (int i = 0; i < BLOCK_SIZE; i++) {
                left[b * BLOCK_SIZE + i] = out[i * 2];
            }
        }

        double mean = 0;
        for (double x : left) mean += x;
        mean /= left.length;
        double acSum = 0;
        for (double x : left) acSum += (x - mean) * (x - mean);
        double acRms = Math.sqrt(acSum / left.length);
        check(acRms > 0.05, name + ": output after the switch is DC/silent (ac rms " + acRms + ")");

        int crossings = 0;
        boolean prev = left[0] >= 0;
        for (int i = 1; i < left.length; i++) {
            boolean cur = left[i] >= 0;
            if (cur != prev) crossings++;
            prev = cur;
        }
        double expected = 2.0 * 440.0 * left.length / SAMPLE_RATE;
        check(Math.abs(crossings - expected) <= expected * 0.05 + 2,
                name + ": " + crossings + " zero crossings, expected about " + expected + " for 440 Hz");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            System.err.println("FAIL: " + message);
            failures++;
        } else {
            System.out.println("PASS: " + message.split(":")[0]);
        }
    }
}
