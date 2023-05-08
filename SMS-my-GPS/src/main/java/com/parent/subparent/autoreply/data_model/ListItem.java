package com.parent.subparent.autoreply.data_model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;

public final class ListItem {
    public String sender;
    public String message_prefix;

    public ListItem(String sender, String message_prefix) {
        this.sender         = sender;
        this.message_prefix = message_prefix;
    }

    @Override
    public String toString() {
        return sender;
    }

    // helpers

    public static ArrayList<ListItem> fromJson(String json) {
        ArrayList<ListItem> arrayList;
        Gson gson = new Gson();
        arrayList = gson.fromJson(json, new TypeToken<ArrayList<ListItem>>(){}.getType());
        return arrayList;
    }

    public static String toJson(ArrayList<ListItem> arrayList) {
        String json = new Gson().toJson(arrayList);
        return json;
    }

    // matched against text SMS messages
    public static boolean matches(ArrayList<ListItem> arrayList, String sender, String message) {
        ListItem item;
        int prefix_length;
        String prefix;

        if ((sender == null) || sender.isEmpty())
            return false;

        if ((message == null) || message.isEmpty())
            return false;

        for (int i=0; i < arrayList.size(); i++) {
            try {
                item = arrayList.get(i);

                // sanity checks
                if ((item.sender == null) || item.sender.isEmpty() || (item.message_prefix == null) || item.message_prefix.isEmpty())
                    continue;

                if (!sender.endsWith(item.sender) && !item.sender.equals("*"))
                    continue;

                prefix_length = item.message_prefix.length();

                if (prefix_length > message.length())
                    continue;

                prefix = message.substring(0, prefix_length);

                if (prefix.equals(item.message_prefix))
                    return true;
            }
            catch(Exception e) { continue; }
        }
        return false;
    }

    // matched against silent data-only SMS messages
    public static boolean matches(ArrayList<ListItem> arrayList, String sender) {
        ListItem item;

        if ((sender == null) || sender.isEmpty())
            return false;

        for (int i=0; i < arrayList.size(); i++) {
            try {
                item = arrayList.get(i);

                // sanity checks
                if ((item.sender == null) || item.sender.isEmpty() || (item.message_prefix == null) || item.message_prefix.isEmpty())
                    continue;

                if (!sender.endsWith(item.sender) || item.sender.equals("*"))
                    continue;

                return true;
            }
            catch(Exception e) { continue; }
        }
        return false;
    }
}
