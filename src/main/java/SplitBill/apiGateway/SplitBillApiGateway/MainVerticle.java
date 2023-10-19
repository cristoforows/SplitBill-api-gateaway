package SplitBill.apiGateway.SplitBillApiGateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import SplitBill.apiGateway.utils.JwtGenerator;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start() {
    vertx.deployVerticle(new SignInVerticle());
    // Create a Router
    Router router = Router.router(vertx);

    router.post("/api/v1/auth").consumes("application/json")
      .handler(BodyHandler.create())
      .handler(this::GAuthHandler);  // accept Google ID Token and platform OS

    router.post("/api/v1/signUp").consumes("application/json")
      .handler(BodyHandler.create())
      .handler(this::FormAuthHandler)
      .handler(this::OTPHandler)
      .handler(this::SignUpHandler);  // sign up

    router.post("/api/v1/signIn").consumes("application/json")
      .handler(BodyHandler.create())
      .handler(this::FormAuthHandler)
      .handler(this::OTPHandler)
      .handler(this::SignInHandler);  // sign in

    router.get("/api/v1/test").consumes("application/json")
        .handler(BodyHandler.create())
        .handler(this::AuthHandler)
        .handler(this::testHandler);  // sign in


    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888)
      .onSuccess(server ->
        System.out.println(
          "HTTP server started on port " + server.actualPort()
        )
      );
  }

  private void FormAuthHandler(RoutingContext routingContext) {
    //check JWT token for sign up / log in
    String token ="";
    try {
      token = routingContext.request().getHeader("Authorization").split(" ")[1];
    }catch (Exception e) {
      routingContext.response().setStatusCode(401).end("Unauthorized");
    }

    // Verify the authentication token.
    boolean isTokenValid = JwtGenerator.verifyJwtToken(token, vertx);
    if(isTokenValid){
      routingContext.next();
    }else{
      routingContext.response().setStatusCode(401).end("Unauthorized");
    }
  }

  private void AuthHandler(RoutingContext routingContext){
    //check JWT token for sign up / log in
    String token = routingContext.request().getHeader("Authorization").split(" ")[1];

    // Verify the authentication token.
    boolean isTokenValid = JwtGenerator.verifyAccessToken(token,vertx);
    if(isTokenValid){
      routingContext.next();
    }else{
      routingContext.response().setStatusCode(401).end("Unauthorized");
    }
  }

  private void testHandler(RoutingContext routingContext){
    routingContext.response().end("test");
  }

  private void OTPHandler(RoutingContext routingContext){
    routingContext.next();
  }

  private void SignUpHandler(RoutingContext routingContext) {
    //send post request to user service to register new user
    //create access and refresh token
    String payload = routingContext.body().asString();
    vertx.eventBus().request("signUp.handler.addr", payload, reply -> {
      if (reply.succeeded()) {
        routingContext.response().putHeader("Content-type", "application/json").end(reply.result().body().toString());
      } else {
        routingContext.response().end("User exists");
      }
    });
  }

  private void SignInHandler(RoutingContext routingContext) {
    String payload = routingContext.body().asString();
    vertx.eventBus().request("signIn.handler.addr", payload, reply -> {
      if (reply.succeeded()) {
        routingContext.response().putHeader("Content-type", "application/json").end(reply.result().body().toString());
      } else {
        routingContext.response().end("User exists");
      }
    });
  }

  void GAuthHandler(RoutingContext routingContext) {
    String payload = routingContext.body().asString();
    vertx.eventBus().request("login.handler.addr", payload, reply -> {
      if (reply.succeeded()) {
        routingContext.response().putHeader("Content-type", "application/json").end(reply.result().body().toString());
      } else {
        routingContext.response().end("User exists");
      }
    });
  }


}
