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

public class VersionDialogFragment extends DialogFragment {
    private View.OnClickListener positiveCallback;
    public EditText et_account;
    public EditText et_secret;
    public TextView tv_title;

    public static VersionDialogFragment getInstance() {
        VersionDialogFragment versionDialogFragment = new VersionDialogFragment();
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
        final View view = inflater.inflate(R.layout.dialog, null);
        tv_title =  view.findViewById(R.id.tv_title);
        et_account =  view.findViewById(R.id.account);
        et_secret =  view.findViewById(R.id.secret);
        Button btn_sure = view.findViewById(R.id.btn_sure);
        btn_sure.setOnClickListener(positiveCallback);
        builder.setView(view);
        return builder.create();
    }



}
