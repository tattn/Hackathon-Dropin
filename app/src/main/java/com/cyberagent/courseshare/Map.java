package com.cyberagent.courseshare;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.internal.d;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import org.apache.http.Header;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.AsynchronousCloseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TooManyListenersException;

/**
 * Created by tatsuya tanaka on 9/13/2014.
 */
public class Map {
	private CourseActivity owner;
	MapWrapperLayout mapView;
	private SupportMapFragment mapFragment;
	private GoogleMap map;

	private MapAPIManager apiManager;
	Handler  handler;

	private Pin startPin;
	private Pin goalPin;
	private ArrayList<Pin> pins;	// 寄り道候補のピン
	private ArrayList<Pin> waypoints;	// 確定した寄り道場所

	private Polyline previewLine;
	private Polyline line;

	public Map(CourseActivity owner, MapWrapperLayout mapView, SupportMapFragment mapFragment, MapAPIManager apiManager) {
		this.owner = owner;
		this.mapView = mapView;
		this.mapFragment = mapFragment;
		this.apiManager = apiManager;
		this.pins = new ArrayList<Pin>();
		this.waypoints = new ArrayList<Pin>();
		this.handler = new Handler();

		mapFragment.setRetainInstance(true);

		setUpMapIfNeeded();

		// MapWrapperLayout initialization
		// 39 - default marker height
		// 20 - offset between the default InfoWindow bottom edge and it's content bottom edge
		mapView.init(map, getPixelsFromDp(owner, 39 + 20));
	}

	public void setUpMapIfNeeded() {
		if (this.map == null) {
			this.map = this.mapFragment.getMap();
			if (this.map != null) {
				setUpMap();
			}
		}
	}

	private void setUpMap() {
		this.map.setInfoWindowAdapter(new CustomInfoAdapter());
		this.map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
			@Override
			public void onMapClick(LatLng latLng) {
				removePreviewRoute();
			}
		});
	}

	public void setStartAndGoal(Spot start, Spot goal) {
		this.startPin = addPin(start);
		this.goalPin = addPin(goal);
		setRoute();
	}

	public void setStart(Spot start) {
		this.startPin = addPin(start);
		if (this.goalPin != null)
			setRoute();
	}

	public void setGoal(Spot goal) {
		this.goalPin = addPin(goal);
		if (this.startPin != null) {
			setRoute();
		}
	}

	/**
	 * ピンを追加します。
	 * @param spot 追加するスポット
	 * @return 追加したピン
	 */
	public Pin addPin(Spot spot) {
		BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
		MarkerOptions options = new MarkerOptions()
				.position(spot.getCoordinates())
				.title(spot.getName())
				.snippet(spot.getDescription())
				.icon(icon);
		Pin pin = new Pin(this.map.addMarker(options), spot);
		this.pins.add(pin);
		return pin;
	}

	/**
	 * すべてのピンと道を削除します。
	 */
	public void removeAllPins() {
		Iterator<Pin> i = this.pins.iterator();
		while(i.hasNext()){
			Pin pin = i.next();
			if (!this.waypoints.contains(pin) && !pin.equals(this.startPin) && !pin.equals(this.goalPin)) {
				pin.marker.remove();
				i.remove();
			}
		}
	}

	/**
	 * 道を設定します。
	 * @param pin 設定するピン
	 */
	public void setPreviewRoute(final Pin pin) {
		removePreviewRoute();

		LatLng start = getPrevPin().spot.getCoordinates();
		LatLng goal;
		if (this.goalPin == null) {
			goal = pin.spot.getCoordinates();
		} else {
			goal = this.goalPin.spot.getCoordinates();
		}
		ArrayList<LatLng> wp = new ArrayList<LatLng>();
		if (this.goalPin != null) {
			for (Pin w : this.waypoints)
				wp.add(w.spot.getCoordinates());
			wp.add(pin.spot.getCoordinates());
		}
		this.apiManager.routingPlaces(start, goal, wp, new OnEndDirectionsRequestListener() {
			@Override
			public void onEndDirectionListener(ArrayList<LatLng> latLngs, HashMap<String, Object> data) {
				if (previewLine != null) return; // 非同期で連続実行されるのを防止
				PolylineOptions lineOptions = new PolylineOptions();
				lineOptions.addAll(latLngs);
				lineOptions.width(11);
				lineOptions.color(0x550000ff);
				previewLine = map.addPolyline(lineOptions);
			}
		});
	}

	abstract class MapTask implements Runnable {
		Pin pin;
		MapTask(Pin p) { pin = p; }

		@Override
		public abstract void run();
	}

	public void setRoute() {
		removeRoute();

		LatLng start = this.startPin.spot.getCoordinates();
		LatLng goal = this.goalPin.spot.getCoordinates();
		ArrayList<LatLng> wp = new ArrayList<LatLng>();
		for (Pin pin : this.waypoints)
			wp.add(pin.spot.getCoordinates());

		this.apiManager.routingPlaces(start, goal, wp, new OnEndDirectionsRequestListener() {
			@Override
			public void onEndDirectionListener(ArrayList<LatLng> latLngs, HashMap<String, Object> data) {
				if (line != null) return; // 非同期で連続実行されるのを防止
				removePreviewRoute();
				PolylineOptions lineOptions = new PolylineOptions();
				lineOptions.addAll(latLngs);
				lineOptions.width(11);
				lineOptions.color(0x5500ff00);
				line = map.addPolyline(lineOptions);
			}
		});

		/*(new Thread(new MapTask(p) {
			@Override
			public void run() {
				while (previewLine == null) {
					try {
						Thread.sleep(500);
					} catch ( InterruptedException e ) {
						e.printStackTrace();
					}
				}

				handler.post(new MapTask(this.pin) {
					@Override
					public void run() {
						previewLine.setColor(0x5500ff00);
						line = previewLine;
						previewLine = null;
					}
				});
			}
		})).start();*/

	}

	/**
	 * 道を削除します。
	 */
	public void removePreviewRoute() {
		if (previewLine != null) {
			previewLine.remove();
			previewLine = null;
		}
	}

	public void removeRoute() {
		if (line != null) {
			line.remove();
			line = null;
		}
	}

	/**
	 * 経由地を追加します。
	 * @param pin 追加するピン
	 */
	public void addWeyPoint(Pin pin) {
		BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
		pin.marker.setIcon(icon);

		this.waypoints.add(pin);

		removeRoute();
		setRoute();

		owner.resetSpotList();
	}

	/**
	 * 経由地から取り除きます。
	 * @param pin 取り除くピン
	 */
	public void removeWayPoint(Pin pin) {

		removeRoute();

		pin.marker.remove();
		this.pins.remove(pin);
		this.waypoints.remove(pin);

		setRoute();

		owner.resetSpotList();
	}

	/**
	 * 経由地を入れ替えます。
	 * @param pin1
	 * @param pin2
	 */
	public void swapWayPoint(Pin pin1, Pin pin2) {

	}

	/**
	 * カメラの中心位置を設定します。
	 * @param center 中心位置の緯度経度
	 */
	public void setCenter(LatLng center) {
		CameraPosition.Builder builder = new CameraPosition.Builder()
				.bearing(0)			// カメラの向き
				.tilt(0)			// カメラの傾き
				.zoom(16)
				.target(center);
		//this.map.moveCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
		this.map.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()));
	}

	public Pin getStartPin() {
		return this.startPin;
	}

	public Pin getGoalPin() {
		return this.goalPin;
	}

	public Pin getPrevPin() {
		if (this.waypoints.isEmpty())
			return this.startPin;
		else
			return waypoints.get(waypoints.size() - 1);
	}

	public Pin getNewPin() {
		if (this.waypoints.isEmpty())
			return this.goalPin;
		else
			return waypoints.get(waypoints.size() - 1);
	}

	public ArrayList<Pin> getWaypoints() {
		return this.waypoints;
	}

	private Pin getPinFromMarker(Marker marker) {
		for (Pin p : pins)
			if (marker.equals(p.marker)) {
				return p;
			}
		return null;
	}

	private Polyline addLines(ArrayList<LatLng> points, int color) {
		PolylineOptions lineOptions = new PolylineOptions();
		lineOptions.addAll(points);
		lineOptions.width(11);
		lineOptions.color(color);
		return this.map.addPolyline(lineOptions);
	}

	private static int getPixelsFromDp(Context context, float dp) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int)(dp * scale + 0.5f);
	}

	public void confirmPin(Pin pin) {
		switch (owner.getTaskType()) {
			case START:
				setStart(pin.spot);
				break;
			case GOAL:
				setGoal(pin.spot);
				break;
			case WAYPOINT:
				addWeyPoint(pin);
				break;
		}
		removeAllPins();
		removePreviewRoute();

		switch (owner.getTaskType()) {
			case START:
			case GOAL:
				owner.doNextTask();
		}
	}

	/**
	 * マーカーをタップした際に表示するウィンドウの処理
	 */
	private class CustomInfoAdapter implements GoogleMap.InfoWindowAdapter {

		private ViewGroup window;
		private TextView title;
		private TextView snippet;
		private ImageView icon;
		private Button btnDropIn;

		private OnInfoWindowElemTouchListener infoBtnAddListener;
		private OnInfoWindowElemTouchListener infoBtnRemoveListener;

		public CustomInfoAdapter() {
			this.window = (ViewGroup)owner.getLayoutInflater().inflate(R.layout.course_custom_info_window, null);
			this.title = (TextView) window.findViewById(R.id.title);
			this.snippet = (TextView) window.findViewById(R.id.snippet);
			this.icon = (ImageView) window.findViewById(R.id.icon);
			this.btnDropIn = (Button) window.findViewById(R.id.btn_drop_in);
			icon.setMaxWidth(200);
			icon.setMaxHeight(200);

			// 経由地に追加用リスナー
			this.infoBtnAddListener = new OnInfoWindowElemTouchListener(this.btnDropIn,
					owner.getResources().getDrawable(R.drawable.btn_ok),
					owner.getResources().getDrawable(R.drawable.btn_ok))
			{
				@Override
				protected void onClickConfirmed(View v, Marker marker) {
					confirmPin(getPinFromMarker(marker));
				}
			};

			// 経由地から削除用のリスナー
			this.infoBtnRemoveListener = new OnInfoWindowElemTouchListener(this.btnDropIn,
					owner.getResources().getDrawable(R.drawable.btn_ok),
					owner.getResources().getDrawable(R.drawable.btn_ok))
			{
				@Override
				protected void onClickConfirmed(View v, Marker marker) {
					removeWayPoint(getPinFromMarker(marker));
				}
			};
		}

		@Override
		public View getInfoWindow(Marker marker) {
            Pin pin = getPinFromMarker(marker);
			this.title.setText(marker.getTitle());
			this.snippet.setText(marker.getSnippet());
			//icon.setImageResource(imgID);

			mapView.setMarkerWithInfoWindow(marker, this.window);
            Spot spot = pin.spot;
            final ArrayList<URI> imgURIs = spot.getImageURI();
            if (imgURIs != null) {
                URL url = null;
                InputStream inputStream;
                AsyncHttpClient client = new AsyncHttpClient();
                try {
                    Log.v("INPUT_TEST", "error 1");
                    url = imgURIs.get(0).toURL();
                    Log.v("INPUT_TEST", "error 2");
                    final ImageView imgView = this.icon;
                    client.get(url.toString(), new BinaryHttpResponseHandler(){
                        @Override
                        public void onSuccess(byte[] binaryData) {
                            super.onSuccess(binaryData);
                            Log.v("INPUT_TEST", "IMG request sucsess");
                            Bitmap bmp = BitmapFactory.decodeByteArray(binaryData, 0, binaryData.length);
                            imgView.setImageBitmap(bmp);
                        }
                    });
                    /*
                    inputStream = url.openStream();
                    Log.v("INPUT_TEST", "error 3");
                    Drawable drawable = Drawable.createFromStream(inputStream, "");
                    Log.v("INPUT_TEST", "error 4");

                    this.icon.setImageDrawable(drawable);
                    */
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


			if (!waypoints.contains(pin) && !pin.equals(startPin) && !pin.equals(goalPin)) {
				this.infoBtnAddListener.setMarker(marker);
				this.btnDropIn.setOnTouchListener(infoBtnAddListener);
				switch (owner.getTaskType()) {
					case START:
						this.btnDropIn.setText("現在地に設定");
						break;
					case GOAL:
						this.btnDropIn.setText("目的地に設定");
						setPreviewRoute(pin);
						break;
					case WAYPOINT:
						this.btnDropIn.setText("寄り道する");
						setPreviewRoute(pin);
						break;
				}
				this.btnDropIn.setEnabled(true);

			} else if (pin.equals(startPin) || pin.equals(goalPin)) {
				this.btnDropIn.setEnabled(false);
			} else {
				this.infoBtnRemoveListener.setMarker(marker);
				this.btnDropIn.setOnTouchListener(infoBtnRemoveListener);
				this.btnDropIn.setText("取り消し");
				this.btnDropIn.setEnabled(true);
			}

			return this.window; // ここでViewを返すとオリジナルの吹き出しが作れるっぽい
		}

		@Override
		public View getInfoContents(Marker marker) {
			// getInfoWindow()の戻り値がnullの時だけ呼ばれるっぽい
			return null;
		}

	}

	class Pin {
		public Spot spot;
		public Marker marker;

		Pin(Marker marker, Spot spot) {
			this.marker = marker;
			this.spot = spot;
		}
	}

}
