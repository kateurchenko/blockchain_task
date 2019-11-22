package com.task;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.LinkedHashMap;
import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * Created by kate on 11/19/19.
 */
public class TransactionsTest {

    @Test
    public void testTransactions() {
        RestAssured.baseURI = "https://api.blockcypher.com";
        String hash = given()
                .get(Endpoints.MAIN)
                .jsonPath()
                .getString("hash");

        String txid = (String) given()
                .pathParams("block", hash)
                .get(Endpoints.BLOCKS)
                .jsonPath()
                .getList("txids")
                .get(0);

        List<String> addrs = given()
                .pathParams("tx", txid)
                .get(Endpoints.TXIDS)
                .jsonPath()
                .getList("addresses");

        SoftAssert softAssert = new SoftAssert();

        for (String address : addrs) {
            JsonPath addressResponse = given()
                    .pathParams("adr", address)
                    .get(Endpoints.ADDRS)
                    .jsonPath();
            List<LinkedHashMap> transactions = addressResponse
                    .getList("txrefs");

            long total_sent = transactions.stream()
                    .filter(t -> t.containsKey("spent"))
                    .filter(t -> t.get("spent").equals(true))
                    .map(t -> t.get("value"))
                    .mapToLong(i -> (i instanceof Integer) ? Long.valueOf((int) i) : (Long) i)
                    .sum();

            long sumForTransactionsWithoutSpent = transactions.stream()
                    .filter(t -> !t.containsKey("spent"))
                    .map(t -> t.get("value"))
                    .mapToLong(i -> (i instanceof Integer) ? Long.valueOf((int) i) : (Long) i)
                    .sum();

            long total_received = transactions.stream()
                    .filter(t -> t.containsKey("spent"))
                    .filter(t -> t.get("spent").equals(false))
                    .map(t -> t.get("value"))
                    .mapToLong(i -> (i instanceof Integer) ? Long.valueOf((int) i) : (Long) i)
                    .sum() + sumForTransactionsWithoutSpent;
            long calculatedBalance = total_received - total_sent;

            Object balance = addressResponse.get("balance");
            Long balanceFromResponse = (balance instanceof Integer) ? Long.valueOf((int) balance) : (Long) balance;
            softAssert.assertTrue(balanceFromResponse.compareTo(calculatedBalance) == 0,
                    "address " + address + " has incorrect balance " +
                            "(expected: " + balanceFromResponse + ", but found " + calculatedBalance + ")");
            System.out.println("address " + address + " has balances: " +
                    "from response =  " + balanceFromResponse + ", calculated = " + calculatedBalance);
        }
        softAssert.assertAll();
    }
}
