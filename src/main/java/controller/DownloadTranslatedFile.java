package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.DBConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/DownloadTranslatedFile")
public class DownloadTranslatedFile extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public DownloadTranslatedFile() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request,response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Entered into Translated zip download servlet");
//		System.out.println("Entered into servlet");
		int id = Integer.parseInt(request.getParameter("fileId"));
		try {

		    

		    Connection con = DBConnection.getConnection();

		    String sql = "SELECT uploaded_content FROM files WHERE file_id = ?";
		    PreparedStatement ps = con.prepareStatement(sql);
		    ps.setInt(1, id);

		    ResultSet rs = ps.executeQuery();

		    if (rs.next()) {

		        response.reset();
		        response.setContentType("application/octet-stream");
		        response.setHeader("Content-Disposition", "attachment; filename=\"download.zip\"");

		        InputStream in = rs.getBinaryStream("uploaded_content");

		        if (in == null) {
		            System.out.println("InputStream is NULL");
		            return;
		        }

		        OutputStream out = response.getOutputStream();

		        byte[] buffer = new byte[8192];
		        int length;
		        int total = 0;

		        while ((length = in.read(buffer)) != -1) {
		            out.write(buffer, 0, length);
		            total += length;
		        }

		        System.out.println("Total bytes written: " + total);

		        out.flush();
		        out.close();
		        in.close();
		    }
		    else {
		        System.out.println("No file found for id");
		    }

		    rs.close();
		    ps.close();
		    con.close();

		}
		catch(Exception e) {
		    e.printStackTrace();
		}


	}

}
