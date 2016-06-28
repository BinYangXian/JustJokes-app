package com.jikexueyuan.justjokes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.jikexueyuan.justjokes.MyAdapter.ICallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainFragment extends Fragment implements ICallback, AbsListView.OnScrollListener {

    private MyDB myDB;
    private SQLiteDatabase dbWrite, dbRead;
    private MyAdapter adapter = null;
    private List<HashMap<String, String>> postLists;
    private ListView listView;
    final static String ACTION = "com.jikexueyuan.justjokes.intent.action.NetWorkConnectedBroadcastReceiver";
    private View rootView, moreView;

    //不同常量对应不同功能的异步任务模式_upDataMode：
    private String _upDataMode = null;
    private final static String ASYNC_TASK_LOAD_FIRST = "1";//首次安装应用启动后分页加载
    private final static String ASYNC_TASK_LOAD_MORE = "2";

    private final static int FIRST_PAGE = 1;//开启应用首次加载
    private int loadPage = 1;
    private int lastItem;
    private boolean isFinishLoadCurrentPage = true;
    private boolean isRefreshingPage = false;

    private boolean isCurrentPageLastPage = false;
    private boolean isPrevPageLastPage = false;
    private List<HashMap<String, String>> tempCacheLists;//为了刷新的快速通道而建
    private boolean haveMoreView=false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        registerReceiver(); //注册断网后来网的 广播监听接收器。
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        listView = (ListView) rootView.findViewById(R.id.listView);//当fragment切换的时候需要重新设置
        if (!isCurrentPageLastPage) {//当fragment切换的时候需要重新设置，而排除错误的加载moreView
            moreView = View.inflate(getActivity(), R.layout.load_more, null);//加载更多的提示view
            listView.addFooterView(moreView); //添加底部提示view，一定要在setAdapter之前添加，否则会报错。
            haveMoreView=true;
        }
        pullToRefresh(rootView);
        listView.setOnScrollListener(this); //设置listview的滚动事件
        if (adapter == null) {  //当adapter没有初始化时
            initChangeListView();
        } else {
            listView.setAdapter(adapter);//当fragment切换的时候需要重新设置adapter，所以不能再initAdapter()中执行此
        }

        return rootView;
    }

    private void initChangeListView() {
        postLists = new ArrayList<>();
        tempCacheLists = new ArrayList<>();
        myDB = new MyDB(getActivity());

        if (isNetworkConnected()) {  //如果有网，开启异步 任务 比较数据，判断是否更新数据库数据并刷新显示
            Log.i("Note", "马上开始异步比较网络数据后,如有更新再刷新显示");
            isRefreshingPage = true;
            asyncTaskForUpDataCacheToChangeListView("http://kanxiaohua.applinzi.com/get-posts.php?page="
                    + FIRST_PAGE, ASYNC_TASK_LOAD_FIRST);
        } else {
            postLists = getPostListsToPrepareForLoad();//没网时就调用数据库cache表中数据直接显示
            adapter = new MyAdapter(getActivity(), postLists, MainFragment.this);
            listView.setAdapter(adapter);
            isRefreshingPage = false;
        }
    }

    private void asyncTaskForUpDataCacheToChangeListView(String url, String upDataMode) {//参数2.加载更多/更新
        new AsyncTask<String, Void, List<HashMap<String, String>>>() {


            @Override
            protected List<HashMap<String, String>> doInBackground(String... params) {
                try {
                    URL url = new URL(params[0]);
                    Log.i("params[0]=", params[0]);
                    _upDataMode = params[1];
                    URLConnection connection = url.openConnection();
                    InputStream is = connection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is, "utf-8");
                    BufferedReader br = new BufferedReader(isr);
                    String line;
                    StringBuilder builder = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        builder.append(line);
                    }
                    JSONObject root = new JSONObject(builder.toString());
                    JSONArray totalPostsArray = root.getJSONArray("paging");
                    String title = "";
                    String date = "";
                    String content = "";
                    int id;
                    dbWrite = myDB.getWritableDatabase();
                    dbRead = myDB.getReadableDatabase();
                    ContentValues cv = new ContentValues();
                    int i;
                    for (i = 0; i < totalPostsArray.length(); i++) {
                        JSONObject jsonPost = totalPostsArray.getJSONObject(i);

                        title = jsonPost.getString("post_title");
                        date = "发布日期:" + jsonPost.getString("post_date");
                        content = jsonPost.getString("post_content");
                        id = jsonPost.getInt("ID");

                        Cursor cachePost_date = dbRead.query("cache", new String[]{"post_date"}, "post_date=?"
                                , new String[]{date}, null, null, null); //改为唯一的时间标记是否需要更新
                        if (!cachePost_date.moveToPosition(0)) {//当数据库中没有此ID column时，才插入数据到数据库；
                            // 同时每次插入postLists的0号位，以防多事地再把数据从数据库中查出来
                            System.out.println(title + date + content + "\n");//测试
                            cv.put("post_title", title);
                            cv.put("post_date", date);
                            cv.put("post_content", content);
                            cv.put("ID", id);
                            cv.put("page", loadPage);
                            dbWrite.insert("cache", null, cv); //缓存在sql中
                        }
                        // /为服务器删除数据后，更新时删除本地数据，当数据库中有此ID column时，才加入临时List中，减少重复动作
                        HashMap<String, String> post = new HashMap<>();
                        post.put("post_date", date);
                        tempCacheLists.add(0, post);
                        Log.i("Note", "刷新 1");
                    }
                    //删除客户端多余数据：
                    Cursor c = dbRead.query("cache", new String[]{"post_date"}, "page=?"
                            , new String[]{String.valueOf(loadPage)}, null, null, "ID ASC");//"ID ASC"最新数据在最底部，优化速度
                    c.moveToPosition(-1);
                    int j;
                    Log.i("Note", "刷新 2");
                    while (c.moveToNext()) {
                        String post_date = c.getString(c.getColumnIndex("post_date"));
                        for (j = 0; j < tempCacheLists.size(); j++) {
                            if (tempCacheLists.get(j).get("post_date").equals(post_date))//如temp中有此条c之数据
                                break;
                        }
                        if (j == tempCacheLists.size()) {
                            dbWrite.delete("cache", "post_date=?", new String[]{post_date});
                        }
                    }
                    Log.i("Note", "刷新 3");
                    tempCacheLists.clear();//清空对比用的临时Lists

                    dbRead.close();
                    dbWrite.close();
                    br.close();
                    isr.close();
                    is.close();

                    postLists = getPostListsToPrepareForLoad();
                    return postLists;

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<HashMap<String, String>> lists) {
                switch (Integer.parseInt(_upDataMode)) {
                    case 1:        // 并启动加载之功能。
                        adapter = new MyAdapter(getActivity(), lists, MainFragment.this);
                        listView.setAdapter(adapter);
                        isRefreshingPage = false;
                        Toast.makeText(getActivity(), "刷新完成", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        if (isPrevPageLastPage) {  //上一页是最后一页时，加载当前也当然无数据变化，就不用加载了
                            finishLoadMoreAction();
                            loadPage--;//还原加载更多功能的副作用，因为没有更多可加载。
                        } else {
                            adapter.setPostLists(lists); //数据变化传入adapter
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getActivity(), "已加载好了更多数据", Toast.LENGTH_SHORT).show();
                        }
                        Log.i("TAG", "加载了更多数据");
                        isFinishLoadCurrentPage = true;
                        break;
                }

                super.onPostExecute(lists);
            }
        }.execute(url, upDataMode);
    }

    private List<HashMap<String, String>> getPostListsToPrepareForLoad() {
        Log.i("Note", "准备初始化adapter");
        String title;
        String date;
        String content;
        dbRead = myDB.getReadableDatabase();
        Cursor c = dbRead.query("cache", null, "page=?", new String[]{String.valueOf(loadPage)}, null, null, "ID DESC");//c为ID的降序
        Log.i("Note", "刷新 4");
        int firstItemIndexOfNextPage = (loadPage - 1) * 10;
        c.moveToPosition(-1);
        int i;
        for (i = firstItemIndexOfNextPage; i < loadPage * 10; i++) { //每一次对postLists只增加需要加载的数据，不重复加载
            if (!c.moveToNext()) { //当下一条数据为空时，跳出循环
                break;
            }
            Log.i("Note", "刷新 5、"+i);
            HashMap<String, String> post = new HashMap<>();
            title = c.getString(c.getColumnIndex("post_title"));
            date = c.getString(c.getColumnIndex("post_date"));
            content = c.getString(c.getColumnIndex("post_content"));
            post.put("post_title", title);
            post.put("post_date", date);
            post.put("post_content", content);
            postLists.add(i, post);
        }
        if (i < loadPage * 10 && i != firstItemIndexOfNextPage) {
            isCurrentPageLastPage = true;
        } else if (i == firstItemIndexOfNextPage && _upDataMode.equals(ASYNC_TASK_LOAD_MORE)) { //当本次查询
            // 到数据与上次查询相同，则i值没有变化，说明没有更多数据，上一页是最后一页。
            isPrevPageLastPage = true;
        }
        dbRead.close();
        return postLists;
    }



    @Override
    public void innerButtonClicked(View v) {
//        Log.i("Note", "监听到item内部按钮点击事件");
        String post_content = postLists.get((Integer) v.getTag()).get("post_content");
        Bundle bundle = new Bundle();
        bundle.putString("post_content", post_content);
        ContentFragment contentFragment = new ContentFragment();
        contentFragment.setArguments(bundle);

        getFragmentManager().beginTransaction().addToBackStack(null)
                .replace(R.id.container, contentFragment).commit();
    }


    //当网络变化时
//    private BroadcastReceiver netWorkConnectedBroadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (isFinishLoadCurrentPage) {
//                if (!isRefreshingPage) {  //单一情况下动作，以免重复动作
//                    initRefresh();
//                    listView.addFooterView(moreView);
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getActivity(), "已恢复网络连接，正在默默的为你查看更新", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                    asyncTaskForUpDataCacheToChangeListView("http://kanxiaohua.applinzi.com/get-posts.php?page="
//                            + FIRST_PAGE, ASYNC_TASK_LOAD_FIRST);
//
//                }
//            } else {
//                Toast.makeText(getActivity(), "网络恢复，自动刷新被加载更多动作阻断，请手动刷新", Toast.LENGTH_SHORT).show();
//            }
//        }
//    };


    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    /**
     * Created by fangc on 2016/5/15.
     */
    public void pullToRefresh(final View rootView) {
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeLayout);

//        swipeRefreshLayout.setColorSchemeResources(R.color.swipe_color_1,
//        R.color.swipe_color_2,
//        R.color.swipe_color_3,
//        R.color.swipe_color_4);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.LARGE);
        ;
//        swipeRefreshLayout.setProgressBackgroundColor(R.color.swipe_background_color);
        swipeRefreshLayout.setPadding(20, 20, 20, 20);
        swipeRefreshLayout.setProgressViewOffset(true, 100, 200);
        swipeRefreshLayout.setDistanceToTriggerSync(50);
        swipeRefreshLayout.setProgressViewEndTarget(true, 100);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {

                        Log.i("Note", "下拉刷新");
                        System.out.println("网络情况：" + isNetworkConnected());
                        if (isNetworkConnected()) {
                            if (isFinishLoadCurrentPage) {
                                if (!isRefreshingPage) {
                                    initRefresh();
                                    Log.i("Note", "刷新 0");
                                    asyncTaskForUpDataCacheToChangeListView("http://kanxiaohua.applinzi.com/get-posts.php?page="
                                            + FIRST_PAGE, ASYNC_TASK_LOAD_FIRST);
                                } else {
                                    Toast.makeText(getActivity(), "骚年，正在刷新中", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getActivity(), "骚年，此时正在加载更多，请稍后刷新", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), "似乎没有网络连接，世界上最遥远的距离就是没网", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }

        );

    }

    private void initRefresh() {
        isRefreshingPage = true;
        postLists.clear();
        adapter = null;
        loadPage=1;
        if (!haveMoreView){
            listView.addFooterView(moreView);
            moreView.setVisibility(View.VISIBLE);
            haveMoreView=true;
        }
        isCurrentPageLastPage=false;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        lastItem = firstVisibleItem + visibleItemCount - 1; //减1是因为上面加了个addFooterView
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //下拉到空闲时候，且最后一个item的数等于数据的总数时，加载更多
        if (lastItem == postLists.size() && scrollState == SCROLL_STATE_IDLE) {
            Log.i("TAG", "拉到最底部");
            moreView.setVisibility(View.VISIBLE);

            mHandler.sendEmptyMessage(0);

        }
    }

    //声明Handler
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {

            if (!isCurrentPageLastPage) {//当前页有十条数据，下一页极小情况为空，要试着加载才知道具体是否为空
                if (!isRefreshingPage && isFinishLoadCurrentPage) {//以防用户在首页未加载完成后，
                    // 上拉加载第二页，或本页未加载完就又开启异步任务加载下一页，或由于误操作多次下拉到底，则易卡机。
                    isFinishLoadCurrentPage = false;
                    if (isNetworkConnected()) {
                        asyncTaskForUpDataCacheToChangeListView("http://kanxiaohua.applinzi.com/get-posts.php?page="
                                + (++loadPage), ASYNC_TASK_LOAD_MORE); //
                    } else {
                        ++loadPage;
                        postLists = getPostListsToPrepareForLoad();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getActivity(), "已加载好了更多数据", Toast.LENGTH_SHORT).show();
                        isFinishLoadCurrentPage = true;
                    }

                } else {
                    Toast.makeText(getActivity(), "骚年，已正在加载", Toast.LENGTH_SHORT).show();
                }
            } else {
                finishLoadMoreAction();
            }
        }

    };

    private void finishLoadMoreAction() {
        moreView.setVisibility(View.GONE);
        Toast.makeText(getActivity(), "木有更多数据！", Toast.LENGTH_SHORT).show();
        listView.removeFooterView(moreView); //移除底部视图
        haveMoreView=false;
    }
}


