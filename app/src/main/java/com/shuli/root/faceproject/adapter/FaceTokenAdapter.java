package com.shuli.root.faceproject.adapter;

import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.shuli.faceproject.greendaodemo.greendao.GreenDaoManager;
import com.shuli.faceproject.greendaodemo.greendao.gen.AccountDao;
import com.shuli.faceproject.greendaodemo.greendao.gen.PeopleDao;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.bean.Account;
import com.shuli.root.faceproject.bean.People;

import java.util.List;



/**
 * Created by xingchaolei on 2017/12/5.
 */

public class FaceTokenAdapter extends BaseAdapter {

    private List<String> mFaceTokens;
    private LayoutInflater mLayoutInflater;
    private ItemButtonClickListener mItemButtonClickListener;

    public FaceTokenAdapter() {
        super();
    }

    public List<String> getData() {
        return mFaceTokens;
    }

    public void setData(List<String> data) {
        mFaceTokens = data;
    }

    public void setOnItemButtonClickListener(ItemButtonClickListener listener) {
        mItemButtonClickListener = listener;
    }

    @Override
    public int getCount() {
        return mFaceTokens == null ? 0 : mFaceTokens.size();
    }

    @Override
    public Object getItem(int position) {
        return mFaceTokens == null ? null : mFaceTokens.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.layout_item_face_token, parent, false);
            holder = new ViewHolder();
            holder.faceTokenNameTv =  convertView.findViewById(R.id.tv_face_token);
            holder.name = convertView.findViewById(R.id.tv_name);
            holder.gonghao = convertView.findViewById(R.id.tv_gonghao);
            holder.deleteFaceTokenIv =  convertView.findViewById(R.id.iv_delete_face);
            holder.unbindGroupTv =  convertView.findViewById(R.id.tv_face_unbind);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        holder.faceTokenNameTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemButtonClickListener != null) {
                    mItemButtonClickListener.onItemDeleteButtonClickListener(position);
                }
            }
        });
        holder.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemButtonClickListener != null) {
                    mItemButtonClickListener.onItemDeleteButtonClickListener(position);
                }
            }
        });
        holder.gonghao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemButtonClickListener != null) {
                    mItemButtonClickListener.onItemDeleteButtonClickListener(position);
                }
            }
        });

        holder.unbindGroupTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemButtonClickListener != null) {
                    mItemButtonClickListener.onItemUnbindButtonClickListener(position);
                }
            }
        });

        holder.faceTokenNameTv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cmb = (ClipboardManager) v.getContext().getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(holder.faceTokenNameTv.getText());
                Toast.makeText(v.getContext(), "FaceToke已复制", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        String token = mFaceTokens.get(position);
        holder.faceTokenNameTv.setText(token);
        People people = GreenDaoManager.getInstance().getSession().getPeopleDao().queryBuilder()
                .where(PeopleDao.Properties.Face_token.eq(token)).build().unique();
        if(people!=null){
            holder.name.setText(people.getName());
            holder.gonghao.setText(people.getGonghao());
        }
        return convertView;
    }


    public static class ViewHolder {
        TextView faceTokenNameTv;
        TextView name;
        TextView gonghao;
        ImageView deleteFaceTokenIv;
        ImageView unbindGroupTv;
    }


    public interface ItemButtonClickListener {

        void onItemDeleteButtonClickListener(int position);

        void onItemUnbindButtonClickListener(int position);

    }
}
