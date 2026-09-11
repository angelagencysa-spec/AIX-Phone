package ai.aixos.phone;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_VOICE = 101;
    private static final int REQ_PERMS = 102;
    private TextView output;
    private EditText command;
    private LinearLayout appsBox;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("ar", "SA"));
            }
        });
        buildUi();
    }

    private GradientDrawable card(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private TextView label(String value, int size, boolean centered) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(Color.rgb(25, 25, 25));
        if (centered) v.setGravity(Gravity.CENTER);
        return v;
    }

    private Button button(String title, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setOnClickListener(listener);
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 40, 28, 40);
        root.setBackgroundColor(Color.rgb(245, 245, 245));

        TextView title = label("AIX", 46, true);
        title.setTypeface(null, 1);
        root.addView(title);
        root.addView(label("AI Experience Interface", 14, true));

        output = label("جاهز. تحدث أو اكتب ما تريد تنفيذه.", 17, false);
        output.setPadding(22, 22, 22, 22);
        output.setBackground(card(Color.WHITE, 26));
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, -2);
        op.setMargins(0, 24, 0, 18);
        root.addView(output, op);

        command = new EditText(this);
        command.setHint("مثال: افتح الكاميرا، افتح واتساب، اعرض التطبيقات");
        command.setTextSize(17);
        command.setMinLines(2);
        command.setPadding(20, 18, 20, 18);
        command.setBackground(card(Color.WHITE, 22));
        root.addView(command, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.addView(button("تنفيذ", v -> execute(command.getText().toString())), new LinearLayout.LayoutParams(0, -2, 1));
        actionRow.addView(button("تحدث", v -> startVoice()), new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(actionRow);

        root.addView(button("اجعل AIX الشاشة الرئيسية", v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS))));
        root.addView(button("صلاحيات الصوت والكاميرا", v -> requestCorePermissions()));
        root.addView(button("تقرير الجهاز", v -> showDeviceReport()));
        root.addView(button("كل التطبيقات", v -> renderApps()));

        appsBox = new LinearLayout(this);
        appsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(appsBox);

        TextView footer = label("AIX Phone v3 — واجهة تشغيل ذكية تعمل فوق Android دون Root.", 13, true);
        footer.setPadding(0, 28, 0, 16);
        root.addView(footer);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void execute(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            say("اكتب أمرًا أولًا");
            return;
        }
        String x = s.toLowerCase(Locale.ROOT);
        try {
            if (x.contains("واي فاي") || x.contains("wifi")) {
                openSetting(Settings.ACTION_WIFI_SETTINGS, "فتحت إعدادات الواي فاي");
            } else if (x.contains("بلوتوث") || x.contains("bluetooth")) {
                openSetting(Settings.ACTION_BLUETOOTH_SETTINGS, "فتحت إعدادات البلوتوث");
            } else if (x.contains("الصوت") || x.contains("صوت الجهاز")) {
                openSetting(Settings.ACTION_SOUND_SETTINGS, "فتحت إعدادات الصوت");
            } else if (x.contains("الكاميرا")) {
                startActivity(new Intent("android.media.action.IMAGE_CAPTURE"));
                say("فتحت الكاميرا");
            } else if (x.contains("الإعدادات") || x.contains("الاعدادات") || x.equals("اعدادات")) {
                openSetting(Settings.ACTION_SETTINGS, "فتحت الإعدادات");
            } else if (x.contains("التطبيقات") || x.contains("البرامج")) {
                renderApps();
            } else if (x.startsWith("افتح ")) {
                openAppByName(s.substring(5).trim());
            } else if (x.contains("الجهاز") || x.contains("الهاتف")) {
                showDeviceReport();
            } else {
                say("فهمت الأمر: " + s + "\nهذا الإصدار ينفذ أوامر الهاتف المحلية، ويمكن توسيع محرك AIX لاحقًا.");
            }
        } catch (Exception e) {
            say("تعذر تنفيذ الأمر على هذا الجهاز");
        }
    }

    private void openSetting(String action, String message) {
        startActivity(new Intent(action));
        say(message);
    }

    private void startVoice() {
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث إلى AIX");
            startActivityForResult(i, REQ_VOICE);
        } catch (Exception e) {
            say("التعرف الصوتي غير متاح على هذا الجهاز");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> values = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (values != null && !values.isEmpty()) {
                command.setText(values.get(0));
                execute(values.get(0));
            }
        }
    }

    private void requestCorePermissions() {
        if (Build.VERSION.SDK_INT < 23) return;
        ArrayList<String> permissions = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (permissions.isEmpty()) {
            say("الصلاحيات المطلوبة مفعلة");
        } else {
            requestPermissions(permissions.toArray(new String[0]), REQ_PERMS);
        }
    }

    private void say(String message) {
        output.setText(message);
        if (tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "aix");
        }
    }

    private void showDeviceReport() {
        say("الشركة: " + Build.MANUFACTURER +
                "\nالموديل: " + Build.MODEL +
                "\nAndroid: " + Build.VERSION.RELEASE +
                "\nSDK: " + Build.VERSION.SDK_INT +
                "\nHardware: " + Build.HARDWARE);
    }

    private List<ResolveInfo> launchableApps() {
        Intent q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> result = getPackageManager().queryIntentActivities(q, 0);
        Collections.sort(result, Comparator.comparing(a -> a.loadLabel(getPackageManager()).toString(), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private void renderApps() {
        appsBox.removeAllViews();
        List<ResolveInfo> list = launchableApps();
        int shown = 0;
        for (ResolveInfo r : list) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            String name = r.loadLabel(getPackageManager()).toString();
            appsBox.addView(button(name, v -> {
                Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
                if (launch != null) startActivity(launch);
            }));
            shown++;
        }
        say("تم العثور على " + shown + " تطبيقًا");
    }

    private void openAppByName(String requested) {
        String needle = requested.toLowerCase(Locale.ROOT);
        for (ResolveInfo r : launchableApps()) {
            String name = r.loadLabel(getPackageManager()).toString();
            String normalized = name.toLowerCase(Locale.ROOT);
            if (normalized.contains(needle) || needle.contains(normalized)) {
                Intent launch = getPackageManager().getLaunchIntentForPackage(r.activityInfo.packageName);
                if (launch != null) {
                    startActivity(launch);
                    say("فتحت " + name);
                    return;
                }
            }
        }
        say("لم أجد تطبيقًا باسم " + requested);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
