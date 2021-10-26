package webdata;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Utils.
 */
class Utils {

    /**
     * @return log2 num.
     */
    static double log2(int num) {
        return Math.log(num) / Math.log(2);
    }

    /**
     * Converts Array list of bytes to to an array bytes.
     *
     * @param bytes Array list of bytes.
     * @return array of bytes.
     */
    static byte[] convertToArray(ArrayList<Byte> bytes) {
        byte[] bytesArr = new byte[bytes.size()];
        for (int i = 0; i < bytesArr.length; ++i) {
            bytesArr[i] = bytes.get(i);
        }
        return bytesArr;
    }

    /**
     * Creates new directory if it does not exist.
     *
     * @param dir directory name
     * @throws IOException
     */
    static void createDirectory(String dir) throws IOException {
        Path path = Paths.get(dir);
        Files.createDirectories(path);
    }

    /**
     * Deletes directory recursively with all its contents.
     *
     * @param dir Directory for deletion.
     */
    static void deleteDirectory(String dir) {
        File file = new File(dir);
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                f.delete();
            }
        }
        file.delete();
    }

    private static final int BYTES_IN_GB = 1073741824;

    public static void memoryStatus(String event) {
        Runtime runtime = Runtime.getRuntime();
        double memory_size = runtime.totalMemory();
        double memory_free = runtime.freeMemory();
        double memory_free_percent = 100.0 * memory_free / memory_size;
        System.out.printf("[%s] Free memory: %.2f%% ([%.3g GB] out of [%.3g GB] allocated)%n",
                event,
                memory_free_percent,
                (memory_size - memory_free) / BYTES_IN_GB,
                memory_size / BYTES_IN_GB
        );
    }
}
