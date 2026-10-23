package vn.edu.coursemanager;

import org.json.JSONObject;

public class Category {
    public String id = "", name = "", description = "";

    public static Category fromJson(JSONObject json) {
        Category c = new Category();
        c.id = json.optString("id", "");
        c.name = json.optString("name", "");
        c.description = json.optString("description", "");
        return c;
    }

    public JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("name", name.trim());
        json.put("description", description.trim());
        return json;
    }
}
