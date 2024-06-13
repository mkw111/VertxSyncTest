package com.example.starter;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FnMember {

  public static void apiGetUsageOfOneMonth(RoutingContext rc) {

    int nSeqSite = Integer.parseInt(rc.request().getParam("SeqSite"));
    int nSeqMeter = Integer.parseInt(rc.request().getParam("SeqMeter"));
    int nTargetYear = Integer.parseInt(rc.request().getParam("Year"));
    int nTargetMonth = Integer.parseInt(rc.request().getParam("Month"));

    // for_test
    //nTargetYear=2021;

    ServiceSvc.getInstance().m_Logger.trace("--> apiGetUsageOfOneMonth");
    ServiceSvc.getInstance().m_Logger.trace("      SeqSite=" + nSeqSite);
    ServiceSvc.getInstance().m_Logger.trace("      SeqMeter=" + nSeqMeter);
    ServiceSvc.getInstance().m_Logger.trace("      Year=" + nTargetYear);
    ServiceSvc.getInstance().m_Logger.trace("      Month=" + nTargetMonth);

    JsonObject jo=new JsonObject();

    //      jo.put("seq_site", nSeqSite);
    jo.put("seq_meter", nSeqMeter);
    jo.put("target_year", nTargetYear);
    jo.put("target_month", nTargetMonth);

    Future<JsonObject> ft=FnMember.getUsageOfOneMonth(jo);
    ft.onComplete(ar -> {
      if (ar.failed()) {
        ServiceSvc.getInstance().m_Logger.error("composed future failed...");
        ar.cause().printStackTrace();

        VertxException ve=new VertxException("composed future failed");
        rc.fail(ve); // Internal Server Error
      }
      else {
        //    ServiceSvc.getInstance().m_Logger.trace("compose handler called="+jo);

        HttpServerResponse response = rc.response().putHeader("content-type", "application/json; charset=utf-8");
        JsonObject joResult=jo.getJsonObject("result");
        String strJson = joResult.encode();
        response.end(strJson);
      }
    });

  }
  public static Future<JsonObject> getUsageOfOneMonth(JsonObject jo) {
    ServiceSvc.getInstance().m_Logger.trace("getUsageOfOneMonth called : "+jo);

    Promise<JsonObject> prm=Promise.promise();

    ServiceSvc.getInstance().m_Jdbc.getConnection(ar->{
      if (ar.failed()) {
        ServiceSvc.getInstance().m_Logger.trace("DB connection failed...");
        prm.fail(ar.cause());
      }
      else {
        ServiceSvc.getInstance().m_Logger.trace("DB connection ok...");

        String strSql="call GetKwhListDaily(?, ?)";

        //int nSeqSite=jo.getInteger("seq_site");
        int nSeqMeter=jo.getInteger("seq_meter");
        int nYear=jo.getInteger("target_year");
        int nMonth=jo.getInteger("target_month");

        LocalDate ldTarget=LocalDate.of(nYear, nMonth, 1);
        DateTimeFormatter dtf=DateTimeFormatter.ofPattern("yyyyMM");
        String strDateTarget=dtf.format(ldTarget);

        JsonArray jsa=new JsonArray().add(nSeqMeter).add(strDateTarget);
        ServiceSvc.getInstance().m_Logger.trace("call GetKwhListDaily : SeqMeter=" + nSeqMeter + ", DateTarget = "+strDateTarget);

        SQLConnection conn = ar.result();
        conn.callWithParams(strSql, jsa, null, res -> {
          conn.close();

          if (res.failed()) {
            ServiceSvc.getInstance().m_Logger.trace("Query Failed..."+res.cause());
            prm.fail(res.cause());
          }
          else {
            ResultSet rs = res.result();
            JsonArray ja=new JsonArray();

            List<JsonObject> ljso = rs.getRows();
            for (JsonObject jj : ljso) {
              ja.add(jj);
            }

            //   ServiceSvc.getInstance().m_Logger.trace("result jsonarray="+ja.toString());

            JsonObject joRes=new JsonObject();
            jo.put("result", joRes);

            joRes.put("target_time", strDateTarget);
            ParseKwhList prs=new ParseKwhList(ServiceSvc.getInstance());
            prs.initForDaily(nYear, nMonth);
            prs.parse(ja, joRes);

            //    ServiceSvc.getInstance().m_Logger.trace("getUsageOfOneMonth completed : "+jo);
            prm.complete(jo);
          }

        });
      }

    });

    return prm.future();
  }
}
