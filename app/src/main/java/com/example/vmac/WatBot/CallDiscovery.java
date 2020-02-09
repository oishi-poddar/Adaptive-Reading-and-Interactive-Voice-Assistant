package com.example.vmac.WatBot;

import android.os.AsyncTask;
import android.util.Log;

import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.watson.discovery.v1.Discovery;
import com.ibm.watson.discovery.v1.model.QueryOptions;
import com.ibm.watson.discovery.v1.model.QueryResponse;

import static android.content.ContentValues.TAG;

public class CallDiscovery extends AsyncTask<String, Void, String> {

    private Exception exception;

    protected String doInBackground(String... params) {
        try {

            IamOptions options = new IamOptions.Builder()
                    .apiKey("Lrsk5kWvXH7EXDZcj8Iuij-wdF_s6FcvtavoQa4lA8Yx")
                    .build();
            Discovery discovery = new Discovery("2019-09-20",options);
            discovery.setEndPoint("https://gateway-lon.watsonplatform.net/discovery/api");

//Build an empty query on an existing environment/collection
            String environmentId = "6559639d-7ddf-4ac5-8bf0-e90c7d1cd57b";
            String collectionId = "6d9c8cdf-d15c-4b51-a881-5f3f5a262e5c";
            QueryOptions queryOptions = new QueryOptions.Builder(environmentId, collectionId).query(params[0]).highlight(true).build();
            QueryResponse queryResponse = discovery.query(queryOptions).execute().getResult();
            String highlight= queryResponse.getResults().get(0).get("highlight").toString();
            Log.i(TAG,""+highlight.substring(highlight.indexOf("answer=")+7,highlight.indexOf(".,")));
            return (highlight.substring(highlight.indexOf("answer=")+7,highlight.indexOf(".,")));
        } catch (Exception e) {
            this.exception = e;
            Log.i(TAG,"exception: "+exception);
        }
        return "done";
    }

    protected void onPostExecute() {
        // TODO: check this.exception
        // TODO: do something with the feed
    }
}