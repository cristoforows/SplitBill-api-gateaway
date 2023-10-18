package SplitBill.apiGateway.SplitBillApiGateway;


import SplitBill.apiGateway.utils.JwtGenerator;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.http.HttpTransport;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;


public class SignInVerticle extends AbstractVerticle {

  String AndroidClientID = "488800823172-3g6vkc18qc4oh2k4364lp39i0156ealb.apps.googleusercontent.com";
  String IOSClientID = "488800823172-db5uh8fjq74gsb88mbcj7q3kmiifbsr6.apps.googleusercontent.com";

  private boolean isEmailRegistered = false;
  private String phoneNumber = "";
  private String user_id;


  @Override
  public void start() throws Exception {

    // initial sign in
    vertx.eventBus().consumer("login.handler.addr", message -> {
      //help to modify the code to accept json object
      JsonObject payload = new JsonObject(message.body().toString());
      String email = "";
      String name = "";
      String picture = "";

      //check if access token is valid
      String ClientID = "";
      String idTokenString = payload.getString("idToken");
      String platformOS = payload.getString("platformOS");

      if (platformOS.equals("Android")) {
        ClientID = AndroidClientID;
      } else if (platformOS.equals("IOS")) {
        ClientID = IOSClientID;
      } else {
        message.fail(400, "PlatformOS is not valid");
        throw new RuntimeException("PlatformOS is not valid");
      }

      // Create an HttpTransport instance
      HttpTransport httpTransport;
      try {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      } catch (GeneralSecurityException | IOException e) {
        message.fail(500, "Internal Server Error");
        throw new RuntimeException(e);
      }

      JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      GooglePublicKeysManager publicKeysManager = new GooglePublicKeysManager(httpTransport, jsonFactory);
      GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(publicKeysManager)
        .setAudience(Collections.singletonList(ClientID))
        .build();
      GoogleIdToken idToken = null;

      try {
        idToken = verifier.verify(idTokenString);
      } catch (GeneralSecurityException | IOException e) {
        message.fail(401, "Invalid ID token.");
        throw new RuntimeException(e);
      }

      Payload idTokenPayload = idToken.getPayload();

      // Get profile information from payload
      email = idTokenPayload.getEmail();
      name = (String) idTokenPayload.get("name");
      picture = (String) idTokenPayload.get("picture");


      //check if email is in the database
      //do post request to the user service to check if email is in the database
      //send get request to user service to check if email is in the database
      WebClient client = WebClient.create(vertx);
      client
        .get(8080, "billManager.com", "/user")
        .addQueryParam("email", email)
        .send()
        .onSuccess(this::handleResponse);

      JsonObject responsePayload = new JsonObject();
      if (isEmailRegistered) {
        responsePayload
          .put("isEmailRegistered", true)
          .put("sub", user_id)
          .put("name", name)
          .put("email", email);
      } else {
        responsePayload
          .put("isEmailRegistered", false)
          .put("sub", "new user")
          .put("name", name)
          .put("email", email);
      }

      JwtGenerator jwtGenerator = new JwtGenerator();
      String jwtToken = jwtGenerator.generateInitialToken(responsePayload);
      responsePayload
        .put("picture", picture)
        .put("phone_number", phoneNumber)
        .put("token", jwtToken);

      message.reply(responsePayload);
    });

    //sign up
    vertx.eventBus().consumer("signUp.handler.addr", message -> {
      //forward to user service
      WebClient client = WebClient.create(vertx);
      client
        .post(8080, "billManager.com", "/signUp")
        .sendJsonObject(new JsonObject(message.body().toString()))
        .onSuccess(response -> {
          if (response.statusCode() == 200) {
            JsonObject payloadJson = response.bodyAsJsonObject();

            message.reply(prepareToken(payloadJson));
          } else {
            message.fail(response.statusCode(), response.bodyAsString());
          }
        });
    });

    //sign in
    vertx.eventBus().consumer("signIn.handler.addr", message -> {
      //extract payload from jwtToken
      String jwtToken = message.headers().get("Authorization").split(" ")[0];
      Base64.Decoder decoder = Base64.getUrlDecoder();
      String jwtTokenPayload = Arrays.toString((decoder.decode(jwtToken.split("\\.")[1])));
      JsonObject payloadJson = new JsonObject(jwtTokenPayload);

      message.reply(prepareToken(payloadJson));
    });
  }

  private JsonObject prepareToken(JsonObject payloadJson){
    String id = payloadJson.getString("sub");
    String email = payloadJson.getString("email");
    String name = payloadJson.getString("name");

    JsonObject tokenInformation = new JsonObject().put("sub", id).put("email", email).put("name", name);
    JwtGenerator jwtGenerator = new JwtGenerator();
    String accessToken = jwtGenerator.generateAccessToken(tokenInformation);
    String refreshToken = jwtGenerator.generateRefreshToken(tokenInformation);

    return new JsonObject().put("access_token", accessToken).put("refresh_token", refreshToken);
  }

  private void handleResponse(HttpResponse<Buffer> response) {
    if (response.statusCode() == 200) {
      isEmailRegistered = true;
      phoneNumber = response.bodyAsJsonObject().getString("phoneNumber");
      user_id = response.bodyAsJsonObject().getString("user_id");
    } else if (response.statusCode() == 404) {
      // Redirect to the sign-up page.
      isEmailRegistered = false;
    } else {
      throw new RuntimeException("User service is not working");
    }
  }
}


