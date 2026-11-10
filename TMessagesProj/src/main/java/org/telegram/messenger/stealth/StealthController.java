package org.telegram.messenger.stealth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StealthController {

    private static final String PREFS_NAME = "stealth_settings";

    public static final int RESULT_WRONG = 0;
    public static final int RESULT_MASTER = 1;
    public static final int RESULT_DECOY = 2;
    public static final int RESULT_PANIC = 3;

    public static final int MODE_NORMAL = 0;
    public static final int MODE_DECOY = 1;

    private static volatile StealthController Instance;

    private final SharedPreferences preferences;
    private final Set<Long> allowedDialogIds = new HashSet<>();
    private final Set<Long> decoyDialogIds = new HashSet<>();

    private String masterPinHash;
    private byte[] masterPinSalt;

    private String decoyPinHash;
    private byte[] decoyPinSalt;
    private boolean decoyEnabled;

    private String panicPinHash;
    private byte[] panicPinSalt;
    private boolean panicEnabled;

    private String secretCode;
    private boolean autoLock;
    private boolean strictNotifications;
    private boolean stealthEnabled;

    private int currentMode = MODE_NORMAL;
    private boolean isUnlocked = false;

    public static StealthController getInstance() {
        StealthController localInstance = Instance;
        if (localInstance == null) {
            synchronized (StealthController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new StealthController();
                }
            }
        }
        return localInstance;
    }

    private StealthController() {
        Context context = ApplicationLoader.applicationContext;
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadConfig();
    }

    private synchronized void loadConfig() {
        masterPinHash = preferences.getString("master_pin_hash", "");
        String saltStr = preferences.getString("master_pin_salt", "");
        masterPinSalt = TextUtils.isEmpty(saltStr) ? new byte[0] : Base64.decode(saltStr, Base64.DEFAULT);

        decoyPinHash = preferences.getString("decoy_pin_hash", "");
        String decoySaltStr = preferences.getString("decoy_pin_salt", "");
        decoyPinSalt = TextUtils.isEmpty(decoySaltStr) ? new byte[0] : Base64.decode(decoySaltStr, Base64.DEFAULT);
        decoyEnabled = preferences.getBoolean("decoy_enabled", false);

        panicPinHash = preferences.getString("panic_pin_hash", "");
        String panicSaltStr = preferences.getString("panic_pin_salt", "");
        panicPinSalt = TextUtils.isEmpty(panicSaltStr) ? new byte[0] : Base64.decode(panicSaltStr, Base64.DEFAULT);
        panicEnabled = preferences.getBoolean("panic_enabled", false);

        secretCode = preferences.getString("secret_code", "#7777#");
        autoLock = preferences.getBoolean("auto_lock", true);
        strictNotifications = preferences.getBoolean("strict_notifications", true);
        stealthEnabled = preferences.getBoolean("stealth_enabled", true);

        allowedDialogIds.clear();
        String allowedStr = preferences.getString("allowed_dialog_ids", "[]");
        try {
            JSONArray arr = new JSONArray(allowedStr);
            for (int i = 0; i < arr.length(); i++) {
                allowedDialogIds.add(arr.getLong(i));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        decoyDialogIds.clear();
        String decoyStr = preferences.getString("decoy_dialog_ids", "[]");
        try {
            JSONArray arr = new JSONArray(decoyStr);
            for (int i = 0; i < arr.length(); i++) {
                decoyDialogIds.add(arr.getLong(i));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public synchronized boolean isPasswordSet() {
        return !TextUtils.isEmpty(masterPinHash) && masterPinSalt.length > 0;
    }

    public synchronized boolean isStealthEnabled() {
        return stealthEnabled;
    }

    public synchronized void setStealthEnabled(boolean enabled) {
        this.stealthEnabled = enabled;
        preferences.edit().putBoolean("stealth_enabled", enabled).apply();
        notifyReload();
    }

    public synchronized String getSecretCode() {
        return secretCode;
    }

    public synchronized void setSecretCode(String code) {
        this.secretCode = code;
        preferences.edit().putString("secret_code", code).apply();
    }

    public synchronized boolean isAutoLock() {
        return autoLock;
    }

    public synchronized void setAutoLock(boolean autoLock) {
        this.autoLock = autoLock;
        preferences.edit().putBoolean("auto_lock", autoLock).apply();
    }

    public synchronized boolean isStrictNotifications() {
        return strictNotifications;
    }

    public synchronized void setStrictNotifications(boolean strictNotifications) {
        this.strictNotifications = strictNotifications;
        preferences.edit().putBoolean("strict_notifications", strictNotifications).apply();
    }

    public synchronized boolean isDecoyEnabled() {
        return decoyEnabled;
    }

    public synchronized void setDecoyEnabled(boolean enabled) {
        this.decoyEnabled = enabled;
        preferences.edit().putBoolean("decoy_enabled", enabled).apply();
    }

    public synchronized boolean isPanicEnabled() {
        return panicEnabled;
    }

    public synchronized void setPanicEnabled(boolean enabled) {
        this.panicEnabled = enabled;
        preferences.edit().putBoolean("panic_enabled", enabled).apply();
    }

    public synchronized void setMasterPin(String pin) {
        if (TextUtils.isEmpty(pin)) return;
        masterPinSalt = new byte[16];
        Utilities.random.nextBytes(masterPinSalt);
        masterPinHash = hashPin(pin, masterPinSalt);
        preferences.edit()
                .putString("master_pin_hash", masterPinHash)
                .putString("master_pin_salt", Base64.encodeToString(masterPinSalt, Base64.DEFAULT))
                .apply();
    }

    public synchronized void setDecoyPin(String pin, boolean enabled) {
        this.decoyEnabled = enabled;
        if (!TextUtils.isEmpty(pin)) {
            decoyPinSalt = new byte[16];
            Utilities.random.nextBytes(decoyPinSalt);
            decoyPinHash = hashPin(pin, decoyPinSalt);
        }
        preferences.edit()
                .putString("decoy_pin_hash", decoyPinHash)
                .putString("decoy_pin_salt", decoyPinSalt.length > 0 ? Base64.encodeToString(decoyPinSalt, Base64.DEFAULT) : "")
                .putBoolean("decoy_enabled", decoyEnabled)
                .apply();
    }

    public synchronized void setPanicPin(String pin, boolean enabled) {
        this.panicEnabled = enabled;
        if (!TextUtils.isEmpty(pin)) {
            panicPinSalt = new byte[16];
            Utilities.random.nextBytes(panicPinSalt);
            panicPinHash = hashPin(pin, panicPinSalt);
        }
        preferences.edit()
                .putString("panic_pin_hash", panicPinHash)
                .putString("panic_pin_salt", panicPinSalt.length > 0 ? Base64.encodeToString(panicPinSalt, Base64.DEFAULT) : "")
                .putBoolean("panic_enabled", panicEnabled)
                .apply();
    }

    public synchronized int verifyPin(String pin) {
        if (TextUtils.isEmpty(pin)) return RESULT_WRONG;

        // 1. Check Panic PIN
        if (panicEnabled && panicPinSalt.length > 0 && !TextUtils.isEmpty(panicPinHash)) {
            String hash = hashPin(pin, panicPinSalt);
            if (panicPinHash.equals(hash)) {
                wipeAllData();
                return RESULT_PANIC;
            }
        }

        // 2. Check Decoy PIN
        if (decoyEnabled && decoyPinSalt.length > 0 && !TextUtils.isEmpty(decoyPinHash)) {
            String hash = hashPin(pin, decoyPinSalt);
            if (decoyPinHash.equals(hash)) {
                currentMode = MODE_DECOY;
                isUnlocked = true;
                notifyReload();
                return RESULT_DECOY;
            }
        }

        // 3. Check Master PIN
        if (masterPinSalt.length > 0 && !TextUtils.isEmpty(masterPinHash)) {
            String hash = hashPin(pin, masterPinSalt);
            if (masterPinHash.equals(hash)) {
                currentMode = MODE_NORMAL;
                isUnlocked = true;
                notifyReload();
                return RESULT_MASTER;
            }
        }

        return RESULT_WRONG;
    }

    public synchronized void lock() {
        if (autoLock) {
            isUnlocked = false;
            currentMode = MODE_NORMAL;
            notifyReload();
        }
    }

    public synchronized void forceLock() {
        isUnlocked = false;
        currentMode = MODE_NORMAL;
        notifyReload();
    }

    public synchronized boolean isUnlocked() {
        return isUnlocked;
    }

    public synchronized int getCurrentMode() {
        return currentMode;
    }

    public synchronized boolean isChatAllowed(long dialogId) {
        if (!stealthEnabled) {
            return true;
        }
        if (currentMode == MODE_DECOY) {
            return decoyDialogIds.contains(dialogId);
        }
        // In normal mode: only whitelisted chats are visible
        return allowedDialogIds.contains(dialogId);
    }

    public synchronized Set<Long> getAllowedDialogIds() {
        return Collections.unmodifiableSet(new HashSet<>(allowedDialogIds));
    }

    public synchronized void setAllowedDialogIds(ArrayList<Long> ids) {
        allowedDialogIds.clear();
        if (ids != null) {
            allowedDialogIds.addAll(ids);
        }
        JSONArray arr = new JSONArray();
        for (Long id : allowedDialogIds) {
            arr.put(id);
        }
        preferences.edit().putString("allowed_dialog_ids", arr.toString()).apply();
        notifyReload();
    }

    public synchronized Set<Long> getDecoyDialogIds() {
        return Collections.unmodifiableSet(new HashSet<>(decoyDialogIds));
    }

    public synchronized void setDecoyDialogIds(ArrayList<Long> ids) {
        decoyDialogIds.clear();
        if (ids != null) {
            decoyDialogIds.addAll(ids);
        }
        JSONArray arr = new JSONArray();
        for (Long id : decoyDialogIds) {
            arr.put(id);
        }
        preferences.edit().putString("decoy_dialog_ids", arr.toString()).apply();
        notifyReload();
    }

    public synchronized void wipeAllData() {
        allowedDialogIds.clear();
        decoyDialogIds.clear();
        masterPinHash = "";
        masterPinSalt = new byte[0];
        decoyPinHash = "";
        decoyPinSalt = new byte[0];
        panicPinHash = "";
        panicPinSalt = new byte[0];
        isUnlocked = false;
        currentMode = MODE_NORMAL;
        preferences.edit().clear().apply();
        notifyReload();
    }

    public synchronized String exportConfigJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("version", 1);
            obj.put("exported_at", System.currentTimeMillis());

            obj.put("master_pin_hash", masterPinHash);
            obj.put("master_pin_salt", masterPinSalt.length > 0 ? Base64.encodeToString(masterPinSalt, Base64.DEFAULT) : "");

            obj.put("decoy_enabled", decoyEnabled);
            obj.put("decoy_pin_hash", decoyPinHash);
            obj.put("decoy_pin_salt", decoyPinSalt.length > 0 ? Base64.encodeToString(decoyPinSalt, Base64.DEFAULT) : "");

            obj.put("panic_enabled", panicEnabled);
            obj.put("panic_pin_hash", panicPinHash);
            obj.put("panic_pin_salt", panicPinSalt.length > 0 ? Base64.encodeToString(panicPinSalt, Base64.DEFAULT) : "");

            obj.put("secret_code", secretCode);
            obj.put("auto_lock", autoLock);
            obj.put("strict_notifications", strictNotifications);
            obj.put("stealth_enabled", stealthEnabled);

            JSONArray allowedArr = new JSONArray();
            for (Long id : allowedDialogIds) {
                allowedArr.put(id);
            }
            obj.put("allowed_dialog_ids", allowedArr);

            JSONArray decoyArr = new JSONArray();
            for (Long id : decoyDialogIds) {
                decoyArr.put(id);
            }
            obj.put("decoy_dialog_ids", decoyArr);

            return obj.toString(2);
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    public synchronized boolean importConfigJson(String jsonStr) {
        if (TextUtils.isEmpty(jsonStr)) return false;
        try {
            JSONObject obj = new JSONObject(jsonStr);
            if (!obj.has("version")) return false;

            SharedPreferences.Editor editor = preferences.edit();

            if (obj.has("master_pin_hash")) {
                editor.putString("master_pin_hash", obj.getString("master_pin_hash"));
            }
            if (obj.has("master_pin_salt")) {
                editor.putString("master_pin_salt", obj.getString("master_pin_salt"));
            }
            if (obj.has("decoy_enabled")) {
                editor.putBoolean("decoy_enabled", obj.getBoolean("decoy_enabled"));
            }
            if (obj.has("decoy_pin_hash")) {
                editor.putString("decoy_pin_hash", obj.getString("decoy_pin_hash"));
            }
            if (obj.has("decoy_pin_salt")) {
                editor.putString("decoy_pin_salt", obj.getString("decoy_pin_salt"));
            }
            if (obj.has("panic_enabled")) {
                editor.putBoolean("panic_enabled", obj.getBoolean("panic_enabled"));
            }
            if (obj.has("panic_pin_hash")) {
                editor.putString("panic_pin_hash", obj.getString("panic_pin_hash"));
            }
            if (obj.has("panic_pin_salt")) {
                editor.putString("panic_pin_salt", obj.getString("panic_pin_salt"));
            }
            if (obj.has("secret_code")) {
                editor.putString("secret_code", obj.getString("secret_code"));
            }
            if (obj.has("auto_lock")) {
                editor.putBoolean("auto_lock", obj.getBoolean("auto_lock"));
            }
            if (obj.has("strict_notifications")) {
                editor.putBoolean("strict_notifications", obj.getBoolean("strict_notifications"));
            }
            if (obj.has("stealth_enabled")) {
                editor.putBoolean("stealth_enabled", obj.getBoolean("stealth_enabled"));
            }
            if (obj.has("allowed_dialog_ids")) {
                editor.putString("allowed_dialog_ids", obj.getJSONArray("allowed_dialog_ids").toString());
            }
            if (obj.has("decoy_dialog_ids")) {
                editor.putString("decoy_dialog_ids", obj.getJSONArray("decoy_dialog_ids").toString());
            }

            editor.apply();
            loadConfig();
            notifyReload();
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    private void notifyReload() {
        try {
            int account = UserConfig.selectedAccount;
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private String hashPin(String pin, byte[] salt) {
        try {
            byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[salt.length + pinBytes.length + salt.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(pinBytes, 0, combined, salt.length, pinBytes.length);
            System.arraycopy(salt, 0, combined, salt.length + pinBytes.length, salt.length);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(combined);
            return bytesToHex(digest);
        } catch (Exception e) {
            FileLog.e(e);
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
