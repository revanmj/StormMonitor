package com.revanmj.stormmonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by revanmj on 28.07.2013.
 */

public class DataFile {
    static public void WriteToFile (List<Integer> cities, File f) throws IOException {
        PrintWriter pw = new PrintWriter(f);
        for (int i = 0; i < cities.size(); i++)
            pw.println(cities.get(i).toString());
        pw.close();
    }

    static public List<Integer> ReadFromFile (File f) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        List<Integer> cities = new ArrayList<Integer>();
        String line;
        while ((line = br.readLine())!=null){
            cities.add(Integer.parseInt(line));
        }
        br.close();

        return cities;
    }
}
