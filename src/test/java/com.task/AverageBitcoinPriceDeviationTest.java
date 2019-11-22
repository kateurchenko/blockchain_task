package com.task;


import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.when;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.toMap;

/**
 * Test that checks maximum deviation of average bitcoin price for last 30 days between two services
 */
public class AverageBitcoinPriceDeviationTest {

    public static final String BITCOINAVERAGE_URL = "https://apiv2.bitcoinaverage.com/indices/global/history/BTCUSD?period=alltime";
    public static final String BLOCKCHAIN_INFO_URL = "https://api.blockchain.info/charts/market-price?timespan=30days&format=json";
    public static final double MAX_DEVIATION = 0.2;
    private SoftAssert softAssert = new SoftAssert();

    @Test
    public void testAverageDeviation() {
        Map<LocalDate, Float> bitcoinaverageStats = when()
                .get(BITCOINAVERAGE_URL)
                .jsonPath()
                .getList("")
                .stream()
                .limit(30)
                .map(a -> (LinkedHashMap) a)
                .collect(toMap((LinkedHashMap a) ->
                                convertStringToLocalDate((String) a.get("time")),
                        (LinkedHashMap a) -> (float) a.get("average")));

        Map<LocalDate, Float> blockchainStats = when()
                .get(BLOCKCHAIN_INFO_URL)
                .jsonPath()
                .getList("values")
                .stream()
                .map(a -> (LinkedHashMap) a)
                .collect(toMap((LinkedHashMap a) -> getDateFromResponseItem(a),
                        (LinkedHashMap a) -> (float) a.get("y")));

        bitcoinaverageStats
                .forEach((k, v) -> checkDeviation(blockchainStats, k, v));
        softAssert.assertAll();

    }

    private void checkDeviation(Map<LocalDate, Float> blockchainStats, LocalDate k, Float firstValue) {
        Float secondValue = blockchainStats.get(k);
        float deviation = Math.abs(((firstValue - secondValue) / secondValue) * 100);
        System.out.println(String.format("On %s there is a deviation of %s", k, deviation));
        softAssert.assertTrue(deviation <= MAX_DEVIATION
                , String.format("Deviation on %s is more than %s - it's %s", k, MAX_DEVIATION, deviation));
    }

    private LocalDate getDateFromResponseItem(LinkedHashMap a) {
        return convertEpocSecondsToDate(Long.valueOf((int) a.get("x")));
    }

    //could be moved to some DateUtils class
    private LocalDate convertEpocSecondsToDate(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    //could be moved to some DateUtils class
    private LocalDate convertStringToLocalDate(String input) {
        return LocalDate.parse(input, ofPattern("yyyy-MM-dd HH:mm:ss"));

    }
}