package webdata;

import java.io.*;
import java.util.ArrayList;

/**
 * DictionaryWriter class.
 */
class DictionaryWriter {
    /**
     * ---- CONSTANTS ----
     **/
    private static final String TOKENS_FRONT_CODING_INDEX = "/tokens_front_coding_index";
    private static final String CONCATENATED_TOKENS = "/concatenated_tokens";

    /**
     * ---- FIELDS ----
     **/
    private final int k;
    private final DictionaryEncoder dictionaryEncoder;
    private DataOutputStream frontCodingIndex;
    private DataOutputStream concatenatedTokens;

    /**
     * Dictionary Writer constructor.
     *
     * @param parser Parser object.
     * @param kValue value for K in K - 1 front encoding blocks size.
     * @param dir    directory to save dictionary files in.
     */
    DictionaryWriter(Parser parser, String dir, int kValue) {
        k = kValue;
        dictionaryEncoder = new DictionaryEncoder(parser, dir, kValue);

        try {
            frontCodingIndex = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(dir + TOKENS_FRONT_CODING_INDEX)));

            concatenatedTokens = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(dir + CONCATENATED_TOKENS)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes token's postings list to postings_lists file.
     *
     * @param blockIdx token's index.
     * @throws IOException IOException.
     */
    private long writeConcatenatedTokens(int blockIdx) throws IOException {
        concatenatedTokens.writeChars(dictionaryEncoder.getConcatenatedString(blockIdx));
        return concatenatedTokens.size();
    }

    /***
     * Writes K in K - 1 front encoding index to disk.
     * The index is saved in the directory specified by the user.
     * The files created: tokens_front_coding_index, postings_lists, concatenated_tokens.
     * @throws IOException IOException.
     */
    void write() throws IOException {
        int numOfTerms = dictionaryEncoder.getNumOfTerm();

        frontCodingIndex.writeInt(numOfTerms);
        frontCodingIndex.writeInt(dictionaryEncoder.getNumOfTokens());

        for (int i = 0; i < numOfTerms; ++i) {
            frontCodingIndex.writeInt(dictionaryEncoder.getTokenReviewFreq(i)); // write review collectionFreq
            frontCodingIndex.writeInt(dictionaryEncoder.getTokenFreq(i)); // write collectionFreq
            frontCodingIndex.writeLong(dictionaryEncoder.getPostingListPointer(i)); // write posting list pointer

            if (i % k != k - 1) {
                frontCodingIndex.writeShort(dictionaryEncoder.getTokenLength(i)); // write length
            }
            if (i % k == 0) {   // new block
                writeConcatenatedTokens(i / k); // write term pointer
            } else {
                frontCodingIndex.writeShort(dictionaryEncoder.getTokenPrefix(i)); // write prefix length
            }
        }
        frontCodingIndex.close();
        concatenatedTokens.close();
    }
}
