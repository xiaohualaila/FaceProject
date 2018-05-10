package com.shuli.root.faceproject.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.shuli.root.faceproject.R;
import com.shuli.root.faceproject.adapter.FaceTokenAdapter;
import com.shuli.root.faceproject.base.BaseFragment;
import java.util.ArrayList;
import butterknife.BindView;
import butterknife.OnClick;

public class QueryFragment  extends BaseFragment {
    @BindView(R.id.lv_group_info)
    ListView lv_info;
    private FaceTokenAdapter faceTokenAdapter;
    private OnQueryFragmentInteractionListener mListener;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_query;
    }

    @Override
    protected void init() {
        faceTokenAdapter = new FaceTokenAdapter();
        ArrayList<String> list =  mListener.QueryData();
        if(list != null &&list.size()>0){
            faceTokenAdapter.setData(list);
            lv_info.setAdapter(faceTokenAdapter);
        }
        unbindButton();
    }



    @OnClick(R.id.tv_back)
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_back:
                mListener.toCameraActivity();
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CameraFragment.OnFragmentInteractionListener) {
            mListener = (OnQueryFragmentInteractionListener) context;
        }
        else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    private void unbindButton() {
        faceTokenAdapter.setOnItemButtonClickListener(new FaceTokenAdapter.ItemButtonClickListener() {
            @Override
            public void onItemDeleteButtonClickListener(int position) {
            }

            @Override
            public void onItemUnbindButtonClickListener(int position) {
                final String token = faceTokenAdapter.getData().get(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("解绑");
                builder.setMessage("确定是否要解绑?");
                //点击对话框以外的区域是否让对话框消失
                builder.setCancelable(true);
                //设置正面按钮
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<String> list = mListener.unbindData(token);
                        if(list != null &&list.size()>0) {
                            faceTokenAdapter.setData(list);
                            lv_info.setAdapter(faceTokenAdapter);
                        }else {
                            Toast.makeText(getActivity(),"解绑失败！",Toast.LENGTH_LONG).show();
                        }
                        dialog.dismiss();
                    }
                });
                //设置反面按钮
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                Dialog dialog = builder.create();
                dialog.show();
            }
        });

    }


    public interface OnQueryFragmentInteractionListener {
        void toCameraActivity();
        ArrayList<String> QueryData();
        ArrayList<String> unbindData(String token);
    }
}
