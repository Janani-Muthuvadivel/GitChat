package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.DBConnection;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/VendorLanguages")
public class VendorLanguages extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public VendorLanguages() {
        super();
    }

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Entered Vendor languages");
		String vendorName = request.getParameter("vendor");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (vendorName == null || vendorName.trim().isEmpty()) {
            out.print("{\"error\":\"vendorName parameter is required\"}");
            return;
        }

        List<String> languages = new ArrayList<>();

        String sql = "SELECT l.language_name " +
                     "FROM vendors v " +
                     "JOIN vendor_languages vl ON v.vendor_id = vl.vendor_id " +
                     "JOIN languages l ON vl.language_id = l.language_id " +
                     "WHERE v.vendor_name = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vendorName);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                languages.add(rs.getString("language_name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"error\":\"Database error\"}");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"vendor\":\"").append(vendorName).append("\",");
        json.append("\"languages\":[");

        for (int i = 0; i < languages.size(); i++) {
            json.append("\"").append(languages.get(i)).append("\"");
            if (i < languages.size() - 1) {
                json.append(",");
            }
        }

        json.append("]}");

        out.print(json.toString());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}