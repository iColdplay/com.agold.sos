package com.agold.sos;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.agold.sos.database.NumberProvider;
import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;

import java.util.ArrayList;

/**
 * Created by root on 17-5-4.
 */

public class RecyclerViewAdapter extends RecyclerSwipeAdapter<RecyclerViewAdapter.SimpleViewHolder> {
    private NumberProvider mNumberprovider;
    private Cursor mCursor;

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        SwipeLayout swipeLayout;
        TextView textViewPos;
        TextView textViewData;
        TextView textViewDataName;
        Button buttonDelete;

        public SimpleViewHolder(View itemView) {
            super(itemView);
            swipeLayout = (SwipeLayout) itemView.findViewById(R.id.swipe);
            textViewPos = (TextView) itemView.findViewById(R.id.position);
            textViewData = (TextView) itemView.findViewById(R.id.text_data);
            textViewDataName = (TextView) itemView.findViewById(R.id.text_data_name);
            buttonDelete = (Button) itemView.findViewById(R.id.delete);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(getClass().getSimpleName(), "onItemSelected: " + textViewData.getText().toString());
                    Toast.makeText(view.getContext(), "onItemSelected: " + textViewData.getText().toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private Context mContext;
    private ArrayList<String> mDataset;
    private ArrayList<String> mNameset;

    //protected SwipeItemRecyclerMangerImpl mItemManger = new SwipeItemRecyclerMangerImpl(this);

    public RecyclerViewAdapter(Context context, ArrayList<String> objects,ArrayList<String> objectsName) {
        this.mContext = context;
        this.mDataset = objects;
        this.mNameset = objectsName;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerveiw_item, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SimpleViewHolder viewHolder, final int position) {
        String item = mDataset.get(position);
        String itemName = mNameset.get(position);
        viewHolder.swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);
        viewHolder.swipeLayout.addSwipeListener(new SimpleSwipeListener() {
            @Override
            public void onOpen(SwipeLayout layout) {
               // YoYo.with(Techniques.Tada).duration(500).delay(100).playOn(layout.findViewById(R.id.trash));
            }
        });
        viewHolder.swipeLayout.setOnDoubleClickListener(new SwipeLayout.DoubleClickListener() {
            @Override
            public void onDoubleClick(SwipeLayout layout, boolean surface) {
                Toast.makeText(mContext, "DoubleClick", Toast.LENGTH_SHORT).show();
            }
        });
        viewHolder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mItemManger.removeShownLayouts(viewHolder.swipeLayout);
                mDataset.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, mDataset.size());
                mItemManger.closeAllItems();
                //Toast.makeText(view.getContext(), "Deleted " + viewHolder.textViewData.getText().toString() + "!", Toast.LENGTH_SHORT).show();

                //数据库操作
                String deletedNum = viewHolder.textViewData.getText().toString();
                mNumberprovider = new NumberProvider(view.getContext());
                mNumberprovider.open();
                if(mCursor != null){
                    mCursor.close();
                }
                mCursor = mNumberprovider.query();
                if(mCursor != null){
                    mCursor.moveToFirst();
                    for(int i = 0;i < mCursor.getCount();i++){
                        android.util.Log.i("ly20170504","detected contact number -->"+mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NUM)));
                        if(deletedNum.contains(mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NUM)))){
                            android.util.Log.i("ly20170504","gonna detected contact number -->"+deletedNum);
                            Boolean deleteResult = mNumberprovider.deleteData(mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NUM)));
                            if(deleteResult){
                                Toast.makeText(view.getContext(), "删除紧急联系人 成功", Toast.LENGTH_SHORT).show();
                                //通知界面刷新
                                Intent notify = new Intent("agold.sos.should.refresh");
                                view.getContext().sendBroadcast(notify);
                                continue;
                            }else{
                                Toast.makeText(view.getContext(), "删除紧急联系人 失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                        mCursor.moveToNext();
                    }
                }
                mCursor.close();
                mNumberprovider.close();
            }
        });
        viewHolder.textViewPos.setText((position + 1) + ".");
        viewHolder.textViewData.setText("号码：" + item);
        android.util.Log.i("ly20170505","show this name in recyclerView -->"+mNameset.get(position));
        viewHolder.textViewDataName.setText("名称："+ mNameset.get(position));
        mItemManger.bind(viewHolder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }
}
