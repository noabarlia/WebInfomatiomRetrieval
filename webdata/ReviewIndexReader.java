package webdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * ReviewIndexReader class.
 */
class ReviewIndexReader {

    /**
     * --- FIELDS ---
     */
    private static final int REVIEW_INDEX_ROW_SIZE = 15; // size in bytes
    private static final String REVIEW_INDEX_FILENAME = "/review_metadata_index";
    private RandomAccessFile reviewIndexFile;

    /**
     * Review Index Reader constructor.
     */
    ReviewIndexReader(String dir) {
        try {
            this.reviewIndexFile = new RandomAccessFile(dir + REVIEW_INDEX_FILENAME, "r");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the product identifier for the given review
     * Returns null if there is no review with the given identifier
     */
    String getProductId(int reviewId) {
        int productIdLength = 10;
        int productOffset = 0;
        byte[] buffer;

        try {
            buffer = read(reviewId, productIdLength, productOffset);
        } catch (IOException e) {
            return null;
        }

        return new String(buffer);
    }

    /**
     * Returns the score for a given review
     * Returns -1 if there is no review with the given identifier
     */
    int getReviewScore(int reviewId) {
        int scoreLength = 1;
        int scoreOffset = 10;
        byte[] buffer;

        try {
            buffer = read(reviewId, scoreLength, scoreOffset);
        } catch (IOException e) {
            return -1;
        }

        return buffer[0]; // score is 1 byte
    }

    /**
     * Returns the numerator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    int getReviewHelpfulnessNumerator(int reviewId) {
        int HelpfulnessNumeratorLength = 1;
        int HelpfulnessNumeratorOffset = 11;
        byte[] buffer;

        try {
            buffer = read(reviewId, HelpfulnessNumeratorLength,
                    HelpfulnessNumeratorOffset);
        } catch (IOException e) {
            return -1;
        }

        return buffer[0]; // Helpfulness numerator is 1 byte
    }

    /**
     * Returns the denominator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    int getReviewHelpfulnessDenominator(int reviewId) {
        int HelpfulnessDenominatorLength = 1;
        int HelpfulnessDenominatorOffset = 12;
        byte[] buffer;

        try {
            buffer = read(reviewId, HelpfulnessDenominatorLength,
                    HelpfulnessDenominatorOffset);
        } catch (IOException e) {
            return -1;
        }

        return buffer[0]; // Helpfulness denominator is 1 byte
    }

    /**
     * Returns the number of tokens in a given review
     * Returns -1 if there is no review with the given identifier
     */
    int getReviewLength(int reviewId) {
        int TokenCountLength = 2;
        int TokenCountOffset = 13;
        byte[] buffer;

        try {
            buffer = read(reviewId, TokenCountLength,
                    TokenCountOffset);
        } catch (IOException e) {
            return -1;
        }

        ByteBuffer bb = ByteBuffer.allocate(TokenCountLength);
        bb.put(buffer[0]);
        bb.put(buffer[1]);
        return bb.getShort(0);
    }

    /**
     * Return the number of product reviews available in the system
     */
    int getNumberOfReviews() {
        try {
            return (int) (reviewIndexFile.length() / REVIEW_INDEX_ROW_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Reads from review metadata index and returns read buffer.
     *
     * @param reviewId review ID.
     * @param offset   row offset index file.
     * @param len      length of the attribute in bytes.
     * @return returns read buffer.
     */
    private byte[] read(int reviewId, int len, int offset) throws IOException {
        byte[] buffer = new byte[len];
        reviewIndexFile.seek(REVIEW_INDEX_ROW_SIZE * (reviewId - 1) + offset);
        reviewIndexFile.readFully(buffer);
        return buffer;
    }
}
