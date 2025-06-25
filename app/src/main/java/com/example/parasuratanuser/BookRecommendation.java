package com.example.parasuratanuser;

public class BookRecommendation {
    private String title;
    private String author;
    private String imageBase64;

    public BookRecommendation(String title, String author, String imageBase64) {
        this.title = title;
        this.author = author;
        this.imageBase64 = imageBase64;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getImageBase64() {
        return imageBase64;
    }
}
