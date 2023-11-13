package SplitBill.apiGateway.SplitBillApiGateway;


import com.google.cloud.documentai.v1.*;
import com.google.protobuf.ByteString;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

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

      List<Document.Entity> documentEntities = documentResponse.getEntitiesList();
      JsonObject response = new JsonObject();
      JsonObject tmpItem = new JsonObject();
      response.put("items", new JsonArray());

      for (Document.Entity entity: documentEntities) {
        if (entity.getType().equals("item")) {
          tmpItem = new JsonObject();
          List<Document.Entity> properties = entity.getPropertiesList();
          for (Document.Entity property : properties) {
            if (property.getType().equals("item_name")) {
              tmpItem.put(property.getType(), property.getMentionText());
            } else if (property.getType().equals("item_price") || property.getType().equals("item_discount")) {
              if (property.getNormalizedValue().getMoneyValue().getNanos() != 0) {
                tmpItem.put(property.getType(), property.getNormalizedValue().getMoneyValue().getUnits() * 100 + property.getNormalizedValue().getMoneyValue().getNanos() / 10000000);
              } else {
                tmpItem.put(property.getType(), property.getNormalizedValue().getMoneyValue().getUnits() * 100);
              }
            } else if (property.getType().equals("item_count")) {
              tmpItem.put(property.getType(), property.getMentionText());
            }
          }
          response.getJsonArray("items").add(tmpItem);
        } else if (entity.getType().equals("place_name")) {
          response.put(entity.getType(), entity.getMentionText());
        } else if (entity.getType().equals("date")) {
          tmpItem = new JsonObject();
          tmpItem.put("year", entity.getNormalizedValue().getDateValue().getYear());
          tmpItem.put("month", entity.getNormalizedValue().getDateValue().getMonth());
          tmpItem.put("day", entity.getNormalizedValue().getDateValue().getDay());
          response.put(entity.getType(), tmpItem);
        } else {
          if (entity.getNormalizedValue().getMoneyValue().getNanos() != 0) {
            response.put(entity.getType(), entity.getNormalizedValue().getMoneyValue().getUnits() * 100 + entity.getNormalizedValue().getMoneyValue().getNanos() / 10000000);
          } else {
            response.put(entity.getType(), entity.getNormalizedValue().getMoneyValue().getUnits() * 100);
          }
        }
      }

      message.reply(response.encodePrettily());
      //iterate through the list and print
    } catch (IOException e) {
      message.fail(500, "Internal Server Error");
      throw new RuntimeException(e);
    }

    message.reply("image is parsed!");
  }
}
