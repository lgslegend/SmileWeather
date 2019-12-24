package com.smile.weather

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.widget.ViewPager2
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.smile.baselib.utils.L
import com.smile.weather.adapter.DetailFragmentAdapter
import com.smile.weather.adapter.DetailFragmentAdapter2
import com.smile.weather.base.BaseActivity
import com.smile.weather.config.Config
import com.smile.weather.databinding.ActivityMainBinding
import com.smile.weather.db.AppDataBase
import com.smile.weather.db.CityDao
import com.smile.weather.db.City
import com.smile.weather.entity.DetailIndex
import com.smile.weather.entity.RefreshEntity
import com.smile.weather.location.LocationManageActivity
import com.smile.weather.location.SearchActivity
import com.smile.weather.sing.SingLiveData
import com.smile.weather.utils.PermissionUtils
import com.smile.weather.vm.MainViewModel

class MainActivity : BaseActivity() {


    private lateinit var mViewModel:MainViewModel
    private lateinit var mLocationClient: LocationClient
    private lateinit var mBinding:ActivityMainBinding
    private lateinit var mListCity:List<City>

    private var mCityNameArray= arrayOf<String?>()
    private var mMapFragment= mutableMapOf<String, DetailFragment>()
    private var mFragments= arrayListOf<DetailFragment>()
    private var mIsAddCity=false

    private val mAdapter:DetailFragmentAdapter by lazy {
        DetailFragmentAdapter(this , mFragments)
    }
    private val mViewPager:ViewPager2 by lazy {
        mBinding.mainContentPage
    }
    private val mDao: CityDao by lazy {
        AppDataBase.instance.getCityDao()
    }
    companion object{
        const val KEY_IS_ADD="key_is_add"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding=DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.handler=MainHandler()

        mViewModel=ViewModelProviders.of(this)[MainViewModel::class.java]

        PermissionUtils.location(this) {
            L.e("获取定位权限")
            mLocationClient=LocationClient(this)
            var option=LocationClientOption()
            option.isOpenGps = true // 打开gps
            option.setCoorType("bd09ll") // 设置坐标类型
            option.setIsNeedAddress(true)

            //设置locationClientOption
            mLocationClient.locOption = option

            //注册LocationListener监听器
            var locationListener=MyLocationListener()
            mLocationClient.registerLocationListener(locationListener)
            mLocationClient.start()

        }
        mViewPager.adapter =mAdapter

        val list=mDao.getAll()
        list.observe(this,Observer<List<City>>{

                data->Log.e("dandy"," main size "+data.size)
                mListCity=data
                initData()
        })

    }

    override fun initData() {
        super.initData()
        mFragments= arrayListOf()
        mCityNameArray=arrayOfNulls(mListCity.size)
      //  var refreshList= arrayListOf<RefreshEntity>()

        for ((i, city) in mListCity.withIndex()){
            if (mMapFragment[city.name] == null){
                var fragment = DetailFragment.newInstance(city.id!!)
                mFragments.add(fragment)
            }
            mCityNameArray[i]=city.name!!

        }
        if (mFragments.isEmpty()){
            mFragments.add(DetailFragment.newInstance(0))
        }

        mAdapter.setData(mFragments)
        if (mListCity.size==1){
            mViewModel.setRefresh(mListCity[0].id!!)

        }

        if (mIsAddCity){
            mViewPager.setCurrentItem(mAdapter.itemCount-1,true)
            mIsAddCity=false
        }
        mBinding.citySize=mListCity.size

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //标记为添加新城市了
        if (intent!!.getBooleanExtra(KEY_IS_ADD, false)){
            mIsAddCity=true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationClient.stop()
    }

    override fun initView() {
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtils.onRequestPermissionsResult(requestCode, permissions,grantResults)
    }

    inner class MyLocationListener: BDAbstractLocationListener(){
        override fun onReceiveLocation(p0: BDLocation?) {

            if (p0==null){
                return
            }
            if (p0.addrStr!=null){
                //判断本地有没有数据，有数据就判断城市是否改变
                val city1=mDao.getLocalCity()
                if (city1!=null){
                    if (p0.address.district!=city1.name){
                        val city=City(1,p0.address.district, p0.address.city,1,"")
                        mDao.insertCity(city)
                    }
                }else{
                    val city=City(1,p0.address.district, p0.address.city,1,"")
                    mDao.insertCity(city)
                }


            }

        }

    }

    open fun getDetailIndex(name:String):DetailIndex{
        var lastIndex=mCityNameArray.lastIndex
        var index=mCityNameArray.indexOf(name)
        if (mCityNameArray.size==1){
            return DetailIndex.NONE
        }
        return when (index) {
            lastIndex -> {
                DetailIndex.RIGHT
            }
            0 -> {
                DetailIndex.LEFT
            }
            else -> DetailIndex.MIDDLE
        }

    }

    inner class MainHandler{
        fun locationClick(view: View){
            L.e("点击了")
            startActivity(Intent(this@MainActivity, LocationManageActivity::class.java))

        }


        fun searchCity(view: View){

            var intent=Intent(this@MainActivity, SearchActivity::class.java)
            if (mListCity.isEmpty()){
                intent.putExtra(SearchActivity.KEY_LAST_ID, 0)

            }else{
                intent.putExtra(SearchActivity.KEY_LAST_ID, mListCity.last().id)

            }
            startActivity(intent)
        }
    }

}