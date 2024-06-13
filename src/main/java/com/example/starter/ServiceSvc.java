package com.example.starter;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

public class ServiceSvc {
  private static final ServiceSvc instance = new ServiceSvc();
  public Logger m_Logger = LoggerFactory.getLogger(ServiceSvc.class);
  public JDBCClient m_Jdbc;
  public PriceService m_Price;

  private ServiceSvc() {
    Vertx vertx = Vertx.vertx();
    JsonObject config = new JsonObject()
      .put("url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
      .put("driver_class", "org.h2.Driver")
      .put("max_pool_size", 30);

    m_Jdbc = JDBCClient.createShared(vertx, config);
    m_Price = new PriceService();

    initDatabase(); // Initialize the database
  }


  public static ServiceSvc getInstance() {
    return instance;
  }

  private void initDatabase() {
    m_Jdbc.getConnection(ar -> {
      if (ar.succeeded()) {
        SQLConnection conn = ar.result();
        conn.execute("CREATE TABLE IF NOT EXISTS Kwh (flag INT, unit INT, dKwh DOUBLE, cptr_date VARCHAR, nSeqMeter INT)", create -> {
          if (create.succeeded()) {
            conn.execute("INSERT INTO Kwh (flag, unit, dKwh, cptr_date, nSeqMeter) VALUES (1, 1, 100.0, '20230601', 1), (2, 2, 100.0, '20230602', 1)", insert -> {
              if (insert.succeeded()) {
                m_Logger.info("Database initialized");
              } else {
                m_Logger.error("Failed to insert data", insert.cause());
              }
              conn.close();
            });
          } else {
            m_Logger.error("Failed to create table", create.cause());
            conn.close();
          }
        });
      } else {
        m_Logger.error("Failed to get connection", ar.cause());
      }
    });
  }

}
