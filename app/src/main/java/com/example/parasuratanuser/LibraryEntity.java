package com.example.parasuratanuser;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "library")
public class LibraryEntity {
    @PrimaryKey @NonNull public String id;

    public String title;
    public String author;
    public String description;
    public String year;
    public String publisher;
    public String genre;
    public String tags;
    public String imageBase64;
    public String status;
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LibraryEntity)) return false;
        LibraryEntity other = (LibraryEntity) obj;

        return Objects.equals(id, other.id) &&
                Objects.equals(title, other.title) &&
                Objects.equals(author, other.author) &&
                Objects.equals(description, other.description) &&
                Objects.equals(year, other.year) &&
                Objects.equals(publisher, other.publisher) &&
                Objects.equals(genre, other.genre) &&
                Objects.equals(tags, other.tags) &&
                Objects.equals(imageBase64, other.imageBase64) &&
                Objects.equals(status, other.status);
    }

}
