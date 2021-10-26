package webdata;

import java.io.*;
import java.util.*;
import java.util.List;

class DictionaryReader {

    /**
     * Record Class.
     */
    class Record {
        int reviewFreq;
        int collectionFreq;
        long postingsListPtr;
        int length;
        int prefix;

        /**
         * Record Constructor.
         */
        Record(int reviewFreq, int collectionFreq, long postingsListPtr, int length, int prefix) {
            this.reviewFreq = reviewFreq;
            this.collectionFreq = collectionFreq;
            this.postingsListPtr = postingsListPtr;
            this.length = length;
            this.prefix = prefix;
        }
    }

    /**
     * ---- CONSTANTS ----
     **/
    private static final String TOKENS_FRONT_CODING_INDEX = "/tokens_front_coding_index";
    private static final String POSTINGS_LISTS = "/postings_lists";
    private static final String CONCATENATED_TOKENS = "/concatenated_tokens";

    /**
     * ---- FIELDS ----
     **/
    private int k;
    private int numTerms;
    private int numTokens;

    private Record[] records;
    private ArrayList<String> terms;

    private DictionaryDecoder dictionaryDecoder;
    private DataInputStream frontCodingIndex;
    private BufferedReader concatenatedTokens;
    private RandomAccessFile postingsLists;     // TODO: maybe change to buffered input reader


    /**
     * DictionaryReader constructor.
     *
     * @param dir    directory.
     * @param kValue k value.
     */
    DictionaryReader(String dir, int kValue) {
        k = kValue;
        dictionaryDecoder = new DictionaryDecoder();

        try {
            frontCodingIndex = new DataInputStream(new BufferedInputStream(new FileInputStream(
                    dir + TOKENS_FRONT_CODING_INDEX)));
            concatenatedTokens = new BufferedReader(new FileReader(dir + CONCATENATED_TOKENS));
            postingsLists = new RandomAccessFile(dir + POSTINGS_LISTS, "r");

            numTerms = frontCodingIndex.readInt();
            numTokens = frontCodingIndex.readInt();

            records = new Record[numTerms];
            terms = new ArrayList<>();

            readFrontCodingIndex();
            readTerms();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads one record (reviewFreq, collectionFreq, postings list ptr, length, term ptr, prefix) from the disk.
     *
     * @param offset offset within block.
     * @return Record.
     * @throws IOException IOException.
     */
    private Record readRecord(int offset) throws IOException {
        int reviewFreq, collectionFreq, length = 0, prefix = 0;
        long postingsListPtr;

        reviewFreq = frontCodingIndex.readInt();
        collectionFreq = frontCodingIndex.readInt();
        postingsListPtr = frontCodingIndex.readLong();

        if (offset != k - 1) {
            length = frontCodingIndex.readShort();
        }
        if (offset != 0) {
            prefix = frontCodingIndex.readShort();
        }
        return new Record(reviewFreq, collectionFreq, postingsListPtr, length, prefix);
    }

    /**
     * Reads front coding index file into an array of Records.
     *
     * @throws IOException IOException.
     */
    private void readFrontCodingIndex() throws IOException {
        for (int i = 0; i < numTerms; i++) {
            records[i] = readRecord(i % k);
        }
    }

    /**
     * Reads the concatenated string from the concatenated_tokens file,
     * starting from termPtr until new line or EOF is reached.
     *
     * @return the concatenated tokens string.
     * @throws IOException IOException.
     */
    private String readConcatString() throws IOException {
        String temp = concatenatedTokens.readLine();
        String line = "";
        for (int i = 1; i < temp.length(); i += 2) {
            line = line.concat(temp.substring(i, i + 1));
        }
        return line;
    }

    /**
     * Reads all terms from concatenated tokens file into terms array.
     *
     * @throws IOException IOException.
     */
    private void readTerms() throws IOException {
        int blocks = (int) Math.ceil((double) numTerms / k);

        for (int i = 0; i < blocks; ++i) {
            int start = i * k;
            int end = Math.min(numTerms, start + k);
            Record[] block = Arrays.copyOfRange(records, start, end);

            String str = readConcatString();
            terms.addAll(dictionaryDecoder.separateTokens(block, str));
        }
    }

    /**
     * Reads postings frequency list from the postings_lists file,
     * starting from start until end or EOF is reached.
     *
     * @param start start pointer.
     * @param end   end pointer.
     * @return List of postings and frequencies.
     * @throws IOException IOException.
     */
    private ArrayList<Integer> readPostingsList(long start, long end) throws IOException {
        ArrayList<Byte> bytes = new ArrayList<>();

        postingsLists.seek(start);
        while (postingsLists.getFilePointer() != end) {
            bytes.add(postingsLists.readByte());
        }

        ArrayList<Integer> gapList = GammaEncoder.decodeSequence(Utils.convertToArray(bytes));
        return dictionaryDecoder.processGapsFrequencyList(gapList);
    }

    /**
     * Binary search the token token in the K in K - 1 dictionary.
     *
     * @param token token to search for.
     * @param start start index.
     * @param end   end index.
     * @return the record index for this token.
     */
    private int binarySearch(String token, int start, int end) {
        int mid = (start + end) / 2;
        int offset;

        int last = Math.min(numTerms, mid * k + k);
        List<String> tokens = terms.subList(mid * k, last);

        if ((offset = tokens.indexOf(token)) >= 0) {    // token is in cur block
            return mid * k + offset;
        }
        if (token.compareTo(tokens.get(0)) < 0) {   // token can be in prev blocks
            return binarySearch(token, start, mid);
        }
        if (token.compareTo(tokens.get(tokens.size() - 1)) > 0) {   // token can be in following blocks
            return binarySearch(token, mid + 1, end);
        }
        return -1;  // token not in dict
    }

    /**
     * Return a postings and frequencies list for token.
     *
     * @param token token.
     * @return postings and frequencies list.
     * @throws IOException IOException.
     */
    private ArrayList<Integer> getPostingsFrequencyList(String token) throws IOException, IllegalArgumentException {
        int termIdx = binarySearch(token, 0, numTerms / k);
        if (termIdx == -1) {
            throw new IllegalArgumentException();
        }

        Record cur = records[termIdx];
        if (termIdx + 1 == numTerms) {
            return readPostingsList(cur.postingsListPtr, postingsLists.length());
        }
        Record next = records[termIdx + 1];
        return readPostingsList(cur.postingsListPtr, next.postingsListPtr);
    }

    /**
     * Return the number of times that a given token (i.e., word) appears in reviews indexed.
     * Returns 0 if the token not in dictionary or if an error occurred.
     *
     * @param token token.
     */
    int getCollectionFrequency(String token) {
        int recordIdx = binarySearch(token, 0, numTerms / k);
        if (recordIdx < 0) {
            return 0;
        }
        return records[recordIdx].collectionFreq;
    }

    /**
     * Return the number of reviews containing a given token (i.e., word)
     * Returns 0 if there are no reviews containing this token.
     *
     * @param token token.
     */
    int getTokenFrequency(String token) {
        int recordIdx = binarySearch(token, 0, numTerms / k);
        if (recordIdx < 0) {
            return 0;
        }
        return records[recordIdx].reviewFreq;
    }

    /**
     * Return a series of integers of the form id-1, collectionFreq-1, id-2, collectionFreq-2, ... such
     * that id-n is the n-th review containing the given token and collectionFreq-n is the
     * number of times that the token appears in review id-n
     * Only return ids of reviews that include the token
     * Returns an empty Enumeration if there are no reviews containing this token.
     *
     * @param token token.
     * @return Enumeration.
     */
    Enumeration<Integer> getPostingsFrequencyEnumeration(String token) {
        try {
            return Collections.enumeration(getPostingsFrequencyList(token));
        } catch (IOException | IllegalArgumentException e) {
            return Collections.emptyEnumeration();
        }
    }

    /**
     * Returns the number of tokens in the dictionary (includes repeats).
     */
    int getNumOfTokens() {
        return numTokens;
    }
}
