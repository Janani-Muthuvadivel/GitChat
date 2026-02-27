package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import util.DBConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Servlet implementation class ZiaUploadFileServlet
 */
@WebServlet("/ZiaUploadFileServlet")
public class ZiaUploadFileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ZiaUploadFileServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws IOException, ServletException {

	    response.setContentType("text/plain");

	    byte[] zipBytes = (byte[]) request.getAttribute("zipBytes");
//	    int fileId = (int) request.getAttribute("index");
	    String fileIdStr = (String) request.getAttribute("index");
	    int fileId = Integer.parseInt(fileIdStr);

	    try {

	        
//	        InputStream fileInputStream = filePart.getInputStream();
//	        byte[] zipBytes = fileInputStream.readAllBytes();
	        

	        Connection con = DBConnection.getConnection();
	        PreparedStatement psZip = con.prepareStatement(
	        	    "UPDATE files SET uploaded_content = ? WHERE file_id = ?");

	        	psZip.setBytes(1, zipBytes);
	        	psZip.setInt(2, fileId);

	        	psZip.executeUpdate();
	        	
	        PreparedStatement changeStatus = con.prepareStatement("UPDATE requests SET status = ? where file_id = ?");
	        changeStatus.setString(1, "Yet to validate");
	        changeStatus.setInt(2, fileId);
	        changeStatus.executeUpdate();
	        	
	        PreparedStatement validateRecord = con.prepareStatement("SELECT created_at,file_path,user_id,vendor_name from files f join requests r on f.file_id = r.file_id join vendors v on r.vendor_id = v.vendor_id  where f.file_id = ?");
	        validateRecord.setInt(1, fileId);
	        ResultSet recordforvalidation = validateRecord.executeQuery();
	        
	        if(recordforvalidation.next()) {
	        	String fileName = "application.properties";
	        	String filePath = recordforvalidation.getString("file_path");
	        	Date date = recordforvalidation.getDate("created_at");
	        	String userId = recordforvalidation.getString("user_id");
	        	String vendorName = recordforvalidation.getString("vendor_name");
	        	System.out.println("File name : "+fileName +"\nFile path : "+filePath+"\nCreated at : "+date+"\nUser id : "+userId);
	        	sendToCliqBeforeValidation(fileId,fileName,date.toString(),filePath,Long.parseLong(userId),vendorName);
	        }
	       
	        PreparedStatement ps1 = con.prepareStatement(
	                "SELECT request_id, user_id FROM requests WHERE file_id = ?");
	        ps1.setInt(1, fileId);
	        ResultSet rs1 = ps1.executeQuery();

	        int requestId = 0;
	        String user_id = null;

	        if (rs1.next()) {
	            requestId = rs1.getInt("request_id");
	            user_id = rs1.getString("user_id");
	        }

	        long userId = Long.parseLong(user_id);

	       
	        PreparedStatement ps2 = con.prepareStatement(
	                "SELECT l.language_code FROM requested_languages rl " +
	                "JOIN languages l ON rl.language_id = l.language_id " +
	                "WHERE rl.request_id = ?");
	        ps2.setInt(1, requestId);

	        ResultSet rs2 = ps2.executeQuery();
	        List<String> requestedLanguages = new ArrayList<>();

	        while (rs2.next()) {
	            requestedLanguages.add(rs2.getString("language_code"));
	        }

	       
	        PreparedStatement ps3 = con.prepareStatement(
	                "SELECT file_content, file_path, project_id FROM files WHERE file_id = ?");
	        ps3.setInt(1, fileId);

	        ResultSet rs3 = ps3.executeQuery();

	        Properties baseProps = new Properties();
	        String filePath = "";
	        String projectId = "";

	        if (rs3.next()) {
	            Blob blob = rs3.getBlob("file_content");
	            filePath = rs3.getString("file_path");
	            projectId = rs3.getString("project_id");

	            InputStream is = blob.getBinaryStream();
	            baseProps.load(is);
	        }
	        
	        

	        Set<String> baseKeys = baseProps.stringPropertyNames();

	        
	        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes));
	        ZipEntry entry;

	        Map<String, byte[]> validFiles = new HashMap<>();
	        Set<String> foundLanguages = new HashSet<>();

	        boolean validationFailed = false;
	        String errorMessage = "";

	        while ((entry = zis.getNextEntry()) != null) {

	            if (entry.isDirectory()) continue;

	            String fileName = new File(entry.getName()).getName();
	            System.out.println("Actual file name === "+fileName);

	            for (String lang : requestedLanguages) {

	                String expectedFileName = "application_" + lang + ".properties";
	                System.out.println("Expected file name === "+expectedFileName);

	                if (fileName.equals(expectedFileName)) {

	                    foundLanguages.add(lang);

	                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	                    byte[] buffer = new byte[1024];
	                    int len;

	                    while ((len = zis.read(buffer)) != -1) {
	                        baos.write(buffer, 0, len);
	                    }

	                    byte[] fileBytes = baos.toByteArray();

	                    
	                    Properties uploadedProps = new Properties();
	                    uploadedProps.load(new ByteArrayInputStream(fileBytes));

	                    Set<String> uploadedKeys = uploadedProps.stringPropertyNames();

	                    if (!uploadedKeys.equals(baseKeys)) {
	                        validationFailed = true;
	                        errorMessage = "Keys mismatch in " + fileName;
	                        break;
	                    }

	                    
	                    validFiles.put(fileName, fileBytes);
	                }
	            }

	            if (validationFailed) break;

	            zis.closeEntry();
	        }

	        zis.close();

	        
	        if (!validationFailed &&
	                !foundLanguages.containsAll(requestedLanguages)) {

	            validationFailed = true;
	            errorMessage = "Some requested language files are missing in ZIP";
	        }

	      
	        if (validationFailed) {
	        	PreparedStatement ps = con.prepareStatement("UPDATE files SET is_uploadable = ? where file_id = ?");
	        	ps.setString(1, "false");
	        	ps.setInt(2, fileId);
	        	ps.executeUpdate();
	        	ps = con.prepareStatement("UPDATE requests SET status = ? where file_id = ?");
	        	ps.setString(1, "Submitted");
	        	ps.setInt(2, fileId);
	        	ps.executeUpdate();
	        	System.out.println("not ok");
	            response.getWriter().write("FAILED: " + errorMessage);
	            

	        } else {


	        	PreparedStatement ps = con.prepareStatement("UPDATE files SET is_uploadable = ? where file_id = ?");
	        	ps.setString(1, "true");
	        	ps.setInt(2, fileId);
	        	ps.executeUpdate();
	        	ps = con.prepareStatement("UPDATE requests SET status = ? where file_id = ?");
	        	ps.setString(1, "Submitted");
	        	ps.setInt(2, fileId);
	        	ps.executeUpdate();
	        	
	        	System.out.println("ok");
	            response.getWriter().write("SUCCESS");
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        response.getWriter().write("FAILED: Internal server error");
	    }
	}

	
	
	
private void sendToCliqBeforeValidation(int fileId, String fileName, String createdTime, String filePath, long userId,String vendorName) throws IOException, InterruptedException {
	Thread.sleep(3000);
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
            + "\"actioncl\":\"" + "cliqupload" + "\","
            + "\"fileId\":\"" + fileId + "\","
            + "\"fileName\":\"" + fileName + "\","
            + "\"vendorName\":\"" + vendorName + "\","
            + "\"createdTime\":\"" + createdTime + "\""
            + "}";


    try (OutputStream os = conn.getOutputStream()) {
        os.write(jsonPayload.getBytes("UTF-8"));
    }

    int responseCode = conn.getResponseCode();
    System.out.println(conn.getResponseMessage());
    
    
    System.out.println("Cliq response: " + responseCode);
	}
}
