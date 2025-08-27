package synth.components;

import synth.core.AudioComponent;

public class Envelope implements AudioComponent {
    // Instance Variables
    public enum Stage {
        IDLE, ATTACK, DECAY, SUSTAIN, RELEASE
    }

    // State Variables
    protected double sampleRate;
    protected Stage currentStage;
    protected double currentMultiplier;
    protected double attackIncrement;
    protected double decayIncrement;
    protected double releaseIncrement;

    // Settings
    // All timings are measured in seconds.
    protected double attackTime;
    protected double decayTime;
    protected double sustainLevel;
    protected double releaseTime;

    //  Constructor
    public Envelope(double sampleRate){
        this.currentStage = Stage.IDLE;
        this.sampleRate = sampleRate;

        // Default Envelope patch
        setEnvelope(2, 2, 0.5,  3);
    }

    // Envelope Setters

    public void setStage(Stage newStage){
        this.currentStage = newStage;
    }

    public Stage getStage(){
        return this.currentStage;
    }

    public void setAttackTime(double seconds){
        this.attackTime = seconds;
        if(attackTime == 0.0){
            this.attackIncrement = 1.0;
        } else{
            this.attackIncrement = 1.0/(attackTime * sampleRate);
        }
    }

    public void setDecayTime(double seconds){
        this.decayTime = seconds;
        if(decayTime == 0.0){
            this.decayIncrement = 1.0 - sustainLevel;
        } else {
            this.decayIncrement = (1.0 - sustainLevel) / (decayTime * sampleRate);
        }
    }

    public void setSustainLevel(double level){
        this.sustainLevel = level;
    }

    public void setReleaseTime(double seconds) {
        this.releaseTime = seconds;
        if (releaseTime == 0) {
            this.releaseIncrement = sustainLevel;
        } else {
            this.releaseIncrement = sustainLevel / (releaseTime * sampleRate);
        }
    }

    public void setEnvelope(double attackTime, double decayTime, double sustainLevel, double releaseTime){
        // Set times and level using their respective setters to trigger calculations
        setAttackTime(attackTime);
        setSustainLevel(sustainLevel);
        setDecayTime(decayTime);
        setReleaseTime(releaseTime);
    }

    // Envelope State Management

    public void noteOn(){
        this.currentStage = Stage.ATTACK;
        this.currentMultiplier = 0.0;
    }

    public void noteOff(){
        this.currentStage = Stage.RELEASE;
    }

    @Override
    public double processSample(double input) {
        switch (currentStage){
            case IDLE:
                return 0.0;
            case ATTACK:
                currentMultiplier += attackIncrement;
                if (currentMultiplier >= 1.0){
                    currentMultiplier = 1.0;
                    setStage(Stage.DECAY);
                }
                break;
            case DECAY:
                currentMultiplier -= decayIncrement;
                if (currentMultiplier <= sustainLevel){
                    currentMultiplier = sustainLevel;
                    setStage(Stage.SUSTAIN);
                }
                break;
            case SUSTAIN:
                currentMultiplier = sustainLevel;
                break;
            case RELEASE:
                currentMultiplier -= releaseIncrement;
                if (currentMultiplier <= 0.0){
                    currentMultiplier = 0.0;
                    setStage(Stage.IDLE);
                }
                break;
        }
        return input * currentMultiplier;
    }
}
