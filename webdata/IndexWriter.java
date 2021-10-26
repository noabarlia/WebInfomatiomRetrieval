package webdata;

import java.io.IOException;
import java.nio.file.*;

/**
 * Class IndexWriter.
 */
public class IndexWriter {

    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */
    public void write(String inputFile, String dir) {
        try {
            Utils.createDirectory(dir);
            ExternalSort sorter = new ExternalSort();
            Parser parser = new Parser(inputFile, sorter);
            ReviewIndexWriter reviewIndexWriter = new ReviewIndexWriter(parser, dir);
            ProductIndexWriter productIndexWriter = new ProductIndexWriter(parser, dir);

            boolean parsing = true;
            while (parsing) {
                parsing = parser.parseFile();
                reviewIndexWriter.write();
            }
            parser.clearReviewIndexStructs();
            reviewIndexWriter.close();
            productIndexWriter.write();

            parser.parsePostingsLists();
            sorter.mergeSortedPairs(parser.getNumOfTokens());

            DictionaryWriter dictionaryWriter = new DictionaryWriter(parser, dir, 10);
            dictionaryWriter.write();
            sorter.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {
        Utils.deleteDirectory(dir);
    }
}
