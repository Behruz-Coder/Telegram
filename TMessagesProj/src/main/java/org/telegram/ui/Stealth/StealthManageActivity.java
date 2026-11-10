package org.telegram.ui.Stealth;

import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.stealth.StealthController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.UsersSelectActivity;

import java.util.ArrayList;

public class StealthManageActivity extends BaseFragment {

    private TextSettingsCell allowedChatsCell;
    private TextCheckCell decoySwitchCell;
    private TextSettingsCell decoyChatsCell;
    private TextSettingsCell secretCodeCell;
    private TextCheckCell autoLockCell;
    private TextCheckCell strictNotifCell;
    private TextCheckCell stealthSwitchCell;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Yashirin Boshqaruv");

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    StealthController.getInstance().forceLock();
                    Toast.makeText(getParentActivity(), "Ilova qulflandi", Toast.LENGTH_SHORT).show();
                    finishFragment();
                }
            }
        });

        ActionBarMenuItem lockItem = actionBar.createMenu().addItem(1, R.drawable.outline_header_lock_24);
        lockItem.setContentDescription("Qulflash");

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        StealthController controller = StealthController.getInstance();

        // --- SECTION 1: Asosiy rejim va Chatlar ---
        HeaderCell headerMain = new HeaderCell(context);
        headerMain.setText("Chatlar Ko'rinishi va Filtrlash");
        headerMain.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        linearLayout.addView(headerMain);

        stealthSwitchCell = new TextCheckCell(context);
        stealthSwitchCell.setTextAndCheck("Yashirin rejim faol", controller.isStealthEnabled(), true);
        stealthSwitchCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        stealthSwitchCell.setOnClickListener(v -> {
            boolean next = !controller.isStealthEnabled();
            controller.setStealthEnabled(next);
            stealthSwitchCell.setChecked(next);
        });
        linearLayout.addView(stealthSwitchCell);

        allowedChatsCell = new TextSettingsCell(context);
        updateAllowedChatsText();
        allowedChatsCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        allowedChatsCell.setOnClickListener(v -> openChatsSelector(false));
        linearLayout.addView(allowedChatsCell);

        TextInfoPrivacyCell infoCell1 = new TextInfoPrivacyCell(context);
        infoCell1.setText("Telegram faqat shu ro'yxatdagi chatlar, guruhlar va kanallarni ko'rsatadi. Qolgan barcha chatlar yashiriladi.");
        linearLayout.addView(infoCell1);

        // --- SECTION 2: Soxta Rejim (Decoy Mode) ---
        HeaderCell headerDecoy = new HeaderCell(context);
        headerDecoy.setText("Soxta Rejim (Decoy Mode)");
        headerDecoy.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        linearLayout.addView(headerDecoy);

        decoySwitchCell = new TextCheckCell(context);
        decoySwitchCell.setTextAndCheck("Soxta parolni yoqish", controller.isDecoyEnabled(), true);
        decoySwitchCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        decoySwitchCell.setOnClickListener(v -> {
            boolean next = !controller.isDecoyEnabled();
            if (next) {
                showSetDecoyPinDialog();
            } else {
                controller.setDecoyEnabled(false);
                decoySwitchCell.setChecked(false);
            }
        });
        linearLayout.addView(decoySwitchCell);

        decoyChatsCell = new TextSettingsCell(context);
        updateDecoyChatsText();
        decoyChatsCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        decoyChatsCell.setOnClickListener(v -> openChatsSelector(true));
        linearLayout.addView(decoyChatsCell);

        TextInfoPrivacyCell infoCell2 = new TextInfoPrivacyCell(context);
        infoCell2.setText("Majburiy vaziyatda soxta parol terilsa, faqat soxta chatlar ko'rinadi va haqiqiy ma'lumotlar fosh bo'lmaydi.");
        linearLayout.addView(infoCell2);

        // --- SECTION 3: Triggerlar va Parollar ---
        HeaderCell headerTriggers = new HeaderCell(context);
        headerTriggers.setText("Xavfsizlik va Triggerlar");
        headerTriggers.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        linearLayout.addView(headerTriggers);

        TextSettingsCell masterPinCell = new TextSettingsCell(context);
        masterPinCell.setText("Asosiy parolni o'zgartirish", true);
        masterPinCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        masterPinCell.setOnClickListener(v -> showChangeMasterPinDialog());
        linearLayout.addView(masterPinCell);

        secretCodeCell = new TextSettingsCell(context);
        secretCodeCell.setTextAndValue("Qidiruv maxfiy kodi", controller.getSecretCode(), true);
        secretCodeCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        secretCodeCell.setOnClickListener(v -> showEditSecretCodeDialog());
        linearLayout.addView(secretCodeCell);

        TextSettingsCell panicPinCell = new TextSettingsCell(context);
        panicPinCell.setText("Favqulodda tozalash (Panic PIN)", true);
        panicPinCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        panicPinCell.setOnClickListener(v -> showSetPanicPinDialog());
        linearLayout.addView(panicPinCell);

        autoLockCell = new TextCheckCell(context);
        autoLockCell.setTextAndCheck("Ilovadan chiqishda avto-qulflash", controller.isAutoLock(), true);
        autoLockCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        autoLockCell.setOnClickListener(v -> {
            boolean next = !controller.isAutoLock();
            controller.setAutoLock(next);
            autoLockCell.setChecked(next);
        });
        linearLayout.addView(autoLockCell);

        strictNotifCell = new TextCheckCell(context);
        strictNotifCell.setTextAndCheck("Yashirin chatlar bildirishnomasini bloklash", controller.isStrictNotifications(), false);
        strictNotifCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        strictNotifCell.setOnClickListener(v -> {
            boolean next = !controller.isStrictNotifications();
            controller.setStrictNotifications(next);
            strictNotifCell.setChecked(next);
        });
        linearLayout.addView(strictNotifCell);

        TextInfoPrivacyCell infoCell3 = new TextInfoPrivacyCell(context);
        infoCell3.setText("Qidiruv belgisini uzoq bosish yoki qidiruvga maxfiy kodni terish orqali yashirin menyuni ochish mumkin.");
        linearLayout.addView(infoCell3);

        // --- SECTION 4: JSON Backup & Restore ---
        HeaderCell headerBackup = new HeaderCell(context);
        headerBackup.setText("Zaxira Nusxa (JSON)");
        headerBackup.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        linearLayout.addView(headerBackup);

        TextSettingsCell exportCell = new TextSettingsCell(context);
        exportCell.setText("Konfiguratsiyani JSON eksport qilish", true);
        exportCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        exportCell.setOnClickListener(v -> exportJson());
        linearLayout.addView(exportCell);

        TextSettingsCell importCell = new TextSettingsCell(context);
        importCell.setText("JSON dan qayta tiklash (Import)", false);
        importCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        importCell.setOnClickListener(v -> importJson());
        linearLayout.addView(importCell);

        TextInfoPrivacyCell infoCell4 = new TextInfoPrivacyCell(context);
        infoCell4.setText("Ilovani qayta o'rnatganda barcha sozlamalar va ko'rinadigan chatlar ro'yxatini tezda tiklash uchun JSON fayldan foydalaning.");
        linearLayout.addView(infoCell4);

        return fragmentView;
    }

    private void updateAllowedChatsText() {
        if (allowedChatsCell != null) {
            int count = StealthController.getInstance().getAllowedDialogIds().size();
            allowedChatsCell.setTextAndValue("Ko'rinadigan chatlarni tanlash", count + " ta tanlangan", false);
        }
    }

    private void updateDecoyChatsText() {
        if (decoyChatsCell != null) {
            int count = StealthController.getInstance().getDecoyDialogIds().size();
            decoyChatsCell.setTextAndValue("Soxta rejim chatlari", count + " ta tanlangan", false);
        }
    }

    private void openChatsSelector(boolean isDecoy) {
        StealthController controller = StealthController.getInstance();
        ArrayList<Long> current = new ArrayList<>(isDecoy ? controller.getDecoyDialogIds() : controller.getAllowedDialogIds());

        UsersSelectActivity activity = new UsersSelectActivity(true, current, 0);
        activity.setDelegate((ids, flags) -> {
            if (isDecoy) {
                controller.setDecoyDialogIds(ids);
                updateDecoyChatsText();
            } else {
                controller.setAllowedDialogIds(ids);
                updateAllowedChatsText();
            }
        });
        presentFragment(activity);
    }

    private void showChangeMasterPinDialog() {
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Asosiy Parolni O'zgartirish");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        EditText pinEditText = new EditText(context);
        pinEditText.setHint("Yangi parol");
        pinEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(pinEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        EditText confirmEditText = new EditText(context);
        confirmEditText.setHint("Yangi parolni tasdiqlang");
        confirmEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(confirmEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String p1 = pinEditText.getText().toString().trim();
            String p2 = confirmEditText.getText().toString().trim();
            if (TextUtils.isEmpty(p1) || !p1.equals(p2)) {
                Toast.makeText(context, "Parollar mos kelmadi yoki bo'sh!", Toast.LENGTH_SHORT).show();
                return;
            }
            StealthController.getInstance().setMasterPin(p1);
            Toast.makeText(context, "Asosiy parol o'zgartirildi", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }

    private void showSetDecoyPinDialog() {
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Soxta Parol O'rnatish");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        EditText pinEditText = new EditText(context);
        pinEditText.setHint("Soxta parol (PIN)");
        pinEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(pinEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String p = pinEditText.getText().toString().trim();
            if (TextUtils.isEmpty(p)) {
                decoySwitchCell.setChecked(false);
                return;
            }
            StealthController.getInstance().setDecoyPin(p, true);
            decoySwitchCell.setChecked(true);
            Toast.makeText(context, "Soxta parol saqlandi", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Bekor qilish", (dialog, which) -> decoySwitchCell.setChecked(false));
        builder.show();
    }

    private void showSetPanicPinDialog() {
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Favqulodda tozalash (Panic PIN)");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        TextView tv = new TextView(context);
        tv.setText("Ushbu parol kiritilganda ilovadagi barcha maxfiy chatlar ro'yxati va parollar darhol xotiradan tozalab tashlanadi.");
        layout.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        EditText pinEditText = new EditText(context);
        pinEditText.setHint("Panic PIN");
        pinEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(pinEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String p = pinEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(p)) {
                StealthController.getInstance().setPanicPin(p, true);
                Toast.makeText(context, "Panic PIN o'rnatildi", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }

    private void showEditSecretCodeDialog() {
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Qidiruv maxfiy kodi");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        EditText codeEditText = new EditText(context);
        codeEditText.setText(StealthController.getInstance().getSecretCode());
        layout.addView(codeEditText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String c = codeEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(c)) {
                StealthController.getInstance().setSecretCode(c);
                if (secretCodeCell != null) {
                    secretCodeCell.setTextAndValue("Qidiruv maxfiy kodi", c, true);
                }
            }
        });
        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }

    private void exportJson() {
        String json = StealthController.getInstance().exportConfigJson();
        if (TextUtils.isEmpty(json)) {
            Toast.makeText(getParentActivity(), "Eksport qilishda xatolik!", Toast.LENGTH_SHORT).show();
            return;
        }

        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("JSON Eksport");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        TextView tv = new TextView(context);
        tv.setText("Quyidagi JSON matnini nusxalab xavfsiz joyda saqlang yoki ulashing:");
        layout.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        EditText jsonEdit = new EditText(context);
        jsonEdit.setText(json);
        jsonEdit.setTextSize(12);
        jsonEdit.setMaxLines(8);
        jsonEdit.setSelectAllOnFocus(true);
        layout.addView(jsonEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton("Nusxa olish", (dialog, which) -> {
            AndroidUtilities.addToClipboard(json);
            Toast.makeText(context, "JSON xotiraga nusxalandi!", Toast.LENGTH_SHORT).show();
        });
        builder.setNeutralButton("Ulashish", (dialog, which) -> {
            try {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, json);
                sendIntent.setType("text/plain");
                context.startActivity(Intent.createChooser(sendIntent, "JSON konfiguratsiyani yuborish"));
            } catch (Exception e) {
                AndroidUtilities.addToClipboard(json);
            }
        });
        builder.setNegativeButton("Yopish", null);
        builder.show();
    }

    private void importJson() {
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("JSON dan Tiklash (Import)");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        TextView tv = new TextView(context);
        tv.setText("Eski zaxira JSON matnini shu yerga qo'ying:");
        layout.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        EditText jsonEdit = new EditText(context);
        jsonEdit.setHint("JSON matnini kiriting...");
        jsonEdit.setTextSize(13);
        jsonEdit.setMaxLines(6);
        layout.addView(jsonEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setView(layout);
        builder.setPositiveButton("Tiklash", (dialog, which) -> {
            String jsonText = jsonEdit.getText().toString().trim();
            boolean success = StealthController.getInstance().importConfigJson(jsonText);
            if (success) {
                Toast.makeText(context, "Sozlamalar va chatlar muvaffaqiyatli tiklandi!", Toast.LENGTH_SHORT).show();
                updateAllowedChatsText();
                updateDecoyChatsText();
                if (secretCodeCell != null) {
                    secretCodeCell.setTextAndValue("Qidiruv maxfiy kodi", StealthController.getInstance().getSecretCode(), true);
                }
                if (autoLockCell != null) {
                    autoLockCell.setChecked(StealthController.getInstance().isAutoLock());
                }
                if (strictNotifCell != null) {
                    strictNotifCell.setChecked(StealthController.getInstance().isStrictNotifications());
                }
            } else {
                Toast.makeText(context, "Xatolik: Noto'g'ri JSON formati!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Bekor qilish", null);
        builder.show();
    }
}
