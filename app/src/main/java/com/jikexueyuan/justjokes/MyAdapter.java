package com.jikexueyuan.justjokes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * Created by fangc on 2016/1/29.
 */
public class MyAdapter extends BaseAdapter implements View.OnClickListener { //为了将lists中数据显示到activity，可用adapter与ListView资源配合显示，系统的simpleCursorAdapter不能满足时,就自定义
    private ICallback callback;
    private List<HashMap<String, String>> postLists;
    private Context context;       //因为我们很多的操作都依赖于Context，所以此处保留context实例的引用
    // 用来承接上下文语境。

    public MyAdapter(Context context, List<HashMap<String, String>> postLists, ICallback callback) {
        this.postLists = postLists;
        this.context = context;
        this.callback = callback;
    }

    public void setPostLists(List<HashMap<String, String>> postLists) {
        this.postLists = postLists;
    }

    @Override
    public void onClick(View v) {
        callback.innerButtonClicked(v);
    }

    public interface ICallback {
        void innerButtonClicked(View v);
    }

    @Override
    public int getCount() {
        return postLists.size();  //返回集合总数据有多少条
    }

    @Override
    public Object getItem(int position) {
        return postLists.get(position);
    }//返回当前列表项所关联的数据模型对象

    @Override
    public long getItemId(int position) {
        return position;                   //把position当作ItemId，返回当前listView的position，
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {//此方法中，我们必须获取到当前要加载的

        ViewHolder holder;
        if (convertView == null) {//如果等于空，说明并没有加载过此View既item UI对象(在适配器基类中，当有新的item UI对象需要显示时，

            convertView = LayoutInflater.from(context).inflate(R.layout.cell, null);//加载View
            holder = new ViewHolder();            //实例化ViewHolder
            holder.title = (TextView) convertView.findViewById(R.id.title); //建立关联
            holder.date = (TextView) convertView.findViewById(R.id.date);
            holder.innerButton= (ImageButton) convertView.findViewById(R.id.innerButton);
            convertView.setTag(holder);              //通过setTag将（加载过的两条TextView）标签进行存储，本次优化关键点！！！！
        }
        holder = (ViewHolder) convertView.getTag(); //如果convertView不为空，代表它是已经加载过的UI对象（view），

        holder.innerButton.setOnClickListener(this);
        holder.innerButton.setTag(position);

        holder.title.setText(postLists.get(position).get("post_title"));  //cell中加载内容
        holder.date.setText(postLists.get(position).get("post_date"));
        return convertView;  //返回当前View
    }

    private static class ViewHolder {
        private TextView title;
        private TextView date;
        private ImageButton innerButton;
        // TODO:  这里需要在监听item内部button的点击事件，否则直接用系统的ArrayAdapter/simpleAdapter了！
    }
}
