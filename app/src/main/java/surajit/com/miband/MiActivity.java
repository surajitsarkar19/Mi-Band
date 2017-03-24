package surajit.com.miband;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MiActivity extends AppCompatActivity {

    private TextView textViewStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi);

        initLayout();
    }

    private void initLayout() {
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
    }
}
