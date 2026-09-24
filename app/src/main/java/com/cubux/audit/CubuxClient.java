package com.cubux.audit;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class CubuxClient {

    private final String token;

    public CubuxClient(String token) {
        this.token = token;
    }

    public String get(String urlString) throws IOException {

        URL url = new URL(urlString);
        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty(
                "Authorization",
                "Bearer " + token
        );

        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);

        int responseCode = connection.getResponseCode();

        if (responseCode != 200) {
            throw new IOException(
                    "Cubux HTTP " + responseCode
            );
        }

        java.io.InputStream input =
                connection.getInputStream();

        java.io.ByteArrayOutputStream output =
                new java.io.ByteArrayOutputStream();

        byte[] buffer = new byte[8192];
        int length;

        while ((length = input.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }

        input.close();
        connection.disconnect();

        return output.toString("UTF-8");
    }
}
