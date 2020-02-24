import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * This controller is responsible for inserting new users to the database.
 */
@WebServlet(name = "Getbestpointsservlet", urlPatterns = {"/getbestpoints"})
public class GetBestPointsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("Please use the POST method!");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StringBuilder jb = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (Exception e) {
        }
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jb.toString());
        } catch (JSONException e) {
            // crash and burn
            throw new IOException("Error parsing JSON request string");
        }

        //get the data from request and compare them to get the suitable room
        JSONArray requestWifis = jsonObject.getJSONArray("Wifis");
        JSONArray requestCells = jsonObject.getJSONArray("Cells");

        //values to return
        int roomToReturn = -1;
        int returnShortCode = -1;
        String returnCoordinates = "";
        int roomViaBestSignal = -1;
        ArrayList<Integer> finalShortCodesArray = new ArrayList<>();
        ArrayList<String> finalCoordinatesArray = new ArrayList<>();

        //open connection and get parameters first
        Connection conn;
        String url = "jdbc:mysql://localhost:3306/your-db"; //Database -> your-db
        String user = "root";
        String pass = "";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, pass);
            // the mysql select statement
            String query = "SELECT * FROM readings";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = conn.prepareStatement(query);
            ResultSet resultSet = preparedStmt.executeQuery();
            //score for getting the best room
            ArrayList<Double> scoreArray = new ArrayList<>();
            int count = 0;
            //store the values we need from MySql
            ArrayList<Integer> roomsArray = new ArrayList<>();
            ArrayList<Integer> shortCodesArray = new ArrayList<>();
            ArrayList<String> coordinatesArray = new ArrayList<>();
            while (resultSet.next()) {
                //get values
                roomsArray.add(resultSet.getInt("room"));
                shortCodesArray.add(resultSet.getInt("short_code"));
                coordinatesArray.add(resultSet.getString("coordinates"));
                // get wifis
                JSONArray databaseWifis = new JSONArray(resultSet.getString("Wifis"));
                ArrayList<String> databaseBssids = new ArrayList<String>();
                ArrayList<Integer> databaseWifiPowers = new ArrayList<Integer>();
                for (int i = 0; i < databaseWifis.length(); i++) {
                    databaseBssids.add(databaseWifis.getJSONObject(i).getString("bssid"));
                    databaseWifiPowers.add(databaseWifis.getJSONObject(i).getInt("power"));
                }
                //get cells
                JSONArray databaseCells = new JSONArray(resultSet.getString("Cells"));
                ArrayList<String> databaseCellids = new ArrayList<String>();
                ArrayList<Integer> databaseCellSignalStrengths = new ArrayList<Integer>();
                for (int i = 0; i < databaseCells.length(); i++) {
                    databaseCellids.add(databaseCells.getJSONObject(i).getString("id"));
                    databaseCellSignalStrengths.add(databaseCells.getJSONObject(i).getInt("power"));
                }
                //if we have a match we store the euclidian distance, else we just store the power
                double value = 0d; //value to store score
                for (int i = 0; i < requestWifis.length(); i++) {
                    JSONObject jsonWifiObject = requestWifis.getJSONObject(i);
                    String requestWifiBssid = jsonWifiObject.getString("bssid");
                    int requestWifiPower = jsonWifiObject.getInt("power");

                    if (databaseBssids.contains(requestWifiBssid)) {
                        int powerWifi = databaseWifiPowers.get(databaseBssids.indexOf(requestWifiBssid));
                        int pow2 = (powerWifi - requestWifiPower) * (powerWifi - requestWifiPower);
                        value += pow2;
                    } else {
                        int pow2 = requestWifiPower * requestWifiPower;
                        value += pow2;
                    }
                }

                for (int i = 0; i < requestCells.length(); i++) {
                    JSONObject jsonCellObject = requestCells.getJSONObject(i);
                    String requestCellId = jsonCellObject.getString("id");
                    int requestCellSignal = jsonCellObject.getInt("power");

                    if (databaseCellids.contains(requestCellId)) {
                        int signalCell = databaseCellSignalStrengths.get(databaseCellids.indexOf(requestCellId));
                        int pow2 = (signalCell - requestCellSignal) * (signalCell - requestCellSignal);
                        value += pow2;
                    } else {
                        int pow2 = requestCellSignal * requestCellSignal;
                        value += pow2;
                    }
                }
                //get the root of the eukleidian distances
                scoreArray.add(count, Math.sqrt(value));
                //after each resultset we increase the count
                count++;
            }
            conn.close();
            //we have computed all distances

            //then we get the 20 best distances
            boolean firstMinimumFound = true;
            int j=0;
            ArrayList<Double> oldScoreArray = (ArrayList)scoreArray.clone();
            Collections.sort(scoreArray);
            System.out.println(scoreArray);
            while(j<20 && j<oldScoreArray.size()){
                double score = scoreArray.get(j);
                int index = oldScoreArray.indexOf(score);
                if (firstMinimumFound) {
                    roomViaBestSignal = roomsArray.get(index);
                    returnShortCode = shortCodesArray.get(index);
                    returnCoordinates = coordinatesArray.get(index);
                    firstMinimumFound = false;
                }
                finalCoordinatesArray.add(coordinatesArray.get(index));
                finalShortCodesArray.add(shortCodesArray.get(index));
                j++;
            }

        } catch (Exception ex) {
            System.out.println("Error" + ex);
        }
        //create the JSON response
        response.setContentType("application/json");
        JSONObject responseJsonObject = new JSONObject();
        responseJsonObject.put("roomNumber", roomViaBestSignal);
        responseJsonObject.put("shortCode", returnShortCode);
        responseJsonObject.put("coordinates", returnCoordinates);
        responseJsonObject.put("shortCodesArray", finalShortCodesArray);
        responseJsonObject.put("coordinatesArray", finalCoordinatesArray);
        PrintWriter out = response.getWriter();
        out.println(responseJsonObject);

    }


    private double getMin(ArrayList<Double> list) {
        double min = Double.MAX_VALUE;
        for (Double d : list) {
            if (d < min) {
                min = d;
            }
        }
        return min;
    }

    private int getMax(ArrayList<Integer> list) {
        int max = Integer.MIN_VALUE;
        for (Integer integer : list) {
            if (integer > max) {
                max = integer;
            }
        }
        return max;
    }

    @Override
    public String getServletInfo() {
        return "This servlet adds new persons to the database.";
    }

}
