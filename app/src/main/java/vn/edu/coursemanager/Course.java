package vn.edu.coursemanager;

import org.json.JSONObject;

public class Course {
    public String id = "", title = "", description = "", categoryId = "", instructor = "";
    public String thumbnail = "", trailerUrl = "", createdAt = "";
    public double price = 0;
    public int duration = 0;

    public static Course fromJson(JSONObject json) {
        Course c = new Course();
        c.id = json.optString("id", "");
        c.title = json.optString("title", "");
        c.description = json.optString("description", "");
        Object category = json.opt("categoryId");
        c.categoryId = category == null || category == JSONObject.NULL ? "" : String.valueOf(category);
        c.instructor = json.optString("instructor", "");
        c.price = json.optDouble("price", 0);
        c.duration = json.optInt("duration", 0);
        c.thumbnail = json.optString("thumbnail", "");
        c.trailerUrl = json.optString("trailerUrl", "");
        c.createdAt = json.optString("createdAt", "");
        return c;
    }

    public JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("title", title.trim());
        json.put("description", description.trim());
        json.put("categoryId", categoryId);
        json.put("instructor", instructor.trim());
        json.put("price", price);
        json.put("duration", duration);
        json.put("thumbnail", thumbnail.trim());
        json.put("trailerUrl", trailerUrl.trim());
        if (createdAt != null && !createdAt.isEmpty()) json.put("createdAt", createdAt);
        return json;
    }
}
