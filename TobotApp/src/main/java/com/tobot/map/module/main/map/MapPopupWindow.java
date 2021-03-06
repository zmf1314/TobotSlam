package com.tobot.map.module.main.map;

import android.content.Context;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.tobot.map.R;
import com.tobot.map.base.BaseConstant;
import com.tobot.map.db.MyDBSource;
import com.tobot.map.module.common.BasePopupWindow;
import com.tobot.map.module.common.ConfirmDialog;
import com.tobot.map.module.common.LoadTipsDialog;
import com.tobot.map.module.common.NumberInputDialog;
import com.tobot.map.module.main.DataHelper;
import com.tobot.map.module.main.MainHandle;
import com.tobot.map.util.ToastUtils;
import com.tobot.slam.SlamManager;
import com.tobot.slam.agent.listener.OnResultListener;
import com.tobot.slam.data.LocationBean;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * 地图
 *
 * @author houdeming
 * @date 2020/3/15
 */
public class MapPopupWindow extends BasePopupWindow implements PopupWindow.OnDismissListener, NumberInputDialog.OnNumberListener, ConfirmDialog.OnConfirmListener {
    private WeakReference<Handler> mHandlerWeakReference;
    private TextView tvBuildMap;
    private OnMapListener mOnMapListener;
    private AddPointViewDialog mAddPointViewDialog;
    private NumberInputDialog mNumberInputDialog;
    private LoadTipsDialog mLoadTipsDialog;
    private ConfirmDialog mConfirmDialog;

    public MapPopupWindow(Context context, WeakReference<Handler> reference, OnMapListener listener) {
        super(context);
        mHandlerWeakReference = reference;
        mOnMapListener = listener;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.popup_map;
    }

    @Override
    public void initView(View view) {
        tvBuildMap = view.findViewById(R.id.tv_build_map);
        tvBuildMap.setOnClickListener(this);
        view.findViewById(R.id.tv_add_point).setOnClickListener(this);
        view.findViewById(R.id.tv_relocation).setOnClickListener(this);
        view.findViewById(R.id.tv_clean_map).setOnClickListener(this);
        view.findViewById(R.id.tv_save_map).setOnClickListener(this);
    }

    @Override
    public void onDismiss() {
        closeAddPointViewDialog();
        closeNumberInputDialog();
        closeLoadTipsDialog();
        if (isConfirmDialogShow()) {
            mConfirmDialog.getDialog().dismiss();
            mConfirmDialog = null;
        }
    }

    @Override
    public void show(View parent) {
        super.show(parent);
        new MapThread().start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_build_map:
                if (tvBuildMap.isSelected()) {
                    tvBuildMap.setSelected(false);
                } else {
                    tvBuildMap.setSelected(true);
                }
                SlamManager.getInstance().setMapUpdateInThread(tvBuildMap.isSelected(), null);
                break;
            case R.id.tv_add_point:
                dismiss();
                if (mOnMapListener != null) {
                    mOnMapListener.onMapAddPoint();
                }
                break;
            case R.id.tv_relocation:
                dismiss();
                relocation();
                break;
            case R.id.tv_clean_map:
                if (mOnMapListener != null) {
                    mOnMapListener.onMapClean();
                }
                break;
            case R.id.tv_save_map:
                dismiss();
                if (mOnMapListener != null) {
                    mOnMapListener.onMapSave();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onNumber(String number) {
        if (TextUtils.equals(number, "00") || TextUtils.equals(number, "0")) {
            ToastUtils.getInstance(mContext).show(mContext.getString(R.string.number_format_error_tips));
            return;
        }
        closeNumberInputDialog();
        showDialogTips(mContext.getString(R.string.map_save_tips));
        saveMap(number);
    }

    @Override
    public void onConfirm() {
        dismiss();
        cleanMap();
    }

    public void setMapUpdateStatus(boolean isUpdate) {
        tvBuildMap.setSelected(isUpdate);
    }

    public void showAddPointViewDialog(FragmentManager fragmentManager, AddPointViewDialog.OnPointListener listener) {
        if (!isAddPointViewDialogShow()) {
            mAddPointViewDialog = AddPointViewDialog.newInstance();
            mAddPointViewDialog.setOnPointListener(listener);
            mAddPointViewDialog.show(fragmentManager, "ADD_POINT_DIALOG");
        }
    }

    public void showNumberInputDialog(FragmentManager fragmentManager) {
        if (!isNumberInputDialogShow()) {
            mNumberInputDialog = NumberInputDialog.newInstance(mContext.getString(R.string.tv_title_save_map), mContext.getString(R.string.map_rule_tips), mContext.getString(R.string.et_hint_map_tips));
            mNumberInputDialog.setOnNumberListener(this);
            mNumberInputDialog.show(fragmentManager, "NUMBER_INPUT_DIALOG");
        }
    }

    public void showLoadTipsDialog(FragmentManager fragmentManager, String tips) {
        if (!isLoadTipsDialogShow()) {
            mLoadTipsDialog = LoadTipsDialog.newInstance(tips);
            mLoadTipsDialog.show(fragmentManager, "LOAD_TIPS_DIALOG");
        }
    }

    public void closeLoadTipsDialog() {
        try {
            if (isLoadTipsDialogShow()) {
                mLoadTipsDialog.getDialog().dismiss();
                mLoadTipsDialog = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showConfirmDialog(FragmentManager fragmentManager, String tips) {
        if (!isConfirmDialogShow()) {
            mConfirmDialog = ConfirmDialog.newInstance(tips);
            mConfirmDialog.setOnConfirmListener(this);
            mConfirmDialog.show(fragmentManager, "CLEAN_MAP_DIALOG");
        }
    }

    private void relocation() {
        showDialogTips(mContext.getString(R.string.relocation_map_tips));
        SlamManager.getInstance().recoverLocationByDefault(new OnResultListener<Boolean>() {
            @Override
            public void onResult(Boolean data) {
                mHandlerWeakReference.get().obtainMessage(MainHandle.MSG_RELOCATION, data).sendToTarget();
            }
        });
    }

    private void cleanMap() {
        showDialogTips(mContext.getString(R.string.map_clean_tips));
        SlamManager.getInstance().clearMapInThread(new OnResultListener<Boolean>() {
            @Override
            public void onResult(Boolean data) {
                if (data) {
                    MyDBSource.getInstance(mContext).deleteAllLocation();
                }
                mHandlerWeakReference.get().obtainMessage(MainHandle.MSG_CLEAN_MAP, data).sendToTarget();
            }
        });
    }

    private void saveMap(String number) {
        final List<LocationBean> beanList = DataHelper.getInstance().getLocationBeanList(mContext, number);
        SlamManager.getInstance().saveMapInThread(BaseConstant.getMapDirectory(mContext), BaseConstant.getFileName(number), beanList, new OnResultListener<Boolean>() {
            @Override
            public void onResult(Boolean data) {
                mHandlerWeakReference.get().obtainMessage(MainHandle.MSG_SAVE_MAP, data).sendToTarget();
            }
        });
    }

    private boolean isLoadTipsDialogShow() {
        return mLoadTipsDialog != null && mLoadTipsDialog.getDialog() != null && mLoadTipsDialog.getDialog().isShowing();
    }

    private void closeAddPointViewDialog() {
        if (isAddPointViewDialogShow()) {
            mAddPointViewDialog.getDialog().dismiss();
            mAddPointViewDialog = null;
        }
    }

    private void closeNumberInputDialog() {
        if (isNumberInputDialogShow()) {
            mNumberInputDialog.getDialog().dismiss();
            mNumberInputDialog = null;
        }
    }

    private boolean isAddPointViewDialogShow() {
        return mAddPointViewDialog != null && mAddPointViewDialog.getDialog() != null && mAddPointViewDialog.getDialog().isShowing();
    }

    private boolean isNumberInputDialogShow() {
        return mNumberInputDialog != null && mNumberInputDialog.getDialog() != null && mNumberInputDialog.getDialog().isShowing();
    }

    private void showDialogTips(String tips) {
        if (mOnMapListener != null) {
            mOnMapListener.onMapShowTipsDialog(tips);
        }
    }

    private boolean isConfirmDialogShow() {
        return mConfirmDialog != null && mConfirmDialog.getDialog() != null && mConfirmDialog.getDialog().isShowing();
    }

    private class MapThread extends Thread {
        @Override
        public void run() {
            super.run();
            mHandlerWeakReference.get().obtainMessage(MainHandle.MSG_MAP_IS_UPDATE, SlamManager.getInstance().isMapUpdate()).sendToTarget();
        }
    }

    public interface OnMapListener {
        /**
         * 建点
         */
        void onMapAddPoint();

        /**
         * 提示dialog
         */
        void onMapShowTipsDialog(String tips);

        /**
         * 清除地图
         */
        void onMapClean();

        /**
         * 保存地图
         */
        void onMapSave();
    }
}
