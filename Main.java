import webdata.IndexReader;
import webdata.IndexWriter;


public class Main {

    public static void main(String[] args) {
//        Parser p = new Parser("C:\\Users\\Noa Barlia\\Documents\\Noa\\University\\3rd-Year\\SemesterB\\WebInformationRetrieval\\ex1\\src\\100.txt");
        String inputFile = "/cs/+/course/webdata/movies.txt";
        String dir = "Indices";
        long start = System.nanoTime();
        IndexWriter writer = new IndexWriter();
        writer.write(inputFile, dir);
        long end = System.nanoTime();
        long res = end - start;
        System.out.println("Elapsed time for 100000 reviews is: " + (res * 1e-9) + " seconds");



    }
}
