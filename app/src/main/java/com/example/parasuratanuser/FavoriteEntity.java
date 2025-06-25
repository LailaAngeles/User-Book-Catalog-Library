package com.example.parasuratanuser;

// 'Entity' ibig sabihin table ito sa database, at ito yung magiging structure ng data natin.
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

// So dito, ginagawa nating Room Entity si FavoriteEntity. Table name niya ay "favorites".
@Entity(tableName = "favorites")
public class FavoriteEntity {

    // Ito yung primary key, parang unique ID sa bawat record. Dapat hindi ito null.
    @PrimaryKey @NonNull public String id;

    // Ito yung mga columns ng table. Lahat ng 'to ay fields ng isang favorite item.
    public String title;         // title ng libro or item
    public String author;        // pangalan ng author
    public String description;   // maikling summary siguro
    public String year;          // year of publication
    public String publisher;     // kung sino nag-publish
    public String genre;         // genre ng libro
    public String tags;          // mga keywords or category
    public String imageBase64;   // encoded image, usually para sa thumbnail or cover
    public String status;        // status ng item (e.g. "read", "unread", etc.)

    // Okay so eto naman, override natin yung equals function
    // Para lang masabing "equal" ang dalawang FavoriteEntity kung pareho lahat ng fields nila
    @Override
    public boolean equals(Object obj) {
        // Kung parehong object lang, edi true agad
        if (this == obj) return true;

        // Check kung same class ba sila
        if (!(obj instanceof FavoriteEntity)) return false;

        // Cast natin sa FavoriteEntity para ma-compare field by field
        FavoriteEntity other = (FavoriteEntity) obj;

        // I-compare bawat field gamit 'Objects.equals'
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
