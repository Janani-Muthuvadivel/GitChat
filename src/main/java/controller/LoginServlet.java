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

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public LoginServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {

	    String username = request.getParameter("username");
	    String password = request.getParameter("password");

	    boolean isValidUser = false;

	    try {

	        Connection con = DBConnection.getConnection();

	        String sql = "SELECT * FROM vendors WHERE vendor_email=? AND vendor_password=?";
	        PreparedStatement ps = con.prepareStatement(sql);
	        ps.setString(1, username);
	        ps.setString(2, password);

	        ResultSet rs = ps.executeQuery();
	        response.setContentType("application/json");
	        PrintWriter out = response.getWriter();

	        if (rs.next()) {
	            isValidUser = true;
	            int vendorId = rs.getInt("vendor_id");
	            out.print("{\"status\":\"success\",\"vendor_id\":" + vendorId + "}");
	        }
	        else {
	        	out.print("{\"status\":\"fail\"}");
	        }

	        out.flush();
	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	}


}
