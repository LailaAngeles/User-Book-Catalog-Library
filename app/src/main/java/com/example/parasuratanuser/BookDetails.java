package com.example.parasuratanuser;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

/**
 Ibig sabihin lang nito, puwede mo siyang ipasa sa ibang activity gamit ang Intent.putExtra()
 — halimbawa kung gusto mong i-click ang book sa isang list, tapos ilipat siya sa details page.
 */
public class BookDetails implements Serializable {
    private transient Bitmap bitmap;
    //Yung bitmap dito ay ginagamit lang sa loob ng app mo (UI),
    // hindi siya sine-save sa Firestore kasi transient siya.
    // Ginagamit lang ‘to para temporary image display sa app mismo.

    public void setBitmap(Bitmap bitmap) { this.bitmap = bitmap; }
    public Bitmap getBitmap() { return bitmap; }

    /* ---------------- Firestore fields ---------------- */
    private String documentId;
    private String title;
    private String author;
    private String description;
    private String year;
    private String publisher;
    private String genre;
    private String tags;
    @PropertyName("imageBase64")      // make sure Firestore uses this exact key
    private String imageBase64;

    private String type; // "library" or "favorite"

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BookDetails() {
        /* required empty ctor for Firestore */
    }


        //tamang kuha lng ng info para pasapasahan nalng
    public BookDetails(String title,
                       String author,
                       String year,
                       String publisher,
                       String genre,
                       String description,
                       String tags,
                       String imageBase64)
    {
        this.title        = title;
        this.author       = author;
        this.year         = year;
        this.publisher    = publisher;
        this.genre        = genre;
        this.description  = description;
        this.tags         = tags;
        this.imageBase64  = imageBase64;
    }

    /* ---------------- getters / setters ---------------- */

    @Exclude
    public String getDocumentId()                 { return documentId; }
    public void   setDocumentId(String id)        { this.documentId = id; }

    public String getTitle()                      { return title; }
    public void   setTitle(String title)          { this.title = title; }

    public String getAuthor()                     { return author; }
    public void   setAuthor(String author)        { this.author = author; }

    public String getDescription()                { return description; }
    public void   setDescription(String desc)     { this.description = desc; }

    public String getYear()                       { return year; }
    public void   setYear(String year)            { this.year = year; }

    public String getPublisher()                  { return publisher; }
    public void   setPublisher(String publisher)  { this.publisher = publisher; }

    public String getGenre()                      { return genre; }
    public void   setGenre(String genre)          { this.genre = genre; }

    public String getTags()                       { return tags; }
    public void   setTags(String tags)            { this.tags = tags; }

    @PropertyName("imageBase64")
    public String getImageBase64()                { return imageBase64; }
    @PropertyName("imageBase64")
    public void   setImageBase64(String img)      { this.imageBase64 = img; }
    // Inside BookDetails.java
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /* ---------------- helpers ---------------- */
// Sinusuri kung may nakatalang image sa format na Base64
    @Exclude
    public boolean hasImage() {
        return imageBase64 != null && !imageBase64.isEmpty();
    }
    // Ibinabalik ang string na representasyon ng BookDetails object
    @NonNull
    @Override
    public String toString() {
        return "BookDetails{" + title + " by " + author + "}";
    }
    // Sinasabing magkapareho ang dalawang BookDetails kung pareho ang kanilang documentId
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BookDetails)) return false;
        BookDetails other = (BookDetails) obj;
        return documentId != null && documentId.equals(other.documentId);
    }
    // Ibinabalik ang hash code batay sa documentId para sa tamang behavior sa collections tulad ng HashMap

    @Override
    public int hashCode() {
        return documentId != null ? documentId.hashCode() : 0;
    }
}
