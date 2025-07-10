package com.qolorco.paytask;

import com.google.firebase.firestore.FirebaseFirestore;

public class GetDetails {
    String mainTelegram = "https://telegram.me/clickearningapp", mainYoutube = "https://youtube.com/@loudnishant";
    String downloadLink = "https://telegram.me/paytaskapp/4";
    String whatsappChannelLink = "";

    public GetDetails() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("AppData").document("links").get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                mainTelegram = documentSnapshot.getString("mainTelegram");
                mainYoutube = documentSnapshot.getString("mainYoutube");
                downloadLink = documentSnapshot.getString("downloadLink");
                whatsappChannelLink = documentSnapshot.getString("whatsappChannelLink");
            }
        });
    }

    public String getWhatsappChannelLink() {
        return whatsappChannelLink;
    }

    public String getMainYoutube() {
        return mainYoutube;
    }

    public String getMainTelegram() {
        return mainTelegram;
    }

    public String getDownloadLink() {
        return downloadLink;
    }
}
