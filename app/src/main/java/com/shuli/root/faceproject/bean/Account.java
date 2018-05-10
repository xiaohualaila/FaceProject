package com.shuli.root.faceproject.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;

@Entity
public class Account {

    @Id(autoincrement = true)
    private Long _id;
    private String account_name;//账号
    private String account_secret;//密码

    public Account() {
    }

    @Keep
    @Generated(hash = 577612888)
    public Account(String account_name, String account_secret) {
        this.account_name = account_name;
        this.account_secret = account_secret;
    }

    @Generated(hash = 906529715)
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
