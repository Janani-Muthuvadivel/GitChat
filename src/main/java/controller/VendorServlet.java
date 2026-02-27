package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.DBConnection;

@WebServlet("/vendors")
public class VendorServlet extends HttpServlet{
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        List<String> vendorNames = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT vendor_name FROM vendors");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                vendorNames.add(rs.getString("vendor_name"));
            }

            StringBuilder json = new StringBuilder();
            json.append("[");

            for (int i = 0; i < vendorNames.size(); i++) {
                json.append("\"").append(vendorNames.get(i)).append("\"");
                if (i < vendorNames.size() - 1) {
                    json.append(",");
                }
            }

            json.append("]");

            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error\"}");
        }
    }
}