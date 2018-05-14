package com.shuli.root.faceproject.bean;


public class Account {


    private Long _id;
    private String account_name;//账号
    private String account_secret;//密码

    public Account() {
    }

    public Account(String account_name, String account_secret) {
        this.account_name = account_name;
        this.account_secret = account_secret;
    }

    public Account(Long _id, String account_name, String account_secret) {
        this._id = _id;
        this.account_name = account_name;
        this.account_secret = account_secret;
    }

    public Long get_id() {
        return _id;
    }

    public void set_id(Long _id) {
        this._id = _id;
    }

    public String getAccount_name() {
        return account_name;
    }

    public void setAccount_name(String account_name) {
        this.account_name = account_name;
    }

    public String getAccount_secret() {
        return account_secret;
    }

    public void setAccount_secret(String account_secret) {
        this.account_secret = account_secret;
    }
}
