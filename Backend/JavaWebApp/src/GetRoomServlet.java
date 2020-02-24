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
@WebServlet(name = "Getroomservlet", urlPatterns = {"/getroom"})
public class GetRoomServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("Please use the POST method!");
        }
    }

    //old method

    /**
     * @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
     * throws ServletException, IOException {
     * StringBuilder jb = new StringBuilder();
     * String line;
     * try {
     * BufferedReader reader = request.getReader();
     * while ((line = reader.readLine()) != null)
     * jb.append(line);
     * } catch (Exception e) {  }
     * JSONObject jsonObject;
     * try {
     * jsonObject = new JSONObject(jb.toString());
     * } catch (JSONException e) {
     * // crash and burn
     * throw new IOException("Error parsing JSON request string");
     * }
     * <p>
     * //get the data from request and compare them to get the suitable room
     * JSONArray requestWifis = jsonObject.getJSONArray("Wifis");
     * JSONArray requestCells = jsonObject.getJSONArray("Cells");
     * <p>
     * //open connection and get parameters first
     * Connection conn;
     * String url = "jdbc:mysql://localhost:3306/your-db"; //Database -> your-db
     * String user = "root";
     * String pass = "";
     * try {
     * Class.forName("com.mysql.jdbc.Driver");
     * conn = DriverManager.getConnection(url, user, pass);
     * // the mysql select statement
     * String query = "SELECT * FROM readings";
     * // create the mysql insert preparedstatement
     * PreparedStatement preparedStmt = conn.prepareStatement(query);
     * ResultSet resultSet = preparedStmt.executeQuery();
     * //score for getting the best room
     * ArrayList<Integer> scoreArray = new ArrayList<>();
     * int count = 0;
     * ArrayList<Integer> rooms = new ArrayList<Integer>();//store the values we need
     * while (resultSet.next()) {
     * //get room
     * rooms.add(resultSet.getInt("room"));
     * // get wifis
     * JSONArray databaseWifis = new JSONArray(resultSet.getString("Wifis"));
     * ArrayList<String> databaseBssids = new ArrayList<String>();
     * ArrayList<Integer> databaseWifiPowers = new ArrayList<Integer>();
     * for (int i = 0; i < databaseWifis.length(); i++) {
     * databaseBssids.add(databaseWifis.getJSONObject(i).getString("bssid"));
     * databaseWifiPowers.add(databaseWifis.getJSONObject(i).getInt("power"));
     * }
     * //get cells
     * JSONArray databaseCells = new JSONArray(resultSet.getString("Cells"));
     * ArrayList<String> databaseCellids = new ArrayList<String>();
     * ArrayList<Integer> databaseCellSignalStrengths = new ArrayList<Integer>();
     * for (int i = 0; i < databaseCells.length(); i++) {
     * databaseCellids.add(databaseCells.getJSONObject(i).getString("id"));
     * databaseCellSignalStrengths.add(databaseCells.getJSONObject(i).getInt("power"));
     * }
     * <p>
     * scoreArray.add(0);
     * for (int i = 0; i < requestWifis.length(); i++) {
     * JSONObject jsonWifiObject = requestWifis.getJSONObject(i);
     * String testBssid = jsonWifiObject.getString("bssid");
     * int testPower = jsonWifiObject.getInt("power");
     * if (databaseBssids.contains(testBssid)) {
     * int value = scoreArray.get(count);
     * value += 10;
     * int powerWifi = databaseWifiPowers.get(databaseBssids.indexOf(testBssid));
     * int difference = Math.abs(powerWifi - testPower);
     * if (difference < 30) {
     * value += 10 - (difference / 3);
     * }
     * scoreArray.set(count, value);
     * }
     * }
     * <p>
     * for (int i = 0; i < requestCells.length(); i++) {
     * JSONObject jsonCellObject = requestCells.getJSONObject(i);
     * String testId = jsonCellObject.getString("id");
     * int testSignal = jsonCellObject.getInt("power");
     * if (databaseCellids.contains(testId)) {
     * int value = scoreArray.get(count);
     * value += 5;
     * int signalCell = databaseCellSignalStrengths.get(databaseCellids.indexOf(testId));
     * int difference = Math.abs(signalCell - testSignal);
     * if (difference < 30) {
     * value += 5 - 0.5 * (difference / 3);
     * }
     * scoreArray.set(count, value);
     * }
     * }
     * //after each resultset we increase the count
     * count++;
     * }
     * conn.close();
     * <p>
     * //out.println(scoreArray);
     * int max = getMax(scoreArray);
     * int index = scoreArray.indexOf(max);
     * <p>
     * //out.println("Router with: "+ bssids.get(index) +" and url: "+ urls.get(index));
     * //create the JSON response
     * response.setContentType("application/json");
     * JSONObject responseJsonObject = new JSONObject();
     * <p>
     * if (max == 0) {
     * responseJsonObject.put("roomNumber", 0);
     * } else {
     * responseJsonObject.put("roomNumber", rooms.get(index));
     * }
     * <p>
     * PrintWriter out = response.getWriter();
     * out.println(responseJsonObject);
     * <p>
     * } catch (Exception ex) {
     * System.out.println("Error" + ex);
     * }
     * }
     */
    //new method with k-nearest neighbours algorithm
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

        int lowestPower = Integer.MAX_VALUE;
        String wifiWithLowestPower = "";
        //find best wifi with signal
        for (int i = 0; i < requestWifis.length(); i++) {
            JSONObject jsonWifiObject = requestWifis.getJSONObject(i);
            String requestWifiBssid = jsonWifiObject.getString("bssid");
            int requestWifiPower = Math.abs(jsonWifiObject.getInt("power"));
            if(requestWifiPower < lowestPower){
                wifiWithLowestPower = requestWifiBssid;
                lowestPower = requestWifiPower;
            }
        }
        //value to store power difference in wifi
        int bestPowerDifference = Integer.MAX_VALUE;
        int indexOfBestPowerDifference = -1;

        //values to return
        //for test 1
        int roomViaBestSignal = -1;
        int pointViaBestSignal = -1;
        String coordinatesViaBestSignal ="";
        //for test 2
        int roomViaKNN = -1;
        int pointViaKNN = -1;
        String coordinatesViaKNN ="";
        //for test 3
        int roomViaWKNN = -1;
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
                    if(databaseWifis.getJSONObject(i).getString("bssid").equals(wifiWithLowestPower)){
                        int absolutePower = Math.abs(databaseWifis.getJSONObject(i).getInt("power"));
                        int powerDifference = Math.abs(absolutePower-lowestPower);
                        if(powerDifference<bestPowerDifference){
                            bestPowerDifference = powerDifference;
                            indexOfBestPowerDifference = count;
                        }
                    }
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

            //test 1
            roomViaBestSignal = roomsArray.get(indexOfBestPowerDifference);
            coordinatesViaBestSignal = coordinatesArray.get(indexOfBestPowerDifference);
            pointViaBestSignal = shortCodesArray.get(indexOfBestPowerDifference);

            //then we get the 20 best distances
            boolean firstMinimumFound = true;
            int j=0;
            ArrayList<Double> oldScoreArray = (ArrayList)scoreArray.clone();
            Collections.sort(scoreArray);
            while(j<20 && j<oldScoreArray.size()){
                double score = scoreArray.get(j);
                int index = oldScoreArray.indexOf(score);
                if (firstMinimumFound) {
                    roomViaKNN = roomsArray.get(index);
                    pointViaKNN = shortCodesArray.get(index);
                    coordinatesViaKNN = coordinatesArray.get(index);
                    firstMinimumFound = false;
                }
                finalCoordinatesArray.add(coordinatesArray.get(index));
                finalShortCodesArray.add(shortCodesArray.get(index));
                j++;
            }

            //test 2
            //we have computed all distances now we have to find the K nearest neighbors
            //we compute k as sqrt of values
            int K = (int) Math.round(Math.sqrt(count));
            //then we get the k-minimum distances
            ArrayList<Double> distancesArray = new ArrayList<>();
            ArrayList<Integer> newRoomArray = new ArrayList<>();
            for (int i = 1; i <= K; i++) {
                double min = getMin(oldScoreArray);
                int index = oldScoreArray.indexOf(min);
                distancesArray.add(min);
                oldScoreArray.remove(index);
                newRoomArray.add(roomsArray.get(index));
            }
            //find the inverses
            ArrayList<Double> inverseArray = new ArrayList<>();
            for (Double distance : distancesArray) {
                inverseArray.add(1 / distance);
            }
            //we then create the weights for the rest of the values
            Double sumOfInverses = 0d;
            for (Double inv : inverseArray) {
                sumOfInverses += inv;
            }
            ArrayList<Double> weightsArray = new ArrayList<>();
            for (Double inv : inverseArray) {
                weightsArray.add(inv / sumOfInverses);
            }
            //group the weights with the common room number
            ArrayList<Room> roomObject = new ArrayList<>();//get each room in the array
            for (int i = 0; i < newRoomArray.size(); i++) {
                Room myRoom = new Room();
                myRoom.setRoom(newRoomArray.get(i));
                myRoom.setWeight(weightsArray.get(i));
                roomObject.add(myRoom);
            }
            //create a map and group the rooms together
            Map<Integer, List<Room>> mappingRoomsWithWeights = roomObject.stream().collect(Collectors.groupingBy(Room::getRoom));
            ArrayList<Room> finalScoreArray = new ArrayList<>();//store the aggregated values
            for (Map.Entry<Integer, List<Room>> entry : mappingRoomsWithWeights.entrySet()) {
                int k = entry.getKey();
                List<Room> v = entry.getValue();
                //sum of weights per room
                double weightsSum = 0;
                for (Room room : v) {
                    weightsSum += room.getWeight();
                }
                Room newAggregatedRoom = new Room();
                newAggregatedRoom.setRoom(k);
                newAggregatedRoom.setWeight(weightsSum);
                finalScoreArray.add(newAggregatedRoom);
            }
            //Finally we can find the best room
            double bestVote = Double.MIN_VALUE;
            for (Room room : finalScoreArray) {
                if (room.getWeight() > bestVote) {
                    bestVote = room.getWeight();
                    roomViaWKNN = room.getRoom();
                }
            }
//            //Return the short codes and coordinates for the room
//            for (int i = 0; i < shortCodesArray.size(); i++) {
//                int roomToCheck = roomsArray.get(i);
//                if (roomToCheck == roomToReturn || roomToCheck == roomViaBestSignal) {
//                    finalCoordinatesArray.add(coordinatesArray.get(i));
//                    finalShortCodesArray.add(shortCodesArray.get(i));
//                }
//            }
        } catch (Exception ex) {
            System.out.println("Error" + ex);
        }
        //create the JSON response
        response.setContentType("application/json");
        JSONObject responseJsonObject = new JSONObject();
        responseJsonObject.put("roomViaBestSignal", roomViaBestSignal);
        responseJsonObject.put("roomViaKNN", roomViaKNN);
        responseJsonObject.put("roomViaWKNN", roomViaWKNN);
        responseJsonObject.put("pointViaBestSignal", pointViaBestSignal);
        responseJsonObject.put("pointViaKNN", pointViaKNN);
        responseJsonObject.put("coordinatesViaBestSignal", coordinatesViaBestSignal);
        responseJsonObject.put("coordinatesViaKNN", coordinatesViaKNN);
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
