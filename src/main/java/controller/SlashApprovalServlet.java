package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.DBConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@WebServlet("/SlashApprovalServlet")
public class SlashApprovalServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
    public SlashApprovalServlet() {
        super();
       
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/plain");

        int fileId = Integer.parseInt(request.getParameter("fileId"));

        try {

            Connection con = DBConnection.getConnection();

            
            PreparedStatement ps = con.prepareStatement(
                "SELECT uploaded_content, file_path, project_id, user_id, created_at " +
                "FROM files f join requests r on f.file_id = r.file_id WHERE f.file_id = ?");

            ps.setInt(1, fileId);

            ResultSet rs = ps.executeQuery();
            ps = con.prepareStatement("update requests set status = ? where file_id = ?");
            ps.setString(1, "Approved");
            ps.setInt(2, fileId);
            ps.executeUpdate();

            if (!rs.next()) {
                response.getWriter().write("FAILED: File not found");
                return;
            }

            Blob zipBlob = rs.getBlob("uploaded_content");

            if (zipBlob == null) {
                response.getWriter().write("FAILED: No uploaded ZIP found");
                return;
            }

            byte[] zipBytes = zipBlob.getBytes(1, (int) zipBlob.length());

            String filePath = rs.getString("file_path");
            String projectId = rs.getString("project_id");
            String userIdStr = rs.getString("user_id");
            String createdAt = rs.getString("created_at");

            long userId = Long.parseLong(userIdStr);

            
            System.out.println("----- FILE DETAILS -----");
            System.out.println("File ID     : " + fileId);
            System.out.println("File Path   : " + filePath);
            System.out.println("Project ID  : " + projectId);
            System.out.println("User ID     : " + userId);
            System.out.println("Created At  : " + createdAt);
            System.out.println("------------------------");

            
            ZipInputStream zis = new ZipInputStream(
                    new ByteArrayInputStream(zipBytes));

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String fileName = new File(entry.getName()).getName();

                System.out.println("Processing file: " + fileName);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int len;

                while ((len = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }

                byte[] fileBytes = baos.toByteArray();

   
                String base64Content = Base64.getEncoder()
                        .encodeToString(fileBytes);

                
                sendToCliq(fileName,
                        base64Content,
                        userId,
                        filePath,
                        projectId);

                zis.closeEntry();
            }

            zis.close();

            response.getWriter().write("SUCCESS: Files sent to Cliq");

        } catch (Exception e) {

            e.printStackTrace();

            response.getWriter()
                    .write("FAILED: " + e.getMessage());
        }
    }
    
    private void sendToCliq(String fileName, String base64Content,long userId,String filePath, String projectId) throws IOException {
		System.out.println(fileName);

	    String webhookUrl = "https://cliq.zoho.in/api/v2/bots/YOUR_BOT_UNIQUE_NAME/incoming?zapikey=YOUR_API_KEY_HERE";

	    URL url = new URL(webhookUrl);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

	    conn.setRequestMethod("POST");
	    conn.setRequestProperty("Content-Type", "application/json");
	    conn.setDoOutput(true);

	    String jsonPayload =
	            "{"
	            + "\"userId\":\"" + userId + "\","
	            + "\"filePath\":\"" + filePath + "\","
	            + "\"actioncl\":\"" + "gitupload" + "\","
	            + "\"projectId\":\"" + projectId + "\","
	            + "\"fileName\":\"" + fileName + "\","
	            + "\"content\":\"" + base64Content + "\""
	            + "}";



	    
	    try (OutputStream os = conn.getOutputStream()) {
	        os.write(jsonPayload.getBytes("UTF-8"));
	    }

	    int responseCode = conn.getResponseCode();
	    System.out.println(conn.getResponseMessage());
	    
	    
	    System.out.println("Cliq response: " + responseCode);
	}

}
