package vn.edu.coursemanager;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small REST client for the two MockAPI resources used by the assignment. */
public class MockApi {
    private final String baseUrl;

    public MockApi(String baseUrl) {
        this.baseUrl = baseUrl.trim().replaceAll("/+$", "");
    }

    public List<Course> getCourses() throws Exception {
        JSONArray array = new JSONArray(request("GET", "/courses", null));
        List<Course> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) result.add(Course.fromJson(array.getJSONObject(i)));
        return result;
    }

    public List<Category> getCategories() throws Exception {
        JSONArray array = new JSONArray(request("GET", "/categories", null));
        List<Category> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) result.add(Category.fromJson(array.getJSONObject(i)));
        return result;
    }

    public Course createCourse(Course value) throws Exception {
        return Course.fromJson(new JSONObject(request("POST", "/courses", value.toJson())));
    }

    public Course updateCourse(Course value) throws Exception {
        return Course.fromJson(new JSONObject(request("PUT", "/courses/" + segment(value.id), value.toJson())));
    }

    public void deleteCourse(String id) throws Exception { request("DELETE", "/courses/" + segment(id), null); }

    public Category createCategory(Category value) throws Exception {
        return Category.fromJson(new JSONObject(request("POST", "/categories", value.toJson())));
    }

    public Category updateCategory(Category value) throws Exception {
        return Category.fromJson(new JSONObject(request("PUT", "/categories/" + segment(value.id), value.toJson())));
    }

    public void deleteCategory(String id) throws Exception { request("DELETE", "/categories/" + segment(id), null); }

    private String request(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/json");
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = stream == null ? "" : readAll(stream);
        connection.disconnect();
        if (status < 200 || status >= 300) {
            String detail = response.length() > 220 ? response.substring(0, 220) : response;
            throw new Exception("Máy chủ trả về " + status + (detail.isEmpty() ? "" : ": " + detail));
        }
        return response;
    }

    private String readAll(InputStream stream) throws Exception {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String segment(String value) throws Exception {
        return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }
}
