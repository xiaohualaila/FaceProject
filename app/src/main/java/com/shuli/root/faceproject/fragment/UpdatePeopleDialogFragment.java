package com.shuli.root.faceproject.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.shuli.root.faceproject.R;

/**
 * Created by Administrator on 2018/2/25.
 */

public class UpdatePeopleDialogFragment extends DialogFragment {
    private static final String NAME = "name";
    private static final String GONGHAO = "gonghao";
    private View.OnClickListener positiveCallback;
    public TextView tv_title;
    public String name;
    public String gonghao;
    public EditText et_name;
    public EditText et_gonghao;
    public static UpdatePeopleDialogFragment getInstance(String name,String gonghao) {
        Bundle bundle = new Bundle();
        bundle.putString(NAME,name);
        bundle.putString(GONGHAO,gonghao);
        UpdatePeopleDialogFragment versionDialogFragment = new UpdatePeopleDialogFragment();
        versionDialogFragment.setArguments(bundle);
        return versionDialogFragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout((int) (dm.widthPixels * 0.7), ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            }
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        name = bundle.getString(NAME);
        gonghao = bundle.getString(GONGHAO);
    }

    public void show(FragmentManager fragmentManager, View.OnClickListener positiveCallback) {
        this.positiveCallback = positiveCallback;
        show(fragmentManager, "VersionDialogFragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.update_people_dialog, null);
        tv_title =  view.findViewById(R.id.tv_title);
        et_name =  view.findViewById(R.id.et_name);
        et_gonghao =  view.findViewById(R.id.et_gonghao);
        et_name.setText(name);
        et_gonghao.setText(gonghao);
        Button btn_sure = view.findViewById(R.id.btn_sure);
        btn_sure.setOnClickListener(positiveCallback);
        builder.setView(view);
        return builder.create();
    }


}
