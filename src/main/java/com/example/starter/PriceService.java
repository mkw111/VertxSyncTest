package com.example.starter;

import io.vertx.core.Future;
import io.vertx.core.Promise;

import java.util.Arrays;
import java.util.List;

public class PriceService {

  public Future<List<Double>> calculatePrice2(double dKwh, int nSeqMeter) {
    return getPriceByDkwh(dKwh, nSeqMeter);
  }

  private Future<List<Double>> getPriceByDkwh(double dKwh, int nSeqMeter) {
    Promise<List<Double>> promise = Promise.promise();

    // Simulate database call
    List<Double> priceList = Arrays.asList(100.0, 50.0); // Mock data
    promise.complete(priceList);

    return promise.future();
  }

  public double calculatePrice(double dKwh) {
    return dKwh * 10; // Mock calculation
  }
}
