import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;


public class Main {

    public static Map<Integer, String> ids = new HashMap<>();
    public static Map<String, List<Integer>> results = new HashMap<>();
    public static Map<String, List<Integer>> exps = new HashMap<>();

    public static void main(String[] args) throws IOException {
        String pl = args[0];
        String p2 = args[1];
        String p3 = args[2];
        String p4 = args[3];
        String p5 = args[4];
        String p6 = args[5];
        String p7 = args[6];
        String p8 = args[7];
        String p9 = args[8];
        String p10 = args[9];

        System.out.println(pl + " " + p2 + " " + p3 + " " + p4 + " " + p5 + " " + p6 + " " + p7 + " " + p8 + " " + p9 + " " + p10);

        ids.put(1, pl + "_1");
        ids.put(2, p2 + "_2");
        ids.put(3, p3 + "_3");
        ids.put(4, p4 + "_4");
        ids.put(5, p5 + "_5");
        ids.put(6, p6 + "_6");
        ids.put(7, p7 + "_7");
        ids.put(8, p8 + "_8");
        ids.put(9, p9 + "_9");
        ids.put(10, p10 + "_10");

        results.put(pl + "_1", new ArrayList<>());
        results.put(p2 + "_2", new ArrayList<>());
        results.put(p3 + "_3", new ArrayList<>());
        results.put(p4 + "_4", new ArrayList<>());
        results.put(p5 + "_5", new ArrayList<>());
        results.put(p6 + "_6", new ArrayList<>());
        results.put(p7 + "_7", new ArrayList<>());
        results.put(p8 + "_8", new ArrayList<>());
        results.put(p9 + "_9", new ArrayList<>());
        results.put(p10 + "_10", new ArrayList<>());

        exps.put(pl + "_1", new ArrayList<>());
        exps.put(p2 + "_2", new ArrayList<>());
        exps.put(p3 + "_3", new ArrayList<>());
        exps.put(p4 + "_4", new ArrayList<>());
        exps.put(p5 + "_5", new ArrayList<>());
        exps.put(p6 + "_6", new ArrayList<>());
        exps.put(p7 + "_7", new ArrayList<>());
        exps.put(p8 + "_8", new ArrayList<>());
        exps.put(p9 + "_9", new ArrayList<>());
        exps.put(p10 + "_10", new ArrayList<>());

        int i = 5;
        while (i > 0) {
            FileReader myfile = new FileReader("result.txt");
            if (myfile.read() != -1) {
                FileReader myfile2 = new FileReader("result.txt");
                char buf[] = new char[200];
                myfile2.read(buf);
                String result = StringEscapeUtils.escapeJava(String.valueOf(buf)).replace("\\u0000", "").replace("\\r", "");
                String[] split = StringUtils.split(result, "\\n");
                for (int j = 2; j < split.length; j++) {
                    String id = ids.get(j-1);
                    String[] values = split[j].split(" ");
                    results.get(id).add(Integer.parseInt(values[0]));
                    exps.get(id).add(Integer.parseInt(values[1]));
                }

                i--;

                Runtime.getRuntime().exec("cmd /c start cmd.exe /c jarRunner.bat 31001 "
                        + pl + " " + p2 + " " + p3 + " " + p4 + " " + p5 + " " + p6 + " " + p7 + " " + p8 + " " + p9 + " " + p10 + " false 10");
            }


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        writeData(pl + "_1");
        writeData(p2 + "_2");
        writeData(p3 + "_3");
        writeData(p4 + "_4");
        writeData(p5 + "_5");
        writeData(p6 + "_6");
        writeData(p7 + "_7");
        writeData(p8 + "_8");
        writeData(p9 + "_9");
        writeData(p10 + "_10");
        System.out.println();
    }

    public static void writeData(String pl) throws IOException {
        String data = "";
        int sumResult = 0;
        int sumTicks = 0;
        for (Integer i : results.get(pl)) {
            sumResult += i;
            data += (i + "\n");
        }
        data += ((double) sumResult / (double) results.get(pl).size()) + "\n\n";
        for (Integer i : exps.get(pl)) {
            sumTicks += i;
            data += (i + "\n");
        }
        data += ((double) sumTicks / (double) exps.get(pl).size()) + "\n\n";

        File file = new File(pl + ".txt");
        FileWriter fileWriter = new FileWriter(file);
        if (!file.exists()) {
            if (file.createNewFile()) {
                fileWriter.write(data);
            }
        } else {
            fileWriter.write(data);
        }

        fileWriter.flush();
        fileWriter.close();
    }
}
