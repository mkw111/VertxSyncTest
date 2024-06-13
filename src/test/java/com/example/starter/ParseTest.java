package com.example.starter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParseTest {

  @Test
  public void testParseMethod() {
    // Mock input data
    JsonArray jaIn = new JsonArray();
    JsonObject input1 = new JsonObject()
      .put("flag", 1)
      .put("unit", 2)
      .put("dKwh", 100.0)
      .put("cptr_date", "2024-06-13")
      .put("nSeqMeter", 1);
    jaIn.add(input1);

    // Create an instance of the class containing parse method
    ParseKwhList parser = new ParseKwhList(ServiceSvc.getInstance());

    // Create a JsonObject to hold the output
    JsonObject joOut = new JsonObject();

    // Call the parse method
    parser.parse(jaIn, joOut);

    // Log joOut to verify its contents (optional)
    System.out.println("joOut after parsing: " + joOut.encodePrettily());

    // Add assertions to verify expected values
    assertEquals(highDWonSumPrev, joOut.getDouble("total_won_curr_high"));
    assertEquals(lowDWonSumPrev, joOut.getDouble("total_won_curr_low"));
    assertEquals(highDWonSumCurr, joOut.getDouble("total_won_prev_high"));
    assertEquals(lowDWonSumCurr, joOut.getDouble("total_won_prev_low"));
    // Add more assertions for other values as needed
  }

  // Define expected values for assertions (replace with actual expected values)
  private double highDWonSumPrev = 100.0;
  private double lowDWonSumPrev = 50.0;
  private double highDWonSumCurr = 100.0;
  private double lowDWonSumCurr = 50.0;
  // Define more expected values as needed
}
