package webdata;

import java.util.ArrayList;

/**
 * DictionaryDecoder Class.
 */
class DictionaryDecoder {

    /**
     * Separates concatenated string into its composing tokens.
     *
     * @param str   concatenated string.
     * @param block Records block.
     * @return list of tokens.
     */
    ArrayList<String> separateTokens(DictionaryReader.Record[] block, String str) {
        ArrayList<String> terms = new ArrayList<>();
        int start = block[0].length, end;

        terms.add(str.substring(0, start));  // add first token to array
        for (int i = 1 ; i < block.length; ++i) {
            DictionaryReader.Record record = block[i];
            String prev = terms.get(i - 1);
            String prefix = prev.substring(0, record.prefix);

            end = (i < block.length - 1) ? start + record.length - record.prefix : str.length();
            String suffix = str.substring(start, end);

            start += record.length - record.prefix;
            terms.add(prefix + suffix);
        }
        return terms;
    }

    /**
     * Converts a token's gaps list to a postings list contains the reviews indexes and corresponding frequency.
     *
     * @param gapsList a token's gap postings list.
     */
    ArrayList<Integer> processGapsFrequencyList(ArrayList<Integer> gapsList) {
        ArrayList<Integer> postingsFrequencyList = new ArrayList<>();
        int counter = 0;

        for (int i = 0; i < gapsList.size(); i++) {
            if (i % 2 == 0) {
                counter += gapsList.get(i);     // postings
                postingsFrequencyList.add(counter);
            } else {
                postingsFrequencyList.add(gapsList.get(i));  // frequency
            }
        }
        return postingsFrequencyList;
    }
}
