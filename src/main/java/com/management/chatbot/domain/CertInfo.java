package com.management.chatbot.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DialectOverride;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter @Setter
@NoArgsConstructor
public class CertInfo implements Serializable{
    private String image;
    private Timestamp date;

    @Builder
    public CertInfo(String image, Timestamp date) {
        this.image = image;
        this.date = date;
    }

    @Override
    public String toString(){
        return "CertInfo{" +
                "image=" + image +
                ", date=" + date +
                "}";
    }


}