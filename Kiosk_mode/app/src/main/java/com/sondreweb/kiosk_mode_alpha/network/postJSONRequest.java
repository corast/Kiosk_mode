package com.sondreweb.kiosk_mode_alpha.network;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Ikke i bruk.
 *
 * Forsøk på å lage en JSONRequest som kunne sende et Array istedet for et object. Men igjen, fungerte ikke etter planen.
 */

public class postJSONRequest extends JsonArrayRequest {

    @Override protected Map<String, String> getParams() throws AuthFailureError {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("name", "value");
        params.put("besokId", "value");
        params.put("dato", "value");
        params.put("tid", "value");
        return params;
    }

    public postJSONRequest(String url, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, null, listener, errorListener);
    }

}
