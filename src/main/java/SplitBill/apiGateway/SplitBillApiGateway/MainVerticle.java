package SplitBill.apiGateway.SplitBillApiGateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import SplitBill.apiGateway.utils.JwtGenerator;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    vertx.deployVerticle(new SignInVerticle());
    // Create a Router
    Router router = Router.router(vertx);

    router.post("api/v1/auth").consumes("application/json")
      .handler(BodyHandler.create())
      .handler(this::AuthHandler);  // accept Google ID Token and platform OS

    router.post("api/v1/signUp").consumes("application/json")
      .handler(BodyHandler.create())
      .handler(this::FormAuthHandler)
      .handler(this::OTPHandler)
      .handler(this::SignUpHandler);  // sign up

    router.post("api/v1/signIn").consumes("application/json")
      .handler(BodyHandler.create())
      .handler(this::FormAuthHandler)
      .handler(this::OTPHandler)
      .handler(this::SignInHandler);  // sign in


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
    String token = routingContext.request().getHeader("Authorization");

    // Verify the authentication token.
    boolean isTokenValid = JwtGenerator.verifyJwtToken(token);
    if(isTokenValid){
      routingContext.next();
    }else{
      routingContext.response().setStatusCode(401).end("Unauthorized");
    }
  }

  private void OTPHandler(RoutingContext routingContext){

  }

  private void SignUpHandler(RoutingContext routingContext) {
    //send post request to user service to register new user
    //create access and refresh token
    vertx.eventBus().request("signUp.handler.addr", "sign up", reply -> {
      if (reply.succeeded()) {
        routingContext.response().putHeader("Content-type", "application/json").end(reply.result().body().toString());
      } else {
        routingContext.response().end("User exists");
      }
    });
  }

  private void SignInHandler(RoutingContext routingContext) {
    vertx.eventBus().request("signIn.handler.addr", "sign in", reply -> {
      if (reply.succeeded()) {
        routingContext.response().putHeader("Content-type", "application/json").end(reply.result().body().toString());
      } else {
        routingContext.response().end("User exists");
      }
    });
  }


  void AuthHandler(RoutingContext routingContext) {
    vertx.eventBus().request("login.handler.addr", "authenticate", reply -> {
      if (reply.succeeded()) {
        routingContext.response().putHeader("Content-type", "application/json").end(reply.result().body().toString());
      } else {
        routingContext.response().end("User exists");
      }
    });
  }


}
