import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletContext;
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
import java.util.logging.Logger;


/**
 * This controller is responsible for inserting new users to the database.
 */
@WebServlet(name = "Postsignalsservlet", urlPatterns = {"/postsignals"})
public class PostSignalsServlet extends HttpServlet {

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  the servlet request.
     * @param response the servlet response.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("Please use the POST method!");
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  the servlet request.
     * @param response the servlet response.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException      if an I/O error occurs.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        StringBuilder jb = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (Exception e) { /*report an error*/ }

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jb.toString());
        } catch (JSONException e) {
            // crash and burn
            throw new IOException("Error parsing JSON request string");
        }

        //get the data from request and compare them to get the suitable access point

        int roomNumber = jsonObject.getInt("roomNumber");
        int shortCode = jsonObject.getInt("shortCode");
        //String coordinates = jsonObject.getString("coordinates");
        JSONArray requestWifis = jsonObject.getJSONArray("Wifis");
        JSONArray requestCells = jsonObject.getJSONArray("Cells");

        //open connection and get parameters first
        Connection conn;
        String url = "jdbc:mysql://localhost:3306/your-db"; //Database -> your-db
        String user = "root";
        String pass = "";
        //create the JSON response
        response.setContentType("application/json");
        JSONObject responseJsonObject = new JSONObject();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, user, pass);

            String query = " INSERT INTO signalsmapped (room, Wifis, Cells, point)"
                    + " VALUES (?, ?, ?, ?)";

            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = conn.prepareStatement(query);
            preparedStmt.setInt(1, roomNumber);
            preparedStmt.setString(2, requestWifis.toString());
            preparedStmt.setString(3, requestCells.toString());
            preparedStmt.setInt(4, shortCode);
            // execute the preparedstatement
            preparedStmt.executeUpdate();
            //System.out.println("executed correctly");
            conn.close();


        } catch (Exception ex) {
            //System.out.println("Error" + ex);
        }

    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "This servlet adds new persons to the database.";
    }

}
