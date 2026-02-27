package controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.DBConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONArray;
import org.json.JSONObject;


@WebServlet("/TransferRequest")
public class TransferRequest extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
   
    public TransferRequest() {
        super();
        
    }

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws IOException {

	    Connection con = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        // Read simple parameters
	        String projectId = request.getParameter("project_id");
	        String fileName = request.getParameter("file_name");
	        String fileContent = request.getParameter("file_content");
	        String userId = request.getParameter("user_id");
	        String userName = request.getParameter("user_name");
	        String message = request.getParameter("expectation");

	        
	        String serviceJson = request.getParameter("service");       
	        String vendorNameJson = request.getParameter("vendorName");  
	        
	        String languagesJson = request.getParameter("languages");    

	        
	        JSONObject service = new JSONObject(serviceJson);
	        JSONObject vendorName = new JSONObject(vendorNameJson);
	        JSONArray languages = new JSONArray(languagesJson);

	        String serviceValue = service.getString("value");
	        String vendorLabel = vendorName.getString("label");
	        System.out.println(vendorLabel);

	        
	        con = DBConnection.getConnection();
	        con.setAutoCommit(false);

	        
	        String insertFile = "INSERT INTO files (file_path, file_content, project_id,is_uploadable) VALUES (?, ?, ?,?)";
	        ps = con.prepareStatement(insertFile, Statement.RETURN_GENERATED_KEYS);
	        ps.setString(1, serviceValue);
	        ps.setBytes(2, fileContent.getBytes(StandardCharsets.UTF_8));
	        ps.setString(3, projectId);
	        ps.setString(4, "false");
	        ps.executeUpdate();

	        rs = ps.getGeneratedKeys();
	        int fileId = 0;
	        if (rs.next()) fileId = rs.getInt(1);

	        
	        String getVendor = "SELECT vendor_id FROM vendors WHERE vendor_name = ?";
	        ps = con.prepareStatement(getVendor);
	        ps.setString(1, vendorLabel);
	        rs = ps.executeQuery();
	        int vendorId = 0;
	        if (rs.next()) vendorId = rs.getInt("vendor_id");

	        
	        String insertRequest = "INSERT INTO requests (user_id, file_id, vendor_id, status,user_name,message) VALUES (?, ?, ?, ?, ?, ?)";
	        ps = con.prepareStatement(insertRequest, Statement.RETURN_GENERATED_KEYS);
	        ps.setString(1, userId);
	        ps.setInt(2, fileId);
	        ps.setInt(3, vendorId);
	        ps.setString(4, "Pending");
	        ps.setString(5, userName);
	        ps.setString(6, message);
	        ps.executeUpdate();

	        rs = ps.getGeneratedKeys();
	        int requestId = 0;
	        if (rs.next()) requestId = rs.getInt(1);

	        
	        for (int i = 0; i < languages.length(); i++) {
	            String langName = languages.getJSONObject(i).getString("value");
	            String getLang = "SELECT language_id FROM languages WHERE language_name=?";
	            ps = con.prepareStatement(getLang);
	            ps.setString(1, langName);
	            rs = ps.executeQuery();
	            if (rs.next()) {
	                int languageId = rs.getInt("language_id");
	                String insertLang = "INSERT INTO requested_languages (request_id, language_id) VALUES (?, ?)";
	                ps = con.prepareStatement(insertLang);
	                ps.setInt(1, requestId);
	                ps.setInt(2, languageId);
	                ps.executeUpdate();
	            }
	        }
	        con.commit();
	        
	        if(vendorLabel.equals("Zia")) {
	        	System.out.println("Entered forward block");
	        	request.setAttribute("fileId", String.valueOf(fileId));
	        	RequestDispatcher rd = request.getRequestDispatcher("ZiaTranslate");
	        	System.out.println("Ready to forward");
	            rd.forward(request, response);
	        }

	        
	        response.getWriter().write("Request Created Successfully");

	    } catch (Exception e) {
	        try { if (con != null) con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
	        e.printStackTrace();
	        response.getWriter().write("Error Occurred");
	    } finally {
	        try { if (rs != null) rs.close(); if (ps != null) ps.close(); if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
	    }
	}


}
