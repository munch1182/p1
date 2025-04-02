package com.munch1182.lib.helper.sound;

public class WavHelper {
    private WavHelper() {
    }

    public static byte[] wavHeader(RecordHelper record, long dataSize) {
        long total = 36 + dataSize;
        int byteRate = record.getSampleRate() * record.getChannel() * 2;
        byte[] b = new byte[44];
        b[0] = 'R';
        b[1] = 'I';
        b[2] = 'F';
        b[3] = 'F';
        b[4] = (byte) (total & 0xff);
        b[5] = (byte) ((total >> 8) & 0xff);
        b[6] = (byte) ((total >> 16) & 0xff);
        b[7] = (byte) ((total >> 24) & 0xff);
        b[8] = 'W';
        b[9] = 'A';
        b[10] = 'V';
        b[11] = 'E';
        b[12] = 'f';
        b[13] = 'm';
        b[14] = 't';
        b[15] = ' ';
        b[16] = 16;
        b[17] = 0;
        b[18] = 0;
        b[19] = 0;
        b[20] = 1;
        b[21] = 0;
        b[22] = (byte) record.getChannel();
        b[23] = (byte) ((record.getChannel() >> 8) & 0xff);
        b[24] = (byte) (record.getSampleRate() & 0xff);
        b[25] = (byte) ((record.getSampleRate() >> 8) & 0xff);
        b[26] = (byte) ((record.getSampleRate() >> 16) & 0xff);
        b[27] = (byte) ((record.getSampleRate() >> 24) & 0xff);
        b[28] = (byte) (byteRate & 0xff);
        b[29] = (byte) ((byteRate >> 8) & 0xff);
        b[30] = (byte) ((byteRate >> 16) & 0xff);
        b[31] = (byte) ((byteRate >> 24) & 0xff);
        b[32] = (byte) (record.getChannel() * 16 / 8);
        b[33] = 0;
        b[34] = 16;
        b[35] = 0;
        b[36] = 'd';
        b[37] = 'a';
        b[38] = 't';
        b[39] = 'a';
        b[40] = (byte) (dataSize & 0xff);
        b[41] = (byte) ((dataSize >> 8) & 0xff);
        b[42] = (byte) ((dataSize >> 16) & 0xff);
        b[43] = (byte) ((dataSize >> 24) & 0xff);
        return b;
    }
}
