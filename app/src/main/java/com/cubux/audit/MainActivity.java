package com.cubux.audit;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("Cubux Audit");
        title.setTextSize(30);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);
        status.setText(
            "Финансовый аудит\\n\\n" +
            "READ-ONLY\\n" +
            "Записи Cubux не изменяются"
        );
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 40, 0, 0);

        layout.addView(title);
        layout.addView(status);

        setContentView(layout);
    }
}
