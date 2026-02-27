package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.DBConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/SlashValidateServlet")
public class SlashValidateServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public SlashValidateServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	 protected void doPost(HttpServletRequest request, HttpServletResponse response)
	            throws ServletException, IOException
	    {
	        System.out.println("Entered SlashValidateServlet");

	        BufferedReader reader = request.getReader();
	        StringBuilder sb = new StringBuilder();
	        String line;

	        while ((line = reader.readLine()) != null)
	        {
	            sb.append(line);
	        }

	        String json = sb.toString();

	        JSONArray resultArray = new JSONArray();

	        Connection con = null;
	        PreparedStatement ps = null;
	        ResultSet rs = null;

	        try
	        {
	            JSONObject obj = new JSONObject(json);
	            JSONArray fileIds = obj.getJSONArray("fileIds");

	            con = DBConnection.getConnection();

	            ps = con.prepareStatement(
	                "SELECT file_id, is_uploadable FROM files WHERE file_id = ?"
	            );

	            for (int i = 0; i < fileIds.length(); i++)
	            {
	                String fileId = fileIds.getString(i);

	                ps.setString(1, fileId);

	                rs = ps.executeQuery();

	                if (rs.next())
	                {
	                    JSONObject resultObj = new JSONObject();

	                    resultObj.put("fileId", fileId);

	                    String isuploadableStr = rs.getString("is_uploadable");

	                    boolean isuploadable =
	                        isuploadableStr != null &&
	                        isuploadableStr.equalsIgnoreCase("true");
	                    ps = con.prepareStatement("UPDATE requests set status = ? where file_id = ?");
	                    if(isuploadable) {
	                    	ps.setString(1, "Validated");
	                    	
	                    }
	                    else {
	                    	ps.setString(1, "Rejected");
	                    }
	                    ps.setString(2, fileId);
	                    ps.executeUpdate();
	                    System.out.println(isuploadable);
	                    resultObj.put("isuploadable", isuploadable);

	                    resultArray.put(resultObj);
	                }

	                if(rs != null)
	                {
	                    rs.close();
	                }
	            }

	        }
	        catch(Exception e)
	        {
	            e.printStackTrace();
	        }
	        finally
	        {
	            try { if(ps!=null) ps.close(); } catch(Exception e){}
	            try { if(con!=null) con.close(); } catch(Exception e){}
	        }

	       
	        response.setContentType("application/json");
	        response.setCharacterEncoding("UTF-8");

	        PrintWriter out = response.getWriter();

	        out.print(resultArray.toString());

	        out.flush();
	    }

}
