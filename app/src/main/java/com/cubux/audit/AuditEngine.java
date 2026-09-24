package com.cubux.audit;

import android.content.Context;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AuditEngine {

    private final AuditDb db;

    public AuditEngine(Context context) {
        db = new AuditDb(context);
    }

    public Result audit(
            List<JSONObject> items,
            int expectedTotal
    ) throws Exception {

        if (items == null) {
            throw new Exception("Операции отсутствуют");
        }

        if (items.size() != expectedTotal) {
            throw new Exception(
                    "НЕПОЛНАЯ ЗАГРУЗКА: " +
                    items.size() +
                    " из " +
                    expectedTotal
            );
        }

        String now = now();

        Map<String, String> old =
                db.getTransactions();

        Map<String, String> current =
                new HashMap<>();

        int newCount = 0;
        int changedCount = 0;
        int deletedCount = 0;

        for (JSONObject item : items) {

            String id = getId(item);

            if (id == null) {
                throw new Exception(
                        "Обнаружена запись без ID"
                );
            }

            if (current.containsKey(id)) {
                throw new Exception(
                        "Дубликат ID: " + id
                );
            }

            String data =
                    normalize(item).toString();

            current.put(id, data);
        }

        for (Map.Entry<String, String> entry :
                current.entrySet()) {

            String id = entry.getKey();
            String newData = entry.getValue();

            String oldData = old.get(id);

            if (oldData == null) {

                newCount++;

                db.saveChange(
                        now,
                        id,
                        "NEW",
                        null,
                        newData
                );

            } else if (!hash(oldData).equals(
                    hash(newData))) {

                changedCount++;

                db.saveChange(
                        now,
                        id,
                        "CHANGED",
                        oldData,
                        newData
                );
            }
        }

        Set<String> deleted =
                new HashSet<>(old.keySet());

        deleted.removeAll(current.keySet());

        for (String id : deleted) {

            deletedCount++;

            db.saveChange(
                    now,
                    id,
                    "DELETED",
                    old.get(id),
                    null
            );
        }

        for (Map.Entry<String, String> entry :
                current.entrySet()) {

            String id = entry.getKey();
            String data = entry.getValue();

            if (old.containsKey(id)) {

                db.updateTransaction(
                        id,
                        data,
                        now
                );

            } else {

                db.saveTransaction(
                        id,
                        data,
                        now
                );
            }
        }

        for (String id : deleted) {
            db.deleteTransaction(id);
        }

        return new Result(
                current.size(),
                newCount,
                changedCount,
                deletedCount
        );
    }

    private JSONObject normalize(
            JSONObject source
    ) throws Exception {

        String[] fields = {
                "id",
                "uuid",
                "date",
                "category_id",
                "category_uuid",
                "category",
                "amount",
                "currency_code",
                "amount2",
                "currency_code2",
                "account",
                "account_uuid",
                "account2",
                "account2_uuid",
                "loan_id",
                "loan_uuid",
                "project_uuid",
                "receipt_uuid",
                "is_imported",
                "user_id",
                "user_uuid",
                "user_name",
                "description",
                "type"
        };

        JSONObject result =
                new JSONObject();

        for (String field : fields) {

            result.put(
                    field,
                    source.opt(field)
            );
        }

        return result;
    }

    private String getId(
            JSONObject item
    ) {

        Object id =
                item.opt("id");

        if (id == null) {
            return null;
        }

        return String.valueOf(id);
    }

    private String hash(
            String text
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        byte[] bytes =
                digest.digest(
                        text.getBytes("UTF-8")
                );

        StringBuilder result =
                new StringBuilder();

        for (byte b : bytes) {

            result.append(
                    String.format(
                            Locale.US,
                            "%02x",
                            b
                    )
            );
        }

        return result.toString();
    }

    private String now() {

        return new java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.US
        ).format(
                new java.util.Date()
        );
    }

    public static class Result {

        public final int total;
        public final int newCount;
        public final int changedCount;
        public final int deletedCount;

        public Result(
                int total,
                int newCount,
                int changedCount,
                int deletedCount
        ) {

            this.total = total;
            this.newCount = newCount;
            this.changedCount = changedCount;
            this.deletedCount = deletedCount;
        }
    }
}
