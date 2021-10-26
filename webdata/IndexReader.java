package webdata;

import java.util.Enumeration;

public class IndexReader {

    /**
     * ---- FILES ----
     **/
    private DictionaryReader dictionaryReader;
    private ReviewIndexReader reviewIndexReader;
    private ProductIndexReader productIndexReader;

    /**
     * Creates an IndexReader which will read from the given directory
     */
    public IndexReader(String dir) {
        dictionaryReader = new DictionaryReader(dir, 10);
        reviewIndexReader = new ReviewIndexReader(dir);
        productIndexReader = new ProductIndexReader(dir);
    }

    /**
     * Returns the product identifier for the given review
     * Returns null if there is no review with the given identifier
     */
    public String getProductId(int reviewId) {
        return reviewIndexReader.getProductId(reviewId);
    }

    /**
     * Returns the score for a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewScore(int reviewId) {
        return reviewIndexReader.getReviewScore(reviewId);
    }

    /**
     * Returns the numerator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessNumerator(int reviewId) {
        return reviewIndexReader.getReviewHelpfulnessNumerator(reviewId);
    }

    /**
     * Returns the denominator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessDenominator(int reviewId) {
        return reviewIndexReader.getReviewHelpfulnessDenominator(reviewId);
    }

    /**
     * Returns the number of tokens in a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewLength(int reviewId) {
        return reviewIndexReader.getReviewLength(reviewId);
    }

    /**
     * Return the number of reviews containing a given token (i.e., word)
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenFrequency(String token) {
        String lower = token.toLowerCase();
        return dictionaryReader.getTokenFrequency(lower);
    }

    /**
     * Return the number of times that a given token (i.e., word) appears in
     * the reviews indexed
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenCollectionFrequency(String token) {
        String lower = token.toLowerCase();
        return dictionaryReader.getCollectionFrequency(lower);
    }

    /**
     * Return a series of integers of the form id-1, freq-1, id-2, freq-2, ... such
     * that id-n is the n-th review containing the given token and freq-n is the
     * number of times that the token appears in review id-n
     * Only return ids of reviews that include the token
     * Note that the integers should be sorted by id
     * <p>
     * Returns an empty Enumeration if there are no reviews containing this token
     */
    public Enumeration<Integer> getReviewsWithToken(String token) {
        String lower = token.toLowerCase();
        return dictionaryReader.getPostingsFrequencyEnumeration(lower);
    }

    /**
     * Return the number of product reviews available in the system
     */
    public int getNumberOfReviews() {
        return reviewIndexReader.getNumberOfReviews();
    }

    /**
     * Return the number of tokens in the system
     * (Tokens should be counted as many times as they appear)
     */
    public int getTokenSizeOfReviews() {
        return dictionaryReader.getNumOfTokens();
    }

    /**
     * Return the ids of the reviews for a given product identifier
     * Note that the integers returned should be sorted by id
     * <p>
     * Returns an empty Enumeration if there are no reviews for this product
     */
    public Enumeration<Integer> getProductReviews(String productId) {
        return productIndexReader.getProductReviews(productId);
    }


}