package webdata;

import java.io.*;

import java.util.*;

/**
 * ExternalSort class.
 */
class ExternalSort {

    /**
     * ---- CONSTANTS ----
     **/
    private static final String dir = "/tmp/ExternalSort_tmp";
    private static final String TMP_FILENAME = "sortedPairs_tmp";
    private static final String OUTPUT_FILENAME = "mergedPairs_tmp";

    /**
     * ---- FIELDS ----
     **/
    private static final int SEQUENCE_SIZE_BYTES = (int) (160 * (1L << 20)); // 160MB
    private static final int SEQUENCE_SIZE = (int) (1L << 7); // num of blocks in sequence
    private static final int BLOCK_SIZE_BYTES = SEQUENCE_SIZE_BYTES / SEQUENCE_SIZE; // in bytes
    private static final int BLOCK_SIZE = BLOCK_SIZE_BYTES / Integer.BYTES; // ints in block
    static final int NUM_PAIRS = SEQUENCE_SIZE_BYTES / (Integer.BYTES * 2);

    private DataInputStream[] sequences;

    private int numOfSequences;

    /**
     * Constructor.
     *
     * @throws IOException
     */
    ExternalSort() throws IOException {
        Utils.createDirectory(dir);
        numOfSequences = 0;
    }

    /**
     * Writes sorted termId,docId pairs to a temporary file.
     *
     * @param termIdDocIdPairs mapping of termIDs to docIDs.
     * @throws IOException
     */
    void writeSorted(int[][] termIdDocIdPairs, int n) throws IOException {
        Arrays.sort(termIdDocIdPairs, 0, n, Comparator.comparingInt(a -> a[0]));

        DataOutputStream sortedPairTemp = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(dir + "/" + TMP_FILENAME + numOfSequences)));
        numOfSequences++;

        for (int i = 0; i < n; i++) {
            sortedPairTemp.writeInt(termIdDocIdPairs[i][0]);
            sortedPairTemp.writeInt(termIdDocIdPairs[i][1]);
        }
        sortedPairTemp.close();
    }

    /**
     * Creates large index by merging all small indices.
     *
     * @param n total number of pairs.
     * @throws IOException
     */
    void mergeSortedPairs(long n) throws IOException {

        DataOutputStream outputFile = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(dir + "/" + OUTPUT_FILENAME)));

        PriorityQueue<BlockEntry> blockEntryPQ = new PriorityQueue<>(numOfSequences);
        init(blockEntryPQ);
        long k = 0;
        long numIntegers = 2 * n;
        int[] outputBlock = new int[BLOCK_SIZE];

        while (k < numIntegers) {

            BlockEntry entry = blockEntryPQ.poll();
            outputBlock[(int) (k % BLOCK_SIZE)] = entry.getTermId();
            outputBlock[(int) ((k + 1) % BLOCK_SIZE)] = entry.getDocId();

            k += 2;

            if (k % BLOCK_SIZE == 0) {
                flushBlock(outputFile, outputBlock, BLOCK_SIZE);
            }
            addNextBlockEntry(entry.getSequenceNumber(), blockEntryPQ);
        }
        flushBlock(outputFile, outputBlock, (int) (k % BLOCK_SIZE));
        outputFile.close();
        closeInputs();
        Runtime.getRuntime().gc();
    }

    void clear() {
        Utils.deleteDirectory(dir);
    }


    /**
     * Adds next entry in block of given sequence to priority queue of BlockEntries.
     *
     * @param sequenceIndex given sequence to read from.
     * @param blockEntryPQ  priority queue of BlockEntries.
     */
    private void addNextBlockEntry(int sequenceIndex, PriorityQueue<BlockEntry> blockEntryPQ)
            throws IOException {

        try {
            int termId = sequences[sequenceIndex].readInt();
            int docId = sequences[sequenceIndex].readInt();
            blockEntryPQ.add(new BlockEntry(termId, docId, sequenceIndex));

        } catch (EOFException e) {
            sequences[sequenceIndex].close();
            File file = new File(dir + "/" + TMP_FILENAME + sequenceIndex);
            file.delete();
        }
    }

    /**
     * Read first block of each sequence and initialize BlockEntryPQ with first block entries.
     *
     * @param blockEntryPQ priority queue of BlockEntries.
     * @throws IOException
     */
    private void init(PriorityQueue<BlockEntry> blockEntryPQ) throws IOException {
        String tempFilePath = dir + "/" + TMP_FILENAME;

        sequences = new DataInputStream[numOfSequences];

        for (int i = 0; i < numOfSequences; i++) {
            sequences[i] = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(tempFilePath + i)));
            addNextBlockEntry(i, blockEntryPQ);
        }
    }

    /**
     * Closes all inputs files.
     *
     * @throws IOException
     */
    private void closeInputs() throws IOException {
        for (int i = 0; i < numOfSequences; i++) {
            sequences[i].close();    //    private final DataOutputStream sortedPairTMP;
            sequences[i] = null;
        }
        sequences = null;
    }

    /**
     * Flushes output block to given file in disk.
     */
    private void flushBlock(DataOutputStream outputFile, int[] outputBlock, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            outputFile.writeInt(outputBlock[i]);
        }
    }
}
