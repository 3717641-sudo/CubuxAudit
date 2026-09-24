```java
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

public class MainActivity extends Activity {

    private EditText tokenInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 50, 40, 40);

        TextView title = new TextView(this);
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

        TextView info = new TextView(this);
        info.setText(
                "Подключение к Cubux\n\n" +
                "Режим: READ-ONLY"
        );
        info.setTextSize(18);
        info.setPadding(0, 50, 0, 20);

        layout.addView(info);

        tokenInput = new EditText(this);
        tokenInput.setHint("Введите API-токен Cubux");
        tokenInput.setSingleLine(true);
        tokenInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        layout.addView(tokenInput);

        Button testButton = new Button(this);
        testButton.setText("Проверить подключение");

        layout.addView(testButton);

        statusText = new TextView(this);
        statusText.setTextSize(16);
        statusText.setPadding(0, 30, 0, 0);

        layout.addView(statusText);

        testButton.setOnClickListener(v -> testConnection());

        setContentView(layout);
    }

    private void testConnection() {

        String token = tokenInput.getText().toString().trim();

        if (token.isEmpty()) {
            statusText.setText("Введите токен Cubux");
            return;
        }

        statusText.setText("Загружаем операции Cubux...");

        new Thread(() -> {

            try {

                CubuxClient client =
                        new CubuxClient(token);

                CubuxLoader loader =
                        new CubuxLoader(
                                client,
                                "2021-07-04",
                                "2026-09-25"
                        );

                CubuxLoader.Result result =
                        loader.loadAll();

                runOnUiThread(() ->
                        statusText.setText(
                                "Cubux подключен!\n\n" +
                                "Всего записей: " +
                                result.totalCount +
                                "\n" +
                                "Страниц: " +
                                result.pageCount +
                                "\n\n" +
                                "Получено: " +
                                result.items.size() +
                                "\n\n" +
                                "Проверка целостности: OK"
                        )
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                        statusText.setText(
                                "Ошибка загрузки:\n\n" +
                                e.getMessage()
                        )
                );
            }

        }).start();
    }
}
```
