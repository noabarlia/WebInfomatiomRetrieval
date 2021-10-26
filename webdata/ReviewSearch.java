package webdata;

import java.util.*;
import java.util.stream.Collectors;

public class ReviewSearch {

    private IndexReader indexReader;

    /**
     * Constructor
     */
    public ReviewSearch(IndexReader iReader) {
        indexReader = iReader;
    }

    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the vector space ranking function lnn.ltc (using the
     * SMART notation)
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> vectorSpaceSearch(Enumeration<String> query, int k) {

        Map<String, Integer> termInQueryCounter = new HashMap<>();

        buildHist(query, termInQueryCounter);

        String[] terms = getTermList(termInQueryCounter);

        double[] queryScores = getQueryScores(termInQueryCounter, terms);

        Map<Integer, Double> reviewIdToScoreMap = new HashMap<>();
        for (int i = 0; i < terms.length; i++) {
            Enumeration<Integer> postingList = indexReader.getReviewsWithToken(terms[i]);
            while (postingList.hasMoreElements()) {
                int reviewId = postingList.nextElement();
                int freq = postingList.nextElement();
                double termScore = reviewIdToScoreMap.getOrDefault(reviewId, 0.0);
                termScore += (Math.log10(freq) + 1) * queryScores[i];
                reviewIdToScoreMap.put(reviewId, termScore);
            }
        }

        return getBestKSortedKeys(reviewIdToScoreMap, k);
    }

    /**
     * Build histogram for given query i.e, calculate each term's num of appearances.
     *
     * @param query              Given query.
     * @param termInQueryCounter Histogram to fill.
     */
    private void buildHist(Enumeration<String> query, Map<String, Integer> termInQueryCounter) {
        while (query.hasMoreElements()) {
            String term = query.nextElement();
            int count = termInQueryCounter.getOrDefault(term, 0);
            termInQueryCounter.put(term, count + 1);
        }
    }

    /**
     * Extract the term list from the given histogram.
     *
     * @param termInQueryCounter Histogram.
     * @return List of the terms in the histogram.
     */
    private String[] getTermList(Map<String, Integer> termInQueryCounter) {
        Set<String> keySet = termInQueryCounter.keySet();
        String[] terms = new String[keySet.size()];
        keySet.toArray(terms);
        return terms;
    }

    /**
     * Calculates normalized query scores array.
     *
     * @param termInQueryCounter Mapping of string to its number of appearances in the given query.
     * @param terms              The terms in the given query with no duplicates.
     */
    private double[] getQueryScores(Map<String, Integer> termInQueryCounter, String[] terms) {
        int N = indexReader.getTokenSizeOfReviews();
        double[] queryScores = new double[terms.length];
        double sum = 0;
        for (int i = 0; i < terms.length; i++) {
            double df = indexReader.getTokenCollectionFrequency(terms[i]);
            if (df == 0) continue;
            queryScores[i] = Math.log10(N / df) * (Math.log10(termInQueryCounter.get(terms[i])) + 1);
            sum += Math.pow(queryScores[i], 2);
        }

        double norm = Math.sqrt(sum);

        for (int i = 0; i < queryScores.length; i++) {
            queryScores[i] = queryScores[i] / norm;
        }
        return queryScores;
    }

    /**
     * Calculates and returns the smoothing vector for this query.
     *
     * @param terms  Query terms.
     * @param lambda Lambda value.
     * @param N      Number os tokens in the corpus.
     */
    private double[] calcSmoothingVec(String[] terms, double lambda, int N) {
        double[] smoothingVec = new double[terms.length];
        double p;
        int termFreq;

        for (int i = 0; i < terms.length; i++) {
            termFreq = indexReader.getTokenCollectionFrequency(terms[i]);
            p = (double) termFreq / N;
            smoothingVec[i] = (1 - lambda) * (p);
        }

        return smoothingVec;
    }

    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the language model ranking function, smoothed using a
     * mixture model with the given value of lambda
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> languageModelSearch(Enumeration<String> query, double lambda, int k) {
        int N = indexReader.getTokenSizeOfReviews();
        Map<Integer, double[]> reviewIdToScoreMapVec = new HashMap<>();
        Map<String, Integer> termInQueryCounter = new HashMap<>();

        buildHist(query, termInQueryCounter);
        String[] terms = getTermList(termInQueryCounter);
        double[] smoothingVec = calcSmoothingVec(terms, lambda, N);

        for (int i = 0; i < terms.length; i++) {
            Enumeration<Integer> postingList = indexReader.getReviewsWithToken(terms[i]);
            while (postingList.hasMoreElements()) {
                int reviewId = postingList.nextElement();
                int freq = postingList.nextElement();
                double[] termScore = reviewIdToScoreMapVec.getOrDefault(reviewId, new double[terms.length]);
                double p1 = (double) freq / indexReader.getReviewLength(reviewId);

                termScore[i] = lambda * (p1); // update score
                reviewIdToScoreMapVec.put(reviewId, termScore);
            }
        }

        Map<Integer, Double> reviewIdScoreMap = getLanguageModelScore(reviewIdToScoreMapVec, smoothingVec);
        return getBestKSortedKeys(reviewIdScoreMap, k);
    }

    /**
     * Calculates normalized query scores array.
     *
     * @param reviewIdToScoreMapVec Mapping of reviewId to its score vector.
     * @param smoothingVec          The smoothing vector.
     */
    private Map<Integer, Double> getLanguageModelScore(Map<Integer, double[]> reviewIdToScoreMapVec,
                                                       double[] smoothingVec) {
        Map<Integer, Double> reviewIdScoreMap = new HashMap<>(reviewIdToScoreMapVec.size());
        for (Integer key : reviewIdToScoreMapVec.keySet()) {
            double[] reviewScoreVec = reviewIdToScoreMapVec.get(key);
            for (int i = 0; i < smoothingVec.length; i++) {
                reviewScoreVec[i] += smoothingVec[i];
            }
            reviewIdScoreMap.put(key, Arrays.stream(reviewScoreVec).reduce(1.0, (product, e) -> product * e));
        }
        return reviewIdScoreMap;
    }

    /**
     * Returns the best k keys in map, sorted by the score.
     *
     * @param reviewIdToScoreMap (reviewId, score) map.
     * @param k                  the number of keys to return.
     * @param <T>                Type
     */
    private <T extends Comparable<T>> Enumeration<T> getBestKSortedKeys(Map<T, Double> reviewIdToScoreMap, int k) {
        Comparator<Map.Entry<T, Double>> byScores = Map.Entry.comparingByValue(Comparator.reverseOrder());
        Comparator<Map.Entry<T, Double>> byReviewId = Map.Entry.comparingByKey();
        k = Math.min(reviewIdToScoreMap.size(), k);
        return Collections.enumeration(reviewIdToScoreMap.entrySet()
                .stream()
                .sorted(byScores.thenComparing(byReviewId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
                .subList(0, k));
    }

    /**
     * Returns a list of the id-s of the k most highly ranked productIds for the
     * 1
     * given query using a function of your choice
     * The list should be sorted by the ranking
     */
    public Collection<String> productSearch(Enumeration<String> query, int k) {
        Map<String, Double> pidToScoreMap = new HashMap<>();

        while (query.hasMoreElements()) {
            String term = query.nextElement();
            Enumeration<Integer> postingList = indexReader.getReviewsWithToken(term);
            while (postingList.hasMoreElements()) {
                int reviewId = postingList.nextElement();
                int freq = postingList.nextElement();
                String pid = indexReader.getProductId(reviewId);

                // calc score
                int reviewScore = indexReader.getReviewScore(reviewId);
                double helpfulness = (double) indexReader.getReviewHelpfulnessNumerator(reviewId) /
                        indexReader.getReviewHelpfulnessDenominator(reviewId);

                double score = pidToScoreMap.getOrDefault(pid, 1.0);
                score += reviewScore * helpfulness;
                pidToScoreMap.put(pid, score);
            }
        }
        return Collections.list(getBestKSortedKeys(pidToScoreMap, k));
    }
}
