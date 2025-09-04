package synth.utils;

public interface AudioConstants {
    double SAMPLE_RATE = 44100.0;
    int CONTROL_RATE = 512;
    double RENDER_RATE = 100;
    int BUFFER_SIZE = 2048*2;
    int BLOCK_SIZE = 128;
    int NUMBER_OF_VOICES = 12;
    int LOOKUP_TABLE_SIZE = 16384;
}
