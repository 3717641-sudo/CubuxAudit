package com.cubux.audit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CubuxLoader {

    private final CubuxClient client;
    private final String dateFrom;
    private final String dateTo;

    public CubuxLoader(
            CubuxClient client,
            String dateFrom,
            String dateTo
    ) {
        this.client = client;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public Result loadAll() throws Exception {

        List<JSONObject> allItems = new ArrayList<>();

        // Получаем первую страницу
        JSONObject first = getPage(1);

        JSONObject meta = first.optJSONObject("_meta");

        if (meta == null) {
            throw new IOException(
                    "Cubux: отсутствует _meta"
            );
        }

        int totalCount = meta.optInt("totalCount", -1);
        int pageCount = meta.optInt("pageCount", -1);

        if (totalCount < 0 || pageCount < 1) {
            throw new IOException(
                    "Cubux: некорректные данные _meta"
            );
        }

        addItems(first, allItems);

        // Загружаем остальные страницы
        for (int page = 2; page <= pageCount; page++) {

            JSONObject data = getPage(page);

            addItems(data, allItems);
        }

        // Защита от неполной загрузки
        if (allItems.size() != totalCount) {

            throw new IOException(
                    "НЕПОЛНАЯ ЗАГРУЗКА: получено " +
                    allItems.size() +
                    " из " +
                    totalCount
            );
        }

        return new Result(
                totalCount,
                pageCount,
                allItems
        );
    }

    private JSONObject getPage(int page)
            throws Exception {

        String url =
                "https://app.cubux.net/api/v1/transaction/team/250574" +
                "?dateSince=" + dateFrom +
                "&dateUntil=" + dateTo +
                "&mn=1" +
                "&page=" + page;

        String response = client.get(url);

        return new JSONObject(response);
    }

    private void addItems(
            JSONObject data,
            List<JSONObject> destination
    ) throws Exception {

        JSONArray items =
                data.optJSONArray("items");

        if (items == null) {
            throw new IOException(
                    "Cubux: отсутствует items"
            );
        }

        for (int i = 0; i < items.length(); i++) {

            JSONObject item =
                    items.optJSONObject(i);

            if (item != null) {
                destination.add(item);
            }
        }
    }

    public static class Result {

        public final int totalCount;
        public final int pageCount;
        public final List<JSONObject> items;

        public Result(
                int totalCount,
                int pageCount,
                List<JSONObject> items
        ) {
            this.totalCount = totalCount;
            this.pageCount = pageCount;
            this.items = items;
        }
    }
}
