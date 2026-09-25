package com.cubux.audit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
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

        /*
         * Сначала полностью проверяем полученные данные.
         * До этого момента база НЕ изменяется.
         */

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

        /*
         * Дополнительная проверка количества.
         */

        if (current.size() != expectedTotal) {
            throw new Exception(
                    "ОШИБКА ЦЕЛОСТНОСТИ: уникальных ID " +
                    current.size() +
                    " из " +
                    expectedTotal
            );
        }

        int newCount = 0;
        int changedCount = 0;
        int deletedCount = 0;

        List<ChangeItem> detectedChanges =
                new ArrayList<>();

        /*
         * NEW / CHANGED
         */

        for (Map.Entry<String, String> entry :
                current.entrySet()) {

            String id = entry.getKey();
            String newData = entry.getValue();

            String oldData = old.get(id);

            if (oldData == null) {

                newCount++;

                detectedChanges.add(
                        new ChangeItem(
                                id,
                                "NEW",
                                null,
                                newData
                        )
                );

            } else if (!hash(oldData).equals(
                    hash(newData))) {

                changedCount++;

                detectedChanges.add(
                        new ChangeItem(
                                id,
                                "CHANGED",
                                oldData,
                                newData
                        )
                );
            }
        }

        /*
         * DELETED
         */

        Set<String> deleted =
                new HashSet<>(old.keySet());

        deleted.removeAll(current.keySet());

        for (String id : deleted) {

            deletedCount++;

            detectedChanges.add(
                    new ChangeItem(
                            id,
                            "DELETED",
                            old.get(id),
                            null
                    )
            );
        }

        /*
         * Создаём RUNNING.
         *
         * Если дальнейшая запись завершится ошибкой,
         * данные не будут частично применены.
         */

        long runId =
                db.createRunningRun(now);

        SQLiteDatabase database =
                db.beginTransaction();

        boolean success = false;

        try {

            /*
             * Записываем журнал изменений.
             */

            for (ChangeItem change :
                    detectedChanges) {

                db.saveChange(
                        now,
                        change.transactionId,
                        change.type,
                        change.oldData,
                        change.newData
                );
            }

            /*
             * Обновляем текущее состояние.
             */

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

            /*
             * Удаляем из текущего состояния
             * отсутствующие в Cubux операции.
             *
             * САМИ записи Cubux здесь НЕ удаляются.
             * Удаляется только наша локальная копия.
             */

            for (String id : deleted) {

                db.deleteTransaction(id);
            }

            db.endTransaction(
                    database,
                    true
            );

            success = true;

        } finally {

            if (!success) {

                /*
                 * Если транзакция не была успешно
                 * завершена, откатываем изменения.
                 */

                try {
                    db.endTransaction(
                            database,
                            false
                    );
                } catch (Exception ignored) {
                }
            }
        }

        /*
         * Только после успешной фиксации данных
         * RUNNING превращается в OK.
         */

        db.finishRun(
                runId,
                now(),
                current.size(),
                newCount,
                changedCount,
                deletedCount,
                "OK"
        );

        return new Result(
                current.size(),
                newCount,
                changedCount,
                deletedCount,
                runId,
                now
        );
    }

    /*
     * Создание контрольного hash текущего состояния.
     *
     * ID сортируются, поэтому порядок страниц API
     * не влияет на результат.
     */

    public String createControlHash(
            List<JSONObject> items
    ) throws Exception {

        if (items == null) {
            throw new Exception(
                    "Операции отсутствуют"
            );
        }

        List<String> normalized =
                new ArrayList<>();

        Set<String> ids =
                new HashSet<>();

        for (JSONObject item : items) {

            String id = getId(item);

            if (id == null) {
                throw new Exception(
                        "Запись без ID"
                );
            }

            if (!ids.add(id)) {
                throw new Exception(
                        "Дубликат ID: " + id
                );
            }

            normalized.add(
                    normalize(item).toString()
            );
        }

        Collections.sort(normalized);

        StringBuilder builder =
                new StringBuilder();

        for (String value : normalized) {
            builder.append(value);
            builder.append('\n');
        }

        return hash(builder.toString());
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

        String value =
                String.valueOf(id).trim();

        if (value.isEmpty()) {
            return null;
        }

        return value;
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

    private static class ChangeItem {

        final String transactionId;
        final String type;
        final String oldData;
        final String newData;

        ChangeItem(
                String transactionId,
                String type,
                String oldData,
                String newData
        ) {

            this.transactionId =
                    transactionId;

            this.type =
                    type;

            this.oldData =
                    oldData;

            this.newData =
                    newData;
        }
    }

    public static class Result {

        public final int total;
        public final int newCount;
        public final int changedCount;
        public final int deletedCount;
        public final long runId;
        public final String finishedAt;

        public Result(
                int total,
                int newCount,
                int changedCount,
                int deletedCount,
                long runId,
                String finishedAt
        ) {

            this.total = total;
            this.newCount = newCount;
            this.changedCount = changedCount;
            this.deletedCount = deletedCount;
            this.runId = runId;
            this.finishedAt = finishedAt;
        }
    }
}
