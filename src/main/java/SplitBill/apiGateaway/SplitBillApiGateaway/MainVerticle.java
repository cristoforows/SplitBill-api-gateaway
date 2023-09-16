package SplitBill.apiGateaway.SplitBillApiGateaway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    // Create a Router
    Router router = Router.router(vertx);


    router.post("api/v1/login").handler(this::loginHandler);  // login

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888)
      .onSuccess(server ->
        System.out.println(
          "HTTP server started on port " + server.actualPort()
        )
      );
  }

  void loginHandler(RoutingContext routingContext) {
    vertx.eventBus().request("login.handler.addr", "login", reply -> {
      if (reply.succeeded()) {
        routingContext.response().putHeader("Content-type", "application/json").end(reply.result().body().toString());
      } else {
        routingContext.response().end("User is not found");
      }
    });
  }
}
