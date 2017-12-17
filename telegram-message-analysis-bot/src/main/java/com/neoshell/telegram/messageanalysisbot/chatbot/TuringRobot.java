package com.neoshell.telegram.messageanalysisbot.chatbot;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.neoshell.telegram.messageanalysisbot.handler.ChatBotReply;

public class TuringRobot implements ChatBotInterface {

  private static final String API_URL = "http://www.tuling123.com/openapi/api";

  private String apiKey;

  public TuringRobot(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public ChatBotReply chat(String userId, String text) throws Exception {
    JSONObject inputJSONObject = new JSONObject();
    inputJSONObject.put("key", apiKey);
    inputJSONObject.put("info", text);
    inputJSONObject.put("userid", userId);
    String response = doPost(API_URL, inputJSONObject.toString());
    JSONTokener jsonTokener = new JSONTokener(response);
    JSONObject outputJSONObject = (JSONObject) jsonTokener.nextValue();
    int code = outputJSONObject.getInt("code");
    ChatBotReply reply = null;
    // TODO: handle other response type.
    switch (code) {
    case 100000: // Text.
      String replyText = outputJSONObject.getString("text");
      reply = new ChatBotReply(replyText);
      break;
    }
    return reply;
  }

  private String doPost(String url, String content)
      throws ClientProtocolException, IOException {
    HttpClient httpclient = HttpClients.createDefault();
    HttpPost post = new HttpPost(url);
    String response = null;
    StringEntity stringEntity = new StringEntity(content, "UTF-8");
    // Required for sending json
    stringEntity.setContentType("application/json");
    stringEntity.setContentEncoding("UTF-8");
    post.setEntity(stringEntity);
    HttpResponse httpResponse = httpclient.execute(post);
    if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      return EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
    }
    return response;
  }

}
