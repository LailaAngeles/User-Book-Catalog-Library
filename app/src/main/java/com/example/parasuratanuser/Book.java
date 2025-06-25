package com.example.parasuratanuser;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
public class Book {
    private String bookId;
    private String title;
    private String author;
    private String description;
    private String category;
    private String imageUrl;

    // Empty constructor
    public Book() {}

    // Full constructor
    public Book(String bookId, String title, String author, String description, String category, String imageUrl) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
    }

}
