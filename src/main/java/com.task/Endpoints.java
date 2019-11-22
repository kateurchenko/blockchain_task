package com.task;

/**
 * Created by kate on 11/19/19.
 */
public class Endpoints {
    public static final String MAIN = "/v1/btc/main";
    public static final String BLOCKS = MAIN + "/blocks/{block}";
    public static final String TXIDS = MAIN + "/txs/{tx}";
    public static final String ADDRS = MAIN + "/addrs/{adr}";
}
