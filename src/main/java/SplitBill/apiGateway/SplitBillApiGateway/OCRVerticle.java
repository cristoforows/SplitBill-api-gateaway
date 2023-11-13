package SplitBill.apiGateway.SplitBillApiGateway;


import com.google.cloud.documentai.v1.*;
import com.google.protobuf.ByteString;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.Base64;

public class OCRVerticle extends AbstractVerticle {

  String projectId = "sheets-trial-325110";
  String location = "us"; // Format is "us" or "eu".
  String processorId = "c5b1077c23530417";

  @Override
  public void start() {
    vertx.eventBus().consumer("scanner.handler.addr", this::handleScannerRequest);
  }

  private void handleScannerRequest(io.vertx.core.eventbus.Message<Object> message)  {
    //help to modify the code to accept json object
    JsonObject payload = new JsonObject(message.body().toString());

    //connect to google DocumentAI API
    DocumentProcessorServiceSettings settings;
    try {
      settings =
        DocumentProcessorServiceSettings.newBuilder().build();
    } catch (IOException e) {
      message.fail(500, "Internal Server Error");
      throw new RuntimeException(e);
    }

    try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create(settings)){
      String name =
        String.format("projects/%s/locations/%s/processors/%s", projectId, location, processorId);

      byte[] decodedByteArray = Base64.getDecoder().decode(payload.getString("content"));
      ByteString content = ByteString.copyFrom(decodedByteArray);

      RawDocument document =
        RawDocument.newBuilder().setContent(content).setMimeType("image/jpeg").build();

      ProcessRequest request =
        ProcessRequest.newBuilder().setName(name).setRawDocument(document).build();

      ProcessResponse result = client.processDocument(request);
      Document documentResponse = result.getDocument();


    } catch (IOException e) {
      message.fail(500, "Internal Server Error");
      throw new RuntimeException(e);
    }


    message.reply("Hello from OCRVerticle");
  }

}
