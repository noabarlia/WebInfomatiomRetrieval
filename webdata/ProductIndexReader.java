package webdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * ProductIndexReader class.
 */
class ProductIndexReader {

    /**
     * --- FIELDS ---
     */
    private static final int PRODUCT_INDEX_ROW_SIZE = 18; // size in bytes
    private static final String PRODUCT_INDEX_FILENAME = "/product_index";
    private static final int PID_LENGTH = 10;
    private byte[] indexBuffer;
    private RandomAccessFile productIndexFile;

    /**
     * Constructor.
     *
     * @param dir directory.
     */
    ProductIndexReader(String dir) {
        try {
            this.productIndexFile = new RandomAccessFile(dir + PRODUCT_INDEX_FILENAME, "r");
            this.indexBuffer = new byte[(int) this.productIndexFile.length()];
            this.productIndexFile.readFully(indexBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads product id.
     *
     * @param index index.
     */
    private String readPid(int index) {
        int start = index * PRODUCT_INDEX_ROW_SIZE;
        int end = start + PID_LENGTH;
        return new String(getIndexBufferSlice(start, end));
    }

    /**
     * Searches for productId in productId index and returns index of line
     * in file, if not found returns -1.
     *
     * @param productId the search term.
     * @return index of productId in file, -1 if not found.
     */
    private int binarySearch(String productId) throws IOException {
        int n = (int) productIndexFile.length() / PRODUCT_INDEX_ROW_SIZE;
        int start = 0;
        int end = n;

        while (start < end) {
            int mid = (start + end) / 2;

            if (productId.compareTo(readPid(mid)) > 0) {
                start = mid + 1;
            } else {
                end = mid;
            }
        }
        return ((start < n) && (readPid(start).equals(productId))) ? start : -1;
    }

    /**
     * Returns a slice from Index buffer.
     *
     * @param start starting index.
     * @param end   end index.
     * @return buffer slice of given range.
     */
    private byte[] getIndexBufferSlice(int start, int end) {
        return Arrays.copyOfRange(this.indexBuffer, start, end);
    }

    /**
     * Parses int from given bytes array.
     *
     * @param buffer bytes buffer
     * @return parsed int
     */
    private int parseIntFromBytes(byte[] buffer) {
        assert buffer.length == 4;
        return ByteBuffer.wrap(buffer).getInt();
    }

    /**
     * Returns reviews from product Index File located in given lineIndex.
     *
     * @param lineIndex index of line to get reviews from.
     * @return ArrayList of review IDs contain in given line.
     */
    private ArrayList<Integer> getReviews(int lineIndex) {
        int start = PRODUCT_INDEX_ROW_SIZE * lineIndex + PID_LENGTH;
        int end = start + Integer.BYTES;
        int firstReviewIndex = parseIntFromBytes(getIndexBufferSlice(start, end));
        start = PRODUCT_INDEX_ROW_SIZE * lineIndex + PID_LENGTH + Integer.BYTES;
        end = start + Integer.BYTES;
        int size = parseIntFromBytes(getIndexBufferSlice(start, end));
        ArrayList<Integer> reviewIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            reviewIds.add(firstReviewIndex + i);
        }
        return reviewIds;
    }

    /**
     * Return the ids of the reviews for a given product identifier
     * Note that the integers returned should be sorted by id
     *
     * Returns an empty Enumeration if there are no reviews for this product
     */
    Enumeration<Integer> getProductReviews(String productId) {
        try {
            int idx = binarySearch(productId);
            if (idx == -1) {
                return Collections.emptyEnumeration();
            }

            return Collections.enumeration(getReviews(idx));

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyEnumeration();
        }
    }
}
