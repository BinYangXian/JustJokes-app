<?php

require_once 'wp-config.php';
//打印所有参数方法：
//$str =$_GET;
////echo $str;//错误
//print_r($_GET);
//接受get方式的页面请求参数 page=多少。
$page = $_REQUEST['page'];
$conn = mysqli_connect(DB_HOST, DB_USER, DB_PASSWORD);//@符号可以忽略系统警告的显示在页面
if ($conn) {
    mysqli_select_db($conn, DB_NAME);//参数二为数据库的标识

    $perpagenum = 10;
    $result = mysqli_query($conn, "SELECT COUNT(*) FROM wp_posts  WHERE post_status='publish'");
    if ($result) {
        $result_arr = mysqli_fetch_array($result);
        $Total = $result_arr[0];
        $totalpage = ceil($Total / $perpagenum);//上舍，取整
    }
    if ($page <=$totalpage) {  //当还未到达尾页
        if (!isset($_GET['page']) || !intval($_GET['page']) || $_GET['page'] > $totalpage)//page可能的四种状态
        {
            $page = 1;
        } else {
            $page = $_GET['page'];//如果不满足以上四种情况，则page的值为$_GET['page']
        }
        $startnum = ($page - 1) * $perpagenum;//开始条数
        $result = mysqli_query($conn, "SELECT post_content,post_date,post_title,ID FROM wp_posts WHERE post_status='publish' ORDER BY ID DESC limit $startnum,$perpagenum  ");
        $data_count = mysqli_num_rows($result);

        $data_count = mysqli_num_rows($result);
        $postsArray = array();
        $totalPostsArray = array();
        for ($i = 0; $i < $data_count; $i++) {

            array_push($postsArray, mysqli_fetch_array($result));
        }
        $totalPostsArray["paging"] = $postsArray;
        echo(json_encode($totalPostsArray));
    }

} else {
    echo 'connect failed';
}
?>
