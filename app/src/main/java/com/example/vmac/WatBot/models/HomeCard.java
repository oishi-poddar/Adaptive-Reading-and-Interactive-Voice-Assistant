package com.example.vmac.WatBot.models;

public class HomeCard {
    private String text;
    private int image;

    public String getText() {
        return text;
    }

    public int getImage() {
        return image;
    }

    public HomeCard(String text, int image) {
        this.text = text;
        this.image = image;
    }
}
