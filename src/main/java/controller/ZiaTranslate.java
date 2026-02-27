package controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.DBConnection;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/ZiaTranslate")
public class ZiaTranslate extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String API_URL = "https://dl.zoho.in/api/v1/nlp/translation/translate"; 
	private static String ACCESS_TOKEN = "YOUR_ACCESS_TOKEN_HERE";
    private static final String CLIENT_ID = "YOUR_CLIENT_ID_HERE";
    private static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET_HERE";
    private static final String REFRESH_TOKEN = "YOUR_REFRESH_TOKEN_HERE";
    public ZiaTranslate() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Entered Zia Translate");
		String message = (String) request.getAttribute("fileId");
//		String message = "437982";
		System.out.println(message);
		ArrayList<String> languageCodes = new ArrayList<>();
		ArrayList<String> originalvalues = new ArrayList<>();
		ArrayList<String> originalkeys = new ArrayList<>();
		try {
			Connection con = DBConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("select language_code from requests r join requested_languages rl on r.request_id = rl.request_id join languages l on l.language_id = rl.language_id where r.file_id = ?");
			ps.setString(1, message);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String language = rs.getString("language_code");
				languageCodes.add(language);
			}
			ps = con.prepareStatement("select file_content from files where file_id = ?");
			ps.setString(1, message);
			rs = ps.executeQuery();
			if(rs.next()) {
				 InputStream is = rs.getBinaryStream("file_content");

				    Properties props = new Properties();
				    props.load(is);   
				    props.forEach((key, value) -> originalvalues.add((String) value));
				    props.forEach((key, value) -> originalkeys.add((String) key));
				    System.out.println(originalkeys);

				    is.close();
				    String regeneratedtoken = regenerateAccessToken(CLIENT_ID,CLIENT_SECRET,REFRESH_TOKEN);

				    regeneratedtoken = regenerateAccessToken(CLIENT_ID,CLIENT_SECRET,REFRESH_TOKEN);

				    ByteArrayOutputStream baos = new ByteArrayOutputStream();
				    ZipOutputStream zos = new ZipOutputStream(baos);

				    for(String lang : languageCodes) {
				    	Thread.sleep(500);

				        String result = callTranslationAPI(regeneratedtoken, originalvalues, lang);

				        Map<String,String> translatedMap = parseTranslation(result);
				        List<String> valuesList = new ArrayList<>(translatedMap.values());

				        String fileName = "application_" + lang + ".properties";

				        ZipEntry entry = new ZipEntry(fileName);
				        zos.putNextEntry(entry);

				        Properties newProps = new Properties();

				        for(int i = 0; i < originalkeys.size(); i++) {
				            String key = originalkeys.get(i);
				            System.out.println(key+valuesList.get(i));
				            newProps.setProperty(key, valuesList.get(i));
				        }


				        StringBuilder builder = new StringBuilder();

				        for (String key : originalkeys) {
				            builder.append(key)
				                   .append("=")
				                   .append(newProps.getProperty(key))
				                   .append("\n");
				        }

				        zos.write(builder.toString().getBytes("UTF-8"));

				        zos.closeEntry();
				    }

				    zos.close();

				    byte[] zipBytes = baos.toByteArray();
				    request.setAttribute("zipBytes", zipBytes);
				    request.setAttribute("index", message);
				    PreparedStatement psZip = con.prepareStatement(
				    	    "UPDATE files SET uploaded_content = ? WHERE file_id = ?");

				    psZip.setBytes(1, zipBytes);
				    psZip.setString(2, message);

				    psZip.executeUpdate();

				    // forward to second servlet
				    RequestDispatcher dispatcher =
				            request.getRequestDispatcher("/ZiaUploadFileServlet");

				    dispatcher.forward(request, response);

				    
			}
			else {
				System.out.println("No files found");
			}
			System.out.println();
			System.out.println(languageCodes);
			
		}
		catch(Exception e) {
			
		}
		
	}
	
	private String callTranslationAPI(String accessToken,
            ArrayList<String> sentences,
            String targetLanguage) throws IOException {

			String boundary = "----Boundary" + UUID.randomUUID();
			String lineEnd = "\r\n";

			URL url = new URL(API_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setRequestMethod("POST");

			conn.setRequestProperty(
					"Content-Type",
					"multipart/form-data; boundary=" + boundary);

			conn.setRequestProperty(
					"Authorization",
					"Bearer " + accessToken);

			OutputStream output = conn.getOutputStream();

			PrintWriter writer = new PrintWriter(
					new OutputStreamWriter(output, "UTF-8"), true);


			StringBuilder jsonBuilder = new StringBuilder();
				jsonBuilder.append("{\"sentences\": [");

				for (int i = 0; i < sentences.size(); i++) {
					jsonBuilder.append("\"").append(sentences.get(i)).append("\"");

					if (i != sentences.size() - 1) {
						jsonBuilder.append(", ");
					}
				}

				jsonBuilder.append("]}");

				String jsonText = jsonBuilder.toString();
				addFormField(writer, boundary, "text", jsonText, lineEnd);
				addFormField(writer, boundary, "source_language", "auto", lineEnd);	
				addFormField(writer, boundary, "target_language", targetLanguage, lineEnd);

				writer.append("--").append(boundary).append("--").append(lineEnd);
				writer.flush();
				writer.close();


				InputStream inputStream;

				if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
					inputStream = conn.getInputStream();
				else
					inputStream = conn.getErrorStream();


				BufferedReader reader =
						new BufferedReader(new InputStreamReader(inputStream));

				StringBuilder response = new StringBuilder();
				String line;

				while ((line = reader.readLine()) != null)
					response.append(line);

				reader.close();

				return response.toString();
	}
    
	private void addFormField(PrintWriter writer,
            String boundary,
            String name,
            String value,
            String lineEnd) {

writer.append("--").append(boundary).append(lineEnd);
writer.append("Content-Disposition: form-data; name=\"")
.append(name).append("\"")
.append(lineEnd);
writer.append("Content-Type: text/plain; charset=UTF-8")
.append(lineEnd);
writer.append(lineEnd);
writer.append(value).append(lineEnd);
writer.flush();
}
	
	private String regenerateAccessToken(
            String clientId,
            String clientSecret,
            String refreshToken) throws IOException {

        URL url = new URL("https://accounts.zoho.in/oauth/v2/token");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        conn.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded"
        );

        String params =
                "client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8") +
                "&grant_type=refresh_token" +
                "&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8");

        OutputStream os = conn.getOutputStream();
        os.write(params.getBytes());
        os.flush();
        os.close();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null)
            response.append(line);

        reader.close();
        String json = response.toString();

        String accessToken =
                json.split("\"access_token\":\"")[1]
                    .split("\"")[0];

        return accessToken;
    }
	
	public Map<String,String> parseTranslation(String jsonResponse) throws Exception {
		System.out.println("parsetranslation === "+jsonResponse);
	    Map<String,String> translatedMap = new LinkedHashMap<>();

	    JSONObject jsonObject = new JSONObject(jsonResponse);

	    JSONArray translationArray = jsonObject.getJSONArray("translation");

	    for(int i = 0; i < translationArray.length(); i++) {

	        JSONObject obj = translationArray.getJSONObject(i);

	        String source = obj.getString("source");
	        String translated = obj.getString("translate");

	        translatedMap.put(source, translated);
	    }
	    
	    System.out.println(translatedMap);

	    return translatedMap;
	}

}
