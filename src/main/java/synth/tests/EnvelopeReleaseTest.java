package synth.tests;

import synth.components.Envelope;
import synth.utils.AudioConstants;

/**
 * Regression test for the voice-lifecycle bug where releasing a note during
 * Attack or Decay with a sustain level of 0.0 produced a release increment of
 * exactly 0.0, leaving the envelope stuck in the Release stage forever and
 * never returning its voice to the free pool.
 *
 * The fix derives the release increment from the envelope level at the moment
 * of release (see Envelope.noteOff), so the envelope always reaches Idle.
 */
public class EnvelopeReleaseTest {

    private static final double SAMPLE_RATE = AudioConstants.SAMPLE_RATE;
    private static final int BLOCK_SIZE = AudioConstants.BLOCK_SIZE;

    private static int failures = 0;

    public static void main(String[] args) {
        testReleaseDuringAttackWithZeroSustainReachesIdle();
        testReleaseDuringDecayWithZeroSustainReachesIdle();
        testReleaseFromSustainStillTakesReleaseTime();
        testZeroReleaseTimeDropsStraightToIdle();
        testNoteOffBeforeAnyAudioDropsStraightToIdle();

        if (failures > 0) {
            System.err.println(failures + " test(s) FAILED.");
            System.exit(1);
        }
        System.out.println("All envelope release tests passed.");
    }

    /** Releasing mid-Attack with sustain 0.0 must still decay to Idle. */
    private static void testReleaseDuringAttackWithZeroSustainReachesIdle() {
        Envelope envelope = new Envelope(SAMPLE_RATE);
        envelope.setEnvelope(1.0, 0.1, 0.0, 0.2);
        envelope.noteOn();

        // Process ~50 ms of audio: well inside the 1 s attack.
        processSeconds(envelope, 0.05);
        check(envelope.getStage() == Envelope.Stage.ATTACK,
                "release-during-attack: expected ATTACK before noteOff, got " + envelope.getStage());

        envelope.noteOff();

        // One release time plus a small margin must be enough to reach Idle.
        processSeconds(envelope, 0.25);
        check(envelope.getStage() == Envelope.Stage.IDLE,
                "release-during-attack: envelope stuck in " + envelope.getStage()
                        + " after release time elapsed (voice would leak)");
    }

    /** Releasing mid-Decay with sustain 0.0 must still decay to Idle. */
    private static void testReleaseDuringDecayWithZeroSustainReachesIdle() {
        Envelope envelope = new Envelope(SAMPLE_RATE);
        envelope.setEnvelope(0.01, 1.0, 0.0, 0.2);
        envelope.noteOn();

        // Process ~100 ms: past the 10 ms attack, inside the 1 s decay.
        processSeconds(envelope, 0.1);
        check(envelope.getStage() == Envelope.Stage.DECAY,
                "release-during-decay: expected DECAY before noteOff, got " + envelope.getStage());

        envelope.noteOff();

        processSeconds(envelope, 0.25);
        check(envelope.getStage() == Envelope.Stage.IDLE,
                "release-during-decay: envelope stuck in " + envelope.getStage()
                        + " after release time elapsed (voice would leak)");
    }

    /** A normal release from Sustain must take roughly the configured release time. */
    private static void testReleaseFromSustainStillTakesReleaseTime() {
        Envelope envelope = new Envelope(SAMPLE_RATE);
        envelope.setEnvelope(0.01, 0.01, 0.5, 0.5);
        envelope.noteOn();

        processSeconds(envelope, 0.1);
        check(envelope.getStage() == Envelope.Stage.SUSTAIN,
                "release-from-sustain: expected SUSTAIN before noteOff, got " + envelope.getStage());

        envelope.noteOff();

        // Halfway through the release the envelope must still be audible.
        processSeconds(envelope, 0.25);
        check(envelope.getStage() == Envelope.Stage.RELEASE,
                "release-from-sustain: expected RELEASE at half release time, got " + envelope.getStage());

        // After the full release time (plus margin) it must be Idle.
        processSeconds(envelope, 0.3);
        check(envelope.getStage() == Envelope.Stage.IDLE,
                "release-from-sustain: expected IDLE after full release time, got " + envelope.getStage());
    }

    /** A release time of zero must drop the envelope straight to Idle on noteOff. */
    private static void testZeroReleaseTimeDropsStraightToIdle() {
        Envelope envelope = new Envelope(SAMPLE_RATE);
        envelope.setEnvelope(0.01, 0.01, 0.5, 0.0);
        envelope.noteOn();

        processSeconds(envelope, 0.1);
        envelope.noteOff();
        check(envelope.getStage() == Envelope.Stage.IDLE,
                "zero-release-time: expected IDLE immediately after noteOff, got " + envelope.getStage());
    }

    /** noteOff at level 0.0 (no audio processed yet) must drop straight to Idle. */
    private static void testNoteOffBeforeAnyAudioDropsStraightToIdle() {
        Envelope envelope = new Envelope(SAMPLE_RATE);
        envelope.setEnvelope(1.0, 0.1, 0.0, 0.2);
        envelope.noteOn();
        envelope.noteOff();
        check(envelope.getStage() == Envelope.Stage.IDLE,
                "noteOff-at-zero: expected IDLE when released at level 0.0, got " + envelope.getStage());
    }

    private static void processSeconds(Envelope envelope, double seconds) {
        double[] outputBuffer = new double[BLOCK_SIZE];
        int blocks = (int) Math.ceil(seconds * SAMPLE_RATE / BLOCK_SIZE);
        for (int i = 0; i < blocks; i++) {
            envelope.processBlock(null, outputBuffer, BLOCK_SIZE);
        }
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
