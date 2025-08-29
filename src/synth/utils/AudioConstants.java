package synth.utils;

public interface AudioConstants {
    double SAMPLE_RATE = 44100.0;
    double CONTROL_RATE = 1;
    double RENDER_RATE = 64;
    int BUFFER_SIZE = (2048 *2 ) *2;
    int NUMBER_OF_VOICES = 12;
}
