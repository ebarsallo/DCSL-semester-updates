package edu.purdue.squibble;

import java.io.*;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    static BufferedWriter out;
    static BufferedWriter outDetailed;

//    static String path = "/home/ebarsallo/Code/purdue.edu/699-research/android-wear/Squibble/logs/dsn-4/";
    static String path = "/home/ebarsallo/Code/purdue.edu/699-research/android-wear/Squibble/logs/dsn-4/clean";
//    static String path = "/home/ebarsallo/Code/purdue.edu/699-research/android-wear/Squibble/logs/dsn-4/expt3-fake";

    static HashMap<String, Integer> map;

    static int gExp = 0;
    static int gInn = 0;
    static int gCmp = 0;
    static int gTot = 0;

    static String gComponentName = "";
    static String gFileName = "";

    public static void parseLine(String line, BufferedWriter outS, BufferedWriter outD) throws IOException {

        // Pattern: ": exp%d | n {%d} (%d of %d) {ComponentInfo{%s}}"
        String pattern1 = ": expt(\\d) \\Q|\\E n \\Q{\\E(\\d+)\\Q}\\E \\Q(\\E(\\d+) of (\\d+)\\Q)\\E \\Q{\\EComponentInfo\\Q{\\E(.+)\\Q}}\\E";
        Pattern p1 = Pattern.compile(pattern1);
        Matcher m1 = p1.matcher(line);

        String pattern2 = ": (\\w.+)Exception: ";
        Pattern p2 = Pattern.compile(pattern2);
        Matcher m2 = p2.matcher(line);

        /* skip noise */
        if (line.contains("java.lang.IllegalArgumentException: Requested window android.view.ViewRootImpl")) return;

        if (m1.find()) {

            int exp = Integer.parseInt(m1.group(1));
            int inn = Integer.parseInt(m1.group(2));
            int cmp = Integer.parseInt(m1.group(3));
            int tot = Integer.parseInt(m1.group(4));

            String cn = m1.group(5);

            /* check for a change of component */
            if ( !cn.equals(gComponentName) && cmp != gCmp ) {

                if ( gExp != 0 ) print(outS);

                /* reset map */
                map = new HashMap<>();

                /* keep track of variables */
                gExp = exp;
                gInn = inn;
                gCmp = cmp;
                gTot = tot;

                gComponentName = cn;
            }

//            System.out.println(String.format ("%s * %s * %s * %s * %s *",
//                    m1.group(1), m1.group(2), m1.group(3), m1.group(4), m1.group(5)));
        } else if (m2.find())  {

            String rawEx = m2.group(1) + "Exception";

            /* clean exception name */
            String cleanEx = rawEx;
            StringTokenizer st = new StringTokenizer(rawEx);
            while (st.hasMoreTokens())
                cleanEx = st.nextToken();

            int c = map.containsKey(cleanEx) ? map.get(cleanEx) : 0;
            map.put(cleanEx, ++c);


//            System.out.println(String.format("\t -->%s<-- * %s", cleanEx, line));

            String str[] = gComponentName.split("/");
            line = String.format("%s\t%s\t%d\t%s\t%s\t%s", gFileName, str[0], gCmp, str[1], cleanEx, line);
            outD.write(line + "\n");
        }

    }


    public static void print(BufferedWriter out) throws IOException {

        String str[] = gComponentName.split("/");
        String line;

        for (String ex : map.keySet()){
            Integer c = map.get(ex);

            line = String.format("%s\t%s\t%d\t%s\t%s\t%d", gFileName, str[0], gCmp, str[1], ex, c);
            System.out.println(line);
            out.write(line + "\n");
        }

        /* no exception was found */
        if (map.isEmpty()) {
            line = String.format("%s\t%s\t%d\t%s\t\t", gFileName, str[0], gCmp, str[1]);
            System.out.println(line);
            out.write(line + "\n");
        }

    }

    public static void parseFile(File file, BufferedWriter out, BufferedWriter outD) {
        /* reset map */
        map = new HashMap<>();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                parseLine(line, out, outD);
                //stringBuffer.append(line);
                //stringBuffer.append("\n");
            }

            print(out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {


        String filename = "";
        // String filename = path + "/expt3-act-com.android.systemui.log";

        try {

            out = new BufferedWriter(new FileWriter("out.txt",false));
            outDetailed = new BufferedWriter(new FileWriter("out-detailed.txt",false));

            /* proccess either a file or a directory */
            if (filename.isEmpty()) {

                for (final File fileEntry : new File(path).listFiles()) {
                    if (!fileEntry.isDirectory()) {
                        gFileName = fileEntry.getName();
                        parseFile(fileEntry, out, outDetailed);
                    }
                }

            } else {
                File file = new File(filename);
                gFileName = filename;
                parseFile(file, out, outDetailed);
            }

            out.flush();
            out.close();

            outDetailed.flush();
            outDetailed.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }
}
