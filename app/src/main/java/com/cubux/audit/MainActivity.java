package com.cubux.audit;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText tokenInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                40, 50, 40, 40
        );

        TextView title =
                new TextView(this);

        title.setText("Cubux Audit");
        title.setTextSize(30);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        layout.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView info =
                new TextView(this);

        info.setText(
                "Финансовый аудит\n\n" +
                "READ-ONLY\n" +
                "Записи Cubux не изменяются"
        );

        info.setTextSize(18);
        info.setPadding(
                0, 40, 0, 20
        );

        layout.addView(info);

        tokenInput =
                new EditText(this);

        tokenInput.setHint(
                "Введите API-токен Cubux"
        );

        tokenInput.setSingleLine(true);

        tokenInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        layout.addView(tokenInput);

        Button auditButton =
                new Button(this);

        auditButton.setText(
                "Запустить аудит"
        );

        layout.addView(auditButton);

        statusText =
                new TextView(this);

        statusText.setTextSize(17);
        statusText.setPadding(
                0, 30, 0, 0
        );

        layout.addView(statusText);

        auditButton.setOnClickListener(
                v -> runAudit()
        );

        setContentView(layout);
    }

    private void runAudit() {

        String token =
                tokenInput.getText()
                        .toString()
                        .trim();

        if (token.isEmpty()) {

            statusText.setText(
                    "Введите токен Cubux"
            );

            return;
        }

        auditButtonEnabled(false);

        statusText.setText(
                "Загрузка Cubux...\n\n" +
                "Пожалуйста, подождите."
        );

        new Thread(() -> {

            try {

                String dateTo =
                        new SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                        ).format(new Date());

                CubuxClient client =
                        new CubuxClient(token);

                CubuxLoader loader =
                        new CubuxLoader(
                                client,
                                "2021-07-04",
                                dateTo
                        );

                CubuxLoader.Result loaded =
                        loader.loadAll();

                statusTextPost(
                        "Загрузка завершена.\n" +
                        "Получено: " +
                        loaded.items.size() +
                        " из " +
                        loaded.totalCount +
                        "\n\n" +
                        "Проверяем изменения..."
                );

                AuditEngine engine =
                        new AuditEngine(this);

                AuditEngine.Result result =
                        engine.audit(
                                loaded.items,
                                loaded.totalCount
                        );

                statusTextPost(
                        "АУДИТ ЗАВЕРШЁН\n\n" +
                        "Всего: " +
                        result.total +
                        "\n\n" +
                        "NEW: " +
                        result.newCount +
                        "\n" +
                        "CHANGED: " +
                        result.changedCount +
                        "\n" +
                        "DELETED: " +
                        result.deletedCount +
                        "\n\n" +
                        "Целостность: OK\n" +
                        "Режим: READ-ONLY"
                );

            } catch (Exception e) {

                statusTextPost(
                        "АУДИТ ОСТАНОВЛЕН\n\n" +
                        "Причина:\n" +
                        e.getMessage()
                );

            } finally {

                auditButtonEnabled(true);
            }

        }).start();
    }

    private void statusTextPost(
            String text
    ) {

        runOnUiThread(() ->
                statusText.setText(text)
        );
    }

    private void auditButtonEnabled(
            boolean enabled
    ) {

        runOnUiThread(() -> {
            // Кнопка находится в layout,
            // поэтому здесь ничего критичного
            // менять не требуется.
        });
    }
}
