package com.shuli.root.faceproject.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;

/**
 * Created by Administrator on 2017/11/30.
 */
@Entity
public class People {
    @Id(autoincrement = true)
    private Long _id;
    private String name;//姓名
    private String gonghao;//工号
    private String face_token;//人脸特征

    @Keep
    @Generated(hash = 697618186)
    public People() {
    }

    @Keep
    @Generated(hash = 577612888)
    public People(String name,String gonghao, String face_token) {
        this.name = name;
        this.gonghao = gonghao;
        this.face_token = face_token;
    }

    @Generated(hash = 80712147)
    public People(Long _id, String name, String gonghao, String face_token) {
        this._id = _id;
        this.name = name;
        this.gonghao = gonghao;
        this.face_token = face_token;
    }

    public Long get_id() {
        return this._id;
    }
    public void set_id(Long _id) {
        this._id = _id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getGonghao() {
        return gonghao;
    }

    public void setGonghao(String gonghao) {
        this.gonghao = gonghao;
    }

    public String getFace_token() {
        return face_token;
    }

    public void setFace_token(String face_token) {
        this.face_token = face_token;
    }
}
