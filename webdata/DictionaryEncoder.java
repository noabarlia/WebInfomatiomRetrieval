package webdata;

import java.io.*;


/**
 * DictionaryEncoder class.
 */
class DictionaryEncoder {

    private static final String MERGED_PAIRS_TMP = "/tmp/ExternalSort_tmp/mergedPairs_tmp";
    private static final String POSTINGS_LISTS = "/postings_lists";

    /**
     * ---- FIELDS ----
     **/
    private Parser parser;
    private int k;
    private int numTerms;
    private int numTokens;
    private DataInputStream postingsListsInput;
    private DataOutputStream postingsListsOutput;

    private int[] freq;
    private int[] reviewFreq;
    private int[] lengths;
    private int[] prefixLengths;
    private long[] postingListPointers;
    private String[] concatenatedStrings;

    /**
     * Process the input data for the index before it being written to disk.
     *
     * @param parser Parser object.
     * @param kValue value for K in K - 1 front encoding blocks size.
     */
    DictionaryEncoder(Parser parser, String dir, int kValue) {
        this.parser = parser;
        this.k = kValue;

        numTerms = parser.getNumOfTerms();
        numTokens = parser.getNumOfTokens();

        this.freq = new int[numTerms];
        this.reviewFreq = new int[numTerms];
        this.lengths = new int[numTerms];
        this.prefixLengths = new int[numTerms];
        this.postingListPointers = new long[numTerms];
        this.concatenatedStrings = new String[(int) Math.ceil(numTerms / (double) kValue)];

        try {
            postingsListsInput = new DataInputStream(new BufferedInputStream(new FileInputStream(MERGED_PAIRS_TMP)));
            postingsListsOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dir + POSTINGS_LISTS)));

            concatenateTokens();
            writePostings();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the length of the longest common prefix of str1 and str2.
     *
     * @param str1 first token.
     * @param str2 second token.
     * @return Length of the longest common prefix.
     */
    private static int longestCommonPrefix(String str1, String str2) {
        for (int i = 0; i < str1.length(); ++i) {
            if (str1.charAt(i) != str2.charAt(i)) {
                return i;
            }
        }
        return str1.length();
    }

    /**
     * Concatenates every k tokens into a single string.
     */
    private void concatenateTokens() {
        String[] terms = parser.getSortedTerms();

        StringBuilder str = new StringBuilder(terms[0]);
        int commonPrefix;

        lengths[0] = str.length();
        prefixLengths[0] = 0;

        for (int i = 1; i < terms.length; ++i) {
            String cur = terms[i];
            String prev = terms[i - 1];
            commonPrefix = longestCommonPrefix(prev, cur);
            lengths[i] = cur.length();

            if (i % k == 0) {
                concatenatedStrings[i / k - 1] = str.append("\n").toString();
                prefixLengths[i] = 0;
                str = new StringBuilder(cur);
            } else {
                prefixLengths[i] = commonPrefix;
                str.append(cur.substring(commonPrefix)); // concat suffix
            }
        }
        concatenatedStrings[concatenatedStrings.length - 1] = str.toString();
    }

    /**
     * Reads postings list from temp file.
     *
     * @param termId termId
     * @throws IOException IOException.
     */
    private void writePostingsFrequencyList(int termId) throws IOException {
        int repeats = 1;
        int reviewFrequency = 1, frequency = 1;
        int curDocId, prevDocId, curTermId;

        if (termId == 0) {
            postingsListsInput.readInt(); // term Id;
        }
        curDocId = postingsListsInput.readInt(); // first doc Id
        postingsListsOutput.write(GammaEncoder.encode(curDocId));

        while (true) {
            try {
                curTermId = postingsListsInput.readInt();
                if (curTermId != termId) {    // finished posting list
                    break;
                }
                prevDocId = curDocId;
                curDocId = postingsListsInput.readInt();

                int gap = curDocId - prevDocId; // converting to a gap list

                if (gap == 0) {     // token appears more than once in the same review.
                    repeats++;
                } else {
                    postingsListsOutput.write(GammaEncoder.encode(repeats));
                    postingsListsOutput.write(GammaEncoder.encode(gap));
                    reviewFrequency++;
                    repeats = 1;
                }
                frequency++;

            } catch (EOFException eof) {
                break;
            }
        }
        postingsListsOutput.write(GammaEncoder.encode(repeats));
        reviewFreq[termId] = reviewFrequency;
        freq[termId] = frequency;
    }

    /**
     * Reads postings list from temp file.
     *
     * @throws IOException IOException.
     */
    private void writePostings() throws IOException {
        for (int i = 0; i < numTerms; ++i) {
            postingListPointers[i] = postingsListsOutput.size();
            writePostingsFrequencyList(i);
        }
        postingsListsInput.close();
        postingsListsOutput.close();
    }

    /**
     * @return Returns the number of terms.
     */
    int getNumOfTerm() {
        return numTerms;
    }

    /**
     * @return Returns the number of tokens.
     */
    int getNumOfTokens() {
        return numTokens;
    }

    /**
     * @return Returns the termId'th token frequency.
     */
    int getTokenFreq(int termId) {
        return freq[termId];
    }

    /**
     * @return Returns the termId'th token review frequency.
     */
    int getTokenReviewFreq(int termId) {
        return reviewFreq[termId];
    }

    /**
     * @return Returns the termId'th posting list pointer.
     */
    long getPostingListPointer(int termId) {
        return postingListPointers[termId];
    }

    /**
     * @return Returns the termId'th token Length.
     */
    int getTokenLength(int termId) {
        return lengths[termId];
    }

    /**
     * @return Returns the termId'th token prefix length.
     */
    int getTokenPrefix(int termId) {
        return prefixLengths[termId];
    }

    /**
     * @return Returns the i'th block concatenated string.
     */
    String getConcatenatedString(int blockIdx) {
        return concatenatedStrings[blockIdx];
    }
}
