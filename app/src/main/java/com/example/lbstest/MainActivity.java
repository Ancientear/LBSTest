package com.example.lbstest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public LocationClient mLocationClient;
    private TextView positionText;


    private MapView mapView;

    private BaiduMap baiduMap;
    private boolean isFirstLocate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        /*创建一个LocationClient的实例，LocationClient的构建函数接收一个Context参数
        * 这里调用getApplicationContext()方法来获取一个全局的Context参数并传入
        * 然后调用LocationClient的registerLocationListener()方法来注册一个定位监听器
        * 当获取到位置信息的时候，就会回调这个定位监听器
        */
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());

        /*需要调用SDKInitializer的initialize()方法来进行初始化操作
        * 接收一个Context参数并传入
        * 注意初始化操作要在setContentView方法前调用不然就会出错
        * */

        SDKInitializer.initialize(getApplicationContext());

        setContentView(R.layout.activity_main);
        positionText = findViewById(R.id.position_text_view);

        //获取到Mapview实例
        mapView = (MapView)findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();

        baiduMap.setMyLocationEnabled(true);

        /*其中有四个属于危险权限
        * ACCESS_COARSE_LOCATION
        * ACCESS_FINE_LOCATION
        * READ_PHONE_STATE
        * WRITE_EXTERNAL_STORAGE
        * 这四个权限是需要进行运行权限处理的
        * ACCESS_COARSE_LOCATION和ACCESS_FINE_LOCATION属于同一个权限组
        *
        * 我们需要一次性申请三个权限
        *
        * 方法：首先创建一个空的List集合，然后依次判断这3个权限有没有被授权
        * 如果没有授权就添加到List集合中，最后将List转换成数组
        * 再调用ActivityCompat.requestPermissions()方法一次性申请
        *
        * */
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        //集合不为空则代表有需要申请的危险权限
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            requestLocation();
        }


    }


    /*requestLocation()方法中，调用LocationClient的start()开始定位
    * 定位的结果会回调到前面注册的监听器中，也就是MyLocationListener
    * */
    private void requestLocation() {
        initLocation();
        mLocationClient.start();
    }
    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(5000);
        option.setIsNeedAddress(true);
        option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        mLocationClient.setLocOption(option);

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();

        baiduMap.setMyLocationEnabled(false);
    }

    /*onRequestPermissionsResult()方法中
    * 通过一个循环将申请的每一个权限都进行了判断
    * 如果有任何一个权限被拒绝，那么就直接调用finish()方法关闭当前程序
    * 只有当所有的权限都被用户同意了，才会调用requestLocation()方法开始地理位置定位
    * */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                        }
                        finish();
                        return;
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }


    /*加入一个navigateTo()方法
    * 先是将BDLocation对象中的地址位置信息取出并封装到LatLng对象中
    * 然后调用MapStatusUpdateFactory的newLatLng()方法将LatLng对象传入
    * 接着将返回的MapStatusUpdate对象作为参数传入到BaiduMap的animateMapStatus()方法中
    * 为了让地图信息可以显示地更加丰富，这里将缩放级别设置成了16
    *
    * 在下面有个isFirstLocate变量，这个变量的作用是防止多次调用animateMapStatus()方法
    * 因为将地图移动到我们当前的位置只需要在程序第一次定位的时候调用一次就可以了
    *
    *
    *
    * */
    private void navigateTo(BDLocation bdLocation){
        if (isFirstLocate){
            LatLng ll = new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            update = MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.animateMapStatus(update);
            isFirstLocate = false;
        }

        /*添加了MyLocationData的构建逻辑
        * 将Location中包含的经度纬度分别封装到了MyLocationData.Builder当中
        * 最后把MyLocationData设置到了BaiduMap的setMyLocationData()方法中
        * 注意这段逻辑必须卸载isFirstLocate这个if语句的外面
        * 因为让地图移动到我们当前的位置只需要在第一次定位的时候执行
        * 但是设备在地图上显示的位置却应该是随着设备的移动而实时改变的
        *
        * 根据百度地图的限制，如果想要使用这个功能
        * 事先需要先调用BaiduMap的setMyLocationEnabled方法将此功能开启
        * 否则设备的位置将无法在地图上显示
        * 而在程序退出的时候，也要记住将此功能给关闭掉
        * */
        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
        locationBuilder.latitude(bdLocation.getLatitude());
        locationBuilder.longitude(bdLocation.getLongitude());
        MyLocationData locationData = locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }


    /*此处，由于registerLocationListener 过时，应该写成如下：
    * 这里通过BDLocation的getLatitude()方法获取当前位置的纬度，通过getLongitude获得经度
    * 通过getLocType()方法获取当前的定位方式
    * 最终将结果组装成一个字符串，显示到TextView上
    * */
    public class MyLocationListener extends BDAbstractLocationListener{
    //public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            /*StringBuilder currentPosition = new StringBuilder();
            currentPosition.append("纬度：").append(location.getLatitude())
                    .append("\n");
            currentPosition.append("经度：").append(location.getLongitude())
                    .append("\n");
            currentPosition.append("国家：").append(location.getCountry())
                    .append("\n");
            currentPosition.append("省：").append(location.getProvince())
                    .append("\n");
            currentPosition.append("市：").append(location.getCity())
                    .append("\n");
            currentPosition.append("区：").append(location.getDistrict())
                    .append("\n");
            currentPosition.append("街道：").append(location.getStreet())
                    .append("\n");



            currentPosition.append("定位方式：");

            if (location.getLocType() == BDLocation.TypeGpsLocation) {
                currentPosition.append("GPS");
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                currentPosition.append("网络");
            }
            positionText.setText(currentPosition);*/

            //写好navigateTo方法后，当定位到设备当前位置时候
            // 我们在onReceiveLocation()方法中直接把BDLocation对象传给navigateTo()方法
            //这样就能让地图移动到设备所在的位置了
            if (location.getLocType() == BDLocation.TypeGpsLocation || location.getLocType() == BDLocation.TypeNetWorkLocation){
                navigateTo(location);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
