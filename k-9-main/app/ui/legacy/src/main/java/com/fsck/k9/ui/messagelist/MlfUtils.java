package com.fsck.k9.ui.messagelist;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStoreProvider;


public class MlfUtils {

    static LocalFolder getOpenFolder(long folderId, Account account) throws MessagingException {
        LocalStore localStore = DI.get(LocalStoreProvider.class).getInstance(account);
        LocalFolder localFolder = localStore.getFolder(folderId);
        localFolder.open();
        return localFolder;
    }

    static void setLastSelectedFolder(Preferences preferences, List<MessageReference> messages, long folderId) {
        MessageReference firstMsg = messages.get(0);
        Account account = preferences.getAccount(firstMsg.getAccountUuid());
        account.setLastSelectedFolderId(folderId);
    }

    static String buildSubject(String subjectFromCursor, String emptySubject, int threadCount) {
        if (TextUtils.isEmpty(subjectFromCursor)) {
            return emptySubject;
        } else if (threadCount > 1) {
            // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
            return Utility.stripSubject(subjectFromCursor);
        }
        return subjectFromCursor;
    }

    public static String removeSignature(String body){
        body = body.replaceAll("<s>(.*)</s>", "");
        return body.trim();
    };
    public static String getSignature(String body){
        //create a pattern object
        Pattern pattern = Pattern.compile("<s>(.*)</s>");

        //create a matcher object
        Matcher matcher = pattern.matcher(body);

        if( matcher.find() ) {
            return matcher.group(1).trim();
        }else {
            return "";
        }
    };
}
