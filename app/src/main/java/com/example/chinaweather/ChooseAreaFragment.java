package com.example.chinaweather;

import android.app.ProgressDialog;
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

import androidx.fragment.app.Fragment;

import com.example.chinaweather.db.City;
import com.example.chinaweather.db.County;
import com.example.chinaweather.db.Province;
import com.example.chinaweather.util.HttpUtil;
import com.example.chinaweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {


    private static final String TAG = "ChooseAreaFragment";

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;//适配器

    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */

    private int currentLevel;

    //初始化
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)//在准备绘制Fragment界面时调用
    {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        /*
        第一个参数：想要添加的布局
        第二个参数：想要添加到哪个布局上面（null和有值的区别  null时第一个参数中最外层的布局大小无效，有值的时候最外层的布局大小有效）
        第三个参数：是否直接添加到第二个参数布局上面（true代表layout文件填充的View会被直接添加进parent，而传入false则代表创建的View会以其他方式被添加进parent）
            */

        //初始化choose_area内的三个元素
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        //初始化ArrayAdapter数组适配器
        /*
        ArrayAdapter数组适配器用于绑定格式单一的数据，数据源可以是集合或者数组

        列表视图(ListView)以垂直的形式列出需要显示的列表项。

        实现过程：新建适配器->添加数据源到适配器->视图加载适配器
        */
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)//在Activity的onCreate执行完时会调用。
    {
        super.onActivityCreated(savedInstanceState);
        //初始化listView响应方法
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (currentLevel == LEVEL_PROVINCE) {//省级列表
                    selectedProvince = provinceList.get(position);//省级列表数据->选中省份
                    queryCities();//查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);//市级列表数据->选中城市
                    queryCounties();//查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
                }

/*
                else if(currentLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();//返回县级列表编号
                    Intent intent = new Intent(getActivity(),WeatherActivity.class);//跳转到WeatherActivity活动界面
                    intent.putExtra("weather_id",weatherId);//传入钮对值和编号
                    startActivity(intent);
                    getActivity().finish();//关闭本活动
                }
*/
                else if (currentLevel == LEVEL_COUNTY) {//区或者县级列表

                    String weatherId = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity) {//instanceof 严格来说是Java中的一个双目运算符，用来测试一个对象是否为一个类的实例

                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);//传递键对值
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

        //返回按钮
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();//返回市级
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();//
                }
            }
        });
        queryProvinces();//开始加载省级数据
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryProvinces() {

        titleText.setText("中国");//头布局的标题设置成中国
        backButton.setVisibility(View.GONE);//将返回按钮隐藏起来


        provinceList = DataSupport.findAll(Province.class);//从数据库中读取省级信息数据


        if (provinceList.size() > 0) {//读取的长度大于0
            dataList.clear();//清空dataList

            for (Province province : provinceList) {//条件
                dataList.add(province.getProvinceName());//加入省的名字
            }

            adapter.notifyDataSetChanged();//刷新数据
            listView.setSelection(0);//从第几个选择在最上面
            currentLevel = LEVEL_PROVINCE;//选中的级别

        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");//从服务器查询数据
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
     */

    private void queryCities() {

        titleText.setText(selectedProvince.getProvinceName());//显示选中城市
        backButton.setVisibility(View.VISIBLE);//组件正常显示
        //查询条件+将id转换才String类型+要查询的表
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);//查询并获取

        if (cityList.size() > 0) {
            dataList.clear();//清空
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();//更新数据
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;

        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }

    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据。
     */

    private void queryFromServer(String address, final String type) {
        showProgressDialog();//对话框
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();//服务器拿到的数据
                boolean result = false;

                //解析数据

                if ("province".equals(type)) {//省
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {//市
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {//县
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }

                //加载数据
                //数据存在时调用   直接显示

                if (result) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();//关闭进度对话框
                            if ("province".equals(type)) {
                                queryProvinces();//省
                            } else if ("city".equals(type)) {
                                queryCities();//市
                            } else if ("county".equals(type)) {
                                queryCounties();//县
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();//关闭对话框
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }

    /**
     * 显示进度对话框
     */

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());//当前正在加载fragment的Activity
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);//弹出后会点击屏幕，dialog不消失；点击物理返回键dialog消失
        }
        progressDialog.show();//启动对话框
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}