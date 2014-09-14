package com.cyberagent.courseshare;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

/**
 * コース画面のアクティビティ
 */
public class CourseActivity extends FragmentActivity {

	private Map map;

	LayoutInflater layoutFactory;

	private ProgressDialog waitDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		layoutFactory = LayoutInflater.from(this);

		CoursePagerAdapter adapter = new CoursePagerAdapter(this);

		MapWrapperLayout mapView = (MapWrapperLayout)layoutFactory.inflate(R.layout.activity_cource, null);
		adapter.add(new CourseGuideSpotView(this));
		adapter.add(mapView);

		// ViewPager を生成
		ViewPager viewPager = new ViewPager(this);
		viewPager.setAdapter(adapter);

		// レイアウトにセット
		setContentView(viewPager);

		TextView btnSearch = (TextView)mapView.findViewById(R.id.btn_search);
		btnSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
				startActivity(intent);
			}
		});

		this.map = new Map(this, mapView, (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map));

		Bundle b = getIntent().getExtras();
		if (b != null) {
			String keyword = b.getString("keyword");
			if (keyword != null) {
				btnSearch.setText(keyword);
				search(keyword);
			}
		}


		ArrayList<LatLng> points = new ArrayList<LatLng>();

		Spot start = new Spot("渋谷駅", new LatLng(35.658517,139.701334), "JR東日本、東急、東京メトロの駅です。");
		Spot goal = new Spot("原宿駅", new LatLng(35.6701676,139.7026946), "原宿駅です");
		this.map.setStartAndGoal(start, goal);

		Spot spot = new Spot("青山学院大学", new LatLng(35.66147,139.709464),
				"青山学院大学の文系が集まるキャンパスです。");
		this.map.addPin(spot);


		// 中心位置を最初の場所としてカメラを移動
		LatLng center = new LatLng(35.658517, 139.701334);
		this.map.setCenter(center);
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.map.setUpMapIfNeeded();
	}

	public void search(String keyword) {
		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		this.waitDialog.setMessage("検索中");
		this.waitDialog.show();
		// call WebAPI

		this.waitDialog.dismiss();
	}


	public Map getMap() {
		return this.map;
	}

}
