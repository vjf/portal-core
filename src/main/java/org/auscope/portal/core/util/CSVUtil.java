package org.auscope.portal.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class CSVUtil {

    private BufferedReader  csvReader;

    private String[] headers;

    public CSVUtil(InputStream csvStream) throws IOException{
        this.csvReader = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8));
        this.setHeaders();
    }


    public String[] getHeaders(){
        return this.headers;
    }

    private void setHeaders() throws IOException{
        String line = "";
        line = csvReader.readLine();
        if(line!=null){
            headers = line.split(",");
        }else{
            throw new IOException("CSV Headers not found");
        }

    }

    public HashMap<String,ArrayList<String>> getColumnOfInterest(String [] columns) throws IOException{
        int [] columnIndex = getColumnIndex(columns);
        if(columnIndex.length != columns.length){
            throw new IOException("Not all columns are found");
        }
        HashMap<String,ArrayList<String>> result = new HashMap<String,ArrayList<String>>();

        for(String column:columns){
            result.put(column, new ArrayList<String>());
        }

        String line = "";
        while ((line = csvReader.readLine()) != null){
            String[] tokens = line.split(",");

            for(int i=0; i < columnIndex.length; i++){
                result.get(columns[i]).add(tokens[columnIndex[i]]);
            }
        }


        return result;
    }


    private int[] getColumnIndex(String [] columns){
        int [] columnIndex=new int[columns.length];

        for(int i=0; i<columns.length; i++){
            columnIndex[i] = Arrays.asList(this.headers).indexOf(columns[i]);
        }

        return columnIndex;
    }


}