package org.telegram.ui.Stealth;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.stealth.StealthController;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class StealthDialogHelper {

    public static void showUnlockOrSetupDialog(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }

        StealthController controller = StealthController.getInstance();
        if (!controller.isPasswordSet()) {
            showSetupDialog(fragment);
        } else {
            showUnlockDialog(fragment);
        }
    }

    public static void showSetupDialog(BaseFragment fragment) {
        Context context = fragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Yashirin Menyu: Parol O'rnatish");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        TextView messageText = new TextView(context);
        messageText.setText("Yashirin menyuga kirish va ko'rinadigan chatlarni boshqarish uchun yangi parol o'rnating.");
        messageText.setTextSize(14);
        messageText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(messageText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        EditText pinEditText = new EditText(context);
        pinEditText.setHint("Yangi parol");
        pinEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pinEditText.setTextSize(16);
        pinEditText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        pinEditText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        layout.addView(pinEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        EditText confirmEditText = new EditText(context);
        confirmEditText.setHint("Parolni tasdiqlang");
        confirmEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmEditText.setTextSize(16);
        confirmEditText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        confirmEditText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        layout.addView(confirmEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        builder.setView(layout);

        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String pin = pinEditText.getText().toString().trim();
            String confirmPin = confirmEditText.getText().toString().trim();

            if (TextUtils.isEmpty(pin)) {
                Toast.makeText(context, "Parol bo'sh bo'lishi mumkin emas!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pin.equals(confirmPin)) {
                Toast.makeText(context, "Parollar mos kelmadi!", Toast.LENGTH_SHORT).show();
                return;
            }

            StealthController.getInstance().setMasterPin(pin);
            Toast.makeText(context, "Parol muvaffaqiyatli saqlandi!", Toast.LENGTH_SHORT).show();
            fragment.presentFragment(new StealthManageActivity());
        });

        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }

    public static void showUnlockDialog(BaseFragment fragment) {
        Context context = fragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Xavfsizlik Paroli");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        TextView messageText = new TextView(context);
        messageText.setText("Yashirin menyuga kirish uchun parolni kiriting:");
        messageText.setTextSize(14);
        messageText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(messageText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        EditText pinEditText = new EditText(context);
        pinEditText.setHint("Parol");
        pinEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pinEditText.setTextSize(16);
        pinEditText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        pinEditText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        layout.addView(pinEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);

        builder.setPositiveButton("Kirish", (dialog, which) -> {
            String pin = pinEditText.getText().toString().trim();
            int result = StealthController.getInstance().verifyPin(pin);

            if (result == StealthController.RESULT_MASTER) {
                fragment.presentFragment(new StealthManageActivity());
            } else if (result == StealthController.RESULT_DECOY) {
                Toast.makeText(context, "Rejim faollashtirildi", Toast.LENGTH_SHORT).show();
            } else if (result == StealthController.RESULT_PANIC) {
                Toast.makeText(context, "Xavfsizlik sozlamalari tozalandi", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Noto'g'ri parol!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }
}
