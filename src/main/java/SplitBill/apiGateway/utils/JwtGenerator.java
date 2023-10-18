package SplitBill.apiGateway.utils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;


public class JwtGenerator {
  public String generateInitialToken(JsonObject payload) {
    Vertx vertx = Vertx.vertx();

    // Create a JWTAuth object.
    JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("keyboard cat"))); // Set the secret

    // Create expiration time
    JWTOptions options = new JWTOptions().setExpiresInSeconds(600);

    // Generate the token
    String token = provider.generateToken(payload
      .put("iss", "Billy-BillManager")
      .put("sub", "oAuthenticated")
      .put("iat", Instant.now().getEpochSecond()), options);
    vertx.close();

    return token;
  }

  public String generateAccessToken(JsonObject payload) {
    Vertx vertx = Vertx.vertx();

    // Create a JWTAuth object.
    JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("keyboard cat"))); // Set the secret

    // Create expiration time
    JWTOptions options = new JWTOptions().setExpiresInSeconds(3600);

    // Generate the token
    String token = provider.generateToken(payload
      .put("iss", "Billy-BillManager")
      .put("sub", "oAuthenticated")
      .put("iat", Instant.now().getEpochSecond()), options);
    vertx.close();

    return token;
  }

  public String generateRefreshToken(JsonObject payload) {
    Vertx vertx = Vertx.vertx();

    // Create a JWTAuth object.
    JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("keyboard cat"))); // Set the secret

    // Generate the token
    String token = provider.generateToken(payload
      .put("iss", "Billy-BillManager")
      .put("sub", "oAuthenticated")
      .put("iat", Instant.now().getEpochSecond()));
    vertx.close();

    return token;
  }


  public static boolean verifyJwtToken(String JWTToken){
    Vertx vertx = Vertx.vertx();
    AtomicBoolean isJwtTokenValid = new AtomicBoolean(false);

    // Create a JWTAuth object.
    JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setBuffer("keyboard cat"))); // Set the secret

    // Verify a token
    provider.authenticate(new JsonObject().put("jwt", JWTToken), res -> {
        isJwtTokenValid.set(res.succeeded());
    });
    vertx.close();

    return isJwtTokenValid.get();
  }
}
