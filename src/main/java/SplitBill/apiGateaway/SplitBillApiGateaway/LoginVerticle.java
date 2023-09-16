package SplitBill.apiGateaway.SplitBillApiGateaway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class LoginVerticle extends AbstractVerticle {

  @Override
  public void start() {

    vertx.eventBus().consumer("login.handler.addr", message -> {
      JsonObject accessToken = new JsonObject();
      JsonObject authPayload = new JsonObject()
        .put("code", message.body())
        .put("client_id", "952306443252-vj885125b9ki502ftg0fvhvguk8381k0.apps.googleusercontent.com")
        .put("client_secret", "SECRET")
        .put("redirect_uri", "www.example.com")
        .put("grant_type", "authorization_code");

      WebClient client = WebClient.create(vertx);

      client
        .post("accounts.google.com" ,"https://accounts.google.com/o/oauth2/token")
        .sendJsonObject(
          authPayload,
          ar -> {
            if (ar.succeeded()) {
              JsonObject body = ar.result().bodyAsJsonObject();
              accessToken.put("access_token", body.getString("access_token"));
              accessToken.put("refresh_token", body.getString("refresh_token"));
              message.reply(accessToken);
            } else {
              System.out.println("Something went wrong " + ar.cause().getMessage());
            }
          }
        );
    });


  }

}
