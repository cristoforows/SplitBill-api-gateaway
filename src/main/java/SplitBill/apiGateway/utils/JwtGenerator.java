package SplitBill.apiGateway.utils;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

import java.time.Instant;


public class JwtGenerator {

  public enum JwtType {
    INITIAL,
    ACCESS,
    REFRESH
  }

  public static String generateToken(JsonObject payload, Vertx vertx, JwtType type) {
    String secret = useSecret(type);
    return generateJwt(new JsonObject(payload.toString()), vertx, secret, type);
  }

  public static String generateJwt(JsonObject payload, Vertx vertx, String secret, JwtType type) {
    // Create a JWTAuth object.
    JWTAuth provider = generateJwtAuth(vertx, secret);

    // Create expiration time

    JWTOptions options = setExpirationTime(type);

    // Generate the token
    String token = provider.generateToken(payload
      .put("iss", "Billy-BillManager")
      .put("sub", "oAuthenticated")
      .put("iat", Instant.now().getEpochSecond()), options);

    return token;
  }

  public static JWTOptions setExpirationTime(JwtType type) {
    switch (type) {
      case INITIAL:
        return new JWTOptions().setExpiresInSeconds(600);
      case ACCESS:
        return new JWTOptions().setExpiresInSeconds(3600);
      default:
        return new JWTOptions();
    }
  }

  public static void verifyToken(String token, Vertx vertx, JwtType type, Handler<Boolean> resultHandler) {
    String secret = useSecret(type);
    JWTAuth provider = generateJwtAuth(vertx, secret);

    provider.authenticate(new JsonObject().put("token", token), res -> {
      boolean isValid = res.succeeded();
      resultHandler.handle(isValid);
    });
  }

  public static String useSecret(JwtType type) {
    switch (type) {
      case INITIAL:
        return "Initial secret";
      case ACCESS:
        return "Access token secret";
      case REFRESH:
        return "Refresh token secret";
      default:
        return "";
    }
  }

  public static JWTAuth generateJwtAuth(Vertx vertx, String secret) {
    return JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer(secret)));
  }

}
