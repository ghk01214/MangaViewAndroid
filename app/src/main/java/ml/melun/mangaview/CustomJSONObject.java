package ml.melun.mangaview;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Map;

/*
getters return default value instead of throwing exceptions
예외를 던지는 대신 기본값을 반환하는 커스텀 JSONObject 클래스
 */

public class CustomJSONObject extends JSONObject {
    // 기본 생성자
    public CustomJSONObject() {
        super();
    }

    // Map으로부터 객체를 복사하여 생성하는 생성자
    public CustomJSONObject(Map copyFrom) {
        super(copyFrom);
    }

    // JSONTokener로부터 객체를 읽어 생성하는 생성자
    public CustomJSONObject(JSONTokener readFrom) throws JSONException {
        super(readFrom);
    }

    // JSON 문자열로부터 객체를 생성하는 생성자
    public CustomJSONObject(String json) throws JSONException {
        super(json);
    }

    // 기존 JSONObject와 이름 배열로부터 객체를 생성하는 생성자
    public CustomJSONObject(JSONObject copyFrom, String[] names) throws JSONException {
        super(copyFrom, names);
    }

    // 지정된 이름의 값을 가져오거나, 없으면 기본값을 반환합니다.
    public Object get(String name, Object def){
        try {
            return super.get(name);
        }catch (JSONException e){
            return def;
        }
    }

    // 지정된 이름의 boolean 값을 가져오거나, 없으면 기본값을 반환합니다.
    public boolean getBoolean(String name, boolean def){
        try {
            return super.getBoolean(name);
        }catch (JSONException e){
            return def;
        }
    }

    // 지정된 이름의 double 값을 가져오거나, 없으면 기본값을 반환합니다.
    public double getDouble(String name, double def){
        try {
            return super.getDouble(name);
        }catch (JSONException e){
            return def;
        }
    }

    // 지정된 이름의 int 값을 가져오거나, 없으면 기본값을 반환합니다.
    public int getInt(String name, int def){
        try {
            return super.getInt(name);
        }catch (JSONException e){
            return def;
        }
    }

    // 지정된 이름의 long 값을 가져오거나, 없으면 기본값을 반환합니다.
    public long getLong(String name, long def){
        try {
            return super.getLong(name);
        }catch (JSONException e){
            return def;
        }
    }

    // 지정된 이름의 String 값을 가져오거나, 없으면 기본값을 반환합니다.
    public String getString(String name, String def){
        try {
            return super.getString(name);
        }catch (JSONException e){
            return def;
        }
    }

    // 지정된 이름의 JSONArray를 가져오거나, 없으면 기본값을 반환합니다.
    public JSONArray getJSONArray(String name, JSONArray def){
        try {
            return super.getJSONArray(name);
        }catch (JSONException e){
            return def;
        }
    }

    // 지정된 이름의 JSONObject를 가져오거나, 없으면 기본값을 반환합니다.
    public JSONObject getJSONObject(String name, JSONObject def){
        try {
            return super.getJSONObject(name);
        }catch (JSONException e){
            return def;
        }
    }
}
