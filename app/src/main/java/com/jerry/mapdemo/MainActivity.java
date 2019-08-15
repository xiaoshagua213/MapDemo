package com.jerry.mapdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.baidu.mapapi.utils.DistanceUtil;

public class MainActivity extends AppCompatActivity {

    private MyLocationListener myListener = new MyLocationListener();
    //BDAbstractLocationListener为7.2版本新增的Abstract类型的监听接口
    //原有BDLocationListener接口暂时同步保留。具体介绍请参考后文第四步的说明
    public LocationClient mLocationClient = null;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMap();
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLocationClient.start();
            }
        });

    }

    private void initMap() {
        //声明LocationClient类
        mLocationClient = new LocationClient(getApplicationContext());
        //注册监听函数
        mLocationClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();

        //可选，设置定位模式，默认高精度
        //LocationMode.Hight_Accuracy：高精度；
        //LocationMode. Battery_Saving：低功耗；
        //LocationMode. Device_Sensors：仅使用设备；
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        //可选，设置返回经纬度坐标类型，默认GCJ02
        //GCJ02：国测局坐标；
        //BD09ll：百度经纬度坐标；
        //BD09：百度墨卡托坐标；
        //海外地区定位，无需设置坐标类型，统一返回WGS84类型坐标
        option.setCoorType("bd09ll");

        //可选，设置发起定位请求的间隔，int类型，单位ms
        //如果设置为0，则代表单次定位，即仅定位一次，默认为0
        //如果设置非0，需设置1000ms以上才有效
        option.setScanSpan(0);

        //可选，设置是否使用gps，默认false
        //使用高精度和仅用设备两种定位模式的，参数必须设置为true
        option.setOpenGps(true);

        //可选，设置是否当GPS有效时按照1S/1次频率输出GPS结果，默认false
        option.setLocationNotify(false);

        //可选，定位SDK内部是一个service，并放到了独立进程。
        //设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)
        option.setIgnoreKillProcess(false);

        //可选，设置是否收集Crash信息，默认收集，即参数为false
        option.SetIgnoreCacheException(false);

        //可选，V7.2版本新增能力
        //如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位
        option.setWifiCacheTimeOut(5 * 60 * 1000);

        //可选，设置是否需要过滤GPS仿真结果，默认需要，即参数为false
        option.setEnableSimulateGps(false);

        option.setIsNeedLocationPoiList(true);

        //mLocationClient为第二步初始化过的LocationClient对象
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        //更多LocationClientOption的配置，请参照类参考中LocationClientOption类的详细说明
        mLocationClient.setLocOption(option);
    }
}


class MyLocationListener extends BDAbstractLocationListener {

    private static final String TAG = "MyLocationListener";
    private SuggestionSearch mSuggestionSearch;

    @Override
    public void onReceiveLocation(BDLocation location) {
        //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
        //以下只列举部分获取经纬度相关（常用）的结果信息
        //更多结果信息获取说明，请参照类参考中BDLocation类中的说明

        double latitude = location.getLatitude();    //获取纬度信息
        double longitude = location.getLongitude();    //获取经度信息
        float radius = location.getRadius();    //获取定位精度，默认值为0.0f

        Log.i(TAG, "latitude: " + latitude);
        Log.i(TAG, "longitude: " + longitude);
        Log.i(TAG, "radius: " + radius);

        //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准
        String coorType = location.getCoorType();
        Log.i(TAG, "coorType: " + coorType);

        //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明
        int errorCode = location.getLocType();
        Log.i(TAG, "errorCode: " + errorCode);
        nearbyPoiSearch(latitude, longitude);
    }

    public void nearbyPoiSearch(double latitude, double longitude) {
        final LatLng latLng = new LatLng(latitude, longitude);
        //创建poi检索实例
        PoiSearch poiSearch = PoiSearch.newInstance();
        //创建poi监听者
        OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult result) {
                Log.i(TAG, "nearbyPoiSearch: " + result.getTotalPoiNum());
                //获取POI检索结果
                if (result.getAllPoi().size() > 0) {
                    for (int i = 0; i < result.getAllPoi().size(); i++) {
                        Log.i(TAG, "nearbyPoiSearch: " + result.getAllPoi().get(i).name);
                        double distance = DistanceUtil.getDistance(latLng, result.getAllPoi().get(i).location);
                        Log.i(TAG, "distance: " + distance);
                        String telephone = result.getAllPoi().get(i).getPhoneNum();
                        Log.i(TAG, "telephone: " + telephone);

                    }

                }

            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

            }

            @Override
            public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

            }
        };
        //设置poi监听者该方法要先于检索方法searchNearby(PoiNearbySearchOption)前调用，否则会在某些场景出现拿不到回调结果的情况
        poiSearch.setOnGetPoiSearchResultListener(poiListener);
        //设置请求参数
        PoiNearbySearchOption nearbySearchOption = new PoiNearbySearchOption()
                .keyword("餐厅")//检索关键字
                .location(latLng)//检索位置
                .pageNum(0)//分页编号，默认是0页
                .pageCapacity(20)//设置每页容量，默认10条
                .radius(10000);//附近检索半径
        //发起请求
        poiSearch.searchNearby(nearbySearchOption);
        //释放检索对象
        poiSearch.destroy();
    }

    private void setSuggestionSearch(String keyWord, String city) {
        //创建在线建议查询实例
        try {
            mSuggestionSearch = SuggestionSearch.newInstance();

            OnGetSuggestionResultListener listener = new OnGetSuggestionResultListener() {
                @Override
                public void onGetSuggestionResult(SuggestionResult res) {
                    Log.i(TAG, "onGetSuggestionResult: " + res.describeContents());
                    if (res == null || res.getAllSuggestions() == null) {
                        return;
                    }

                    for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
                        if (info.key != null && info.district != null && info.pt != null) {
                            Log.d(TAG, "onGetSuggestionResult: key" + info.key); //关键词
                            Log.d(TAG, "onGetSuggestionResult: district" + info.district); //所在区域
                            Log.d("", "onGetSuggestionResult: pt" + info.pt.latitude); //经纬度

                        }
                    }

                }
            };

            //设置在线建议查询监听
            mSuggestionSearch.setOnGetSuggestionResultListener(listener);

            //发起在线建议查询
            mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                    .keyword(keyWord)
                    .city(city));
        } catch (Exception e) {
            Log.d(TAG, "setSuggestionSearch() e = " + e);

        }
    }
}
