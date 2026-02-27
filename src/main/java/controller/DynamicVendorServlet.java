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

@WebServlet("/DynamicVendorServlet")
public class DynamicVendorServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public DynamicVendorServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		
		System.out.println("Entered the servlet");
	    String vendorId = request.getParameter("vendor_id");
	    System.out.println(vendorId);
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");

	    PrintWriter out = response.getWriter();

	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	    	System.out.println("Entered try block");

	        conn = DBConnection.getConnection();

	        String sql = "SELECT r.request_id, r.file_id, r.created_at, r.status,r.user_name,r.message, " +
	                     "l.language_name, l.language_code " +
	                     "FROM requests r " +
	                     "JOIN requested_languages rl ON r.request_id = rl.request_id " +
	                     "JOIN languages l ON rl.language_id = l.language_id " +
	                     "WHERE r.vendor_id = ? " +
	                     "ORDER BY r.request_id";

	        ps = conn.prepareStatement(sql);
	        ps.setInt(1, Integer.parseInt(vendorId));

	        rs = ps.executeQuery();
	        System.out.println("Executed query");

	        StringBuilder json = new StringBuilder();
	        json.append("[");

	        int currentRequestId = -1;
	        boolean firstRequest = true;
	        boolean firstLanguage = true;

	        while (rs.next()) {

	            int requestId = rs.getInt("request_id");

	            // If new request_id encountered
	            if (requestId != currentRequestId) {

	                // Close previous object (if not first)
	                if (currentRequestId != -1) {
	                    json.append("]}");
	                    firstLanguage = true;
	                }

	                if (!firstRequest) {
	                    json.append(",");
	                }

	                json.append("{");
	                json.append("\"request_id\":").append(requestId).append(",");
	                json.append("\"file_id\":").append(rs.getInt("file_id")).append(",");
	                json.append("\"user_name\":\"").append(rs.getString("user_name")).append("\",");
	                System.out.println(rs.getString("user_name"));
	                json.append("\"message\":\"").append(rs.getString("message")).append("\",");
	                json.append("\"created_at\":\"")
	                    .append(rs.getTimestamp("created_at")).append("\",");
	                json.append("\"status\":\"")
	                    .append(rs.getString("status")).append("\",");

	                json.append("\"languages\":[");

	                currentRequestId = requestId;
	                firstRequest = false;
	            }

	            if (!firstLanguage) {
	                json.append(",");
	            }

	            json.append("\"")
	                .append(rs.getString("language_name"))
	                .append("[")
	                .append(rs.getString("language_code"))
	                .append("]\"");

	            firstLanguage = false;
	        }

	        // Close last object if data exists
	        if (currentRequestId != -1) {
	            json.append("]}");
	        }

	        json.append("]");
	        System.out.println("Returned output");
	        out.write(json.toString());

	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try { if (rs != null) rs.close(); } catch (Exception e) {}
	        try { if (ps != null) ps.close(); } catch (Exception e) {}
	        try { if (conn != null) conn.close(); } catch (Exception e) {}
	    }
	}

}
