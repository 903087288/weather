package com.example.tianqiyubao.util;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tianqiyubao.MainActivity;
import com.example.tianqiyubao.R;
import com.example.tianqiyubao.WeatherActivity;
import com.example.tianqiyubao.db.City;
import com.example.tianqiyubao.db.County;
import com.example.tianqiyubao.db.Province;
import com.example.tianqiyubao.map.mapActivity;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by 田保哲 on 2017/5/3.
 */

public class ChoseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String>adapter;
    private List<String>dataList=new ArrayList<>();
    private Button mMap;


     //省列表****
    private List<Province>provinceList;
    //市列表***
    private List<City>cityList;
    //县列表****
    private List<County>countyList;
    //选中的省份***
    private Province selectedProvince;
    //选中的城市***
    private City selectedCity;
    //选中的级别***
    private int currentLevel;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceSate){
        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText=(TextView) view.findViewById(R.id.title_text);
        backButton=(Button) view.findViewById(R.id.back_button);
        mMap = (Button)view.findViewById(R.id.map);
        mMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog=new AlertDialog.Builder(getActivity());
                dialog.setTitle("温馨提示");
                dialog.setMessage("是否定位到当前城市？");
                dialog.setIcon(R.drawable.location);
                dialog.setCancelable(false);
                dialog.setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i=new Intent();
                        i.setClass(getActivity(),mapActivity.class);
                        startActivity(i);
                    }
                });
           dialog.setNegativeButton("否", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                   dialog.dismiss();
               }
           });
                dialog.create().show();
            }
        });
        listView=(ListView) view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<>(getActivity(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?>parent,View view,int position,long id){
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    queryCities();
                }
                else if (currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCounties();
                }else if (currentLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }

            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTY) {
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                       queryProvinces();
                    }
                }

        });

    queryProvinces();
    }


    /*查询全国所有的省  ，优先从数据库中查询，如果没有查询到再去服务器上查询

     */

    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();
            for (Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }
        else {
            String address="http://guolin.tech/api/china";
            queryFromSever(address,"province");
        }
    }
    /**
     *
     * 查询选中省内的市，优先从数据库查询，如果没有查询到再去服务器上查
     */

    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size()>0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());

            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }
        else {
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            queryFromSever(address,"city");
        }
    }
    /**
     ***
     * 查询选中省内的县，优先从数据库查询，如果没有查询到再去服务器上查
     */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList=DataSupport.where("cityid=?",String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size()>0){
            dataList.clear();
            for (County county:countyList){
                dataList.add(county.getCountyName());

            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }
        else {
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromSever(address,"county");
        }
    }
   private void queryFromSever(String address,final String type){
       showProgressDialog();
       HttpUtil.sendOkHttpRequest(address, new Callback() {
           @Override
           public void onResponse(Call call, Response response)throws IOException {
                 String responseText=response.body().string();
                         boolean result=false;
               if("province".equals(type)){
                   result=Utility.handleProvinceResponse(responseText);
               }
               else if("city".equals(type)){
                   result=Utility.handleCityResponse(responseText,selectedProvince.getId());

               }
               else if("county".equals(type)){
                   result=Utility.handleCountyResponse(responseText,selectedCity.getId());
               }
               if (result){
                   getActivity().runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           closeProgressDialog();
                           if("province".equals(type)) {
                               queryProvinces();
                           }
                           else if("city".equals(type)){
                           queryCities();
                           }
                           else if("county".equals(type)){
                               queryCounties();
                           }

                       }
                   });
               }
           }
           public void onFailure(Call call,IOException e){
               getActivity().runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       closeProgressDialog();
                       Toast.makeText(getActivity(),"加载失败",Toast.LENGTH_LONG).show();
                   }
               });
           }
       });
   }
   private void showProgressDialog(){
       if(progressDialog==null){
           progressDialog=new ProgressDialog(getActivity());
           progressDialog.setMessage("正在加载...");
           progressDialog.setCanceledOnTouchOutside(false);
       }
       progressDialog.show();
   }
/*
* 关闭进度对话框
*
* */
private void closeProgressDialog(){
    if(progressDialog!=null){
        progressDialog.dismiss();
    }
}
}
