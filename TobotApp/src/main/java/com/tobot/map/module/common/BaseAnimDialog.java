package com.tobot.map.module.common;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * @author houdeming
 * @date 2019/10/22
 */
public abstract class BaseAnimDialog extends DialogFragment implements NumberInputDialog.OnNumberListener, ConfirmDialog.OnConfirmListener {
    private NumberInputDialog mNumberInputDialog;
    private ConfirmDialog mConfirmDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 关闭进出动画
//        Window window = getDialog().getWindow();
//        if (window != null) {
//            window.setWindowAnimations(getAnimId());
//        }
        return inflater.inflate(getLayoutResId(), null);
    }

    @Override
    public void onStart() {
        super.onStart();
        // 解决因为系统定制不同问题造成的dialog大小显示的问题
        Dialog dialog = getDialog();
        if (dialog != null) {
            // 点击空白消失
            dialog.setCanceledOnTouchOutside(true);
            Window window = dialog.getWindow();
            if (window != null) {
                // Activity背景不变暗
                WindowManager.LayoutParams params = window.getAttributes();
                params.dimAmount = 0f;
                // 去掉dialog黑框
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setGravity(getGravity());
                window.setLayout(getDialogWidth(), getDialogHeight());
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
    }

    @Override
    public void onPause() {
        super.onPause();
        closeNumberInputDialog();
        if (isConfirmDialogShow()) {
            mConfirmDialog.getDialog().dismiss();
            mConfirmDialog = null;
        }
    }

    protected void showNumberInputDialog(String title, String contentTips, String hint) {
        if (!isNumberInputDialogShow()) {
            mNumberInputDialog = NumberInputDialog.newInstance(title, contentTips, hint);
            mNumberInputDialog.setOnNumberListener(this);
            mNumberInputDialog.show(getFragmentManager(), "NUMBER_INPUT_DIALOG");
        }
    }

    protected void closeNumberInputDialog() {
        if (isNumberInputDialogShow()) {
            mNumberInputDialog.getDialog().dismiss();
            mNumberInputDialog = null;
        }
    }

    protected void showConfirmDialog(String tips) {
        if (!isConfirmDialogShow()) {
            mConfirmDialog = ConfirmDialog.newInstance(tips);
            mConfirmDialog.setOnConfirmListener(this);
            mConfirmDialog.show(getFragmentManager(), "DELETE_DIALOG");
        }
    }

    private boolean isConfirmDialogShow() {
        return mConfirmDialog != null && mConfirmDialog.getDialog() != null && mConfirmDialog.getDialog().isShowing();
    }

    private boolean isNumberInputDialogShow() {
        return mNumberInputDialog != null && mNumberInputDialog.getDialog() != null && mNumberInputDialog.getDialog().isShowing();
    }

    protected abstract int getAnimId();

    protected abstract int getLayoutResId();

    protected abstract int getGravity();

    protected abstract int getDialogWidth();

    protected abstract int getDialogHeight();

    protected abstract void initView(View view);
}
