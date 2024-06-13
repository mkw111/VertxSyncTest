package com.example.starter;

import com.example.starter.ServiceSvc;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.time.YearMonth;
import java.util.List;

public class ParseKwhList {
  public ServiceSvc m_Service=null;
  public ArrayList<Kwh> m_alKwhCurr = new ArrayList<>();
  public ArrayList<Kwh> m_alKwhAvg = new ArrayList<>();
  public ArrayList<Kwh> m_alKwhPrev = new ArrayList<>();

  ParseKwhList(ServiceSvc svc) {
    m_Service=svc;
  }

  public void initForHourly() {
    for (int i=0; i<24; i++) {
      Kwh kwhCurr=new Kwh();
      kwhCurr.m_nUnit=i+1;
      m_alKwhCurr.add(kwhCurr);

      Kwh kwhAvg=new Kwh();
      kwhAvg.m_nUnit=i+1;
      m_alKwhAvg.add(kwhAvg);

      Kwh kwhPrev=new Kwh();
      kwhPrev.m_nUnit=i+1;
      m_alKwhPrev.add(kwhPrev);
    }
  }

  public void initForDaily(int nYear, int nMonth) {
    YearMonth ym=YearMonth.of(nYear, nMonth);
    int nDayCount=ym.lengthOfMonth();

    for (int i=0; i<nDayCount; i++) {
      Kwh kwhCurr=new Kwh();
      kwhCurr.m_nUnit=i+1;
      m_alKwhCurr.add(kwhCurr);

      Kwh kwhAvg=new Kwh();
      kwhAvg.m_nUnit=i+1;
      m_alKwhAvg.add(kwhAvg);

      Kwh kwhPrev=new Kwh();
      kwhPrev.m_nUnit=i+1;
      m_alKwhPrev.add(kwhPrev);
    }
  }

  public void initForMonthly() {
    for (int i=0; i<12; i++) {
      Kwh kwhCurr=new Kwh();
      kwhCurr.m_nUnit=i+1;
      m_alKwhCurr.add(kwhCurr);

      Kwh kwhAvg=new Kwh();
      kwhAvg.m_nUnit=i+1;
      m_alKwhAvg.add(kwhAvg);

      Kwh kwhPrev=new Kwh();
      kwhPrev.m_nUnit=i+1;
      m_alKwhPrev.add(kwhPrev);
    }
  }

  // flag 1=current, 2=average, 3=previous year
  public void parse(JsonArray jaIn, JsonObject joOut) {
    int nFlag;
    int nUnit;
    double dKwh;
    String strDate;
    int nSeqMeter = 0;

    for (int i = 0; i < jaIn.size(); i++) {
      JsonObject jo = jaIn.getJsonObject(i);
      System.out.println("OUT : " + jo);

      nFlag = jo.getInteger("flag");
      nUnit = jo.getInteger("unit");
      dKwh = jo.getDouble("dKwh");
      strDate = jo.getString("cptr_date");
      nSeqMeter = jo.getInteger("nSeqMeter");
      setKwh(nFlag, nUnit, dKwh, strDate);
    }

    // 검침일이 필요함!!!
    calculateWonCurr(nSeqMeter);
    calculateWonPrev(nSeqMeter);

    double dKwhSumCurr = 0.0;
    double dKwhSumPrev = 0.0;

    JsonArray jaCurr = new JsonArray();
    joOut.put("list_usage", jaCurr);

    for (int i = 0; i < m_alKwhCurr.size(); i++) {
      Kwh kwhCurr = m_alKwhCurr.get(i);
      Kwh kwhPrev = m_alKwhPrev.get(i);

      JsonObject joRes = new JsonObject();
      jaCurr.add(joRes);

      joRes.put("unit", kwhCurr.m_nUnit);
      joRes.put("kwh_curr", kwhCurr.m_dKwh);
      joRes.put("won_curr", kwhCurr.m_dWon);
      joRes.put("kwh_prev", kwhPrev.m_dKwh);
      joRes.put("won_prev", kwhPrev.m_dWon);

      dKwhSumCurr += kwhCurr.m_dKwh;
      dKwhSumPrev += kwhPrev.m_dKwh;
    }

    Future<List<Double>> dWonSumCurrListFuture = m_Service.m_Price.calculatePrice2(dKwhSumCurr, nSeqMeter);
    Future<List<Double>> dWonSumPrevListFuture = m_Service.m_Price.calculatePrice2(dKwhSumPrev, nSeqMeter);

    final double dWonSumCurr = m_Service.m_Price.calculatePrice(dKwhSumCurr);
    final double dWonSumPrev = m_Service.m_Price.calculatePrice(dKwhSumPrev);
    final double fDKwhSumCurr = dKwhSumCurr;
    final double fDKwhSumPrev = dKwhSumPrev;

    CompositeFuture.all(dWonSumCurrListFuture, dWonSumPrevListFuture).onComplete(ar -> {
      if (ar.succeeded()) {
        List<Double> dWonSumCurrList = dWonSumCurrListFuture.result();
        List<Double> dWonSumPrevList = dWonSumPrevListFuture.result();

        double highDWonSumCurr = dWonSumCurrList.get(0);
        double lowDWonSumCurr = dWonSumCurrList.get(1);

        double highDWonSumPrev = dWonSumPrevList.get(0);
        double lowDWonSumPrev = dWonSumPrevList.get(1);

        // 모든 값을 비동기 작업이 완료된 후에 설정합니다.
        joOut.put("total_won_curr_high", highDWonSumCurr);
        joOut.put("total_won_curr_low", lowDWonSumCurr);
        joOut.put("total_won_prev_high", highDWonSumPrev);
        joOut.put("total_won_prev_low", lowDWonSumPrev);

        joOut.put("total_kwh_curr", fDKwhSumCurr);
        joOut.put("total_won_curr", dWonSumCurr);
        joOut.put("total_kwh_prev", fDKwhSumPrev);
        joOut.put("total_won_prev", dWonSumPrev);
      } else {
        ar.cause().printStackTrace();
      }
    });
  }


  private Kwh findKwhCurr(int nUnit) {
    for (Kwh kwh : m_alKwhCurr) {
      if (kwh.m_nUnit==nUnit) {
        return kwh;
      }
    }

    return null;
  }

  private Kwh findKwhAvg(int nUnit) {
    for (Kwh kwh : m_alKwhAvg) {
      if (kwh.m_nUnit==nUnit) {
        return kwh;
      }
    }

    return null;
  }

  private Kwh findKwhPrev(int nUnit) {
    for (Kwh kwh : m_alKwhPrev) {
      if (kwh.m_nUnit==nUnit) {
        return kwh;
      }
    }

    return null;
  }

  private Kwh setKwh(int nFlag, int nUnit, double dKwh, String strTime) {

    Kwh kwh=null;

    switch (nFlag) {
      case (1) :
        kwh=findKwhCurr(nUnit);
        break;

      case (2) :
        kwh=findKwhAvg(nUnit);
        break;

      case (3) :
        kwh=findKwhPrev(nUnit);
        break;

      default:
        return null;
    }

    if (kwh==null) {
      return null;
    }

    kwh.m_dKwh=dKwh;
    kwh.m_strUnit=strTime;

    return kwh;
  }

  private void calculateWonCurr(int nSeqMeter) {
    double dKwhSum=0.0;
    double dWonPrev=m_Service.m_Price.calculatePrice(0.0);

    for (Kwh kwh : m_alKwhCurr) {
      dKwhSum += kwh.m_dKwh;
      double dWonCurr=m_Service.m_Price.calculatePrice(dKwhSum);

      //고압 저압 전기세 계산 추가_240613
      Future<List<Double>> listFuture = m_Service.m_Price.calculatePrice2(dKwhSum, nSeqMeter);
      // Future 객체에서 결과를 처리한다.
      listFuture.onComplete(asyncResult -> {
        if (asyncResult.succeeded()) {
          // 비동기 작업이 성공한 경우 결과를 추출한다.
          List<Double> priceList = asyncResult.result();

          // 결과 리스트에서 값을 추출하여 변수에 할당한다.
          double highWon = priceList.get(0);
          double lowWon = priceList.get(1);

          // 여기서 변수(highWon, lowWon)에 추출된 값을 사용하여 원하는 작업을 수행한다.
          System.out.println("High Won: " + highWon + ", Low Won: " + lowWon);

          kwh.m_dWonCurrHigh = highWon; //high
          kwh.m_dWonCurrLow = lowWon; //low
        } else {
          // 비동기 작업이 실패한 경우
          Throwable cause = asyncResult.cause();
          System.err.println("Failed to calculate price: " + cause.getMessage());

          // 실패 처리 로직을 구현할 수 있다.
        }
      });

      kwh.m_dWon=dWonCurr-dWonPrev;
      dWonPrev=dWonCurr;
    }
  }

  private void calculateWonAvg() {
    double dKwhSum=0.0;
    double dWonPrev=m_Service.m_Price.calculatePrice(0.0);

    for (Kwh kwh : m_alKwhAvg) {
      dKwhSum += kwh.m_dKwh;
      double dWonCurr=m_Service.m_Price.calculatePrice(dKwhSum);
      kwh.m_dWon=dWonCurr-dWonPrev;
      dWonPrev=dWonCurr;
    }
  }

  private void calculateWonPrev(int nSeqMeter) {
    double dKwhSum=0.0;
    double dWonPrev=m_Service.m_Price.calculatePrice(0.0);

    for (Kwh kwh : m_alKwhPrev) {
      dKwhSum += kwh.m_dKwh;
      double dWonCurr=m_Service.m_Price.calculatePrice(dKwhSum);

      //고압 저압 전기세 계산 추가_240613
      Future<List<Double>> listFuture = m_Service.m_Price.calculatePrice2(dKwhSum, nSeqMeter);
      // Future 객체에서 결과를 처리한다.
      listFuture.onComplete(asyncResult -> {
        if (asyncResult.succeeded()) {
          // 비동기 작업이 성공한 경우 결과를 추출한다.
          List<Double> priceList = asyncResult.result();

          // 결과 리스트에서 값을 추출하여 변수에 할당한다.
          double highWon = priceList.get(0);
          double lowWon = priceList.get(1);

          // 여기서 변수(highWon, lowWon)에 추출된 값을 사용하여 원하는 작업을 수행한다.
          System.out.println("High Won: " + highWon + ", Low Won: " + lowWon);

          kwh.m_dWonPrevHigh = highWon; //high
          kwh.m_dWonPrevLow = lowWon; //low
        } else {
          // 비동기 작업이 실패한 경우
          Throwable cause = asyncResult.cause();
          System.err.println("Failed to calculate price: " + cause.getMessage());

          // 실패 처리 로직을 구현할 수 있다.
        }
      });

      kwh.m_dWon=dWonCurr-dWonPrev;
      dWonPrev=dWonCurr;
    }
  }
}
