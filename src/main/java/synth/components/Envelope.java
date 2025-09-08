package synth.components;

import synth.core.AudioComponent;

/**
 * Represents an ADSR (Attack, Decay, Sustain, Release) envelope generator.
 * This component modulates the amplitude of an audio signal over time.
 */
public class Envelope implements AudioComponent {
    /**
     * The different stages of the envelope.
     */
    public enum Stage {
        IDLE,
        ATTACK,
        DECAY,
        SUSTAIN,
        RELEASE
    }

    // State Variables
    protected Stage currentStage;
    protected double currentMultiplier;
    protected double attackIncrement;
    protected double decayIncrement;
    protected double releaseIncrement;

    // Pre Computed Constant
    private final double sampleRateReciprocal;

    // Settings
    // All timings are measured in seconds.
    protected double attackTime;
    protected double decayTime;
    protected double sustainLevel;
    protected double releaseTime;

    /**
     * Constructs an Envelope with a given sample rate.
     * @param sampleRate The sample rate of the audio system. Must be a positive value.
     */
    public Envelope(double sampleRate){
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive.");
        }
        this.currentStage = Stage.IDLE;
        this.sampleRateReciprocal = 1.0/ sampleRate;

        // Default Envelope patch
        setEnvelope(2, 2, 0.5,  3);
    }

    /**
     * Sets the current stage of the envelope.
     * @param newStage The stage to set.
     */
    public void setStage(Stage newStage){
        this.currentStage = newStage;
    }

    /**
     * Gets the current stage of the envelope.
     * @return The current stage.
     */
    public Stage getStage(){
        return this.currentStage;
    }

    /**
     * Sets the attack time of the envelope.
     * @param seconds The attack time in seconds. Must not be negative.
     */
    public void setAttackTime(double seconds){
        if (seconds < 0) {
            throw new IllegalArgumentException("Attack time cannot be negative.");
        }
        this.attackTime = seconds;
        if(attackTime == 0.0){
            this.attackIncrement = 1.0;
        } else{
            this.attackIncrement = this.sampleRateReciprocal / this.attackTime;
        }
    }

    /**
     * Sets the decay time of the envelope.
     * @param seconds The decay time in seconds. Must not be negative.
     */
    public void setDecayTime(double seconds){
        if (seconds < 0) {
            throw new IllegalArgumentException("Decay time cannot be negative.");
        }
        this.decayTime = seconds;
        if(decayTime == 0.0){
            this.decayIncrement = 1.0 - sustainLevel;
        } else {
            this.decayIncrement = (1.0 - sustainLevel) * this.sampleRateReciprocal / this.decayTime;
        }
    }

    /**
     * Sets the sustain level of the envelope.
     * @param level The sustain level, from 0.0 to 1.0.
     */
    public void setSustainLevel(double level){
        if (level < 0.0 || level > 1.0) {
            throw new IllegalArgumentException("Sustain level must be between 0.0 and 1.0.");
        }
        this.sustainLevel = level;
    }

    /**
     * Sets the release time of the envelope.
     * @param seconds The release time in seconds. Must not be negative.
     */
    public void setReleaseTime(double seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Release time cannot be negative.");
        }
        this.releaseTime = seconds;
        if (releaseTime == 0) {
            // sustainLevel is already the total amount to change, so no increment needed
            this.releaseIncrement = sustainLevel;
        } else {
            this.releaseIncrement = sustainLevel * this.sampleRateReciprocal / this.releaseTime;
        }
    }

    /**
     * Sets all parameters of the envelope.
     * @param attackTime The attack time in seconds.
     * @param decayTime The decay time in seconds.
     * @param sustainLevel The sustain level (0.0 to 1.0).
     * @param releaseTime The release time in seconds.
     */
    public void setEnvelope(double attackTime, double decayTime, double sustainLevel, double releaseTime){
        // Set times and level using their respective setters to trigger calculations
        setAttackTime(attackTime);
        setSustainLevel(sustainLevel);
        setDecayTime(decayTime);
        setReleaseTime(releaseTime);
    }

    /**
     * Triggers the attack phase of the envelope when a note is played.
     */
    public void noteOn(){
        this.currentStage = Stage.ATTACK;
        this.currentMultiplier = 0.0;
    }

    /**
     * Triggers the release phase of the envelope when a note is released.
     */
    public void noteOff(){
        this.currentStage = Stage.RELEASE;
    }

    /**
     * Processes a block of audio, applying the envelope to each sample.
     * @param inputBuffer The buffer containing the audio signal to be modulated.
     * @param outputBuffer The buffer where the modulated audio will be written.
     * @param blockSize The number of samples to process.
     */
    @Override
    public void processBlock(double[] inputBuffer, double[] outputBuffer, int blockSize) {
        for (int i = 0; i < blockSize; i++) {
            switch (currentStage) {
                case IDLE:
                    currentMultiplier = 0.0;
                    break;
                case ATTACK:
                    currentMultiplier += attackIncrement;
                    if (currentMultiplier >= 1.0) {
                        currentMultiplier = 1.0;
                        setStage(Stage.DECAY);
                    }
                    break;
                case DECAY:
                    currentMultiplier -= decayIncrement;
                    if (currentMultiplier <= sustainLevel) {
                        currentMultiplier = sustainLevel;
                        setStage(Stage.SUSTAIN);
                    }
                    break;
                case SUSTAIN:
                    currentMultiplier = sustainLevel;
                    break;
                case RELEASE:
                    currentMultiplier -= releaseIncrement;
                    if (currentMultiplier <= 0.0) {
                        currentMultiplier = 0.0;
                        setStage(Stage.IDLE);
                    }
                    break;
            }
            if (inputBuffer == null){
                outputBuffer[i] = currentMultiplier;
            } else {
                outputBuffer[i] = inputBuffer[i] * currentMultiplier;
            }
        }
    }
}
