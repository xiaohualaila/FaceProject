package com.shuli.root.faceproject.bean;


/**
 * Created by Administrator on 2017/11/30.
 */

public class People {

    private Long _id;
    private String name;//姓名
    private String gonghao;//工号
    private String face_token;//人脸特征


    public People() {
    }

    public People(String name,String gonghao, String face_token) {
        this.name = name;
        this.gonghao = gonghao;
        this.face_token = face_token;
    }

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
