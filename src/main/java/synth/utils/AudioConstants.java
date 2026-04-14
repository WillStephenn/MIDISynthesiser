package synth.utils;

public interface AudioConstants {
    double SAMPLE_RATE = 44100.0;
    int BLOCK_SIZE = 256;
    int BUFFER_SIZE =  BLOCK_SIZE * 8;
    int NUMBER_OF_VOICES = 8;
    int LOOKUP_TABLE_SIZE = 16384*2;
    double DEVICE_SCAN_INTERVAL_SECONDS = 3.0;
}
