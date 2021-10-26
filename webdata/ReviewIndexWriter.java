package webdata;

import java.io.*;

/**
 * ReviewIndexWriter Class
 */
class ReviewIndexWriter {

    private static final String REVIEW_INDEX_FILENAME = "/review_metadata_index";
    private Parser parser;
    private int reviewsWrittenCount = 0;
    private DataOutputStream reviewIndexFile;

    /**
     * ReviewIndexWriter constructor.
     *
     * @param parser Parser object.
     * @param dir    directory to save files to.
     */
    ReviewIndexWriter(Parser parser, String dir) throws IOException {
        this.parser = parser;
        reviewIndexFile = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(dir + REVIEW_INDEX_FILENAME)));
    }

    /**
     * Writes review metadata to file.
     */
    void write() throws IOException {

        for (int i = 0; i < parser.getNumOfReviews() - reviewsWrittenCount; ++i) {
            reviewIndexFile.writeBytes(parser.getProductIds().get(i + reviewsWrittenCount)); // 10 bytes productID
            reviewIndexFile.writeByte(parser.getScores().get(i));      // 1
            int numerator = parser.getHelpfulness().get(i).getL();
            int denominator = parser.getHelpfulness().get(i).getR();
            reviewIndexFile.writeByte(numerator);                              // 1
            reviewIndexFile.writeByte(denominator);                              // 1
            reviewIndexFile.writeShort(parser.getTokensPerReview().get(i)); // review length 2 bytes
            // total 15 bytes metadata per review
        }
        reviewsWrittenCount = parser.getNumOfReviews();
    }

    void close() throws IOException {
        reviewIndexFile.close();
    }
}
