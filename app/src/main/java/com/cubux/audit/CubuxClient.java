package com.cubux.audit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class CubuxClient {

    private final String token;

    public CubuxClient(String token) {
        this.token = token;
    }

    public String get(String urlString) throws IOException {

        HttpURLConnection connection = null;

        try {

            URL url = new URL(urlString);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + token
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            connection.setInstanceFollowRedirects(true);

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {

                String errorBody = "";

                try {

                    InputStream errorStream =
                            connection.getErrorStream();

                    if (errorStream != null) {

                        ByteArrayOutputStream output =
                                new ByteArrayOutputStream();

                        byte[] buffer =
                                new byte[4096];

                        int length;

                        while ((length =
                                errorStream.read(buffer)) != -1) {

                            output.write(
                                    buffer,
                                    0,
                                    length
                            );
                        }

                        errorStream.close();

                        errorBody =
                                output.toString("UTF-8");
                    }

                } catch (Exception ignored) {
                }

                throw new IOException(
                        "Cubux HTTP " +
                        responseCode +
                        (errorBody.isEmpty()
                                ? ""
                                : "\n" + errorBody)
                );
            }

            InputStream input =
                    connection.getInputStream();

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            byte[] buffer =
                    new byte[8192];

            int length;

            while ((length =
                    input.read(buffer)) != -1) {

                output.write(
                        buffer,
                        0,
                        length
                );
            }

            input.close();

            return output.toString("UTF-8");

        } catch (UnknownHostException e) {

            throw new IOException(
                    "DNS: не найден app.cubux.net\n" +
                    "Проверьте Интернет или DNS."
            );

        } catch (SocketTimeoutException e) {

            throw new IOException(
                    "TIMEOUT: Cubux не ответил за отведённое время."
            );

        } catch (javax.net.ssl.SSLException e) {

            throw new IOException(
                    "SSL/TLS: ошибка защищённого соединения с Cubux.\n" +
                    e.getMessage()
            );

        } catch (IOException e) {

            if (e.getMessage() != null &&
                    e.getMessage().contains("Cubux HTTP")) {

                throw e;
            }

            throw new IOException(
                    "СЕТЬ: не удалось подключиться к Cubux.\n" +
                    e.getClass().getSimpleName() +
                    ": " +
                    e.getMessage()
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
