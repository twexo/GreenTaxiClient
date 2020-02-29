package com.twexdo.client;

public class Sofer {

    private String nume;
    private String telefon;
    private int status;

    public String getURL() {
        return url;
    }

    private String url;

    public Sofer(String nume, String telefon, int status,String url) {
        this.nume = nume;
        this.telefon = telefon;
        this.status = status;
        this.url=url;
    }


    public boolean contain(String n){
        return telefon.equals(n);
    }

    public String getNume() {
        return nume;
    }

    public String getTelefon() {
        return telefon;
    }

    public int getStatus() {
        return status;
    }

}

