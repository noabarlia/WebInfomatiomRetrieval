package webdata;

import java.lang.Math;
import java.util.*;

/**
 * Gamma Encoder class
 */
class GammaEncoder {

    /**
     * Returns a Gamma Code code for int x.
     *
     * @param x integer to encode.
     * @return gamma code.
     */
    static byte[] encode(int x) {
        if (x == 1) return new byte[1];
        if (x == 2) return new byte[]{0B0000100};
        int length = (int) Math.floor(Utils.log2(x));
        String gammaCode = getUnary(length) + getBinary(x);
        BitSet bs = new BitSet(gammaCode.length());
        for (int i = 0; i < gammaCode.length(); ++i) {
            if (gammaCode.charAt(gammaCode.length() - i - 1) == '1') {
                bs.set(i);
            }
        }
        return bs.toByteArray();
    }

    /**
     * Decodes gamma code of one integer.
     *
     * @param gammaCode gamma code.
     * @return integer.
     */
    private static int decode(byte[] gammaCode) {
        BitSet bs = BitSet.valueOf(gammaCode);
        int length = bs.length();
        if (length == 0) return 1;
        int endOfUnary = bs.previousClearBit(length - 1);
        bs.set(endOfUnary);
        long[] result = bs.get(0, endOfUnary + 1).toLongArray();
        return (int) result[0];
    }

    /**
     * Decodes sequence of integer encoded in gamma code.
     *
     * @param gammaCode gamma code.
     * @return List of integers.
     */
    static ArrayList<Integer> decodeSequence(byte[] gammaCode) {
        ArrayList<Integer> seq = new ArrayList<>();
        int end = gammaCode.length, start = end - 1, setBits;
        int numBytes;

        while (end > 0) {
            byte[] gammaCodeSlice = Arrays.copyOfRange(gammaCode, start, end);
            BitSet bs = BitSet.valueOf(gammaCodeSlice);
            setBits = bs.previousSetBit(bs.length() - 1) - bs.previousClearBit(bs.length() - 1);

            if (setBits == 0 || bs.previousClearBit(bs.length() - 1) != -1) {   // finished reading one num
                numBytes = (int) Math.ceil((setBits * 2 + 1) / 8f);
                start = end - numBytes;
                seq.add(GammaEncoder.decode(Arrays.copyOfRange(gammaCode, start, end)));
                end = start;
            }
            start--;
        }
        Collections.reverse(seq);
        return seq;
    }

    /**
     * Returns unary representation of integer.
     *
     * @param length length.
     */
    private static String getUnary(int length) {
        String s = new String(new char[length]).replace("\0", "1");
        return s + "0";
    }

    /**
     * Returns binary representation of integer.
     *
     * @param x integer
     */
    private static String getBinary(int x) {
        return Integer.toBinaryString(x).substring(1);
    }
}
