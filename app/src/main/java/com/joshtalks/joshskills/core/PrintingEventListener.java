package com.joshtalks.joshskills.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

final class PrintingEventListener extends EventListener {
    long callStartNanos;

    @Override
    public void callEnd(@NotNull Call call) {
        printEvent("callEnd");
    }

    @Override
    public void callFailed(@NotNull Call call, @NotNull IOException ioe) {
        printEvent("callFailed");
    }

    @Override
    public void callStart(@NotNull Call call) {
        printEvent("callStart");
    }

    private void printEvent(String name) {
        long nowNanos = System.nanoTime();
        if (name.equals("callStart")) {
            callStartNanos = nowNanos;
        }
        long elapsedNanos = nowNanos - callStartNanos;
        System.out.printf("%.3f %s%n", elapsedNanos / 1000000000d, name);
    }

    @Override
    public void canceled(@NotNull Call call) {
        printEvent("canceled");
    }

    @Override
    public void connectEnd(
            Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        printEvent("connectEnd");
    }

    @Override
    public void connectFailed(@NotNull Call call, InetSocketAddress inetSocketAddress, Proxy proxy,
                              Protocol protocol, IOException ioe) {
        printEvent("connectFailed");
    }

    @Override
    public void connectStart(
            Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        printEvent("connectStart");
    }

    @Override
    public void connectionAcquired(Call call, Connection connection) {
        printEvent("connectionAcquired");
    }

    @Override
    public void connectionReleased(Call call, Connection connection) {
        printEvent("connectionReleased");
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        printEvent("dnsEnd");
    }

    @Override
    public void dnsStart(Call call, String domainName) {
        printEvent("dnsStart");
    }

    @Override
    public void proxySelectEnd(@NotNull Call call, HttpUrl url, List<Proxy> proxies) {
        printEvent("proxySelectEnd");
    }

    @Override
    public void proxySelectStart(@NotNull Call call, @NotNull HttpUrl url) {
        printEvent("proxySelectStart");
    }

    @Override
    public void requestBodyEnd(@NotNull Call call, long byteCount) {
        printEvent("requestBodyEnd");
    }

    @Override
    public void requestBodyStart(Call call) {
        printEvent("requestBodyStart");
    }

    @Override
    public void requestFailed(@NotNull Call call, IOException ioe) {
        printEvent("requestFailed");
    }

    @Override
    public void requestHeadersEnd(Call call, Request request) {
        printEvent("requestHeadersEnd");
    }

    @Override
    public void requestHeadersStart(Call call) {
        printEvent("requestHeadersStart");
    }

    @Override
    public void responseBodyEnd(@NotNull Call call, long byteCount) {
        printEvent("responseBodyEnd");
    }

    @Override
    public void responseBodyStart(@NotNull Call call) {
        printEvent("responseBodyStart");
    }

    @Override
    public void responseFailed(@NotNull Call call, @NotNull IOException ioe) {
        printEvent("responseFailed");
    }

    @Override
    public void responseHeadersEnd(@NotNull Call call, @NotNull Response response) {
        printEvent("responseHeadersEnd");
    }

    @Override
    public void responseHeadersStart(@NotNull Call call) {
        printEvent("responseHeadersStart");
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        printEvent("secureConnectEnd");
    }

    @Override
    public void secureConnectStart(Call call) {
        printEvent("secureConnectStart");
    }
}