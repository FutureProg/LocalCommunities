package nick.com.localcommunity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nick on 16-07-12.
 */
public class Utilities {

    public static String getAreaInfoByType(JSONArray array,String type){
        for(int i = 0; i < array.length();i++){
            try {
                JSONObject obj = array.getJSONObject(i);
                JSONArray types = obj.getJSONArray("types");
                if(types.toString().contains("\""+type+"\"")){
                    return (String)obj.get("short_name");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return null;
    }

}
