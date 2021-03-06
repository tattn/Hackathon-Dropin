package com.cyberagent.courseshare;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;

/**
 * メインとなる場所を表すクラス
 * Created by tatsuya tanaka on 9/11/2014.
 */
public class Spot {
	private String name;
	private LatLng coordinates;
	private String description;
    private ArrayList<URI> imageUri;
	private ArrayList<LatLng> direction;
	private long duration;
    private double rating;
    private int priceLevel;
    private ArrayList<String> types;

	public Spot(String name, LatLng coordinates) {
		this(name, coordinates, null);
	}

	public Spot(String name, LatLng coordinates, String description) {
		this(name, coordinates, description, null);
	}

	public Spot(String name, LatLng coordinates, String description, ArrayList<URI> imageUri) {
		this.name = name;
		this.coordinates = coordinates;
		this.description = description;
		this.imageUri = imageUri;
	}

	void setDirection(ArrayList<LatLng> direction) {
		this.direction = direction;
	}

	void setDuration(long duration) {
		this.duration = duration;
	}

	public String getName() {
		return this.name;
	}

	public LatLng getCoordinates() {
		return this.coordinates;
	}

	public String getDescription() {
		return this.description;
	}

	public ArrayList<URI> getImageURI() {
		return this.imageUri;
	}

	public ArrayList<LatLng> getDirection() {
		return this.direction;
	}

	public long getDuration() {
		return this.duration;
	}

    public double getRating() {
        return this.rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(int priceLevel) {
        this.priceLevel = priceLevel;
    }

    public ArrayList<String> getTypes() {
        return types;
    }

    public void setTypes(ArrayList<String> types) {
        this.types = types;
    }
}

