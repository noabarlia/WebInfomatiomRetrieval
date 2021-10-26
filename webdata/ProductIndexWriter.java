package webdata;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * ProductIndexWriter class.
 */
class ProductIndexWriter {

    /**
     * --- FIELDS ---
     */
    private static final String PRODUCT_INDEX_FILENAME = "/product_index";
    private Parser parser;
    private String dir;

    /**
     * Constructor.
     *
     * @param parser parser.
     * @param dir    directory.
     */
    ProductIndexWriter(Parser parser, String dir) {
        this.parser = parser;
        this.dir = dir;
    }

    /**
     * Writes product index to file.
     */
    void write() throws IOException {
        String path = dir + PRODUCT_INDEX_FILENAME;
        DataOutputStream productIndexFile = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(path)));

        ArrayList<String> pids = parser.getProductIds();
        TreeMap<String, Pair<Integer, Integer>> pidMap = getSortedPidMap(pids);
        for (Map.Entry<String, Pair<Integer, Integer>> entry : pidMap.entrySet()) {
            productIndexFile.writeBytes(entry.getKey());
            productIndexFile.writeInt(entry.getValue().getL());
            productIndexFile.writeInt(entry.getValue().getR());
        }

        productIndexFile.close(); // total 18 bytes per unique PID
    }

    /**
     * Returns mapping of PIDs to its corresponding review IDs, since review IDs are grouped by Pid
     * start index and count is saved in the map.
     *
     * @param pids given product IDs.
     * @return PID mapping sorted by PID.
     */
    private TreeMap<String, Pair<Integer, Integer>> getSortedPidMap(ArrayList<String> pids) {
        TreeMap<String, Pair<Integer, Integer>> map = new TreeMap<>();
        for (int i = 0; i < pids.size(); ++i) {
            if (map.containsKey(pids.get(i))) {
                String pid = pids.get(i);
                Pair<Integer, Integer> oldVal = map.get(pid);
                Pair<Integer, Integer> newVal = new Pair<>(oldVal.getL(), oldVal.getR() + 1);
                map.put(pid, newVal);
            } else {
                map.put(pids.get(i), new Pair<>(i + 1, 1));

            }
        }
        return map;
    }
}
