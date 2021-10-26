package webdata;

/**
 * BlockEntry class
 */
public class BlockEntry implements Comparable {

    /**
     * ---- FIELDS ----
     **/
    private int termId;
    private int docId;
    private int sequenceNumber;


    /**
     * Constructor.
     *
     * @param termId         term Id.
     * @param docId          document Id.
     * @param sequenceNumber sequence number.
     */
    BlockEntry(int termId, int docId, int sequenceNumber) {
        this.termId = termId;
        this.docId = docId;
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Returns term Id of block entry.
     */
    int getTermId() {
        return this.termId;
    }

    /**
     * Returns document Id of block entry.
     */
    int getDocId() {
        return this.docId;
    }

    /**
     * Returns the sequence number of block entry.
     */
    int getSequenceNumber() {
        return this.sequenceNumber;
    }

    /**
     * Comparator for Block Entry.
     *
     * @param o BlockEntry.
     */
    @Override
    public int compareTo(Object o) {
        BlockEntry entry = (BlockEntry) o;
        int res = Integer.compare(this.termId, entry.getTermId());
        if (res == 0) res = Integer.compare(this.docId, entry.getDocId());
        return res;
    }
}
