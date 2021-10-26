package webdata;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Class Parser
 */
class Parser {
    /**
     * ---- CONSTANTS ----
     **/
    private static final String PRODUCT_ID_PATTERN = "(product/productId: )(.*)";
    private static final String HELPFULNESS_PATTERN = "(review/helpfulness: )(.*)";
    private static final String SCORE_PATTERN = "(review/score: )(.*)";
    private static final String TEXT_PATTERN = "(review/text: )(.*)";
    private static final String NON_ALPHANUMERIC = "[^a-zA-Z0-9]++";
    private static final int NUM_PATTERNS = 4;
    private static final int PRODUCT_ID = 0;
    private static final int HELPFULNESS = 1;
    private static final int SCORE = 2;
    private static final int TEXT = 3;
    private static final int CONTENT_GROUP = 2;
    private static final int NUMERATOR = 0;
    private static final int DENOMINATOR = 1;
    private static final int BATCH_SIZE = 1000000;

    /**
     * ---- PATTERNS ----
     **/
    private static Pattern productIdPattern = Pattern.compile(PRODUCT_ID_PATTERN);
    private static Pattern helpfulnessPattern = Pattern.compile(HELPFULNESS_PATTERN);
    private static Pattern scorePattern = Pattern.compile(SCORE_PATTERN);
    private static Pattern textPattern = Pattern.compile(TEXT_PATTERN);

    /**
     * ---- MAP (TERM, TERM-ID) ----
     **/
    private Map<String, Integer> termTermIdMap = new HashMap<>();

    /**
     * ---- FIELDS ----
     **/
    private BufferedReader reader;
    private ExternalSort sorter;
    private String filePath;
    private int matcherCounter = 0;
    private int reviewId = 0;
    private int numTokens = 0;
    private int pairIdx = 0;

    private HashSet<String> terms = new HashSet<>();
    private ArrayList<Integer> tokensCounters = new ArrayList<>();
    private ArrayList<String> productIds = new ArrayList<>();
    private ArrayList<webdata.Pair<Integer, Integer>> helpfulness = new ArrayList<>();
    private ArrayList<Integer> scores = new ArrayList<>();

    private int[][] termIdDocIdPairs = null;
    private String[] sortedTerms;

    /**
     * Parser Constructor.
     *
     * @param filepath filepath to the input review file.
     */
    Parser(String filepath, ExternalSort sorter) {
        try {
            if (filepath.endsWith(".gz")) {
                GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(filepath));
                reader = new BufferedReader(new InputStreamReader(gzip));
            } else {
                reader = new BufferedReader(new FileReader(filepath));
            }
            this.filePath = filepath;
            this.sorter = sorter;
        } catch (Exception e) {
            reader = null;
        }
    }

    /**
     * Returns the next line in the input file, returns null if EOF is reached.
     *
     * @return String line
     */
    private String readLine() {
        try {
            return reader.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Read documents for ReviewIndex and ProductIndex, creates (term,termId) map in memory ----

    /**
     * Returns the next pattern for the parser to look for in the input file,
     * according to the reviews structure.
     *
     * @return Pattern next pattern to look for.
     */
    private Pattern nextPattern() {
        switch (matcherCounter % NUM_PATTERNS) {
        case PRODUCT_ID:
            return Parser.productIdPattern;
        case HELPFULNESS:
            return Parser.helpfulnessPattern;
        case SCORE:
            return Parser.scorePattern;
        default:
            return Parser.textPattern;
        }
    }

    /**
     * Adds review id to productIds array.
     *
     * @param str id string
     */
    private void addId(String str) {
        reviewId++;
        productIds.add(str);
    }

    /**
     * Adds helpfulness to helpfulness array.
     *
     * @param str helpfulness string
     */
    private void addHelpfulness(String str) {
        String[] helpfulness = str.split(NON_ALPHANUMERIC);
        int numerator = Integer.parseInt(helpfulness[NUMERATOR]);
        int denominator = Integer.parseInt(helpfulness[DENOMINATOR]);
        this.helpfulness.add(new Pair<>(numerator, denominator));
    }

    /**
     * Adds score to score array.
     *
     * @param str score string
     */
    private void addScore(String str) {
        int score = Integer.parseInt(str.split(NON_ALPHANUMERIC)[0]);
        scores.add(score);
    }

    /**
     * Returns a sorted set of terms containing all terms from the text string.
     *
     * @param str text string
     * @return Terms sorted set
     */
    private void addTerms(String str) {
        int curTokenCounter = 0;

        String[] tokens = str.split(NON_ALPHANUMERIC, 0);

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            terms.add(token.toLowerCase());
            curTokenCounter++;
        }
        this.tokensCounters.add(curTokenCounter);
    }

    /**
     * Creates term to termId map in memory.
     */
    private void createTermToTermIdMap() {
        sortedTerms = new String[terms.size()];
        terms.toArray(sortedTerms);
        terms = null;
        Runtime.getRuntime().gc();
        Arrays.parallelSort(sortedTerms);

        int termId = 0;
        for (String term : sortedTerms) {
            termTermIdMap.put(term, termId++);
        }
    }

    /**
     * Parse the input file.
     */
    boolean parseFile() {
        Matcher m;
        String line, match;
        int type;
        clearMetaData();

        while ((line = readLine()) != null) {
            m = nextPattern().matcher(line);
            if (m.matches()) {
                type = matcherCounter % NUM_PATTERNS;
                match = m.group(CONTENT_GROUP);
                switch (type) {
                case PRODUCT_ID:
                    addId(match);
                    break;
                case HELPFULNESS:
                    addHelpfulness(match);
                    break;
                case SCORE:
                    addScore(match);
                    break;
                case TEXT:
                    addTerms(match);
                    if (reviewId % BATCH_SIZE == 0) {
                        matcherCounter++;
                        return true;
                    }
                }
                matcherCounter++;
            }
        }
        return false;
    }

    // ---- Read documents for Dictionary ----

    /**
     * Clears all data in the parser.
     */
    private void clearMetaData() {
        helpfulness.clear();
        scores.clear();
        tokensCounters.clear();
    }

    /**
     * Clears all structures in the parser.
     */
    void clearReviewIndexStructs() {
        tokensCounters = null;
        helpfulness = null;
        scores = null;
        Runtime.getRuntime().gc();
    }

    /**
     * Resets parser for second pass.
     *
     * @throws IOException
     */
    private void resetParser() throws IOException {
        productIds = null;
        reviewId = 0;
        if (filePath.endsWith(".gz")) {
            GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(filePath));
            reader = new BufferedReader(new InputStreamReader(gzip));
        } else {
            reader = new BufferedReader(new FileReader(filePath));
        }
    }

    /**
     * @return True if the memory is full, else returns false.
     */
    private boolean isMemoryFull() {
        return numTokens > 0 && numTokens % ExternalSort.NUM_PAIRS == 0;
    }

    /**
     * Adds (tokens, posting lists) pair to the posting list HashMap for each token in the review text,
     * or update an existing token's posting list. If the memory is full, flush data to disk.
     *
     * @param str text string
     */
    private void addToPostingsList(String str) throws IOException {
        String[] tokens = str.split(NON_ALPHANUMERIC, 0);

        for (String token : tokens) {
            // flush to disk
            if (isMemoryFull()) {
                sorter.writeSorted(termIdDocIdPairs, pairIdx);
                pairIdx = 0;
            }

            if (token.isEmpty()) continue;

            String lower = token.toLowerCase();
            termIdDocIdPairs[pairIdx][0] = termTermIdMap.get(lower);
            termIdDocIdPairs[pairIdx][1] = reviewId;

            pairIdx++;
            numTokens++;
        }
    }

    /**
     * Reads documents and writes (termId, docId) pairs to disk.
     */
    void parsePostingsLists() throws IOException {
        createTermToTermIdMap();
        resetParser();
        Matcher m;
        String line, match;
        termIdDocIdPairs = new int[ExternalSort.NUM_PAIRS][2];

        while ((line = this.readLine()) != null) {
            m = textPattern.matcher(line);

            if (m.matches()) {
                match = m.group(CONTENT_GROUP);
                addToPostingsList(match);
            } else if (productIdPattern.matcher(line).matches()) {
                reviewId++;
            }
        }
        sorter.writeSorted(termIdDocIdPairs, pairIdx);
        termIdDocIdPairs = null;
    }

    // ---- getters ----

    /**
     * Returns the number of terms in the dictionary.
     *
     * @return number of terms.
     */
    int getNumOfTerms() {
        return termTermIdMap.size();
    }

    /**
     * Returns the number of tokens in the dictionary.
     *
     * @return number of terms.
     */
    int getNumOfTokens() {
        return numTokens;
    }

    /**
     * Return number of reviews parsed.
     *
     * @return number of total review parsed.
     */
    int getNumOfReviews() {
        return reviewId;
    }

    /**
     * Returns an array containing the number of tokens in each review.
     *
     * @return Tokens counter array.
     */
    ArrayList<Integer> getTokensPerReview() {
        return tokensCounters;
    }

    /**
     * Returns an array of products productIds.
     *
     * @return productIds.
     */
    ArrayList<String> getProductIds() {
        return productIds;
    }

    /**
     * Returns an array of reviews helpfulness.
     *
     * @return helpfulness.
     */
    ArrayList<Pair<Integer, Integer>> getHelpfulness() {
        return helpfulness;
    }

    /**
     * Returns an array of reviews scores.
     *
     * @return helpfulness.
     */
    ArrayList<Integer> getScores() {
        return scores;
    }

    /**
     * Returns sorted terms.
     */
    String[] getSortedTerms() {
        return sortedTerms;
    }
}