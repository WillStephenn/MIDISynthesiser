package synth.tests;

import synth.core.Synthesiser;
import synth.utils.AudioConstants;

/**
 * Regression test for the bug where voice stealing took the oldest voice by
 * note-on time even when it was still held while newer voices were only in
 * their release tail.
 *
 * Stealing now prefers a releasing voice (oldest first) over a held one.
 */
public class VoiceStealingTest {

    private static final double SAMPLE_RATE = AudioConstants.SAMPLE_RATE;
    private static final int BLOCK_SIZE = AudioConstants.BLOCK_SIZE;
    private static final int VOICES = AudioConstants.NUMBER_OF_VOICES;

    private static int failures = 0;

    public static void main(String[] args) {
        testHeldNoteSurvivesBurstOfTaps();
        testReleasingVoiceStolenBeforeOldestHeld();
        testOldestHeldStolenWhenAllHeld();

        if (failures > 0) {
            System.err.println(failures + " test(s) FAILED.");
            System.exit(1);
        }
        System.out.println("All voice stealing tests passed.");
    }

    private static Synthesiser newSynth() {
        Synthesiser synth = new Synthesiser(VOICES, SAMPLE_RATE, BLOCK_SIZE);
        synth.setAmpAttackTime(0.0);
        synth.setAmpSustainLevel(1.0);
        synth.setAmpReleaseTime(2.0);
        return synth;
    }

    private static boolean isHeld(Synthesiser synth, int pitch) {
        byte[] notes = new byte[VOICES];
        int count = synth.getActiveNotes(notes);
        for (int i = 0; i < count; i++) {
            if (notes[i] == pitch) return true;
        }
        return false;
    }

    /** Hold one note, then tap more notes than the pool holds: the held note survives. */
    private static void testHeldNoteSurvivesBurstOfTaps() {
        Synthesiser synth = newSynth();
        double[] buffer = new double[BLOCK_SIZE * 2];
        synth.noteOn((byte) 40, 1.0);
        for (int i = 0; i < VOICES + 2; i++) {
            byte pitch = (byte) (60 + i);
            synth.noteOn(pitch, 1.0);
            synth.processBlock(buffer);
            synth.noteOff(pitch);
            synth.processBlock(buffer);
        }
        check(isHeld(synth, 40), "held-survives-taps: held note was stolen while other voices were only releasing");
    }

    /** With a full pool and one releasing voice, the releasing voice is the one stolen. */
    private static void testReleasingVoiceStolenBeforeOldestHeld() {
        Synthesiser synth = newSynth();
        for (int i = 0; i < VOICES; i++) {
            synth.noteOn((byte) (60 + i), 1.0);
        }
        // Render a block first: a noteOff before any audio drops the voice straight to idle.
        synth.processBlock(new double[BLOCK_SIZE * 2]);
        synth.noteOff((byte) 63);
        synth.noteOn((byte) 100, 1.0);
        check(isHeld(synth, 60), "releasing-first: oldest held voice was stolen despite a releasing one");
        check(!isHeld(synth, 63) && isHeld(synth, 100), "releasing-first: releasing voice should be reused for the new note");
    }

    /** With every voice held, the oldest held voice is still the one stolen. */
    private static void testOldestHeldStolenWhenAllHeld() {
        Synthesiser synth = newSynth();
        for (int i = 0; i < VOICES; i++) {
            synth.noteOn((byte) (60 + i), 1.0);
        }
        synth.noteOn((byte) 100, 1.0);
        check(!isHeld(synth, 60) && isHeld(synth, 61) && isHeld(synth, 100),
                "all-held: expected the oldest held voice to be stolen");
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
